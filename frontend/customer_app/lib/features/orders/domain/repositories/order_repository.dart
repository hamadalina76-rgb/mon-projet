import '../entities/order.dart';

abstract class OrderRepository {
	Future<List<Order>> getCustomerOrders({
		required String customerId,
		int page = 0,
		int size = 20,
	});

	Future<Order> getOrderById(String orderId);
}
