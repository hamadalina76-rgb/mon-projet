import 'package:dio/dio.dart';

import '../models/notification_model.dart';

abstract class NotificationsRemoteDataSource {
	Future<List<NotificationModel>> fetchNotifications({
		required int userId,
		required int page,
		required int size,
	});

	Future<int> getUnreadCount({required int userId});

	Future<void> markAsRead({required String notificationId});

	Future<void> markAllAsRead({required int userId});
}

class NotificationsRemoteDataSourceImpl implements NotificationsRemoteDataSource {
	final Dio dio;

	NotificationsRemoteDataSourceImpl({required this.dio});

	@override
	Future<List<NotificationModel>> fetchNotifications({
		required int userId,
		required int page,
		required int size,
	}) async {
		final response = await dio.get(
			'/api/notifications/$userId',
			queryParameters: {
				'page': page,
				'size': size,
			},
		);

		final data = response.data;
		final dynamic content = data is Map<String, dynamic> ? data['content'] : data;

		if (content is! List) {
			return const [];
		}

		return content
				.whereType<Map>()
				.map((item) => NotificationModel.fromJson(Map<String, dynamic>.from(item)))
				.toList();
	}

	@override
	Future<int> getUnreadCount({required int userId}) async {
		final response = await dio.get('/api/notifications/$userId/unread-count');
		final data = response.data;
		if (data is Map<String, dynamic>) {
			final raw = data['count'] ?? data['unreadCount'] ?? 0;
			return int.tryParse(raw.toString()) ?? 0;
		}
		return 0;
	}

	@override
	Future<void> markAsRead({required String notificationId}) async {
		await dio.put('/api/notifications/$notificationId/read');
	}

	@override
	Future<void> markAllAsRead({required int userId}) async {
		await dio.put('/api/notifications/$userId/read-all');
	}
}
