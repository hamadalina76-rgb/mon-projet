import 'package:dio/dio.dart';

/// Remote data source for authentication
/// Handles all API calls related to authentication
abstract class AuthRemoteDataSource {
  Future<Map<String, dynamic>> login({required String email, required String password});
  Future<Map<String, dynamic>> register({required Map<String, dynamic> data});
  Future<Map<String, dynamic>> verifyPhone({required String phone, required String code});
  Future<void> logout();
  Future<Map<String, dynamic>> refreshToken({required String refreshToken});
  Future<Map<String, dynamic>> getCourierProfile();
  Future<Map<String, dynamic>> updateProfile({required Map<String, dynamic> data});
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  });
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final Dio dio;

  AuthRemoteDataSourceImpl({required this.dio});

  @override
  Future<Map<String, dynamic>> login({required String email, required String password}) async {
    final response = await dio.post('/api/v1/auth/login', data: {
      'email': email,
      'password': password,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> register({required Map<String, dynamic> data}) async {
    final response = await dio.post('/api/v1/auth/register', data: data);
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> verifyPhone({required String phone, required String code}) async {
    final response = await dio.post('/api/v1/auth/verify-phone', data: {
      'phone': phone,
      'code': code,
    });
    return response.data;
  }

  @override
  Future<void> logout() async {
    await dio.post('/api/v1/auth/logout');
  }

  @override
  Future<Map<String, dynamic>> refreshToken({required String refreshToken}) async {
    final response = await dio.post('/api/v1/auth/refresh', data: {
      'refreshToken': refreshToken,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> getCourierProfile() async {
    final response = await dio.get('/couriers/profile');
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> updateProfile({required Map<String, dynamic> data}) async {
    final response = await dio.put('/couriers/profile', data: data);
    return response.data;
  }

  @override
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  }) async {
    final formData = FormData();
    
    // Add document data fields
    documentData.forEach((key, value) {
      formData.fields.add(MapEntry(key, value.toString()));
    });
    
    // Add file uploads
    for (var entry in filePaths.entries) {
      formData.files.add(
        MapEntry(
          entry.key,
          await MultipartFile.fromFile(
            entry.value,
            filename: entry.value.split('/').last,
          ),
        ),
      );
    }
    
    // Use PUT /couriers/current_user - API Gateway adds X-User-Id header from JWT
    // Backend finds courier by userId and updates documentation
    await dio.put(
      '/couriers/current_user',
      data: formData,
      options: Options(
        headers: {'Content-Type': 'multipart/form-data'},
      ),
    );
  }
}
