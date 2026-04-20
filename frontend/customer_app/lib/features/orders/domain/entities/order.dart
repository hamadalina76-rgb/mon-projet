class Order {
  final String id;
  final String? orderNumber;
  final String? partnerId;
  final String? partnerName;
  final String status;
  final String? statusLabel;
  final double total;
  final DateTime? createdAt;
  final DateTime? orderTime;
  final DateTime? estimatedDeliveryTime;
  final int? deliveryTimeMinutes;
  final bool isScheduled;
  final DateTime? scheduledDeliveryTime;
  final DateTime? partnerAcceptedAt;
  final DateTime? partnerReadyAt;
  final int? suggestedPreparationMinutes;
  final int? partnerAcceptedPrepMinutes;
  final int? remainingPreparationMinutes;

  const Order({
    required this.id,
    required this.status,
    this.orderNumber,
    this.partnerId,
    this.partnerName,
    this.statusLabel,
    this.total = 0,
    this.createdAt,
    this.orderTime,
    this.estimatedDeliveryTime,
    this.deliveryTimeMinutes,
    this.isScheduled = false,
    this.scheduledDeliveryTime,
    this.partnerAcceptedAt,
    this.partnerReadyAt,
    this.suggestedPreparationMinutes,
    this.partnerAcceptedPrepMinutes,
    this.remainingPreparationMinutes,
  });

  static String normalizeStatus(String value) => value.trim().toUpperCase();

  String get normalizedStatus => normalizeStatus(status);

  bool get sentToPartner => true;

  bool get isAcceptedByPartner => const {
    'CONFIRMED',
    'PREPARING',
    'READY_FOR_PICKUP',
    'PICKED_UP',
    'IN_DELIVERY',
    'DELIVERED',
  }.contains(normalizedStatus);

  bool get hasCourierAssigned => const {
    'READY_FOR_PICKUP',
    'PICKED_UP',
    'IN_DELIVERY',
    'DELIVERED',
  }.contains(normalizedStatus);

  bool get isOutForDelivery =>
      const {'IN_DELIVERY', 'DELIVERED'}.contains(normalizedStatus);

  bool get isDelivered => normalizedStatus == 'DELIVERED';

  bool get isCancelled => normalizedStatus == 'CANCELLED';

  bool get waitingPartnerAcceptance => normalizedStatus == 'PENDING';

  String get displayStatus {
    switch (normalizedStatus) {
      case 'PENDING':
        return 'Awaiting partner confirmation';
      case 'CONFIRMED':
        return 'Order accepted';
      case 'PREPARING':
        return 'Preparing your order';
      case 'READY_FOR_PICKUP':
        return 'Courier assignment in progress';
      case 'PICKED_UP':
        return 'Picked up by courier';
      case 'IN_DELIVERY':
        return 'Out for delivery';
      case 'DELIVERED':
        return 'Delivered';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return statusLabel?.trim().isNotEmpty == true
            ? statusLabel!.trim()
            : normalizedStatus;
    }
  }
}
