import 'package:dio/dio.dart';

class NetworkInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Retry logic here
    super.onError(err, handler);
  }
}
