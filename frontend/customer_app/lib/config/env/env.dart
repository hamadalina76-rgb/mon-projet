import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../runtime_config.dart';

enum Environment { development, staging, production }

class Env {
  static Environment get currentEnvironment {
    final env = dotenv.env['APP_ENV'] ?? 'development';
    return Environment.values.firstWhere(
      (e) => e.name == env,
      orElse: () => Environment.development,
    );
  }

  static String get appName => dotenv.env['APP_NAME'] ?? 'SpeedLine Customer';
  static String get apiBaseUrl => RuntimeConfig.apiBaseUrl;
  static String get wsUrl => RuntimeConfig.wsUrl;
  static int get apiTimeout => RuntimeConfig.apiTimeoutMs;
  
  static String get googleMapsApiKey => dotenv.env['GOOGLE_MAPS_API_KEY'] ?? '';
  static String get stripePublishableKey => dotenv.env['STRIPE_PUBLISHABLE_KEY'] ?? '';
  
  static bool get firebaseEnabled => dotenv.env['FIREBASE_ENABLED'] == 'true';
  static bool get enableAnalytics => dotenv.env['ENABLE_ANALYTICS'] == 'true';
  static bool get enableCrashlytics => dotenv.env['ENABLE_CRASHLYTICS'] == 'true';
  static bool get enableDebugLogging => dotenv.env['ENABLE_DEBUG_LOGGING'] == 'true';
  
  static bool get isProduction => currentEnvironment == Environment.production;
  static bool get isDevelopment => currentEnvironment == Environment.development;
  static bool get isStaging => currentEnvironment == Environment.staging;
}
