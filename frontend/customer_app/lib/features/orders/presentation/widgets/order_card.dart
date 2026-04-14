import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../domain/entities/order.dart';

class OrderCard extends StatelessWidget {
  final Order order;
  final VoidCallback onTrackTap;

  const OrderCard({
    super.key,
    required this.order,
    required this.onTrackTap,
  });

  String _formatDate(DateTime? value) {
    if (value == null) return 'Unknown date';
    return DateFormat('dd/MM HH:mm').format(value.toLocal());
  }

  String _formatMoney(double value) {
    return '${value.toStringAsFixed(2)} TND';
  }

  Color _statusChipColor(Order order) {
    if (order.isCancelled) return const Color(0xFFB3261E);
    if (order.waitingPartnerAcceptance) return const Color(0xFFB26A00);
    if (order.isDelivered) return const Color(0xFF1B873F);
    return const Color(0xFF175CD3);
  }

  @override
  Widget build(BuildContext context) {
    final dateReference = order.orderTime ?? order.createdAt;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    order.orderNumber?.trim().isNotEmpty == true
                        ? 'Order #${order.orderNumber}'
                        : 'Order #${order.id}',
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                    ),
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: _statusChipColor(order).withOpacity(0.12),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    order.displayStatus,
                    style: TextStyle(
                      color: _statusChipColor(order),
                      fontWeight: FontWeight.w600,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              order.partnerName?.trim().isNotEmpty == true
                  ? order.partnerName!
                  : 'Partner unavailable',
              style: const TextStyle(
                color: Color(0xFF4F5B67),
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              _formatDate(dateReference),
              style: const TextStyle(
                color: Color(0xFF6D7883),
                fontSize: 12,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: Text(
                    _formatMoney(order.total),
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                    ),
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: onTrackTap,
                  icon: const Icon(Icons.my_location_rounded, size: 18),
                  label: const Text('Track'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
