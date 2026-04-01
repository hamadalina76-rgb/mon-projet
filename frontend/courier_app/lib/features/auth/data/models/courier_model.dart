import '../../domain/entities/courier.dart';

class CourierModel extends Courier {
  const CourierModel({
    required String id,
    String? userId,
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    String? photoUrl,
    double? rating,
    int totalDeliveries = 0,
    String? vehicleType,
    String? vehicleNumber,
    String? vehicleModel,
    String? vehicleColor,
    String? identityNumber,
    String? drivingLicenseNumber,
    String? drivingLicenseExpiry,
    String? bankAccountHolder,
    String? bankIban,
    String? identityDocumentFrontImage,
    String? identityDocumentBackImage,
    String? drivingLicenseImage,
    bool isOnline = false,
    bool isVerified = false,
    bool isEmailVerified = false,
    bool documentsVerified = false,
    bool activeDeliverySoundEnabled = false,
    String? status,
    String? courierType,
    String? rejectionReason,
    String? suspensionReason,
  }) : super(
          id: id,
          userId: userId,
          firstName: firstName,
          lastName: lastName,
          email: email,
          phone: phone,
          photoUrl: photoUrl,
          rating: rating,
          totalDeliveries: totalDeliveries,
          vehicleType: vehicleType,
          vehicleNumber: vehicleNumber,
          vehicleModel: vehicleModel,
          vehicleColor: vehicleColor,
          identityNumber: identityNumber,
          drivingLicenseNumber: drivingLicenseNumber,
          drivingLicenseExpiry: drivingLicenseExpiry,
          bankAccountHolder: bankAccountHolder,
          bankIban: bankIban,
          identityDocumentFrontImage: identityDocumentFrontImage,
          identityDocumentBackImage: identityDocumentBackImage,
          drivingLicenseImage: drivingLicenseImage,
          isOnline: isOnline,
          isVerified: isVerified,
          isEmailVerified: isEmailVerified,
          documentsVerified: documentsVerified,
          activeDeliverySoundEnabled: activeDeliverySoundEnabled,
          status: status,
          courierType: courierType,
          rejectionReason: rejectionReason,
          suspensionReason: suspensionReason,
        );

  static bool _resolveActiveDeliverySoundEnabled(Map<String, dynamic> json) {
    final dynamic direct =
        json['activeDeliverySoundEnabled'] ??
        json['deliverySoundEnabled'] ??
        json['soundEnabled'];
    if (direct is bool) {
      return direct;
    }

    final settings = json['settings'];
    if (settings is Map) {
      final dynamic nested = settings['activeDeliverySoundEnabled'] ??
          settings['deliverySoundEnabled'] ??
          settings['soundEnabled'];
      if (nested is bool) {
        return nested;
      }
    }

    return false;
  }

