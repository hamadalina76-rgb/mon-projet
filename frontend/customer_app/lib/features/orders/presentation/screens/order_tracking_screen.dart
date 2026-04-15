import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/widgets/speedline_app_bar.dart';
import '../../../main/presentation/screens/main_scaffold.dart';
import '../../domain/entities/order.dart';
import '../providers/order_notification_provider.dart';
import '../providers/order_provider.dart';

class _TimelineStep {
  final String title;
  final IconData icon;
  final bool completed;
  final DateTime? time;
  final String description;

  const _TimelineStep({
    required this.title,
    required this.icon,
    required this.completed,
    required this.time,
    required this.description,
  });
}

class OrderTrackingScreen extends ConsumerWidget {
  final String orderId;

  const OrderTrackingScreen({super.key, required this.orderId});

  void _goToOrders(BuildContext context) {
    context.go(RouteNames.orders);
  }

  String _money(double value) {
    return '${value.toStringAsFixed(2)} TND';
  }

  String _timeLabel(DateTime? value) {
    if (value == null) return '--:--';
    return DateFormat('HH:mm').format(value.toLocal());
  }

  String _fullDate(DateTime? value, AppLocalizations l10n) {
    if (value == null) return l10n.translate('unknown_date');
    return DateFormat('dd/MM • HH:mm').format(value.toLocal());
  }

  DateTime _resolveOrderStart(Order order) {
    final start = order.orderTime ?? order.createdAt ?? DateTime.now();
    return start.toLocal();
  }

  DateTime? _resolvePartnerAcceptedAt(Order order) {
    if (order.partnerAcceptedAt != null) {
      return order.partnerAcceptedAt!.toLocal();
    }
    if (order.isAcceptedByPartner) {
      return _resolveOrderStart(order).add(const Duration(minutes: 2));
    }
    return null;
  }

  int _resolvePrepMinutes(Order order) {
    final partnerPrep =
        order.partnerAcceptedPrepMinutes ?? order.suggestedPreparationMinutes;
    if (partnerPrep != null && partnerPrep > 0) {
      return partnerPrep.clamp(5, 180);
    }

    final delivery = order.deliveryTimeMinutes ?? 40;
    final prep = (delivery * 0.55).round();
    return prep.clamp(10, 45);
  }

  DateTime? _resolvePrepEnd(Order order) {
    final acceptedAt = _resolvePartnerAcceptedAt(order);
    if (acceptedAt == null) return null;
    return acceptedAt.add(Duration(minutes: _resolvePrepMinutes(order)));
  }

  DateTime? _resolveEstimatedDelivery(Order order) {
    return _resolvePrepEnd(order) ?? order.estimatedDeliveryTime?.toLocal();
  }

  String _resolvePrepSlotLabel(Order order, AppLocalizations l10n) {
    final acceptedAt = _resolvePartnerAcceptedAt(order);
    final prepEnd = _resolvePrepEnd(order);

    if (acceptedAt == null || prepEnd == null) {
      return l10n.translate('order_tracking_preparation_slot_pending');
    }

    return '${_timeLabel(acceptedAt)} - ${_timeLabel(prepEnd)}';
  }

  String _resolvePrepCountdownLabel(
    Order order,
    AppLocalizations l10n,
    DateTime now,
  ) {
    final acceptedAt = _resolvePartnerAcceptedAt(order);
    final prepEnd = _resolvePrepEnd(order);

    if (acceptedAt == null || prepEnd == null) {
      return l10n.translate('order_tracking_preparation_slot_pending');
    }

    final completedStatuses = {
      'READY_FOR_PICKUP',
      'PICKED_UP',
      'IN_DELIVERY',
      'DELIVERED',
    };

    final nowLocal = now.toLocal();
    if (completedStatuses.contains(order.normalizedStatus) ||
        !nowLocal.isBefore(prepEnd)) {
      return l10n.translate('order_tracking_preparation_done');
    }

    final secondsLeft = prepEnd.difference(nowLocal).inSeconds;
    final minutesLeft = (secondsLeft / 60).ceil().clamp(1, 180);

    return '$minutesLeft ${l10n.translate('product_detail_minutes_abbr')} ${l10n.translate('order_tracking_remaining_suffix')}';
  }

