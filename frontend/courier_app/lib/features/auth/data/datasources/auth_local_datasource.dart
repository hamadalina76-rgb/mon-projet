import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Local data source for authentication
/// Manages secure storage of tokens and user data
abstract class AuthLocalDataSource {
  Future<String?> getAccessToken();
  Future<String?> getRefreshToken();
  Future<void> saveTokens({required String accessToken, required String refreshToken});
  Future<void> clearTokens();
  Future<Map<String, dynamic>?> getCourierData();
  Future<void> saveCourierData(Map<String, dynamic> data);
}

class AuthLocalDataSourceImpl implements AuthLocalDataSource {
  final FlutterSecureStorage storage;

  AuthLocalDataSourceImpl({required this.storage});

  static const String _keyAccessToken = 'access_token';
  static const String _keyRefreshToken = 'refresh_token';
  static const String _keyCourierData = 'courier_data';

  @override
  Future<String?> getAccessToken() async {
    return await storage.read(key: _keyAccessToken);
  }

  @override
  Future<String?> getRefreshToken() async {
    return await storage.read(key: _keyRefreshToken);
  }

  @override
  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    await storage.write(key: _keyAccessToken, value: accessToken);
    await storage.write(key: _keyRefreshToken, value: refreshToken);
  }

  @override
  Future<void> clearTokens() async {
    await storage.delete(key: _keyAccessToken);
    await storage.delete(key: _keyRefreshToken);
    await storage.delete(key: _keyCourierData);
  }

  @override
  Future<Map<String, dynamic>?> getCourierData() async {
    // TODO: Implement JSON parsing
    return null;
  }

  @override
  Future<void> saveCourierData(Map<String, dynamic> data) async {
    // TODO: Implement JSON serialization
  }
}
