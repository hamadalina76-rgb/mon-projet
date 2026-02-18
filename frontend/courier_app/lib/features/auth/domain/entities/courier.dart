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
  final String? vehicleModel;
  final String? vehicleColor;
  final String? identityNumber;
  final String? drivingLicenseNumber;
  final String? drivingLicenseExpiry;
  final String? bankAccountHolder;
  final String? bankIban;
  final String? identityDocumentFrontImage;
  final String? identityDocumentBackImage;
  final String? drivingLicenseImage;
  final bool isOnline;
  final bool isVerified;
  final bool documentsVerified;

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
    this.vehicleModel,
    this.vehicleColor,
    this.identityNumber,
    this.drivingLicenseNumber,
    this.drivingLicenseExpiry,
    this.bankAccountHolder,
    this.bankIban,
    this.identityDocumentFrontImage,
    this.identityDocumentBackImage,
    this.drivingLicenseImage,
    this.isOnline = false,
    this.isVerified = false,
    this.documentsVerified = false,
  });
}
