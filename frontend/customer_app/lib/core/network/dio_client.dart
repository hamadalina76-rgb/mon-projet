import 'package:dio/dio.dart';

class DioClient {
  final Dio _dio;

  DioClient(this._dio) {
    _dio
      ..options.baseUrl = 'https://api.speedline.com'
      ..options.connectTimeout = const Duration(milliseconds: 5000)
      ..options.receiveTimeout = const Duration(milliseconds: 3000);
  }

  Dio get dio => _dio;
}
