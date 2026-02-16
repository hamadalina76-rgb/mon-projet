import '../../domain/entities/courier.dart';

class CourierModel extends Courier {
  const CourierModel({
    required String id,
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    String? photoUrl,
    double? rating,
    int totalDeliveries = 0,
    String? vehicleType,
    String? vehicleNumber,
    bool isOnline = false,
    bool isVerified = false,
    bool documentsVerified = false,
  }) : super(
          id: id,
          firstName: firstName,
          lastName: lastName,
          email: email,
          phone: phone,
          photoUrl: photoUrl,
          rating: rating,
          totalDeliveries: totalDeliveries,
          vehicleType: vehicleType,
          vehicleNumber: vehicleNumber,
          isOnline: isOnline,
          isVerified: isVerified,
          documentsVerified: documentsVerified,
        );

  factory CourierModel.fromJson(Map<String, dynamic> json) {
    print('👤 Parsing CourierModel from JSON: $json');
    return CourierModel(
      id: json['id']?.toString() ?? '',
      firstName: json['firstName'] ?? json['first_name'] ?? '',
      lastName: json['lastName'] ?? json['last_name'] ?? '',
      email: json['email'] ?? '',
      phone: json['phone'] ?? json['phoneNumber'] ?? json['phone_number'] ?? '',
      photoUrl: json['photoUrl'] as String?,
      rating: (json['rating'] as num?)?.toDouble(),
      totalDeliveries: json['totalDeliveries'] as int? ?? 0,
      vehicleType: json['vehicleType'] as String?,
      vehicleNumber: json['vehicleNumber'] as String?,
      isOnline: json['isOnline'] as bool? ?? false,
      isVerified: json['isVerified'] as bool? ?? false,
      documentsVerified: json['documentsVerified'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'phone': phone,
      'photoUrl': photoUrl,
      'rating': rating,
      'totalDeliveries': totalDeliveries,
      'vehicleType': vehicleType,
      'vehicleNumber': vehicleNumber,
      'isOnline': isOnline,
      'isVerified': isVerified,
      'documentsVerified': documentsVerified,
    };
  }

  CourierModel copyWith({
    String? id,
    String? firstName,
    String? lastName,
    String? email,
    String? phone,
    String? photoUrl,
    double? rating,
    int? totalDeliveries,
    String? vehicleType,
    String? vehicleNumber,
    bool? isOnline,
    bool? isVerified,
    bool? documentsVerified,
  }) {
    return CourierModel(
      id: id ?? this.id,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      photoUrl: photoUrl ?? this.photoUrl,
      rating: rating ?? this.rating,
      totalDeliveries: totalDeliveries ?? this.totalDeliveries,
      vehicleType: vehicleType ?? this.vehicleType,
      vehicleNumber: vehicleNumber ?? this.vehicleNumber,
      isOnline: isOnline ?? this.isOnline,
      isVerified: isVerified ?? this.isVerified,
      documentsVerified: documentsVerified ?? this.documentsVerified,
    );
  }
}
