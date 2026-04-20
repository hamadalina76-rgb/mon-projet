import '../../domain/entities/order.dart';

class OrderModel extends Order {
  const OrderModel({
    required super.id,
    required super.status,
    super.orderNumber,
    super.partnerId,
    super.partnerName,
    super.statusLabel,
    super.total,
    super.createdAt,
    super.orderTime,
    super.estimatedDeliveryTime,
    super.deliveryTimeMinutes,
    super.isScheduled,
    super.scheduledDeliveryTime,
    super.partnerAcceptedAt,
    super.partnerReadyAt,
    super.suggestedPreparationMinutes,
    super.partnerAcceptedPrepMinutes,
    super.remainingPreparationMinutes,
  });

  factory OrderModel.fromJson(Map<String, dynamic> json) {
    final payload = _extractMap(json);
    final statusHistory = payload['statusHistory'];

    return OrderModel(
      id: (payload['id'] ?? payload['orderId'] ?? '').toString(),
      orderNumber: _nullableString(payload['orderNumber']),
      partnerId: _nullableString(payload['partnerId']),
      partnerName: _nullableString(payload['partnerName']),
      status: (payload['status'] ?? payload['statusLabel'] ?? 'PENDING')
          .toString(),
      statusLabel: _nullableString(payload['statusLabel']),
      total: _toDouble(payload['total']) ?? 0,
      createdAt: _parseDate(payload['createdAt']),
      orderTime: _parseDate(payload['orderTime']),
      estimatedDeliveryTime: _parseDate(payload['estimatedDeliveryTime']),
      deliveryTimeMinutes: _toInt(payload['deliveryTimeMinutes']),
        isScheduled:
          _toBool(payload['isScheduled']) ??
          _parseDate(payload['scheduledDeliveryTime']) != null,
        scheduledDeliveryTime: _parseDate(payload['scheduledDeliveryTime']),
      partnerAcceptedAt:
          _parseDate(payload['partnerAcceptedAt']) ??
          _extractAcceptedTimestamp(statusHistory),
        partnerReadyAt:
          _parseDate(payload['partnerReadyAt']) ??
          _extractReadyTimestamp(statusHistory),
      suggestedPreparationMinutes: _toInt(payload['suggestedPreparationMinutes']),
      partnerAcceptedPrepMinutes:
          _toInt(payload['estimatedPrepMinutes']) ??
          _extractAcceptedPrepMinutes(statusHistory),
      remainingPreparationMinutes:
          _extractRemainingPreparationMinutes(payload, statusHistory),
    );
  }

  static int? _extractRemainingPreparationMinutes(
    Map<String, dynamic> payload,
    dynamic statusHistory,
  ) {
    final directMinutes =
        _toInt(payload['remainingPreparationMinutes']) ??
        _toInt(payload['remainingPrepMinutes']) ??
        _toInt(payload['prepRemainingMinutes']) ??
        _toInt(payload['preparationRemainingMinutes']) ??
        _toInt(payload['preparationCountdownMinutes']) ??
        _toInt(payload['prepCountdownMinutes']) ??
        _toInt(payload['countdownPreparationMinutes']) ??
        _toInt(payload['remainingPreparationTimeMinutes']) ??
        _toInt(payload['remainingPrepTimeMinutes']);

    if (directMinutes != null) {
      return directMinutes < 0 ? 0 : directMinutes;
    }

    final directSeconds =
        _toInt(payload['remainingPreparationSeconds']) ??
        _toInt(payload['remainingPrepSeconds']) ??
        _toInt(payload['prepCountdownSeconds']) ??
        _toInt(payload['preparationCountdownSeconds']) ??
        _toInt(payload['remainingPreparationTimeSeconds']) ??
        _toInt(payload['remainingPrepTimeSeconds']);

    if (directSeconds != null) {
      if (directSeconds <= 0) return 0;
      return (directSeconds / 60).ceil();
    }

    final fromHistory = _extractRemainingPreparationMinutesFromHistory(statusHistory);
    if (fromHistory == null) return null;
    return fromHistory < 0 ? 0 : fromHistory;
  }

  static List<OrderModel> fromCustomerOrdersResponse(dynamic data) {
    final map = _extractMap(data);
    final content = map['content'];

    if (content is List) {
      return content
          .whereType<Map>()
          .map((e) => OrderModel.fromJson(Map<String, dynamic>.from(e)))
          .toList();
    }

    if (data is List) {
      return data
          .whereType<Map>()
          .map((e) => OrderModel.fromJson(Map<String, dynamic>.from(e)))
          .toList();
    }

    if (map.isNotEmpty && map.containsKey('id')) {
      return <OrderModel>[OrderModel.fromJson(map)];
    }

    return const <OrderModel>[];
  }

