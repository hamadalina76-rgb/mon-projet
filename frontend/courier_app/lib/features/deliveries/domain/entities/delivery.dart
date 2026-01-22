enum DeliveryStatus {
  assigned,
  goingToRestaurant,
  arrivedAtRestaurant,
  pickedUp,
  goingToCustomer,
  arrivedAtCustomer,
  delivered,
  cancelled,
}

class Delivery {
  final String id;
  final String orderId;
  final DeliveryStatus status;
  final String restaurantName;
  final String restaurantAddress;
  final double restaurantLat;
  final double restaurantLng;
  final String customerName;
  final String customerAddress;
  final double customerLat;
  final double customerLng;
  final String? customerPhone;
  final double deliveryFee;
  final DateTime? pickupTime;
  final DateTime? deliveryTime;
  final String? photoUrl;
  final String? signatureUrl;
  final String? verificationCode;

  Delivery({
    required this.id,
    required this.orderId,
    required this.status,
    required this.restaurantName,
    required this.restaurantAddress,
    required this.restaurantLat,
    required this.restaurantLng,
    required this.customerName,
    required this.customerAddress,
    required this.customerLat,
    required this.customerLng,
    this.customerPhone,
    required this.deliveryFee,
    this.pickupTime,
    this.deliveryTime,
    this.photoUrl,
    this.signatureUrl,
    this.verificationCode,
  });
}
