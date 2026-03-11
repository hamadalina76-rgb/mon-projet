import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'config/di/injection_container.dart';
import 'core/utils/logger.dart';
import 'config/runtime_config.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  
  await RuntimeConfig.load();
  
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
