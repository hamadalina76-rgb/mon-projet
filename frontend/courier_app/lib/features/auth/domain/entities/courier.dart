class Courier {
  final String id;
  /// Auth user ID (used e.g. for push token registration)
  final String? userId;
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
  final bool isEmailVerified;
  final bool documentsVerified;
  /// Backend status: PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED, AVAILABLE, BUSY, OFFLINE, DEACTIVATED
  final String? status;
  final String? rejectionReason;
  final String? suspensionReason;

  const Courier({
    required this.id,
    this.userId,
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
    this.isEmailVerified = false,
    this.documentsVerified = false,
    this.status,
    this.rejectionReason,
    this.suspensionReason,
  });

  /// True if courier can use the app (home, deliveries, etc.)
  bool get canAccessApp {
    final s = (status ?? '').toUpperCase();
    return s == 'ACTIVE' || s == 'AVAILABLE' || s == 'BUSY' || s == 'OFFLINE';
  }

  bool get isPendingApproval =>
      (status ?? '').toUpperCase() == 'PENDING_APPROVAL';
  bool get isRejected =>
      (status ?? '').toUpperCase() == 'REJECTED';
  bool get isSuspended =>
      (status ?? '').toUpperCase() == 'SUSPENDED';
  bool get isDeactivated =>
      (status ?? '').toUpperCase() == 'DEACTIVATED';

  /// True if account is blocked (cannot use app): suspended, deactivated, or rejected.
  bool get isBlocked => isSuspended || isDeactivated || isRejected;
}
