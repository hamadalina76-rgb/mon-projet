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
    print('📦 Parsing LoginResponse from JSON: $json');
    
    // Handle different possible field names from backend
    final accessToken = json['accessToken'] ?? json['access_token'] ?? '';
    final refreshToken = json['refreshToken'] ?? json['refresh_token'] ?? '';
    
    if (accessToken.isEmpty || refreshToken.isEmpty) {
      print('⚠️ Warning: Missing tokens in response');
      print('  accessToken: $accessToken');
      print('  refreshToken: $refreshToken');
    }
    
    // Handle courier data - might be under different keys or in user field
    final courierData = json['courier'] ?? json['user'] ?? json['data'];
    if (courierData == null) {
      print('❌ Error: No courier/user data found in response');
      throw Exception('Missing courier data in backend response');
    }
    
    return LoginResponse(
      accessToken: accessToken as String,
      refreshToken: refreshToken as String,
      courier: CourierModel.fromJson(courierData as Map<String, dynamic>),
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
