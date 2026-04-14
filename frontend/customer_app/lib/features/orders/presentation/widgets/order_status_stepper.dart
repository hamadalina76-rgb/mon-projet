import 'package:flutter/material.dart';

import '../../domain/entities/order.dart';

class _TrackingStepData {
  final String title;
  final String subtitle;
  final bool completed;

  const _TrackingStepData({
    required this.title,
    required this.subtitle,
    required this.completed,
  });
}

class OrderStatusStepper extends StatelessWidget {
  final Order order;

  const OrderStatusStepper({
    super.key,
    required this.order,
  });

  List<_TrackingStepData> _buildSteps() {
    return <_TrackingStepData>[
      _TrackingStepData(
        title: 'Order sent to partner',
        subtitle: 'Your order has been transmitted successfully.',
        completed: order.sentToPartner,
      ),
      _TrackingStepData(
        title: 'Order accepted',
        subtitle: 'Checked only when the partner confirms your order.',
        completed: order.isAcceptedByPartner,
      ),
      _TrackingStepData(
        title: 'Courier assignment',
        subtitle: 'A courier is being assigned.',
        completed: order.hasCourierAssigned,
      ),
      _TrackingStepData(
        title: 'Out for delivery',
        subtitle: 'Your order is on the way.',
        completed: order.isOutForDelivery,
      ),
      _TrackingStepData(
        title: 'Delivered',
        subtitle: 'Order delivered to your address.',
        completed: order.isDelivered,
      ),
    ];
  }

  Widget _buildDot(bool completed) {
    return Container(
      width: 22,
      height: 22,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: completed ? const Color(0xFF14A44D) : Colors.white,
        border: Border.all(
          color: completed ? const Color(0xFF14A44D) : const Color(0xFFC7D0DA),
        ),
      ),
      child: completed ? const Icon(Icons.check, size: 15, color: Colors.white) : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    final steps = _buildSteps();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (order.isCancelled)
          Container(
            width: double.infinity,
            margin: const EdgeInsets.only(bottom: 14),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFFFDECEC),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Text(
              'This order has been cancelled.',
              style: TextStyle(
                color: Color(0xFF8A1C16),
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ...List<Widget>.generate(steps.length, (index) {
          final step = steps[index];
          final isLast = index == steps.length - 1;

          return Padding(
            padding: EdgeInsets.only(bottom: isLast ? 0 : 14),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Column(
                  children: [
                    _buildDot(step.completed),
                    if (!isLast)
                      Container(
                        width: 2,
                        height: 32,
                        margin: const EdgeInsets.only(top: 2),
                        color: step.completed
                            ? const Color(0xFF14A44D)
                            : const Color(0xFFC7D0DA),
                      ),
                  ],
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        step.title,
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        step.subtitle,
                        style: const TextStyle(
                          color: Color(0xFF4F5B67),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        }),
      ],
    );
  }
}
