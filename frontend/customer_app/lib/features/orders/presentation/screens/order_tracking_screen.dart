import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../config/routes/route_names.dart';
import '../../../../core/widgets/speedline_app_bar.dart';
import '../../../main/presentation/screens/main_scaffold.dart';
import '../providers/order_notification_provider.dart';
import '../providers/order_provider.dart';
import '../widgets/order_status_stepper.dart';

class OrderTrackingScreen extends ConsumerWidget {
  final String orderId;

  const OrderTrackingScreen({
    super.key,
    required this.orderId,
  });

  String _formatDate(DateTime? value) {
    if (value == null) return 'Unknown date';
    return DateFormat('dd/MM HH:mm').format(value.toLocal());
  }

  String _formatMoney(double value) {
    return '${value.toStringAsFixed(2)} TND';
  }

  void _goToOrders(BuildContext context) {
    context.go(RouteNames.orders);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final normalizedOrderId = orderId.trim();

    if (normalizedOrderId.isNotEmpty) {
      ref.listen(orderNotificationStreamProvider, (previous, next) {
        next.whenData((event) {
          final eventOrderId = event.orderId?.trim();
          if (eventOrderId == null || eventOrderId != normalizedOrderId) {
            return;
          }
          ref.invalidate(orderByIdProvider(normalizedOrderId));
        });
      });
    }

    final orderAsync = normalizedOrderId.isEmpty
        ? null
        : ref.watch(orderByIdProvider(normalizedOrderId));

    return MainScaffold(
      currentPath: RouteNames.orders,
      child: PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, result) {
          if (didPop) return;
          _goToOrders(context);
        },
        child: Scaffold(
          appBar: SpeedlineAppBar(
            title: 'Order Tracking',
            leading: IconButton(
              onPressed: () => _goToOrders(context),
              icon: Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.arrow_back_ios_new, size: 18),
              ),
            ),
          ),
          body: SafeArea(
            child: normalizedOrderId.isEmpty
                ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text('Invalid order id.'),
                          const SizedBox(height: 12),
                          OutlinedButton(
                            onPressed: () => _goToOrders(context),
                            child: const Text('Go to my orders'),
                          ),
                        ],
                      ),
                    ),
                  )
                : RefreshIndicator(
                    onRefresh: () async {
                      ref.invalidate(orderByIdProvider(normalizedOrderId));
                      await ref.read(orderByIdProvider(normalizedOrderId).future);
                    },
                    child: ListView(
                      padding: const EdgeInsets.all(20),
                      children: [
                        if (orderAsync != null)
                          orderAsync.when(
                            loading: () => const Card(
                              child: Padding(
                                padding: EdgeInsets.all(18),
                                child: Center(child: CircularProgressIndicator()),
                              ),
                            ),
                            error: (error, _) => Card(
                              child: Padding(
                                padding: const EdgeInsets.all(14),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'Unable to load order tracking right now.',
                                    ),
                                    const SizedBox(height: 10),
                                    OutlinedButton.icon(
                                      onPressed: () =>
                                          ref.invalidate(orderByIdProvider(normalizedOrderId)),
                                      icon: const Icon(Icons.refresh, size: 18),
                                      label: const Text('Retry'),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            data: (order) => Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
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
                                      const Text(
                                        'Order ID',
                                        style: TextStyle(
                                          color: Color(0xFF5A6773),
                                          fontSize: 13,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        order.orderNumber?.trim().isNotEmpty == true
                                            ? '#${order.orderNumber}'
                                            : order.id,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w600,
                                          fontSize: 16,
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      Text(
                                        order.displayStatus,
                                        style: const TextStyle(
                                          color: Color(0xFF4F5B67),
                                          fontWeight: FontWeight.w600,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        'Total: ${_formatMoney(order.total)}',
                                        style: const TextStyle(
                                          color: Color(0xFF4F5B67),
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        'Placed on ${_formatDate(order.orderTime ?? order.createdAt)}',
                                        style: const TextStyle(
                                          color: Color(0xFF6D7883),
                                          fontSize: 12,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 20),
                                Text(
                                  order.waitingPartnerAcceptance
                                      ? 'Waiting for partner confirmation. The acceptance step remains unchecked until partner action.'
                                      : 'Live updates are enabled. Status changes refresh automatically.',
                                  style: const TextStyle(
                                    color: Color(0xFF4F5B67),
                                    height: 1.35,
                                  ),
                                ),
                                const SizedBox(height: 24),
                                OrderStatusStepper(order: order),
                              ],
                            ),
                          ),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton(
                            onPressed: () => _goToOrders(context),
                            child: const Text('Go to my orders'),
                          ),
                        ),
                      ],
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}