  factory CourierModel.fromJson(Map<String, dynamic> json) {
    print('👤 Parsing CourierModel from JSON: $json');
    return CourierModel(
      id: json['id']?.toString() ?? '',
      userId: json['userId']?.toString(),
      firstName: json['firstName'] ?? json['first_name'] ?? '',
      lastName: json['lastName'] ?? json['last_name'] ?? '',
      email: json['email'] ?? '',
      phone: json['phone'] ?? json['phoneNumber'] ?? json['phone_number'] ?? '',
      photoUrl: json['photoUrl'] ?? json['profilePhoto'] ?? json['profilePicture'] as String?,
      rating: (json['rating'] as num?)?.toDouble(),
      totalDeliveries: json['totalDeliveries'] as int? ?? 0,
      vehicleType: json['vehicleType'] as String?,
      vehicleNumber: json['vehicleNumber'] as String?,
      vehicleModel: json['vehicleModel'] as String?,
      vehicleColor: json['vehicleColor'] as String?,
      identityNumber: json['identityNumber'] as String?,
      drivingLicenseNumber: json['drivingLicenseNumber'] as String?,
      drivingLicenseExpiry: json['drivingLicenseExpiry'] as String?,
      bankAccountHolder: json['bankAccountHolder'] as String?,
      bankIban: json['bankIban'] as String?,
      identityDocumentFrontImage: json['identityDocumentFrontImage'] as String?,
      identityDocumentBackImage: json['identityDocumentBackImage'] as String?,
      drivingLicenseImage: json['drivingLicenseImage'] as String?,
      isOnline: json['isOnline'] as bool? ?? false,
      isVerified: json['isVerified'] as bool? ?? false,
      isEmailVerified: json['isEmailVerified'] as bool? ?? json['emailVerified'] as bool? ?? false,
      documentsVerified: json['documentsVerified'] as bool? ?? false,
      activeDeliverySoundEnabled: _resolveActiveDeliverySoundEnabled(json),
      status: json['status']?.toString(),
      courierType: json['courierType']?.toString(),
      rejectionReason: json['rejectionReason'] as String?,
      suspensionReason: json['suspensionReason'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'phone': phone,
      'photoUrl': photoUrl,
      'rating': rating,
      'totalDeliveries': totalDeliveries,
      'vehicleType': vehicleType,
      'vehicleNumber': vehicleNumber,
      'vehicleModel': vehicleModel,
      'vehicleColor': vehicleColor,
      'identityNumber': identityNumber,
      'drivingLicenseNumber': drivingLicenseNumber,
      'drivingLicenseExpiry': drivingLicenseExpiry,
      'bankAccountHolder': bankAccountHolder,
      'bankIban': bankIban,
      'identityDocumentFrontImage': identityDocumentFrontImage,
      'identityDocumentBackImage': identityDocumentBackImage,
      'drivingLicenseImage': drivingLicenseImage,
      'isOnline': isOnline,
      'isVerified': isVerified,
      'isEmailVerified': isEmailVerified,
      'documentsVerified': documentsVerified,
      'activeDeliverySoundEnabled': activeDeliverySoundEnabled,
      'status': status,
      'courierType': courierType,
      'rejectionReason': rejectionReason,
      'suspensionReason': suspensionReason,
    };
  }

  CourierModel copyWith({
    String? id,
    String? userId,
    String? firstName,
    String? lastName,
    String? email,
    String? phone,
    String? photoUrl,
    double? rating,
    int? totalDeliveries,
    String? vehicleType,
    String? vehicleNumber,
    String? vehicleModel,
    String? vehicleColor,
    String? identityNumber,
    String? drivingLicenseNumber,
    String? drivingLicenseExpiry,
    String? bankAccountHolder,
    String? bankIban,
    String? identityDocumentFrontImage,
    String? identityDocumentBackImage,
    String? drivingLicenseImage,
    bool? isOnline,
    bool? isVerified,
    bool? isEmailVerified,
    bool? documentsVerified,
    bool? activeDeliverySoundEnabledValue,
    String? status,
    String? courierType,
    String? rejectionReason,
    String? suspensionReason,
  }) {
    return CourierModel(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      photoUrl: photoUrl ?? this.photoUrl,
      rating: rating ?? this.rating,
      totalDeliveries: totalDeliveries ?? this.totalDeliveries,
      vehicleType: vehicleType ?? this.vehicleType,
      vehicleNumber: vehicleNumber ?? this.vehicleNumber,
      vehicleModel: vehicleModel ?? this.vehicleModel,
      vehicleColor: vehicleColor ?? this.vehicleColor,
      identityNumber: identityNumber ?? this.identityNumber,
      drivingLicenseNumber: drivingLicenseNumber ?? this.drivingLicenseNumber,
      drivingLicenseExpiry: drivingLicenseExpiry ?? this.drivingLicenseExpiry,
      bankAccountHolder: bankAccountHolder ?? this.bankAccountHolder,
      bankIban: bankIban ?? this.bankIban,
      identityDocumentFrontImage: identityDocumentFrontImage ?? this.identityDocumentFrontImage,
      identityDocumentBackImage: identityDocumentBackImage ?? this.identityDocumentBackImage,
      drivingLicenseImage: drivingLicenseImage ?? this.drivingLicenseImage,
      isOnline: isOnline ?? this.isOnline,
      isVerified: isVerified ?? this.isVerified,
      isEmailVerified: isEmailVerified ?? this.isEmailVerified,
      documentsVerified: documentsVerified ?? this.documentsVerified,
        activeDeliverySoundEnabled:
          activeDeliverySoundEnabledValue ?? activeDeliverySoundEnabled,
      status: status ?? this.status,
        courierType: courierType ?? this.courierType,
      rejectionReason: rejectionReason ?? this.rejectionReason,
        suspensionReason: suspensionReason ?? this.suspensionReason,
    );
  }
}
