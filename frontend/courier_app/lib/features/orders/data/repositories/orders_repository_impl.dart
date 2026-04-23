import '../../domain/repositories/orders_repository.dart';
import '../datasources/orders_remote_datasource.dart';

class OrdersRepositoryImpl implements OrdersRepository {
  final OrdersRemoteDataSource remoteDataSource;

  OrdersRepositoryImpl({required this.remoteDataSource});

  @override
  Future<void> acceptOffer(int orderId, int courierId) {
    return remoteDataSource.acceptOffer(orderId, courierId);
  }

  @override
  Future<void> declineOffer(int orderId, int courierId, String reason) {
    return remoteDataSource.declineOffer(orderId, courierId, reason);
  }
}
