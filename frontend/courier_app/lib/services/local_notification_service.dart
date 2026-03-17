import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class LocalNotificationService {
  static const String _trackingChannelId = 'tracking_channel';
  static const String _batteryChannelId = 'battery_channel';
  static const int _trackingNotificationId = 3001;
  static const int _batteryNotificationId = 3002;

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized) {
      return;
    }

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings();
    const settings = InitializationSettings(android: androidInit, iOS: iosInit);

    await _plugin.initialize(settings);
    _initialized = true;
  }

  Future<void> showTrackingActiveNotification() async {
    await initialize();
    const androidDetails = AndroidNotificationDetails(
      _trackingChannelId,
      'Tracking Actif',
      channelDescription: 'Notification persistante du tracking courier',
      importance: Importance.low,
      priority: Priority.low,
      ongoing: true,
      onlyAlertOnce: true,
    );

    const details = NotificationDetails(
      android: androidDetails,
      iOS: DarwinNotificationDetails(presentAlert: false, presentSound: false),
    );

    await _plugin.show(
      _trackingNotificationId,
      'SpeedLine Tracking',
      'Position partagee en arriere-plan',
      details,
    );
  }

  Future<void> cancelTrackingActiveNotification() async {
    await _plugin.cancel(_trackingNotificationId);
  }

  Future<void> showLowBatteryNotification(int batteryLevel) async {
    await initialize();

    const androidDetails = AndroidNotificationDetails(
      _batteryChannelId,
      'Batterie',
      channelDescription: 'Alerte batterie faible pendant tracking',
      importance: Importance.high,
      priority: Priority.high,
    );

    const details = NotificationDetails(
      android: androidDetails,
      iOS: DarwinNotificationDetails(),
    );

    await _plugin.show(
      _batteryNotificationId,
      'Batterie faible',
      'Niveau batterie $batteryLevel% pendant le tracking',
      details,
    );
  }
}
