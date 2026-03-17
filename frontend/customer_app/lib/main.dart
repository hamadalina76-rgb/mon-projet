import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
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

final logger = Logger();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

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

  // Initialize SharedPreferences before anything else
  final sharedPreferences = await SharedPreferences.getInstance();
  
  // Setup DI
  setupInjection();

  // Initialize services
  await _initializeServices();

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

    final notificationService = LocalNotificationService();
    await notificationService.initialize();
    await notificationService.requestPermission();

    logger.i('All services initialized successfully');
  } catch (e, stackTrace) {
    logger.e('Service initialization failed: $e\n$stackTrace');
  }
}

class SpeedLineApp extends ConsumerWidget {
  const SpeedLineApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locale = ref.watch(localeProvider);

    return MaterialApp.router(
      title: 'SpeedLine',
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
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
