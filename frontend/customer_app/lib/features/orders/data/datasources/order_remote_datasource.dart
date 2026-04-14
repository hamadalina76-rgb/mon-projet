import 'package:dio/dio.dart';

import '../../../../core/api/api_endpoints.dart';
import '../models/order_model.dart';

class OrderRemoteDataSource {
	final Dio _dio;

	OrderRemoteDataSource({required Dio dio}) : _dio = dio;

	Future<List<OrderModel>> fetchCustomerOrders({
		required String customerId,
		int page = 0,
		int size = 20,
	}) async {
		final response = await _dio.get(
			ApiEndpoints.customerOrders(customerId),
			queryParameters: {'page': page, 'size': size},
		);

		return OrderModel.fromCustomerOrdersResponse(response.data);
	}

	Future<OrderModel> fetchOrderById(String orderId) async {
		final response = await _dio.get(ApiEndpoints.orderById(orderId));
		final payload = _extractMap(response.data);
		return OrderModel.fromJson(payload);
	}

	Map<String, dynamic> _extractMap(dynamic data) {
		if (data is Map<String, dynamic>) {
			final nested = data['data'];
			if (nested is Map<String, dynamic>) {
				return nested;
			}
			if (nested is Map) {
				return Map<String, dynamic>.from(nested);
			}
			return data;
		}

		if (data is Map) {
			return Map<String, dynamic>.from(data);
		}

		return <String, dynamic>{};
	}
}
