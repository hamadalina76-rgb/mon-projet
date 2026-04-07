import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/utils/delivery_zone_utils.dart';
import '../../location/presentation/providers/location_provider.dart';
import '../../profile/data/models/address_model.dart';
import '../../profile/presentation/providers/address_provider.dart';
import '../cart_providers.dart';
import 'widgets/cart_item_tile.dart';
import 'widgets/minimum_order_banner.dart';
import 'widgets/order_summary_card.dart';
import 'widgets/partner_closed_banner.dart';
import 'widgets/partner_header.dart';

class CartScreen extends ConsumerStatefulWidget {
  const CartScreen({super.key});

  @override
  ConsumerState<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends ConsumerState<CartScreen> {
  static const String _paymentMethodCash = 'CASH';

  String? _loadedPartnerId;
  PartnerCartInfo? _partnerInfo;
  String? _promoCode;
  String? _selectedPaymentMethod;
  double _promoDiscount = 0;
  bool _isPlacingOrder = false;

  ({double lat, double lng}) _resolveCoordinates() {
    final selectedLoc = ref.read(locationNotifierProvider).location;
    if (selectedLoc != null) {
      return (lat: selectedLoc.latitude, lng: selectedLoc.longitude);
    }

    final addresses = ref.read(addressNotifierProvider).valueOrNull ?? [];
    AddressModel? target;

    for (final a in addresses) {
      if (a.isDefault && a.latitude != null && a.longitude != null) {
        target = a;
        break;
      }
    }

    if (target == null) {
      for (final a in addresses) {
        if (a.latitude != null && a.longitude != null) {
          target = a;
          break;
        }
      }
    }

    if (target != null) {
      return (lat: target.latitude!, lng: target.longitude!);
    }

    return (lat: 0.0, lng: 0.0);
  }

  double _computeServiceFee(double subtotal) {
    final info = _partnerInfo;
    if (info == null) return 0;

    final base = info.serviceFeeValue;
    if (base <= 0) return 0;

    if (info.serviceFeeIsPercentage) {
      final calculated = subtotal * (base / 100);
      return calculated.clamp(0, double.infinity).toDouble();
    }

    return base;
  }

  bool _isCurrentPartnerOutOfZone({double? userLat, double? userLng}) {
    final info = _partnerInfo;
    if (info == null) return false;

    return isOutsideDeliveryZone(
      deliveryRadius: info.deliveryRadius,
      distanceKm: info.distanceKm,
      userLat: userLat,
      userLng: userLng,
      partnerLat: info.latitude,
      partnerLng: info.longitude,
    );
  }

  void _ensurePartnerLoaded(String? partnerId) {
    if (_loadedPartnerId == partnerId) return;

    _loadedPartnerId = partnerId;
    _partnerInfo = null;
    _promoCode = null;
    _selectedPaymentMethod = null;
    _promoDiscount = 0;

    if (partnerId == null || partnerId.isEmpty) {
      if (mounted) setState(() {});
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final info = await ref
          .read(cartRepositoryProvider)
          .fetchPartnerInfo(partnerId);
      if (!mounted) return;
      setState(() {
        _partnerInfo = info;
      });
    });
  }

