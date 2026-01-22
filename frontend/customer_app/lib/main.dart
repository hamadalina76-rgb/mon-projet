import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'core/themes/app_theme.dart';
import 'config/routes/app_router.dart';
import 'config/dependency_injection/injection.dart';
import 'services/firebase_service.dart';
import 'services/analytics_service.dart';
import 'services/crash_reporting_service.dart';
import 'services/local_notification_service.dart';
import 'package:logger/logger.dart';

final logger = Logger();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Load environment variables
  await dotenv.load(fileName: '.env.development');
  
  // Setup DI
  setupInjection();

  // Initialize services
  await _initializeServices();

  runApp(
    const ProviderScope(
      child: SpeedLineApp(),
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

class SpeedLineApp extends StatelessWidget {
  const SpeedLineApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'SpeedLine',
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      debugShowCheckedModeBanner: false,
      routerConfig: appRouter,
      localizationsDelegates: const [
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
