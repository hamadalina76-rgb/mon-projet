import 'dart:async';

import 'package:dio/dio.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';

import '../api/api_endpoints.dart';
import '../../services/local_notification_service.dart';

typedef OrderTapCallback = void Function(String orderId);

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  try {
    await Firebase.initializeApp();
  } catch (_) {
    // Firebase may already be initialized.
  }

  final localNotificationService = LocalNotificationService.instance;
  try {
    await localNotificationService.initialize();
  } catch (_) {
    // Service may already be initialized.
  }

  // In background/terminated states, FCM notifications that already contain
  // the "notification" block are displayed by the OS. Showing another local
  // notification here would duplicate it.
  if (message.notification != null) {
    return;
  }

  await localNotificationService.showFromRemoteMessage(message);
}

class FcmService {
  final Logger _logger;
  final Dio _dio;
  final FlutterSecureStorage _secureStorage;
  final LocalNotificationService _localNotificationService;
  final FirebaseMessaging? _injectedFirebaseMessaging;
  FirebaseMessaging? _firebaseMessaging;

  bool _initialized = false;
  bool _firebaseAvailable = false;
  String? _activeUserId;
  OrderTapCallback? _onOrderTap;
  StreamSubscription<String>? _tokenRefreshSubscription;

  FcmService({
    required Logger logger,
    required Dio dio,
    required FlutterSecureStorage secureStorage,
    required LocalNotificationService localNotificationService,
    FirebaseMessaging? firebaseMessaging,
  }) : _logger = logger,
       _dio = dio,
       _secureStorage = secureStorage,
       _localNotificationService = localNotificationService,
       _injectedFirebaseMessaging = firebaseMessaging;

  Future<void> initialize({OrderTapCallback? onOrderTap}) async {
    _onOrderTap = onOrderTap;
    if (_initialized) return;
    _initialized = true;

    try {
      await Firebase.initializeApp();
      _firebaseMessaging = _injectedFirebaseMessaging ?? FirebaseMessaging.instance;
      _firebaseAvailable = true;
    } catch (e, stackTrace) {
      _logger.w('Firebase init skipped: $e');
      _logger.d(stackTrace.toString());
      return;
    }

    final messaging = _firebaseMessaging;
    if (messaging == null) {
      _logger.w('Firebase Messaging unavailable after Firebase initialization.');
      return;
    }

    final settings = await messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );
    _logger.i('FCM permission status: ${settings.authorizationStatus.name}');

    await messaging.setForegroundNotificationPresentationOptions(
      alert: true,
      badge: true,
      sound: true,
    );

    FirebaseMessaging.onMessage.listen((message) async {
      await _localNotificationService.showFromRemoteMessage(message);
    });

    FirebaseMessaging.onMessageOpenedApp.listen(_handleMessageTap);

    final initialMessage = await messaging.getInitialMessage();
    if (initialMessage != null) {
      _handleMessageTap(initialMessage);
    }

    _tokenRefreshSubscription = messaging.onTokenRefresh.listen((token) {
      final userId = _activeUserId;
      if (userId == null || userId.trim().isEmpty) return;
      unawaited(_registerTokenOnServer(userId: userId, token: token));
    });
  }

  Future<void> registerTokenForUser(String? userId) async {
    _activeUserId = userId;
    final messaging = _firebaseMessaging;

    if (
        !_firebaseAvailable ||
        messaging == null ||
        userId == null ||
        userId.trim().isEmpty) {
      return;
    }

    final authToken = await _secureStorage.read(key: 'auth_token');
    if (authToken == null || authToken.trim().isEmpty) {
      _logger.w('Skipping FCM token registration: missing auth token.');
      return;
    }

    final token = await messaging.getToken();
    if (token == null || token.trim().isEmpty) {
      _logger.w('Skipping FCM token registration: device token unavailable.');
      return;
    }

    await _registerTokenOnServer(userId: userId, token: token);
  }

  Future<void> _registerTokenOnServer({
    required String userId,
    required String token,
  }) async {
    final userIdAsInt = int.tryParse(userId);
    if (userIdAsInt == null) {
      _logger.w('Skipping FCM token registration: non numeric userId=$userId');
      return;
    }

    final payload = {
      'userId': userIdAsInt,
      'token': token,
      'deviceType': _deviceType,
      'deviceId': '$userIdAsInt-$_deviceType',
    };

    final endpoints = <String>[
      // Canonical endpoint exposed by notification-service.
      '${ApiEndpoints.NOTIFICATION_BASE}/push-token',
      // Legacy aliases kept for backward compatibility across environments.
      ApiEndpoints.REGISTER_PUSH_TOKEN,
      ApiEndpoints.REGISTER_DEVICE,
    ];

    for (final endpoint in endpoints) {
      try {
        await _dio.post(endpoint, data: payload);
        _logger.i('FCM token registered on $endpoint');
        return;
      } on DioException catch (e) {
        final status = e.response?.statusCode ?? 0;
        _logger.w(
          'FCM token registration failed on $endpoint [$status]: ${e.message}',
        );

        // Continue trying fallback endpoints when one path is unavailable.
        if (status == 401 || status == 403) {
          _logger.w('FCM token registration unauthorized on $endpoint');
        }
      } catch (e) {
        _logger.w('FCM token registration failed on $endpoint: $e');
      }
    }

    _logger.w('FCM token registration failed on all configured endpoints.');
  }

  void _handleMessageTap(RemoteMessage message) {
    final orderId = _extractOrderId(message.data);
    if (orderId == null || orderId.trim().isEmpty) return;
    _onOrderTap?.call(orderId);
  }

  String? _extractOrderId(Map<String, dynamic> data) {
    final orderId =
        data['orderId']?.toString() ??
        data['order_id']?.toString() ??
        data['id']?.toString();

    if (orderId == null || orderId.trim().isEmpty) {
      return null;
    }

    return orderId.trim();
  }

  String get _deviceType {
    if (kIsWeb) return 'WEB';
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return 'ANDROID';
      case TargetPlatform.iOS:
        return 'IOS';
      default:
        return 'UNKNOWN';
    }
  }

  Future<void> dispose() async {
    await _tokenRefreshSubscription?.cancel();
  }
}
