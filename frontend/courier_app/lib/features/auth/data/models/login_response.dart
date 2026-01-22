import 'courier_model.dart';

class LoginResponse {
  final String accessToken;
  final String refreshToken;
  final CourierModel courier;

  LoginResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.courier,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      courier: CourierModel.fromJson(json['courier'] as Map<String, dynamic>),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'courier': courier.toJson(),
    };
  }
}
