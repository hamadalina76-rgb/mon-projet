import '../../domain/entities/order.dart';
import '../../domain/repositories/order_repository.dart';
import '../datasources/order_remote_datasource.dart';

class OrderRepositoryImpl implements OrderRepository {
	final OrderRemoteDataSource _remoteDataSource;

	OrderRepositoryImpl({required OrderRemoteDataSource remoteDataSource})
			: _remoteDataSource = remoteDataSource;

	@override
	Future<List<Order>> getCustomerOrders({
		required String customerId,
		int page = 0,
		int size = 20,
	}) async {
		return _remoteDataSource.fetchCustomerOrders(
			customerId: customerId,
			page: page,
			size: size,
		);
	}

	@override
	Future<Order> getOrderById(String orderId) {
		return _remoteDataSource.fetchOrderById(orderId);
	}
}
