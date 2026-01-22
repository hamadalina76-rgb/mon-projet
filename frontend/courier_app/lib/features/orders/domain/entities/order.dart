class Order {
  final String id;
  final String restaurantName;
  final String restaurantAddress;
  final double restaurantLat;
  final double restaurantLng;
  final String customerName;
  final String customerAddress;
  final double customerLat;
  final double customerLng;
  final double deliveryFee;
  final double distance;
  final int estimatedDuration;
  final DateTime pickupTime;
  final List<OrderItem> items;
  final String status;

  Order({
    required this.id,
    required this.restaurantName,
    required this.restaurantAddress,
    required this.restaurantLat,
    required this.restaurantLng,
    required this.customerName,
    required this.customerAddress,
    required this.customerLat,
    required this.customerLng,
    required this.deliveryFee,
    required this.distance,
    required this.estimatedDuration,
    required this.pickupTime,
    required this.items,
    required this.status,
  });
}

class OrderItem {
  final String name;
  final int quantity;
  final double price;

  OrderItem({
    required this.name,
    required this.quantity,
    required this.price,
  });
}
