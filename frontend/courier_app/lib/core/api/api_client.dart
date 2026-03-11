import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../config/runtime_config.dart';

class ApiClient {
  late final Dio dio;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  ApiClient() {
    dio = Dio(
      BaseOptions(
        baseUrl: RuntimeConfig.apiBaseUrl,
        connectTimeout: Duration(milliseconds: RuntimeConfig.apiTimeoutMs),
        receiveTimeout: Duration(milliseconds: RuntimeConfig.apiTimeoutMs),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Add auth interceptor to include JWT token in requests
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Get token from secure storage
          final token = await _storage.read(key: 'access_token');

          if (token != null && token.isNotEmpty) {
            // Add Authorization header
            options.headers['Authorization'] = 'Bearer $token';
            print('🔑 Added Authorization header to request: ${options.path}');
          } else {
            print('⚠️ No token found for request: ${options.path}');
          }

          return handler.next(options);
        },
        onError: (error, handler) async {
          // Handle 401 Unauthorized - token might be expired
          if (error.response?.statusCode == 401) {
            print('🔒 401 Unauthorized - Token might be expired');
            // TODO: Implement token refresh logic here if needed
          }
          return handler.next(error);
        },
      ),
    );

    // Add logging interceptor for debugging
    dio.interceptors.add(
      LogInterceptor(
        request: true,
        requestHeader: true,
        requestBody: true,
        responseHeader: true,
        responseBody: true,
        error: true,
        logPrint: (obj) => print('🌐 API: $obj'),
      ),
    );
  }
}
