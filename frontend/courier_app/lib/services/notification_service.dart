import 'dart:async';
import 'dart:io';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import '../config/di/injection_container.dart';
import '../core/api/api_client.dart';
import '../core/utils/logger.dart';

/// Actions envoyées par le backend (approbation, blocage, etc.)
const String kActionCourierApproved = 'COURIER_APPROVED';
const String kActionCourierSuspended = 'COURIER_SUSPENDED';
const String kActionCourierRejected = 'COURIER_REJECTED';
const String kActionCourierDeactivated = 'COURIER_DEACTIVATED';
const String kActionCourierReactivated = 'COURIER_REACTIVATED';

/// Handles FCM push notifications and registration with backend.
/// Call [initialize] early (e.g. from main), then [registerWithBackend] after login.
class NotificationService {
  static final NotificationService _instance = NotificationService._();
  factory NotificationService() => _instance;

  NotificationService._();

  final FirebaseMessaging _fcm = FirebaseMessaging.instance;
  final FlutterLocalNotificationsPlugin _localNotifications = FlutterLocalNotificationsPlugin();
  final StreamController<void> _profileRefreshedController = StreamController<void>.broadcast();
  Future<void> Function(String action)? _onCourierAction;

  static const AndroidNotificationChannel _channel = AndroidNotificationChannel(
    'speedline_courier_channel',
    'SpeedLine Notifications',
    description: 'Notifications pour compte, commandes et messages',
    importance: Importance.high,
    playSound: true,
    enableVibration: true,
  );

  Future<void> initialize() async {
    await _fcm.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
    );
    const initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: _onNotificationTap,
    );

    if (Platform.isAndroid) {
      await _localNotifications
          .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
          ?.createNotificationChannel(_channel);
    }

    // Foreground: show local notification when message received
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);
    FirebaseMessaging.onMessageOpenedApp.listen(_handleMessageOpenedApp);

    final token = await _fcm.getToken();
    AppLogger.info('FCM Token obtained: ${token != null ? "yes" : "no"}');

    AppLogger.info('Notification service initialized');
  }

  /// Stream écouté par MainNavigationScreen pour rafraîchir l'accès (ex: après approbation).
  Stream<void> get onProfileRefreshed => _profileRefreshedController.stream;

  /// Déclenche un rafraîchissement du profil côté UI (home, etc.).
  void notifyProfileRefreshed() {
    if (!_profileRefreshedController.isClosed) {
      _profileRefreshedController.add(null);
    }
  }

  /// À appeler au démarrage de l'app (ex. dans CourierApp) pour réagir aux notifications compte approuvé/bloqué.
  void setCourierActionHandler(Future<void> Function(String action) handler) {
    _onCourierAction = handler;
  }

  void _onNotificationTap(NotificationResponse response) {
    final payload = response.payload;
    if (payload != null && payload.isNotEmpty) {
      AppLogger.info('Notification tapped: $payload');
      _handleActionFromPayload(payload);
    }
  }

  void _handleForegroundMessage(RemoteMessage message) {
    _handleActionFromData(message.data);
    AppLogger.info('Foreground message: ${message.notification?.title}');
    final notification = message.notification;
    final android = message.notification?.android;
    if (notification != null) {
      _localNotifications.show(
        message.hashCode,
        notification.title ?? 'SpeedLine',
        notification.body ?? '',
        NotificationDetails(
          android: AndroidNotificationDetails(
            _channel.id,
            _channel.name,
            channelDescription: _channel.description,
            importance: Importance.high,
            priority: Priority.high,
            icon: android?.smallIcon ?? '@mipmap/ic_launcher',
          ),
          iOS: const DarwinNotificationDetails(),
        ),
        payload: message.data['action'] ?? message.data.toString(),
      );
    }
  }

  void _handleMessageOpenedApp(RemoteMessage message) {
    AppLogger.info('Message opened app: ${message.data}');
    _handleActionFromData(message.data);
  }

  void _handleActionFromData(Map<String, dynamic> data) {
    final action = data['action']?.toString();
    if (action != null && action.isNotEmpty) _handleActionFromPayload(action);
  }

  void _handleActionFromPayload(String payload) {
    final action = payload.trim();
    if (action != kActionCourierApproved &&
        action != kActionCourierSuspended &&
        action != kActionCourierRejected &&
        action != kActionCourierDeactivated &&
        action != kActionCourierReactivated) return;
    AppLogger.info('Courier status action received: $action');
    _onCourierAction?.call(action).then((_) {
      notifyProfileRefreshed();
    });
  }

  Future<String?> getToken() async {
    return await _fcm.getToken();
  }

  /// Register the current FCM token with the backend so the user receives push (e.g. account approved/blocked).
  /// Call after login with the auth [userId] (from courier.userId or JWT sub).
  Future<void> registerWithBackend(int userId) async {
    final token = await getToken();
    if (token == null || token.isEmpty) {
      AppLogger.warning('No FCM token to register');
      return;
    }
    try {
      final dio = getIt<ApiClient>().dio;
      final deviceType = Platform.isIOS ? 'IOS' : (Platform.isAndroid ? 'ANDROID' : 'WEB');
      await dio.post(
        '/api/notifications/push-token',
        data: {
          'userId': userId,
          'token': token,
          'deviceType': deviceType,
          'deviceId': null,
        },
      );
      AppLogger.info('Push token registered for user $userId');
    } catch (e) {
      AppLogger.warning('Failed to register push token: $e');
    }
  }
}

/// Top-level handler for background FCM messages (app terminated or in background).
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  AppLogger.info('Background message: ${message.notification?.title}');
  // When app is in background/terminated, FCM with notification payload shows system notification automatically.
}
