import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../providers/current_courier_provider.dart';
import '../../domain/entities/order.dart';
import '../providers/orders_provider.dart';

/// Full-screen overlay shown when a DELIVERY_OFFER arrives via WebSocket.
/// Automatically dismisses after [_offerTimeout] if the courier doesn't respond.
class IncomingOfferOverlay extends ConsumerStatefulWidget {
  final DeliveryOffer offer;

  const IncomingOfferOverlay({super.key, required this.offer});

  @override
  ConsumerState<IncomingOfferOverlay> createState() => _IncomingOfferOverlayState();
}

class _IncomingOfferOverlayState extends ConsumerState<IncomingOfferOverlay>
    with SingleTickerProviderStateMixin {
  static const _offerTimeout = 30;
  late int _remaining;
  Timer? _timer;
  late AnimationController _ringController;

  @override
  void initState() {
    super.initState();
    _remaining = _offerTimeout;
    _ringController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: _offerTimeout),
    )..forward();

    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_remaining <= 1) {
        t.cancel();
        _onTimeout();
      } else {
        if (mounted) setState(() => _remaining--);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _ringController.dispose();
    super.dispose();
  }

  void _onTimeout() {
    if (!mounted) return;
    ref.read(ordersProvider.notifier).dismissOffer();
    Navigator.of(context).pop();
  }

  Future<void> _accept() async {
    _timer?.cancel();
    final courierAsync = ref.read(currentCourierProvider);
    final courierId = courierAsync.value?.id;
    if (courierId == null) {
      if (mounted) Navigator.of(context).pop();
      return;
    }
    await ref.read(ordersProvider.notifier).acceptOffer(int.tryParse(courierId) ?? 0);
    if (mounted) Navigator.of(context).pop();
  }

  Future<void> _decline() async {
    _timer?.cancel();
    final courierAsync = ref.read(currentCourierProvider);
    final courierId = courierAsync.value?.id;
    if (courierId == null) {
      if (mounted) Navigator.of(context).pop();
      return;
    }
    await ref.read(ordersProvider.notifier).declineOffer(
          int.tryParse(courierId) ?? 0,
          reason: 'DECLINED_BY_COURIER',
        );
    if (mounted) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final offer = widget.offer;
    final isLoading = ref.watch(ordersProvider).isResponding;

    return Scaffold(
      backgroundColor: Colors.black87,
      body: SafeArea(
        child: Column(
          children: [
            // ── Header
            Padding(
              padding: EdgeInsets.all(20.r),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (offer.isUrgent) ...[
                    Icon(Icons.bolt, color: Colors.amber, size: 22.sp),
                    SizedBox(width: 6.w),
                  ],
                  Text(
                    offer.isUrgent ? 'Commande URGENTE !' : 'Nouvelle commande',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 22.sp,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),

            // ── Timer ring
            Stack(
              alignment: Alignment.center,
              children: [
                SizedBox(
                  width: 100.r,
                  height: 100.r,
                  child: AnimatedBuilder(
                    animation: _ringController,
                    builder: (_, __) => CircularProgressIndicator(
                      value: 1 - _ringController.value,
                      strokeWidth: 6,
                      backgroundColor: Colors.white24,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        _remaining > 10 ? AppColors.primary : Colors.redAccent,
                      ),
                    ),
                  ),
                ),
                Text(
                  '$_remaining',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 28.sp,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),

            SizedBox(height: 24.h),

            // ── Order card
            Expanded(
              child: SingleChildScrollView(
                padding: EdgeInsets.symmetric(horizontal: 20.w),
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16.r),
                  ),
                  padding: EdgeInsets.all(20.r),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (offer.orderNumber != null)
                        _InfoRow(
                          icon: Icons.receipt_long,
                          label: 'Commande',
                          value: offer.orderNumber!,
                          bold: true,
                        ),
                      if (offer.partnerName != null) ...[
                        Divider(height: 20.h),
                        _InfoRow(
                          icon: Icons.store,
                          label: 'Restaurant',
                          value: offer.partnerName!,
                        ),
                      ],
                      if (offer.pickupAddress != null) ...[
                        SizedBox(height: 8.h),
                        _InfoRow(
                          icon: Icons.place,
                          iconColor: Colors.orange,
                          label: 'Retrait',
                          value: offer.pickupAddress!,
                        ),
                      ],
                      if (offer.dropoffAddress != null) ...[
                        SizedBox(height: 8.h),
                        _InfoRow(
                          icon: Icons.flag,
                          iconColor: Colors.green,
                          label: 'Livraison',
                          value: offer.dropoffAddress!,
                        ),
                      ],
                      Divider(height: 24.h),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _Chip(
                            icon: Icons.attach_money,
                            label: '${offer.deliveryFee.toStringAsFixed(1)} TND',
                            color: Colors.green.shade700,
                          ),
                          if (offer.etaPickupMin != null)
                            _Chip(
                              icon: Icons.access_time,
                              label: '~${offer.etaPickupMin} min retrait',
                              color: Colors.blue.shade700,
                            ),
                          if (offer.etaDeliveryMin != null)
                            _Chip(
                              icon: Icons.delivery_dining,
                              label: '~${offer.etaDeliveryMin} min livr.',
                              color: Colors.purple.shade700,
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),

            SizedBox(height: 16.h),

            // ── Action buttons
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 16.h),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: isLoading ? null : _decline,
                      icon: const Icon(Icons.close, color: Colors.redAccent),
                      label: Text(
                        'Refuser',
                        style: TextStyle(color: Colors.redAccent, fontSize: 16.sp),
                      ),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: Colors.redAccent, width: 2),
                        padding: EdgeInsets.symmetric(vertical: 14.h),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12.r),
                        ),
                      ),
                    ),
                  ),
                  SizedBox(width: 16.w),
                  Expanded(
                    flex: 2,
                    child: ElevatedButton.icon(
                      onPressed: isLoading ? null : _accept,
                      icon: isLoading
                          ? SizedBox(
                              width: 18.r,
                              height: 18.r,
                              child: const CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.check, color: Colors.white),
                      label: Text(
                        'Accepter',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        padding: EdgeInsets.symmetric(vertical: 14.h),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12.r),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final Color? iconColor;
  final String label;
  final String value;
  final bool bold;

  const _InfoRow({
    required this.icon,
    this.iconColor,
    required this.label,
    required this.value,
    this.bold = false,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18.sp, color: iconColor ?? Colors.grey[600]),
        SizedBox(width: 8.w),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(fontSize: 11.sp, color: Colors.grey[500]),
              ),
              Text(
                value,
                style: TextStyle(
                  fontSize: 14.sp,
                  fontWeight: bold ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Chip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;

  const _Chip({required this.icon, required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 6.h),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20.r),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14.sp, color: color),
          SizedBox(width: 4.w),
          Text(label, style: TextStyle(fontSize: 11.sp, color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
