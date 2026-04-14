import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lottie/lottie.dart';

import '../../../cart/cart_providers.dart';
import '../../../../config/routes/route_names.dart';

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

  String _formatEta(DateTime? value) {
    if (value == null) return 'Soon';
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
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

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFE8FFF6), Color(0xFFF8FBFF)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Card(
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                    side: const BorderSide(color: Color(0xFFE4E8EE)),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Lottie.asset(
                          'assets/lottie/success.json',
                          width: 170,
                          height: 170,
                          repeat: true,
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'Order Confirmed',
                          style: TextStyle(
                            fontSize: 27,
                            fontWeight: FontWeight.w700,
                            letterSpacing: -0.3,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Text(
                          widget.orderNumber == null ||
                                  widget.orderNumber!.trim().isEmpty
                              ? 'Your order has been placed successfully.'
                              : 'Order #${widget.orderNumber} has been placed.',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 15,
                            color: Color(0xFF4F5B67),
                            height: 1.35,
                          ),
                        ),
                        const SizedBox(height: 20),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: const Color(0xFFF4F7FB),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if ((widget.partnerName ?? '').trim().isNotEmpty)
                                Text(
                                  widget.partnerName!.trim(),
                                  style: const TextStyle(
                                    fontSize: 15,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              if ((widget.partnerName ?? '').trim().isNotEmpty)
                                const SizedBox(height: 8),
                              Text(
                                'Estimated delivery: ${_formatEta(widget.estimatedDeliveryTime)}',
                                style: const TextStyle(
                                  fontSize: 14,
                                  color: Color(0xFF4F5B67),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 20),
                        Text(
                          'Opening tracking in $countdown s',
                          style: const TextStyle(
                            fontSize: 14,
                            color: Color(0xFF4F5B67),
                          ),
                        ),
                        const SizedBox(height: 18),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton.icon(
                            onPressed: _goToTracking,
                            icon: const Icon(Icons.my_location_rounded),
                            label: const Text('Track now'),
                            style: FilledButton.styleFrom(
                              minimumSize: const Size.fromHeight(52),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(14),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 10),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton(
                            onPressed: () => context.go(RouteNames.explore),
                            style: OutlinedButton.styleFrom(
                              minimumSize: const Size.fromHeight(52),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(14),
                              ),
                            ),
                            child: const Text('Continue shopping'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
