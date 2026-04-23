import 'package:dio/dio.dart';

abstract class OrdersRemoteDataSource {
  Future<void> acceptOffer(int orderId, int courierId);
  Future<void> declineOffer(int orderId, int courierId, String reason);
}

class OrdersRemoteDataSourceImpl implements OrdersRemoteDataSource {
  final Dio dio;

  OrdersRemoteDataSourceImpl({required this.dio});

  @override
  Future<void> acceptOffer(int orderId, int courierId) async {
    await dio.put(
      '/deliveries/by-order/$orderId/accept',
      queryParameters: {'courierId': courierId},
    );
  }

  @override
  Future<void> declineOffer(int orderId, int courierId, String reason) async {
    await dio.put(
      '/deliveries/by-order/$orderId/decline',
      queryParameters: {'courierId': courierId, 'reason': reason},
    );
  }
}
