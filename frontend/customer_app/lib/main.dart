import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'core/themes/app_theme.dart';
import 'config/routes/app_router.dart';
import 'config/dependency_injection/injection.dart';
import 'core/localization/app_localizations.dart';
import 'core/localization/locale_provider.dart';
import 'core/constants/app_colors.dart';
import 'services/firebase_service.dart';
import 'services/analytics_service.dart';
import 'services/crash_reporting_service.dart';
import 'services/local_notification_service.dart';
import 'package:logger/logger.dart';
import 'config/runtime_config.dart';
import 'config/routes/route_names.dart';
import 'features/orders/presentation/providers/order_notification_provider.dart';
import 'features/orders/presentation/providers/order_provider.dart';
import 'features/auth/domain/entities/user.dart';
import 'core/websocket/websocket_service.dart';
import 'core/notifications/fcm_service.dart';

final logger = Logger();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);

  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Global default status bar style for all screens.
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: AppColors.black,
      statusBarIconBrightness: Brightness.light,
      statusBarBrightness: Brightness.dark,
    ),
  );

  // Initialize Hive (local storage)
  await Hive.initFlutter();

  // Load environment variables
  await dotenv.load(fileName: '.env.development');

  // Load runtime config from assets/config/config.json
  await RuntimeConfig.load();

  // Initialize SharedPreferences before anything else
  final sharedPreferences = await SharedPreferences.getInstance();

  // Setup DI
  setupInjection();

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(sharedPreferences),
      ],
      child: const SpeedLineApp(),
    ),
  );
}

Future<void> _initializeServices() async {
  try {
    final firebaseService = FirebaseService(logger);
    await firebaseService.initialize();

    final analyticsService = AnalyticsService(logger);
    await analyticsService.initialize();

    final crashReportingService = CrashReportingService(logger);
    await crashReportingService.initialize();

    final notificationService = LocalNotificationService.instance;
    await notificationService.initialize();

    logger.i('All services initialized successfully');
  } catch (e, stackTrace) {
    logger.e('Service initialization failed: $e\n$stackTrace');
  }
}

class SpeedLineApp extends ConsumerStatefulWidget {
  const SpeedLineApp({super.key});

  @override
  ConsumerState<SpeedLineApp> createState() => _SpeedLineAppState();
}

class _SpeedLineAppState extends ConsumerState<SpeedLineApp> {
  late final FcmService _fcmService;
  ProviderSubscription<User?>? _authSubscription;
  ProviderSubscription<AsyncValue<RealtimeOrderEvent>>? _orderEventsSubscription;

  @override
  void initState() {
    super.initState();

    _fcmService = FcmService(
      logger: ref.read(loggerProvider),
      dio: ref.read(apiClientProvider).dio,
      secureStorage: ref.read(secureStorageProvider),
      localNotificationService: LocalNotificationService.instance,
    );

    LocalNotificationService.instance.setOnNotificationTapCallback(
      _handleNotificationTap,
    );

    _authSubscription = ref.listenManual<User?>(
      currentUserProvider,
      (previous, next) {
        final previousUserId = previous?.id;
        final nextUserId = next?.id;
        if (previousUserId == nextUserId) {
          return;
        }
        unawaited(_fcmService.registerTokenForUser(nextUserId));
      },
      fireImmediately: true,
    );

    _orderEventsSubscription = ref.listenManual<AsyncValue<RealtimeOrderEvent>>(
      orderNotificationStreamProvider,
      (previous, next) {
        next.whenData((event) {
          _handleRealtimeOrderEvent(event);
        });
      },
    );

    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_bootstrapAsyncServices());
    });
  }

  Future<void> _bootstrapAsyncServices() async {
    await _initializeServices();
    await _initializeFcmNotifications();
  }

  Future<void> _initializeFcmNotifications() async {
    try {
      await LocalNotificationService.instance.requestPermission();

      await _fcmService.initialize(onOrderTap: (orderId) {
        appRouter.go(RouteNames.orderTracking(orderId));
      });

      final currentUser = ref.read(currentUserProvider);
      await _fcmService.registerTokenForUser(currentUser?.id);
    } catch (e, stackTrace) {
      logger.w('FCM initialization skipped: $e\n$stackTrace');
    }
  }

  String? _normalizeOrderId(dynamic value) {
    final orderId = value?.toString().trim();
    if (orderId == null || orderId.isEmpty) {
      return null;
    }
    return orderId;
  }

  String? _extractOrderIdFromPayload(String? payload) {
    final normalizedPayload = payload?.trim();
    if (normalizedPayload == null || normalizedPayload.isEmpty) {
      return null;
    }

    try {
      final decoded = jsonDecode(normalizedPayload);
      if (decoded is Map) {
        final payloadMap = Map<String, dynamic>.from(decoded);
        return _normalizeOrderId(
              payloadMap['orderId'] ?? payloadMap['order_id'] ?? payloadMap['id'],
            ) ??
            _normalizeOrderId(payloadMap['data'] is Map
                ? Map<String, dynamic>.from(
                    payloadMap['data'],
                  )['orderId'] ??
                      Map<String, dynamic>.from(payloadMap['data'])['order_id']
                : null);
      }
    } catch (_) {
      return normalizedPayload;
    }

    return null;
  }

  void _handleNotificationTap(String? payload) {
    final orderId = _extractOrderIdFromPayload(payload);
    if (orderId == null) {
      return;
    }

    appRouter.go(RouteNames.orderTracking(orderId));
  }

  void _handleRealtimeOrderEvent(RealtimeOrderEvent event) {
    ref.invalidate(customerOrdersProvider);

    final orderId = _normalizeOrderId(event.orderId);
    if (orderId != null) {
      ref.invalidate(orderByIdProvider(orderId));
    }
  }

  @override
  void dispose() {
    _authSubscription?.close();
    _orderEventsSubscription?.close();
    unawaited(_fcmService.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final locale = ref.watch(localeProvider);

    return MaterialApp.router(
      title: 'SpeedLine',
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.light,
      debugShowCheckedModeBanner: false,
      routerConfig: appRouter,
      locale: locale,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('en', 'US'),
        Locale('fr', 'FR'),
        Locale('ar', 'SA'),
      ],
    );
  }
}