  static Map<String, dynamic> _extractMap(dynamic raw) {
    if (raw is Map<String, dynamic>) {
      final data = raw['data'];
      if (data is Map<String, dynamic>) {
        return data;
      }
      if (data is Map) {
        return Map<String, dynamic>.from(data);
      }
      return raw;
    }

    if (raw is Map) {
      final map = Map<String, dynamic>.from(raw);
      final data = map['data'];
      if (data is Map) {
        return Map<String, dynamic>.from(data);
      }
      return map;
    }

    return <String, dynamic>{};
  }

  static String? _nullableString(dynamic raw) {
    final value = raw?.toString().trim();
    if (value == null || value.isEmpty) return null;
    return value;
  }

  static DateTime? _parseDate(dynamic raw) {
    final value = raw?.toString().trim();
    if (value == null || value.isEmpty) return null;
    return DateTime.tryParse(value);
  }

  static double? _toDouble(dynamic raw) {
    if (raw is num) return raw.toDouble();
    if (raw == null) return null;
    return double.tryParse(raw.toString());
  }

  static int? _toInt(dynamic raw) {
    if (raw is int) return raw;
    if (raw is num) return raw.toInt();
    if (raw == null) return null;
    return int.tryParse(raw.toString());
  }

  static bool? _toBool(dynamic raw) {
    if (raw is bool) return raw;
    if (raw is num) return raw != 0;
    if (raw == null) return null;

    final normalized = raw.toString().trim().toLowerCase();
    if (normalized == 'true' || normalized == '1') return true;
    if (normalized == 'false' || normalized == '0') return false;
    return null;
  }

  static DateTime? _extractAcceptedTimestamp(dynamic statusHistory) {
    DateTime? best;
    if (statusHistory is! List) return null;

    for (final raw in statusHistory) {
      if (raw is! Map) continue;
      final item = Map<String, dynamic>.from(raw);
      final status = (item['status'] ?? '').toString().trim().toUpperCase();
      if (status != 'PREPARING' && status != 'CONFIRMED') continue;

      final candidate = _parseDate(item['timestamp']);
      if (candidate == null) continue;
      if (best == null || candidate.isBefore(best)) {
        best = candidate;
      }
    }

    return best;
  }

  static int? _extractAcceptedPrepMinutes(dynamic statusHistory) {
    if (statusHistory is! List) return null;

    for (final raw in statusHistory) {
      if (raw is! Map) continue;
      final item = Map<String, dynamic>.from(raw);
      final status = (item['status'] ?? '').toString().trim().toUpperCase();
      if (status != 'PREPARING' && status != 'CONFIRMED') continue;

      final prep = _toInt(item['estimatedPrepMinutes']) ?? _toInt(item['prepMinutes']);
      if (prep != null && prep > 0) return prep;
    }

    return null;
  }

  static int? _extractRemainingPreparationMinutesFromHistory(dynamic statusHistory) {
    if (statusHistory is! List) return null;

    DateTime? latestTimestamp;
    int? latestRemaining;

    for (final raw in statusHistory) {
      if (raw is! Map) continue;
      final item = Map<String, dynamic>.from(raw);
      final timestamp = _parseDate(item['timestamp']);

      final remainingMinutes =
          _toInt(item['remainingPreparationMinutes']) ??
          _toInt(item['remainingPrepMinutes']) ??
          _toInt(item['prepRemainingMinutes']) ??
          _toInt(item['preparationCountdownMinutes']) ??
          _toInt(item['prepCountdownMinutes']);

      int? normalizedRemaining = remainingMinutes;
      if (normalizedRemaining == null) {
        final remainingSeconds =
            _toInt(item['remainingPreparationSeconds']) ??
            _toInt(item['remainingPrepSeconds']) ??
            _toInt(item['prepCountdownSeconds']);
        if (remainingSeconds != null) {
          normalizedRemaining = remainingSeconds <= 0
              ? 0
              : (remainingSeconds / 60).ceil();
        }
      }

      if (normalizedRemaining == null) continue;

      if (latestTimestamp == null ||
          (timestamp != null && timestamp.isAfter(latestTimestamp))) {
        latestTimestamp = timestamp;
        latestRemaining = normalizedRemaining;
      }
    }

    return latestRemaining;
  }

  static DateTime? _extractReadyTimestamp(dynamic statusHistory) {
    if (statusHistory is! List) return null;

    DateTime? bestReadyForPickup;
    DateTime? bestLater;

    for (final raw in statusHistory) {
      if (raw is! Map) continue;
      final item = Map<String, dynamic>.from(raw);
      final status = (item['status'] ?? '').toString().trim().toUpperCase();
      final timestamp = _parseDate(item['timestamp']);
      if (timestamp == null) continue;

      if (status == 'READY_FOR_PICKUP') {
        if (bestReadyForPickup == null || timestamp.isBefore(bestReadyForPickup)) {
          bestReadyForPickup = timestamp;
        }
      }

      if (status == 'PICKED_UP' || status == 'IN_DELIVERY' || status == 'DELIVERED') {
        if (bestLater == null || timestamp.isBefore(bestLater)) {
          bestLater = timestamp;
        }
      }
    }

    return bestReadyForPickup ?? bestLater;
  }
}
