import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../cart_providers.dart';
import 'promo_code_field.dart';

class OrderSummaryCard extends ConsumerStatefulWidget {
  final double subtotal;
  final String? partnerId;
  final double serviceFee;
  final double discount;
  final String? promoCode;
  final ValueChanged<double?> onDeliveryFeeChanged;
  final void Function(String? promoCode, double discount) onPromoChanged;

  const OrderSummaryCard({
    super.key,
    required this.subtotal,
    required this.partnerId,
    required this.serviceFee,
    required this.discount,
    required this.promoCode,
    required this.onDeliveryFeeChanged,
    required this.onPromoChanged,
  });

  @override
  ConsumerState<OrderSummaryCard> createState() => _OrderSummaryCardState();
}

class _OrderSummaryCardState extends ConsumerState<OrderSummaryCard> {
  late final TextEditingController _promoController;
  bool _isApplyingPromo = false;
  bool _isLoadingDeliveryFee = false;
  double? _deliveryFee;
  double? _freeDeliveryThreshold;

  @override
  void initState() {
    super.initState();
    _promoController = TextEditingController(text: widget.promoCode ?? '');
    _loadDeliveryFee();
  }

  @override
  void didUpdateWidget(covariant OrderSummaryCard oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.partnerId != widget.partnerId) {
      _loadDeliveryFee();
    }

    if (oldWidget.promoCode != widget.promoCode) {
      _promoController.text = widget.promoCode ?? '';
    }

    if (oldWidget.subtotal != widget.subtotal) {
      widget.onDeliveryFeeChanged(_effectiveDeliveryFee());
    }
  }

  @override
  void dispose() {
    _promoController.dispose();
    super.dispose();
  }

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DA';
    return '${value.toStringAsFixed(2)} DA';
  }

  double? _effectiveDeliveryFee() {
    if (_deliveryFee == null) return null;
    if ((_freeDeliveryThreshold ?? 0) > 0 &&
        widget.subtotal >= (_freeDeliveryThreshold ?? 0)) {
      return 0;
    }
    return _deliveryFee;
  }

  Future<void> _loadDeliveryFee() async {
    final partnerId = widget.partnerId;
    if (partnerId == null || partnerId.isEmpty) {
      setState(() {
        _deliveryFee = null;
        _freeDeliveryThreshold = null;
      });
      widget.onDeliveryFeeChanged(null);
      return;
    }

    setState(() => _isLoadingDeliveryFee = true);
    try {
      final info = await ref
          .read(cartRepositoryProvider)
          .fetchDeliveryFee(partnerId);
      if (!mounted) return;

      setState(() {
        _deliveryFee = info.deliveryFee;
        _freeDeliveryThreshold = info.freeDeliveryThreshold;
        _isLoadingDeliveryFee = false;
      });
      widget.onDeliveryFeeChanged(_effectiveDeliveryFee());
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _deliveryFee = null;
        _freeDeliveryThreshold = null;
        _isLoadingDeliveryFee = false;
      });
      widget.onDeliveryFeeChanged(null);

      // Silent retry on network failure.
      unawaited(
        Future<void>.delayed(const Duration(seconds: 3), () {
          if (!mounted || widget.partnerId != partnerId) return;
          _loadDeliveryFee();
        }),
      );
    }
  }

  Future<void> _applyPromo() async {
    final code = _promoController.text.trim();
    final partnerId = widget.partnerId;

    if (partnerId == null || partnerId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Aucun partenaire sélectionné pour appliquer le code.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (code.isEmpty) {
      widget.onPromoChanged(null, 0);
      return;
    }

    setState(() => _isApplyingPromo = true);
    try {
      final result = await ref
          .read(cartRepositoryProvider)
          .validatePromo(
            code: code,
            partnerId: partnerId,
            subtotal: widget.subtotal,
          );

      if (!mounted) return;

      if (result.isValid) {
        widget.onPromoChanged(code, result.discount);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result.message),
            backgroundColor: Colors.green,
          ),
        );
      } else {
        widget.onPromoChanged(null, 0);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message), backgroundColor: Colors.red),
        );
      }
    } catch (e) {
      if (!mounted) return;
      widget.onPromoChanged(null, 0);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Échec validation promo: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isApplyingPromo = false);
      }
    }
  }

  Widget _summaryRow(
    String label,
    String value, {
    Color? color,
    FontWeight? weight,
    double? fontSize,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                color: color,
                fontWeight: weight,
                fontSize: fontSize,
              ),
            ),
          ),
          Text(
            value,
            style: TextStyle(
              color: color,
              fontWeight: weight,
              fontSize: fontSize,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final deliveryFee = _effectiveDeliveryFee();
    final total =
        (widget.subtotal +
                (deliveryFee ?? 0) +
                widget.serviceFee -
                widget.discount)
            .clamp(0, double.infinity)
            .toDouble();

    return Card(
      margin: const EdgeInsets.only(top: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Résumé de commande',
              style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            const SizedBox(height: 10),
            _summaryRow('Sous-total', _money(widget.subtotal)),
            _summaryRow(
              'Frais de livraison',
              _isLoadingDeliveryFee
                  ? '...'
                  : (deliveryFee == null ? '— DA' : _money(deliveryFee)),
            ),
            _summaryRow('Frais de service', _money(widget.serviceFee)),
            if (widget.discount > 0)
              _summaryRow(
                'Réduction',
                '- ${_money(widget.discount)}',
                color: Colors.green,
                weight: FontWeight.w700,
              ),
            const Divider(height: 20),
            _summaryRow(
              'Total',
              _money(total),
              weight: FontWeight.w800,
              fontSize: 18,
            ),
            const SizedBox(height: 12),
            PromoCodeField(
              controller: _promoController,
              isLoading: _isApplyingPromo,
              onApply: _applyPromo,
            ),
          ],
        ),
      ),
    );
  }
}
