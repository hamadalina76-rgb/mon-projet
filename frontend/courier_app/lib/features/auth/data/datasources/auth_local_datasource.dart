import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dart:convert';

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
    final raw = await storage.read(key: _keyCourierData);
    if (raw == null || raw.isEmpty) return null;
    try {
      final Map<String, dynamic> data = json.decode(raw) as Map<String, dynamic>;
      return data;
    } catch (e) {
      // If parsing fails, clear the invalid value and return null
      await storage.delete(key: _keyCourierData);
      return null;
    }
  }

  @override
  Future<void> saveCourierData(Map<String, dynamic> data) async {
    try {
      final raw = json.encode(data);
      await storage.write(key: _keyCourierData, value: raw);
    } catch (e) {
      print('❌ Failed to save courier data locally: $e');
    }
  }
}
