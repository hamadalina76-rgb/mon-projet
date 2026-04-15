import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lottie/lottie.dart';

import '../../../cart/cart_providers.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';

final orderConfirmationCountdownProvider = StateNotifierProvider.autoDispose
    .family<_OrderConfirmationCountdownNotifier, int, int>((ref, initialValue) {
      return _OrderConfirmationCountdownNotifier(initialValue);
    });

class _OrderConfirmationCountdownNotifier extends StateNotifier<int> {
  Timer? _timer;

  _OrderConfirmationCountdownNotifier(super.state) {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (state <= 0) {
        timer.cancel();
        return;
      }
      state = state - 1;
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

class OrderConfirmationScreen extends ConsumerStatefulWidget {
  final String orderId;
  final String? orderNumber;
  final String? partnerName;
  final DateTime? estimatedDeliveryTime;

  const OrderConfirmationScreen({
    super.key,
    required this.orderId,
    this.orderNumber,
    this.partnerName,
    this.estimatedDeliveryTime,
  });

  @override
  ConsumerState<OrderConfirmationScreen> createState() =>
      _OrderConfirmationScreenState();
}

class _OrderConfirmationScreenState
    extends ConsumerState<OrderConfirmationScreen> {
  static const int _autoRedirectSeconds = 5;

  bool _didNavigate = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() async {
      await ref.read(cartNotifierProvider.notifier).clearCart();
    });
  }

  void _goToTracking() {
    if (_didNavigate || !mounted) return;
    _didNavigate = true;

    if (widget.orderId.trim().isEmpty) {
      context.go(RouteNames.orders);
      return;
    }

    context.go(RouteNames.orderTracking(widget.orderId));
  }

  String _formatEta(DateTime? value, AppLocalizations l10n) {
    if (value == null) return l10n.translate('order_confirmation_eta_soon');
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    ref.listen<int>(
      orderConfirmationCountdownProvider(_autoRedirectSeconds),
      (previous, next) {
        if (next <= 0) {
          _goToTracking();
        }
      },
    );

    final countdown =
        ref.watch(orderConfirmationCountdownProvider(_autoRedirectSeconds));
    final progress =
        (countdown / _autoRedirectSeconds).clamp(0.0, 1.0).toDouble();

    return Scaffold(
      backgroundColor: AppColors.black.withOpacity(0.5),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: AppColors.border),
                  boxShadow: const [
                    BoxShadow(
                      color: AppColors.shadow,
                      blurRadius: 24,
                      offset: Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Lottie.asset(
                      'assets/lottie/success.json',
                      width: 150,
                      height: 150,
                      repeat: true,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l10n.translate('order_confirmation_success_title'),
                      style: const TextStyle(
                        fontSize: 26,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                        letterSpacing: -0.2,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      widget.orderNumber == null ||
                              widget.orderNumber!.trim().isEmpty
                          ? l10n.translate('order_confirmation_success_message')
                          : '${l10n.translate('order_number_prefix')}${widget.orderNumber} ${l10n.translate('order_confirmation_success_suffix')}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 15,
                        color: AppColors.textSecondary,
                        height: 1.35,
                      ),
                    ),
                    const SizedBox(height: 18),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppColors.secondary2,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if ((widget.partnerName ?? '').trim().isNotEmpty)
                            Text(
                              widget.partnerName!.trim(),
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          if ((widget.partnerName ?? '').trim().isNotEmpty)
                            const SizedBox(height: 6),
                          Row(
                            children: [
                              const Icon(
                                Icons.schedule_rounded,
                                size: 16,
                                color: AppColors.primary,
                              ),
                              const SizedBox(width: 6),
                              Text(
                                '${l10n.translate('order_confirmation_estimated_delivery')}: ${_formatEta(widget.estimatedDeliveryTime, l10n)}',
                                style: const TextStyle(
                                  fontSize: 14,
                                  color: AppColors.textSecondary,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      '${l10n.translate('order_confirmation_opening_tracking_in')} $countdown ${l10n.translate('sec')}',
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 10),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(999),
                      child: LinearProgressIndicator(
                        minHeight: 7,
                        value: progress,
                        backgroundColor: AppColors.softGrey,
                        valueColor: const AlwaysStoppedAnimation<Color>(
                          AppColors.primary,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _goToTracking,
                        icon: const Icon(Icons.my_location_rounded),
                        label: Text(l10n.translate('track_order')),
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: AppColors.surface,
                          minimumSize: const Size.fromHeight(50),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                        onPressed: () => context.go(RouteNames.explore),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.textPrimary,
                          side: const BorderSide(color: AppColors.border),
                          minimumSize: const Size.fromHeight(48),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        child: Text(l10n.translate('continue_shopping')),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
