import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import 'app.dart';
import 'config/di/injection_container.dart';
import 'core/utils/logger.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  
  // Load environment variables (optional for now)
  try {
    await dotenv.load(fileName: '.env.production');
  } catch (e) {
    AppLogger.warning('Environment file not found, using defaults');
  }
  
  // Setup dependency injection
  await setupDependencies();
  
  AppLogger.info('App initialized successfully');
  
  // Run app
  runApp(
    const ProviderScope(
      child: CourierApp(),
    ),
  );
}
