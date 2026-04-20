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
import '../utils/order_status_palette.dart';

class _TimelineStep {
  final String title;
  final IconData icon;
  final bool completed;
  final bool inProgress;
  final Color accentColor;
  final DateTime? time;
  final String description;

  const _TimelineStep({
    required this.title,
    required this.icon,
    required this.completed,
    required this.inProgress,
    required this.accentColor,
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

  DateTime? _resolveScheduledDeliveryTime(Order order) {
    return order.scheduledDeliveryTime?.toLocal();
  }

  int _resolvePrepMinutes(Order order) {
    final partnerPrep =
        order.partnerAcceptedPrepMinutes ?? order.suggestedPreparationMinutes;
    if (partnerPrep != null && partnerPrep > 0) {
      return partnerPrep;
    }

    final delivery = order.deliveryTimeMinutes ?? 40;
    final prep = (delivery * 0.55).round();
    return prep.clamp(10, 45);
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

  DateTime? _resolvePreparationReadyAt(Order order) {
    if (order.partnerReadyAt != null) {
      return order.partnerReadyAt!.toLocal();
    }

    final acceptedAt = _resolvePartnerAcceptedAt(order);
    if (acceptedAt == null) return null;

    final hasReachedReadyStatus = const {
      'READY_FOR_PICKUP',
      'PICKED_UP',
      'IN_DELIVERY',
      'DELIVERED',
    }.contains(order.normalizedStatus);

    if (!hasReachedReadyStatus) {
      return null;
    }

    return acceptedAt.add(Duration(minutes: _resolvePrepMinutes(order)));
  }

  DateTime? _resolvePreparationStartAt(Order order) {
    final prepMinutes = _resolvePrepMinutes(order);
    final scheduledDeliveryTime = _resolveScheduledDeliveryTime(order);

    if (order.isScheduled && scheduledDeliveryTime != null) {
      return scheduledDeliveryTime.subtract(Duration(minutes: prepMinutes));
    }

    final acceptedAt = _resolvePartnerAcceptedAt(order);
    if (acceptedAt != null) return acceptedAt;

    final prepEnd = _resolvePrepEnd(order);
    if (prepEnd == null) return null;
    return prepEnd.subtract(Duration(minutes: prepMinutes));
  }

  DateTime? _resolvePrepEnd(Order order) {
    final scheduledDeliveryTime = _resolveScheduledDeliveryTime(order);
    if (order.isScheduled && scheduledDeliveryTime != null) {
      return scheduledDeliveryTime;
    }

    final acceptedAt = _resolvePartnerAcceptedAt(order);
    if (acceptedAt == null) return null;

    return _resolvePreparationReadyAt(order) ??
        acceptedAt.add(Duration(minutes: _resolvePrepMinutes(order)));
  }

  int? _resolveExactPrepMinutes(Order order) {
    if (order.isScheduled && _resolveScheduledDeliveryTime(order) != null) {
      return null;
    }

    final acceptedAt = _resolvePartnerAcceptedAt(order);
    final prepReadyAt = _resolvePreparationReadyAt(order);
    if (acceptedAt == null || prepReadyAt == null) return null;

    final diffSeconds = prepReadyAt.difference(acceptedAt).inSeconds;
    if (diffSeconds <= 0) return null;
    return (diffSeconds / 60).ceil();
  }

  String _resolvePrepTimeLabel(Order order, AppLocalizations l10n) {
    final chosenPrep = _resolvePrepMinutes(order);

    if (order.isScheduled && _resolveScheduledDeliveryTime(order) != null) {
      return '$chosenPrep ${l10n.translate('product_detail_minutes_abbr')}';
    }

    final exactPrep = _resolveExactPrepMinutes(order);
    if (exactPrep == null) {
      return '$chosenPrep ${l10n.translate('product_detail_minutes_abbr')}';
    }

    return '$exactPrep ${l10n.translate('product_detail_minutes_abbr')} '
        '(${l10n.translate('order_tracking_partner_selected_prefix')} '
        '$chosenPrep ${l10n.translate('product_detail_minutes_abbr')})';
  }

  DateTime? _resolveEstimatedDelivery(Order order) {
    final scheduledDeliveryTime = _resolveScheduledDeliveryTime(order);
    if (order.isScheduled && scheduledDeliveryTime != null) {
      return scheduledDeliveryTime;
    }

    final acceptedAt = _resolvePartnerAcceptedAt(order);
    if (acceptedAt != null) {
      return _resolvePrepEnd(order) ??
          acceptedAt.add(Duration(minutes: _resolvePrepMinutes(order)));
    }

    return order.estimatedDeliveryTime?.toLocal();
  }

  String _resolvePrepSlotLabel(Order order, AppLocalizations l10n) {
    final prepStart = _resolvePreparationStartAt(order);
    final prepEnd = _resolvePrepEnd(order);

    if (prepStart == null || prepEnd == null) {
      return l10n.translate('order_tracking_preparation_slot_pending');
    }

    return '${_timeLabel(prepStart)} - ${_timeLabel(prepEnd)}';
  }

  String _formatCountdownDurationLabel(
    int minutes,
    AppLocalizations l10n, {
    int prepMinutesToReduce = 0,
  }) {
    final reduction = prepMinutesToReduce < 0 ? 0 : prepMinutesToReduce;
    final reducedMinutes = minutes - reduction;
    final normalizedReducedMinutes = reducedMinutes < 1 ? 1 : reducedMinutes;

    if (normalizedReducedMinutes >= 1440) {
      final days = normalizedReducedMinutes ~/ 1440;
      final remainingAfterDays = normalizedReducedMinutes % 1440;
      final hours = remainingAfterDays ~/ 60;
      final remainingMinutes = remainingAfterDays % 60;

      return '$days ${l10n.translate('product_detail_days_abbr')} '
          '${hours.toString().padLeft(2, '0')} '
          '${l10n.translate('product_detail_hours_abbr')} '
          '${remainingMinutes.toString().padLeft(2, '0')} '
          '${l10n.translate('product_detail_minutes_abbr')}';
    }

    if (normalizedReducedMinutes >= 60) {
      final hours = normalizedReducedMinutes ~/ 60;
      final remainingMinutes = normalizedReducedMinutes % 60;
      return '$hours ${l10n.translate('product_detail_hours_abbr')} '
          '${remainingMinutes.toString().padLeft(2, '0')} '
          '${l10n.translate('product_detail_minutes_abbr')}';
    }

    return '$normalizedReducedMinutes ${l10n.translate('product_detail_minutes_abbr')}';
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

    if (nowLocal.isBefore(acceptedAt)) {
      return l10n.translate('order_tracking_preparation_slot_pending');
    }

    final secondsLeft = prepEnd.difference(nowLocal).inSeconds;
    final minutesLeft = (secondsLeft / 60).ceil();
    final normalizedMinutesLeft = minutesLeft < 1 ? 1 : minutesLeft;
    final durationLabel = _formatCountdownDurationLabel(
      normalizedMinutesLeft,
      l10n,
      prepMinutesToReduce: _resolvePrepMinutes(order),
    );

    return '$durationLabel ${l10n.translate('order_tracking_remaining_suffix')}';
  }

  List<_TimelineStep> _stepsForOrder(
    Order order,
    AppLocalizations l10n,
    DateTime now,
  ) {
    final start = _resolveOrderStart(order);
    final acceptedAt = _resolvePartnerAcceptedAt(order);
    final prep = _resolvePrepMinutes(order);
    final preparingAt = _resolvePreparationStartAt(order);
    final outAt = _resolvePrepEnd(order);
    final prepSlotLabel = _resolvePrepSlotLabel(order, l10n);
    final prepCountdownLabel = _resolvePrepCountdownLabel(order, l10n, now);
    final prepTimeLabel = _resolvePrepTimeLabel(order, l10n);
    final deliveredAt =
        _resolveEstimatedDelivery(order) ??
        _resolveScheduledDeliveryTime(order) ??
        start.add(Duration(minutes: prep));
    final status = order.normalizedStatus;

    final preparationCompleted = const {
      'READY_FOR_PICKUP',
      'PICKED_UP',
      'IN_DELIVERY',
      'DELIVERED',
    }.contains(status);

    final outForDeliveryStarted =
        order.isOutForDelivery ||
        const {'READY_FOR_PICKUP', 'PICKED_UP', 'IN_DELIVERY'}.contains(status);

    final outForDeliveryCompleted = order.isDelivered;

    final preparationInProgress =
        order.isAcceptedByPartner &&
        !preparationCompleted &&
        !order.isDelivered &&
        !outForDeliveryStarted;

    final outForDeliveryInProgress =
        outForDeliveryStarted && !outForDeliveryCompleted;

    return [
      _TimelineStep(
        title: l10n.translate('order_tracking_step_order_placed'),
        icon: Icons.receipt_long_rounded,
        completed: true,
        inProgress: false,
        accentColor: OrderStatusPalette.forStatus('PENDING').dot,
        time: start,
        description: l10n.translate('order_tracking_step_order_placed_desc'),
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_partner_confirmation'),
        icon: Icons.storefront_rounded,
        completed: order.isAcceptedByPartner,
        inProgress: !order.isAcceptedByPartner && !order.isDelivered,
        accentColor: OrderStatusPalette.forStatus('CONFIRMED').dot,
        time: acceptedAt,
        description:
            '${l10n.translate('order_tracking_step_partner_confirmation_desc')} | '
            '${l10n.translate('order_tracking_preparation_countdown')}: '
            '$prepCountdownLabel',
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_preparation'),
        icon: Icons.soup_kitchen_rounded,
        completed: preparationCompleted,
        inProgress: preparationInProgress,
        time: preparationCompleted || preparationInProgress
            ? preparingAt
            : null,
        accentColor: OrderStatusPalette.forStatus('PREPARING').dot,
        description:
            '${l10n.translate('order_tracking_preparation_slot')}: $prepSlotLabel | '
            '${l10n.translate('order_tracking_preparation_time')}: $prepTimeLabel',
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_out_for_delivery'),
        icon: Icons.delivery_dining_rounded,
        completed: outForDeliveryCompleted,
        inProgress: outForDeliveryInProgress,
        accentColor: OrderStatusPalette.forStatus('READY').dot,
        time: outForDeliveryInProgress || outForDeliveryCompleted
            ? outAt
            : null,
        description: l10n.translate(
          'order_tracking_step_out_for_delivery_desc',
        ),
      ),
      _TimelineStep(
        title: l10n.translate('order_tracking_step_delivered'),
        icon: Icons.check_circle_rounded,
        completed: order.isDelivered,
        inProgress: false,
        accentColor: OrderStatusPalette.forStatus('DELIVERED').dot,
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
                          Text(
                            l10n.translate('order_tracking_invalid_order_id'),
                          ),
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
                                    Text(
                                      l10n.translate(
                                        'order_tracking_load_error',
                                      ),
                                    ),
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
                                  final prepTimeLabel = _resolvePrepTimeLabel(
                                    order,
                                    l10n,
                                  );
                                  final statusStyle =
                                      OrderStatusPalette.forStatus(
                                        order.normalizedStatus,
                                      );
                                  final estimatedDelivery =
                                      _resolveEstimatedDelivery(order);
                                  final prepSlotLabel = _resolvePrepSlotLabel(
                                    order,
                                    l10n,
                                  );
                                  final now = DateTime.now();
                                  final steps = _stepsForOrder(
                                    order,
                                    l10n,
                                    now,
                                  );

                                  return Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Align(
                                        alignment: Alignment.centerLeft,
                                        child: Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 10,
                                            vertical: 5,
                                          ),
                                          decoration: BoxDecoration(
                                            color: statusStyle.background,
                                            border: Border.all(
                                              color: statusStyle.border,
                                            ),
                                            borderRadius: BorderRadius.circular(
                                              999,
                                            ),
                                          ),
                                          child: Row(
                                            mainAxisSize: MainAxisSize.min,
                                            children: [
                                              Container(
                                                width: 6,
                                                height: 6,
                                                decoration: BoxDecoration(
                                                  color: statusStyle.dot,
                                                  shape: BoxShape.circle,
                                                ),
                                              ),
                                              const SizedBox(width: 5),
                                              Text(
                                                OrderStatusPalette.localizedLabel(
                                                  rawStatus:
                                                      order.normalizedStatus,
                                                  l10n: l10n,
                                                  fallbackLabel:
                                                      order.statusLabel,
                                                ),
                                                style: TextStyle(
                                                  fontSize: 14,
                                                  fontWeight: FontWeight.w700,
                                                  color: statusStyle.foreground,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ),
                                      const SizedBox(height: 14),
                                      Container(
                                        width: double.infinity,
                                        padding: const EdgeInsets.all(16),
                                        decoration: BoxDecoration(
                                          color: AppColors.surface,
                                          borderRadius: BorderRadius.circular(
                                            20,
                                          ),
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
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
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
                                                    maxLines: 1,
                                                    overflow:
                                                        TextOverflow.ellipsis,
                                                    style: const TextStyle(
                                                      fontSize: 17,
                                                      fontWeight:
                                                          FontWeight.w800,
                                                    ),
                                                  ),
                                                ),
                                                const SizedBox(width: 10),
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
                                            Text(
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
                                            const SizedBox(height: 12),
                                            Row(
                                              children: [
                                                Expanded(
                                                  child: _InfoChip(
                                                    icon:
                                                        Icons.schedule_rounded,
                                                    label: l10n.translate(
                                                      'order_tracking_preparation_slot',
                                                    ),
                                                    value: prepSlotLabel,
                                                    secondaryLabel: l10n.translate(
                                                      'order_tracking_preparation_time',
                                                    ),
                                                    secondaryValue:
                                                        prepTimeLabel,
                                                  ),
                                                ),
                                                const SizedBox(width: 10),
                                                Expanded(
                                                  child: _InfoChip(
                                                    icon: Icons
                                                        .local_shipping_rounded,
                                                    label: l10n.translate(
                                                      'order_tracking_estimated_delivery',
                                                    ),
                                                    value: _timeLabel(
                                                      estimatedDelivery,
                                                    ),
                                                  ),
                                                ),
                                              ],
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(height: 16),
                                      Text(
                                        l10n.translate(
                                          'order_tracking_live_process',
                                        ),
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
                                          borderRadius: BorderRadius.circular(
                                            20,
                                          ),
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

                                              return TweenAnimationBuilder<
                                                double
                                              >(
                                                tween: Tween(begin: 0, end: 1),
                                                duration: Duration(
                                                  milliseconds:
                                                      350 + (index * 120),
                                                ),
                                                curve: Curves.easeOutCubic,
                                                builder:
                                                    (context, value, child) {
                                                      return Opacity(
                                                        opacity: value,
                                                        child:
                                                            Transform.translate(
                                                              offset: Offset(
                                                                0,
                                                                (1 - value) *
                                                                    12,
                                                              ),
                                                              child: child,
                                                            ),
                                                      );
                                                    },
                                                child: _TimelineRow(
                                                  step: step,
                                                  isLast: isLast,
                                                  timeLabel: _timeLabel(
                                                    step.time,
                                                  ),
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
  final String? secondaryLabel;
  final String? secondaryValue;

  const _InfoChip({
    required this.icon,
    required this.label,
    required this.value,
    this.secondaryLabel,
    this.secondaryValue,
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
                if (secondaryLabel != null && secondaryValue != null) ...[
                  const SizedBox(height: 2),
                  Text(
                    '$secondaryLabel: $secondaryValue',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TimelineRow extends StatefulWidget {
  final _TimelineStep step;
  final bool isLast;
  final String timeLabel;

  const _TimelineRow({
    required this.step,
    required this.isLast,
    required this.timeLabel,
  });

  @override
  State<_TimelineRow> createState() => _TimelineRowState();
}

class _TimelineRowState extends State<_TimelineRow>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1700),
    );
    _syncPulseState();
  }

  @override
  void didUpdateWidget(covariant _TimelineRow oldWidget) {
    super.didUpdateWidget(oldWidget);
    _syncPulseState();
  }

  void _syncPulseState() {
    if (widget.step.inProgress) {
      if (!_pulseController.isAnimating) {
        _pulseController.repeat();
      }
      return;
    }

    _pulseController.stop();
    _pulseController.value = 0;
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  Widget _buildPulseRing({required double phase, required Color color}) {
    final t = (_pulseController.value + phase) % 1.0;
    final scale = 1 + (t * 1.05);
    final opacity = ((1 - t) * 0.42).clamp(0.0, 0.42);

    return Transform.scale(
      scale: scale,
      child: Container(
        width: 30,
        height: 30,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
            color: color.withValues(alpha: opacity),
            width: 1.8,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final step = widget.step;
    final isLast = widget.isLast;
    final accent = step.completed || step.inProgress
        ? step.accentColor
        : AppColors.secondaryGrey;

    final stepBackground = step.inProgress
        ? accent.withValues(alpha: 0.12)
        : (step.completed
              ? accent.withValues(alpha: 0.08)
              : AppColors.surfaceLight);

    return Padding(
      padding: EdgeInsets.only(bottom: isLast ? 0 : 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              SizedBox(
                width: 36,
                height: 36,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    if (step.inProgress)
                      AnimatedBuilder(
                        animation: _pulseController,
                        builder: (context, child) =>
                            _buildPulseRing(phase: 0, color: accent),
                      ),
                    if (step.inProgress)
                      AnimatedBuilder(
                        animation: _pulseController,
                        builder: (context, child) =>
                            _buildPulseRing(phase: 0.5, color: accent),
                      ),
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
                  ],
                ),
              ),
              if (!isLast)
                AnimatedContainer(
                  duration: const Duration(milliseconds: 450),
                  width: 2,
                  height: 44,
                  color: step.completed || step.inProgress
                      ? accent
                      : AppColors.softGrey,
                ),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 350),
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
              decoration: BoxDecoration(
                color: stepBackground,
                borderRadius: BorderRadius.circular(12),
                border: step.inProgress
                    ? Border.all(color: accent.withValues(alpha: 0.35))
                    : null,
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
                    widget.timeLabel,
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: step.completed || step.inProgress
                          ? accent
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
