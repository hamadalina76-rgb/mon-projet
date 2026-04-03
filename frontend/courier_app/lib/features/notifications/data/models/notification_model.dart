import '../../domain/entities/notification.dart';

class NotificationModel {
	final String id;
	final NotificationType type;
	final String title;
	final String message;
	final DateTime timestamp;
	final bool isRead;
	final Map<String, dynamic>? data;

	const NotificationModel({
		required this.id,
		required this.type,
		required this.title,
		required this.message,
		required this.timestamp,
		required this.isRead,
		this.data,
	});

	factory NotificationModel.fromJson(Map<String, dynamic> json) {
		final createdAtRaw =
				json['createdAt'] ?? json['created_at'] ?? json['timestamp'];
		final parsedTimestamp = DateTime.tryParse(createdAtRaw?.toString() ?? '');

		final notificationData = json['data'];
		final safeData = notificationData is Map
				? Map<String, dynamic>.from(notificationData)
				: null;

		final title = (json['title'] ?? '').toString().trim();
		final message = (json['message'] ?? '').toString().trim();

		return NotificationModel(
			id: (json['id'] ?? json['_id'] ?? '').toString(),
			type: _mapType(
				rawType: (json['type'] ?? 'SYSTEM').toString(),
				title: title,
				message: message,
			),
			title: title.isEmpty ? 'Notification' : title,
			message: message,
			timestamp: parsedTimestamp?.toLocal() ?? DateTime.now(),
			isRead: json['isRead'] == true || json['read'] == true,
			data: safeData,
		);
	}

	AppNotification toEntity() {
		return AppNotification(
			id: id,
			type: type,
			title: title,
			message: message,
			timestamp: timestamp,
			isRead: isRead,
			data: data,
		);
	}

	static NotificationType _mapType({
		required String rawType,
		required String title,
		required String message,
	}) {
		final normalized = rawType.toUpperCase();
		final content = '$title $message'.toLowerCase();

		if (content.contains('cancel') || content.contains('annul')) {
			return NotificationType.orderCancelled;
		}

		return switch (normalized) {
			'ORDER' => NotificationType.newOrder,
			'DELIVERY' => NotificationType.newOrder,
			'PAYMENT' => NotificationType.earning,
			'PROMOTION' => NotificationType.bonus,
			'SYSTEM' => NotificationType.system,
			'PARTNER' => NotificationType.system,
			'COURIER' => NotificationType.system,
			_ => NotificationType.system,
		};
	}
}