  List<_TimelineStep> _stepsForOrder(
    Order order,
    AppLocalizations l10n,
    DateTime now,
  ) {
    final start = _resolveOrderStart(order);
    final acceptedAt = _resolvePartnerAcceptedAt(order);
    final prep = _resolvePrepMinutes(order);
    final preparingAt = acceptedAt;
    final outAt = acceptedAt?.add(Duration(minutes: prep));
    final prepSlotLabel = _resolvePrepSlotLabel(order, l10n);
    final prepCountdownLabel = _resolvePrepCountdownLabel(order, l10n, now);
    final deliveredAt =
        _resolveEstimatedDelivery(order) ?? start.add(Duration(minutes: prep));

    return [
      _TimelineStep(
        title: l10n.translate('order_tracking_step_order_placed'),
        icon: Icons.receipt_long_rounded,
        completed: true,
        time: start,
        description: l10n.translate('order_tracking_step_order_placed_desc'),
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_partner_confirmation'),
        icon: Icons.storefront_rounded,
        completed: order.isAcceptedByPartner,
        time: acceptedAt,
        description: l10n.translate('order_tracking_step_partner_confirmation_desc'),
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_preparation'),
        icon: Icons.soup_kitchen_rounded,
        completed: const {
          'PREPARING',
          'READY_FOR_PICKUP',
          'PICKED_UP',
          'IN_DELIVERY',
          'DELIVERED',
        }.contains(order.normalizedStatus),
        time:
            const {
              'PREPARING',
              'READY_FOR_PICKUP',
              'PICKED_UP',
              'IN_DELIVERY',
              'DELIVERED',
            }.contains(order.normalizedStatus)
            ? preparingAt
            : null,
        description:
            '${l10n.translate('order_tracking_preparation_slot')}: $prepSlotLabel | '
            '${l10n.translate('order_tracking_preparation_countdown')}: $prepCountdownLabel',
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_out_for_delivery'),
        icon: Icons.delivery_dining_rounded,
        completed: order.isOutForDelivery,
        time: order.isOutForDelivery ? outAt : null,
        description: l10n.translate('order_tracking_step_out_for_delivery_desc'),
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_delivered'),
        icon: Icons.check_circle_rounded,
        completed: order.isDelivered,
        time: order.isDelivered ? deliveredAt : null,
        description: l10n.translate('order_tracking_step_delivered_desc'),
      ),
    ];
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
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
          backgroundColor: AppColors.background,
          appBar: SpeedlineAppBar(
            title: l10n.translate('order_tracking_title'),
            leading: IconButton(
              onPressed: () => _goToOrders(context),
              icon: Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: AppColors.surface.withValues(alpha: 0.22),
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
                          Text(l10n.translate('order_tracking_invalid_order_id')),
                          const SizedBox(height: 12),
                          OutlinedButton(
                            onPressed: () => _goToOrders(context),
                            child: Text(l10n.translate('go_to_my_orders')),
                          ),
                        ],
                      ),
                    ),
                  )
                : RefreshIndicator(
                    color: AppColors.primary,
                    onRefresh: () async {
                      ref.invalidate(orderByIdProvider(normalizedOrderId));
                      await ref.read(
                        orderByIdProvider(normalizedOrderId).future,
                      );
                    },
                    child: ListView(
                      padding: const EdgeInsets.all(18),
                      children: [
                        if (orderAsync != null)
                          orderAsync.when(
                            loading: () => const Card(
                              child: Padding(
                                padding: EdgeInsets.all(18),
                                child: Center(
                                  child: CircularProgressIndicator(),
                                ),
                              ),
                            ),
                            error: (error, _) => Card(
                              child: Padding(
                                padding: const EdgeInsets.all(14),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(l10n.translate('order_tracking_load_error')),
                                    const SizedBox(height: 10),
                                    OutlinedButton.icon(
                                      onPressed: () => ref.invalidate(
                                        orderByIdProvider(normalizedOrderId),
                                      ),
                                      icon: const Icon(Icons.refresh, size: 18),
                                      label: Text(l10n.translate('retry')),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            data: (order) {
                              return StreamBuilder<int>(
                                stream: Stream<int>.periodic(
                                  const Duration(seconds: 30),
                                  (tick) => tick,
                                ),
                                initialData: 0,
                                builder: (context, snapshot) {
                                  final prepMinutes = _resolvePrepMinutes(order);
                                  final estimatedDelivery =
                                      _resolveEstimatedDelivery(order);
                                  final prepSlotLabel =
                                      _resolvePrepSlotLabel(order, l10n);
                                  final now = DateTime.now();
                                  final steps = _stepsForOrder(order, l10n, now);

                                  return Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Container(
                                        width: double.infinity,
                                        padding: const EdgeInsets.all(16),
                                        decoration: BoxDecoration(
                                          color: AppColors.surface,
                                          borderRadius: BorderRadius.circular(20),
                                          border: Border.all(
                                            color: AppColors.border,
                                          ),
                                          boxShadow: [
                                            BoxShadow(
                                              color: AppColors.shadow,
                                              blurRadius: 12,
                                              offset: const Offset(0, 8),
                                            ),
                                          ],
                                        ),
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              order.displayStatus,
                                              style: const TextStyle(
                                                fontSize: 16,
                                                fontWeight: FontWeight.w700,
                                                color: AppColors.primary,
                                              ),
                                            ),
                                            const SizedBox(height: 12),
                                            Row(
                                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                              children: [
                                                Text(
                                                  l10n.translate('order_tracking_estimated_delivery'),
                                                  style: const TextStyle(
                                                    color: AppColors.textSecondary,
                                                    fontSize: 12,
                                                  ),
                                                ),
                                                Text(
                                                  _timeLabel(estimatedDelivery),
                                                  style: const TextStyle(
                                                    fontWeight: FontWeight.w700,
                                                    color: AppColors.primary,
                                                  ),
                                                ),
                                              ],
                                            ),
                                            const SizedBox(height: 10),
                                            Row(
                                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                              children: [
                                                Text(
                                                  l10n.translate('order_tracking_preparation_slot'),
                                                  style: const TextStyle(
                                                    color: AppColors.textSecondary,
                                                    fontSize: 12,
                                                  ),
                                                ),
                                                Flexible(
                                                  child: Text(
                                                    prepSlotLabel,
                                                    maxLines: 1,
                                                    overflow: TextOverflow.ellipsis,
                                                    textAlign: TextAlign.end,
                                                    style: const TextStyle(
                                                      fontWeight: FontWeight.w700,
                                                    ),
                                                  ),
                                                ),
                                              ],
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(height: 14),
                                      Container(
                                        width: double.infinity,
                                        padding: const EdgeInsets.all(16),
                                        decoration: BoxDecoration(
                                          color: AppColors.surface,
                                          borderRadius: BorderRadius.circular(20),
                                          border: Border.all(
                                            color: AppColors.border,
                                          ),
                                          boxShadow: [
                                            BoxShadow(
                                              color: AppColors.shadow,
                                              blurRadius: 12,
                                              offset: const Offset(0, 5),
                                            ),
                                          ],
                                        ),
                                        child: Column(
                                          children: [
                                            Row(
                                              children: [
                                                Expanded(
                                                  child: _InfoChip(
                                                    icon: Icons.schedule_rounded,
                                                    label: l10n.translate('order_tracking_preparation_slot'),
                                                    value: prepSlotLabel,
                                                  ),
                                                ),
                                                const SizedBox(width: 10),
                                                Expanded(
                                                  child: _InfoChip(
                                                    icon: Icons
                                                        .local_shipping_rounded,
                                                    label: l10n.translate('order_tracking_preparation_time'),
                                                    value:
                                                        '$prepMinutes ${l10n.translate('product_detail_minutes_abbr')}',
                                                  ),
                                                ),
                                              ],
                                            ),
                                            const SizedBox(height: 10),
                                            Row(
                                              children: [
                                                Expanded(
                                                  child: Text(
                                                    order.orderNumber
                                                                ?.trim()
                                                                .isNotEmpty ==
                                                            true
                                                        ? '#${order.orderNumber}'
                                                        : '#${order.id}',
                                                    style: const TextStyle(
                                                      fontSize: 17,
                                                      fontWeight: FontWeight.w800,
                                                    ),
                                                  ),
                                                ),
                                                Text(
                                                  _money(order.total),
                                                  style: const TextStyle(
                                                    fontSize: 18,
                                                    fontWeight: FontWeight.w800,
                                                  ),
                                                ),
                                              ],
                                            ),
                                            const SizedBox(height: 2),
                                            Align(
                                              alignment: Alignment.centerLeft,
                                              child: Text(
                                                _fullDate(
                                                  order.orderTime ??
                                                      order.createdAt,
                                                  l10n,
                                                ),
                                                style: const TextStyle(
                                                  color: AppColors.textSecondary,
                                                  fontWeight: FontWeight.w600,
                                                  fontSize: 12,
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(height: 16),
                                      Text(
                                        l10n.translate('order_tracking_live_process'),
                                        style: const TextStyle(
                                          fontSize: 19,
                                          fontWeight: FontWeight.w800,
                                          color: AppColors.textPrimary,
                                        ),
                                      ),
                                      const SizedBox(height: 10),
                                      Container(
                                        width: double.infinity,
                                        padding: const EdgeInsets.all(14),
                                        decoration: BoxDecoration(
                                          color: AppColors.surface,
                                          borderRadius: BorderRadius.circular(20),
                                          border: Border.all(
                                            color: AppColors.border,
                                          ),
                                        ),
                                        child: Column(
                                          children: List<Widget>.generate(
                                            steps.length,
                                            (index) {
                                              final step = steps[index];
                                              final isLast =
                                                  index == steps.length - 1;

                                              return TweenAnimationBuilder<double>(
                                                tween: Tween(begin: 0, end: 1),
                                                duration: Duration(
                                                  milliseconds: 350 +
                                                      (index * 120),
                                                ),
                                                curve: Curves.easeOutCubic,
                                                builder:
                                                    (context, value, child) {
                                                  return Opacity(
                                                    opacity: value,
                                                    child: Transform.translate(
                                                      offset: Offset(
                                                        0,
                                                        (1 - value) * 12,
                                                      ),
                                                      child: child,
                                                    ),
                                                  );
                                                },
                                                child: _TimelineRow(
                                                  step: step,
                                                  isLast: isLast,
                                                  timeLabel:
                                                      _timeLabel(step.time),
                                                ),
                                              );
                                            },
                                          ),
                                        ),
                                      ),
                                    ],
                                  );
                                },
                              );
                            },
                          ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton(
                            onPressed: () => _goToOrders(context),
                            child: Text(l10n.translate('go_to_my_orders')),
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

class _InfoChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _InfoChip({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.surfaceLight,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                Text(
                  value,
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TimelineRow extends StatelessWidget {
  final _TimelineStep step;
  final bool isLast;
  final String timeLabel;

  const _TimelineRow({
    required this.step,
    required this.isLast,
    required this.timeLabel,
  });

  @override
  Widget build(BuildContext context) {
    final accent = step.completed ? AppColors.primary : AppColors.secondaryGrey;

    return Padding(
      padding: EdgeInsets.only(bottom: isLast ? 0 : 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 450),
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  color: step.completed ? accent : AppColors.surface,
                  shape: BoxShape.circle,
                  border: Border.all(color: accent, width: 1.6),
                ),
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: step.completed
                      ? const Icon(
                          Icons.check_rounded,
                          color: Colors.white,
                          size: 18,
                        )
                      : Icon(step.icon, color: accent, size: 17),
                ),
              ),
              if (!isLast)
                AnimatedContainer(
                  duration: const Duration(milliseconds: 450),
                  width: 2,
                  height: 44,
                  color: step.completed ? accent : AppColors.softGrey,
                ),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Container(
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
              decoration: BoxDecoration(
                color: step.completed
                    ? AppColors.primary.withValues(alpha: 0.08)
                    : AppColors.surfaceLight,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          step.title,
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          step.description,
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 10),
                  Text(
                    timeLabel,
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: step.completed
                          ? AppColors.primary
                          : AppColors.grey,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
