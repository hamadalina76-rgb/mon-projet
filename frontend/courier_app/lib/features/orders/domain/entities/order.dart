class DeliveryOffer {
  final int orderId;
  final String? orderNumber;
  final String? partnerName;
  final String? pickupAddress;
  final String? dropoffAddress;
  final double deliveryFee;
  final double? partnerLat;
  final double? partnerLon;
  final double? customerLat;
  final double? customerLon;
  final int? etaPickupMin;
  final int? etaDeliveryMin;
  final bool isUrgent;
  final String? dispatchMode;
  final DateTime offeredAt;

  const DeliveryOffer({
    required this.orderId,
    this.orderNumber,
    this.partnerName,
    this.pickupAddress,
    this.dropoffAddress,
    this.deliveryFee = 0.0,
    this.partnerLat,
    this.partnerLon,
    this.customerLat,
    this.customerLon,
    this.etaPickupMin,
    this.etaDeliveryMin,
    this.isUrgent = false,
    this.dispatchMode,
    required this.offeredAt,
  });

  factory DeliveryOffer.fromJson(Map<String, dynamic> json) {
    return DeliveryOffer(
      orderId: _toInt(json['orderId']) ?? 0,
      orderNumber: json['orderNumber'] as String?,
      partnerName: json['partnerName'] as String?,
      pickupAddress: json['pickupAddress'] as String?,
      dropoffAddress: json['dropoffAddress'] as String?,
      deliveryFee: _toDouble(json['deliveryFee']) ?? 0.0,
      partnerLat: _toDouble(json['partnerLat']),
      partnerLon: _toDouble(json['partnerLon']),
      customerLat: _toDouble(json['customerLat']),
      customerLon: _toDouble(json['customerLon']),
      etaPickupMin: _toInt(json['etaPickupMin']),
      etaDeliveryMin: _toInt(json['etaDeliveryMin']),
      isUrgent: json['isUrgent'] as bool? ?? false,
      dispatchMode: json['dispatchMode'] as String?,
      offeredAt: json['offeredAt'] != null
          ? DateTime.tryParse(json['offeredAt'] as String) ?? DateTime.now()
          : DateTime.now(),
    );
  }

  static int? _toInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    if (v is double) return v.toInt();
    return int.tryParse(v.toString());
  }

  static double? _toDouble(dynamic v) {
    if (v == null) return null;
    if (v is double) return v;
    if (v is int) return v.toDouble();
    return double.tryParse(v.toString());
  }
}

class OrderItem {
  final String name;
  final int quantity;
  final double price;

  OrderItem({required this.name, required this.quantity, required this.price});
}
