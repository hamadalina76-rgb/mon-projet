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
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final Dio dio;

  AuthRemoteDataSourceImpl({required this.dio});

  @override
  Future<Map<String, dynamic>> login({required String email, required String password}) async {
    final response = await dio.post('/auth/login', data: {
      'email': email,
      'password': password,
      'role': 'COURIER',
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> register({required Map<String, dynamic> data}) async {
    final response = await dio.post('/auth/register/courier', data: data);
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> verifyPhone({required String phone, required String code}) async {
    final response = await dio.post('/auth/verify-phone', data: {
      'phone': phone,
      'code': code,
    });
    return response.data;
  }

  @override
  Future<void> logout() async {
    await dio.post('/auth/logout');
  }

  @override
  Future<Map<String, dynamic>> refreshToken({required String refreshToken}) async {
    final response = await dio.post('/auth/refresh', data: {
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
}
