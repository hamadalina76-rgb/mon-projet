import '../../domain/entities/notification.dart';
import '../../domain/repositories/notifications_repository.dart';
import '../datasources/notifications_remote_datasource.dart';

class NotificationsRepositoryImpl implements NotificationsRepository {
	final NotificationsRemoteDataSource remoteDataSource;

	NotificationsRepositoryImpl({required this.remoteDataSource});

	@override
	Future<List<AppNotification>> fetchNotifications({
		required int userId,
		required int page,
		required int size,
	}) async {
		final models = await remoteDataSource.fetchNotifications(
			userId: userId,
			page: page,
			size: size,
		);
		return models.map((e) => e.toEntity()).toList();
	}

	@override
	Future<int> getUnreadCount({required int userId}) {
		return remoteDataSource.getUnreadCount(userId: userId);
	}

	@override
	Future<void> markAsRead({required String notificationId}) {
		return remoteDataSource.markAsRead(notificationId: notificationId);
	}

	@override
	Future<void> markAllAsRead({required int userId}) {
		return remoteDataSource.markAllAsRead(userId: userId);
	}
}
