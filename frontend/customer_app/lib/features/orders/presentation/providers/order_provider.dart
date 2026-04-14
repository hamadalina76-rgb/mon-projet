import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/dependency_injection/injection.dart';
import '../../data/datasources/order_remote_datasource.dart';
import '../../data/repositories/order_repository_impl.dart';
import '../../domain/entities/order.dart';
import '../../domain/repositories/order_repository.dart';

final orderRemoteDataSourceProvider = Provider<OrderRemoteDataSource>((ref) {
	final dio = ref.watch(apiClientProvider).dio;
	return OrderRemoteDataSource(dio: dio);
});

final orderRepositoryProvider = Provider<OrderRepository>((ref) {
	return OrderRepositoryImpl(
		remoteDataSource: ref.watch(orderRemoteDataSourceProvider),
	);
});

final customerOrdersProvider = FutureProvider<List<Order>>((ref) async {
	final user = ref.watch(currentUserProvider);
	if (user == null || user.id.trim().isEmpty) {
		return const <Order>[];
	}

	return ref
			.watch(orderRepositoryProvider)
			.getCustomerOrders(customerId: user.id);
});

final orderByIdProvider = FutureProvider.family<Order, String>((
	ref,
	orderId,
) async {
	final normalizedOrderId = orderId.trim();
	return ref.watch(orderRepositoryProvider).getOrderById(normalizedOrderId);
});

@Deprecated('Use customerOrdersProvider instead.')
final orderProvider = Provider<List<Order>>((ref) {
	return ref.watch(customerOrdersProvider).maybeWhen(
				data: (orders) => orders,
				orElse: () => const <Order>[],
			);
});
