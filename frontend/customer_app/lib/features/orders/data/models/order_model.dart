import '../../domain/entities/order.dart';

class OrderModel extends Order {
  const OrderModel({
    required super.id,
    required super.status,
    super.orderNumber,
    super.partnerName,
    super.statusLabel,
    super.total,
    super.createdAt,
    super.orderTime,
    super.estimatedDeliveryTime,
    super.deliveryTimeMinutes,
  });

  factory OrderModel.fromJson(Map<String, dynamic> json) {
    final payload = _extractMap(json);

    return OrderModel(
      id: (payload['id'] ?? payload['orderId'] ?? '').toString(),
      orderNumber: _nullableString(payload['orderNumber']),
      partnerName: _nullableString(payload['partnerName']),
      status: (payload['status'] ?? payload['statusLabel'] ?? 'PENDING')
          .toString(),
      statusLabel: _nullableString(payload['statusLabel']),
      total: _toDouble(payload['total']) ?? 0,
      createdAt: _parseDate(payload['createdAt']),
      orderTime: _parseDate(payload['orderTime']),
      estimatedDeliveryTime: _parseDate(payload['estimatedDeliveryTime']),
      deliveryTimeMinutes: _toInt(payload['deliveryTimeMinutes']),
    );
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
}
