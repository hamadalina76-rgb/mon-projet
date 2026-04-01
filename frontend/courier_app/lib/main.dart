import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'app.dart';
import 'config/di/injection_container.dart';
import 'core/utils/logger.dart';
import 'services/notification_service.dart';
import 'config/runtime_config.dart';
import 'services/tracking_workmanager_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  
  await RuntimeConfig.load();
  AppLogger.info(
    'Runtime config loaded: apiBaseUrl=${RuntimeConfig.apiBaseUrl}, wsUrl=${RuntimeConfig.wsUrl}',
  );
  await Hive.initFlutter();

  await setupDependencies();

  runApp(
    const ProviderScope(
      child: CourierApp(),
    ),
  );

  // Non-critical startup tasks run in background to avoid UI blocking/ANR popup.
  unawaited(_postLaunchInitialization());
}

Future<void> _postLaunchInitialization() async {
  // Firebase (FCM) : optionnel. Si google-services.json est absent sur Android, l'app démarre sans push.
  try {
    await Firebase.initializeApp();
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
    await NotificationService().initialize(requestPermissionOnStart: false);
  } catch (e) {
    AppLogger.warning('Firebase not configured (push disabled): $e');
  }

  try {
    await TrackingWorkmanagerService().initialize().timeout(const Duration(seconds: 8));
  } catch (e) {
    AppLogger.warning('Workmanager init skipped at startup: $e');
  }

  AppLogger.info('Post-launch services initialized');
}
