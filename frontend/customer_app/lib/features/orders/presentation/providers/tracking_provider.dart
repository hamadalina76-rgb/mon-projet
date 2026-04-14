import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/order.dart';
import 'order_provider.dart';

final trackingProvider = FutureProvider.family<Order, String>((ref, orderId) {
	return ref.watch(orderByIdProvider(orderId).future);
});
