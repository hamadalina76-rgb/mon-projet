import 'package:dio/dio.dart';

/// Remote data source for orders
abstract class OrdersRemoteDataSource {
  Future<List<Map<String, dynamic>>> getAvailableOrders({
    double? lat,
    double? lng,
    int? maxDistance,
  });
  Future<Map<String, dynamic>> acceptOrder(String orderId);
  Future<Map<String, dynamic>> rejectOrder(String orderId, String reason);
  Future<Map<String, dynamic>> getOrderDetails(String orderId);
}

class OrdersRemoteDataSourceImpl implements OrdersRemoteDataSource {
  final Dio dio;

  OrdersRemoteDataSourceImpl({required this.dio});

  @override
  Future<List<Map<String, dynamic>>> getAvailableOrders({
    double? lat,
    double? lng,
    int? maxDistance,
  }) async {
    final response = await dio.get('/orders/available', queryParameters: {
      if (lat != null) 'lat': lat,
      if (lng != null) 'lng': lng,
      if (maxDistance != null) 'maxDistance': maxDistance,
    });
    return List<Map<String, dynamic>>.from(response.data);
  }

  @override
  Future<Map<String, dynamic>> acceptOrder(String orderId) async {
    final response = await dio.post('/orders/$orderId/accept');
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> rejectOrder(String orderId, String reason) async {
    final response = await dio.post('/orders/$orderId/reject', data: {
      'reason': reason,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> getOrderDetails(String orderId) async {
    final response = await dio.get('/orders/$orderId');
    return response.data;
  }
}
