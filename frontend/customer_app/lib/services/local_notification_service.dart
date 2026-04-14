import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';

typedef NotificationTapCallback = void Function(String? payload);

class LocalNotificationService {
  LocalNotificationService._();

  static final LocalNotificationService instance = LocalNotificationService._();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  NotificationTapCallback? _onNotificationTap;
  String? _pendingTapPayload;

  @pragma('vm:entry-point')
  static void _onBackgroundTap(NotificationResponse response) {
    // Navigation is handled when the app isolate resumes.
  }

  Future<void> initialize({NotificationTapCallback? onNotificationTap}) async {
    setOnNotificationTapCallback(onNotificationTap);

    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const initializationSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notificationsPlugin.initialize(
      settings: initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
      onDidReceiveBackgroundNotificationResponse: _onBackgroundTap,
    );

    final launchDetails =
        await _notificationsPlugin.getNotificationAppLaunchDetails();
    final didLaunchFromNotification =
        launchDetails?.didNotificationLaunchApp ?? false;
    final payload = launchDetails?.notificationResponse?.payload;
    if (didLaunchFromNotification) {
      _dispatchNotificationTap(payload);
    }
  }

  void setOnNotificationTapCallback(NotificationTapCallback? callback) {
    _onNotificationTap = callback;

    if (_onNotificationTap == null) return;

    final pendingPayload = _pendingTapPayload;
    _pendingTapPayload = null;
    if (pendingPayload != null) {
      _onNotificationTap?.call(pendingPayload);
    }
  }

  Future<void> requestPermission() async {
    final androidPlugin = _notificationsPlugin
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >();
    await androidPlugin?.requestNotificationsPermission();

    final iosPlugin = _notificationsPlugin
        .resolvePlatformSpecificImplementation<
          IOSFlutterLocalNotificationsPlugin
        >();
    await iosPlugin?.requestPermissions(
      alert: true,
      badge: true,
      sound: true,
    );

    if (await Permission.notification.isDenied) {
      await Permission.notification.request();
    }
  }

  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    const androidDetails = AndroidNotificationDetails(
      'speedline_channel',
      'SpeedLine Notifications',
      channelDescription: 'Notifications for SpeedLine orders and deliveries',
      importance: Importance.high,
      priority: Priority.high,
    );

    const iosDetails = DarwinNotificationDetails();

    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      id: id,
      title: title,
      body: body,
      notificationDetails: details,
      payload: payload,
    );
  }

  Future<void> showFromRemoteMessage(RemoteMessage message) async {
    final data = message.data;
    final orderId =
        data['orderId']?.toString() ??
        data['order_id']?.toString() ??
        data['id']?.toString() ??
        '';

    final payload = jsonEncode({
      'orderId': orderId,
      'type': data['type']?.toString(),
      'source': 'fcm',
    });

    final title =
        message.notification?.title ?? data['title']?.toString() ?? 'SpeedLine';
    final body =
        message.notification?.body ?? data['message']?.toString() ?? 'New update';

    final notificationId = _resolveRemoteNotificationId(message, orderId);

    await showNotification(
      id: notificationId,
      title: title,
      body: body,
      payload: payload,
    );
  }

  int _resolveRemoteNotificationId(RemoteMessage message, String orderId) {
    final messageId = message.messageId?.trim();
    final backendNotificationId =
        message.data['notificationId']?.toString().trim();

    final rawId =
        (messageId != null && messageId.isNotEmpty)
            ? messageId
            : (backendNotificationId != null && backendNotificationId.isNotEmpty)
            ? backendNotificationId
            : orderId.trim().isNotEmpty
            ? orderId.trim()
            : DateTime.now().microsecondsSinceEpoch.toString();

    return rawId.hashCode & 0x7fffffff;
  }

  void _onNotificationTapped(NotificationResponse response) {
    _dispatchNotificationTap(response.payload);
  }

  void _dispatchNotificationTap(String? payload) {
    if (_onNotificationTap != null) {
      _onNotificationTap?.call(payload);
      return;
    }

    if (payload != null && payload.trim().isNotEmpty) {
      _pendingTapPayload = payload;
    }
  }
}