  Future<void> _confirmClearCart() async {
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Vider le panier ?'),
          content: const Text('Êtes-vous sûr de vouloir vider votre panier ?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Annuler'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('Vider'),
            ),
          ],
        );
      },
    );

    if (accepted == true) {
      await ref.read(cartNotifierProvider.notifier).clearCart();
    }
  }

  Future<void> _placeOrder() async {
    final selectedPaymentMethod = _selectedPaymentMethod;
    if (selectedPaymentMethod == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez choisir un mode de paiement.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;
    final outOfZone = _isCurrentPartnerOutOfZone(
      userLat: zoneUserLat,
      userLng: zoneUserLng,
    );
    if (outOfZone) {
      if (!mounted) return;
      final l10n = AppLocalizations.of(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('partner_details_out_of_zone')),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() => _isPlacingOrder = true);
    try {
      await ref
          .read(cartNotifierProvider.notifier)
          .placeOrder(
            promoCode: _promoCode,
            paymentMethod: selectedPaymentMethod,
          );

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Commande validée avec succès.'),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Échec de la commande: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isPlacingOrder = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    ref.watch(addressNotifierProvider);
    ref.watch(locationNotifierProvider);

    final cartState = ref.watch(cartNotifierProvider);
    final items = cartState.items;
    final subtotal = cartState.subtotal;
    final partnerId = items.isNotEmpty ? items.first.partnerId : null;

    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;

    _ensurePartnerLoaded(partnerId);

    final fallbackPartnerName = items.isNotEmpty ? items.first.partnerName : '';
    final fallbackPartnerLogo = items.isNotEmpty
        ? items.first.partnerLogoUrl
        : '';

    final partnerName = _partnerInfo?.partnerName ?? fallbackPartnerName;
    final partnerLogoUrl = _partnerInfo?.partnerLogoUrl ?? fallbackPartnerLogo;
    final minimumOrder = _partnerInfo?.minimumOrder ?? 0;
    final isOpen = _partnerInfo?.isOpen ?? true;
    final outOfZone = _isCurrentPartnerOutOfZone(
      userLat: zoneUserLat,
      userLng: zoneUserLng,
    );
    final serviceFee = _computeServiceFee(subtotal);

    final minNotReached = minimumOrder > 0 && subtotal < minimumOrder;
    final missingAmount = (minimumOrder - subtotal)
        .clamp(0, double.infinity)
        .toDouble();
    final hasSelectedPaymentMethod = _selectedPaymentMethod != null;

    final canCheckout =
        items.isNotEmpty &&
        !minNotReached &&
        isOpen &&
        !outOfZone &&
        hasSelectedPaymentMethod &&
        !_isPlacingOrder;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mon panier'),
        actions: [
          if (items.isNotEmpty)
            TextButton(
              onPressed: _confirmClearCart,
              child: const Text(
                'Vider',
                style: TextStyle(
                  color: Colors.red,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
        ],
      ),
      body: items.isEmpty
          ? const _EmptyCartView()
          : ListView(
              padding: const EdgeInsets.fromLTRB(14, 14, 14, 110),
              children: [
                PartnerHeader(
                  partnerName: partnerName,
                  partnerLogoUrl: partnerLogoUrl,
                ),
                if (!isOpen) const PartnerClosedBanner(),
                if (outOfZone)
                  Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF2F2),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFFFD7D7)),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.location_off_outlined,
                          size: 18,
                          color: Colors.red,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            '${l10n.translate('partner_details_out_of_zone')}.',
                            style: const TextStyle(
                              color: Colors.red,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                if (minNotReached)
                  MinimumOrderBanner(
                    minimumOrder: minimumOrder,
                    missingAmount: missingAmount,
                  ),
                ...items.map(
                  (item) => CartItemTile(
                    item: item,
                    onQuantityChanged: (newQty) {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .updateQuantity(
                            itemKey: item.uniqueKey,
                            quantity: newQty,
                          );
                    },
                    onRemove: () {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .removeItem(item.uniqueKey);
                    },
                    onCustomizationChanged: (options, note) {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .updateItemCustomization(
                            itemKey: item.uniqueKey,
                            selectedOptions: options,
                            kitchenNote: note,
                          );
                    },
                  ),
                ),
                OrderSummaryCard(
                  subtotal: subtotal,
                  partnerId: partnerId,
                  serviceFee: serviceFee,
                  discount: _promoDiscount,
                  promoCode: _promoCode,
                  onDeliveryFeeChanged: (_) {},
                  onPromoChanged: (promoCode, discount) {
                    setState(() {
                      _promoCode = promoCode;
                      _promoDiscount = discount;
                    });
                  },
                ),
                const SizedBox(height: 10),
                _PaymentMethodCard(
                  selectedMethod: _selectedPaymentMethod,
                  onMethodChanged: (method) {
                    setState(() {
                      _selectedPaymentMethod = method;
                    });
                  },
                ),
              ],
            ),
      bottomNavigationBar: items.isEmpty
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(14, 8, 14, 10),
              child: SizedBox(
                height: 52,
                child: ElevatedButton(
                  onPressed: canCheckout ? _placeOrder : null,
                  child: Text(
                    canCheckout
                        ? 'Passer la commande'
                        : (!isOpen
                              ? 'Partenaire fermé'
                              : outOfZone
                              ? l10n.translate('delivery_out_of_zone_badge')
                              : !hasSelectedPaymentMethod
                              ? 'Choisir le paiement'
                              : minNotReached
                              ? 'Minimum non atteint'
                              : 'Panier vide'),
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
              ),
            ),
    );
  }
}

class _PaymentMethodCard extends StatelessWidget {
  final String? selectedMethod;
  final ValueChanged<String?> onMethodChanged;

  const _PaymentMethodCard({
    required this.selectedMethod,
    required this.onMethodChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isCashSelected =
        selectedMethod == _CartScreenState._paymentMethodCash;

    return Card(
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Mode de paiement',
              style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            const SizedBox(height: 6),
            const Text(
              'Veuillez choisir un mode de paiement avant validation.',
              style: TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 8),
            InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () => onMethodChanged(_CartScreenState._paymentMethodCash),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isCashSelected
                        ? Theme.of(context).colorScheme.primary
                        : Colors.black12,
                    width: isCashSelected ? 1.6 : 1,
                  ),
                  color: isCashSelected
                      ? Theme.of(
                          context,
                        ).colorScheme.primary.withValues(alpha: 0.07)
                      : Colors.transparent,
                ),
                child: Row(
                  children: [
                    const Icon(Icons.payments_outlined),
                    const SizedBox(width: 10),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Paiement à la livraison',
                            style: TextStyle(fontWeight: FontWeight.w600),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Règlement en espèces à la réception.',
                            style: TextStyle(color: Colors.black54),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      isCashSelected
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked,
                      color: isCashSelected
                          ? Theme.of(context).colorScheme.primary
                          : Colors.black45,
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCartView extends StatelessWidget {
  const _EmptyCartView();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.shopping_bag_outlined, size: 56, color: Colors.black38),
            SizedBox(height: 8),
            Text(
              'Votre panier est vide',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            SizedBox(height: 6),
            Text(
              'Ajoutez des articles depuis un partenaire pour commencer.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
          ],
        ),
      ),
    );
  }
}
