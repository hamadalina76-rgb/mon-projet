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
        );

  factory CourierModel.fromJson(Map<String, dynamic> json) {
    return CourierModel(
      id: json['id'] as String,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      email: json['email'] as String,
      phone: json['phone'] as String,
      photoUrl: json['photoUrl'] as String?,
      rating: (json['rating'] as num?)?.toDouble(),
      totalDeliveries: json['totalDeliveries'] as int? ?? 0,
      vehicleType: json['vehicleType'] as String?,
      vehicleNumber: json['vehicleNumber'] as String?,
      isOnline: json['isOnline'] as bool? ?? false,
      isVerified: json['isVerified'] as bool? ?? false,
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
    );
  }
}
