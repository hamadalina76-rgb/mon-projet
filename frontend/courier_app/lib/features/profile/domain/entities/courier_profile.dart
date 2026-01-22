class CourierProfile {
  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String phone;
  final String? photoUrl;
  final double rating;
  final int totalDeliveries;
  final String? vehicleType;
  final String? vehicleNumber;
  final String? vehicleBrand;
  final String? vehicleModel;
  final String? vehicleColor;
  final List<Document> documents;
  final bool isVerified;
  final bool isOnline;

  CourierProfile({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.phone,
    this.photoUrl,
    required this.rating,
    required this.totalDeliveries,
    this.vehicleType,
    this.vehicleNumber,
    this.vehicleBrand,
    this.vehicleModel,
    this.vehicleColor,
    required this.documents,
    required this.isVerified,
    required this.isOnline,
  });
}

class Document {
  final String id;
  final String type;
  final String url;
  final String status;
  final DateTime uploadedAt;
  final DateTime? verifiedAt;

  Document({
    required this.id,
    required this.type,
    required this.url,
    required this.status,
    required this.uploadedAt,
    this.verifiedAt,
  });
}
