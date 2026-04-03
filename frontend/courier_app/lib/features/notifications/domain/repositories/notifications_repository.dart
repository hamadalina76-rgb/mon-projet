import '../entities/notification.dart';

abstract class NotificationsRepository {
	Future<List<AppNotification>> fetchNotifications({
		required int userId,
		required int page,
		required int size,
	});

	Future<int> getUnreadCount({required int userId});

	Future<void> markAsRead({required String notificationId});

	Future<void> markAllAsRead({required int userId});
}
