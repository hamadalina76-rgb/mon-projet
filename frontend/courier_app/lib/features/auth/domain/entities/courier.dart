class Courier {
  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String phone;
  final String? photoUrl;
  final double? rating;
  final int totalDeliveries;
  final String? vehicleType;
  final String? vehicleNumber;
  final bool isOnline;
  final bool isVerified;

  const Courier({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.phone,
    this.photoUrl,
    this.rating,
    this.totalDeliveries = 0,
    this.vehicleType,
    this.vehicleNumber,
    this.isOnline = false,
    this.isVerified = false,
  });
}
