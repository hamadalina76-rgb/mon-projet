import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'config/di/injection_container.dart';
import 'core/utils/logger.dart';
import 'services/notification_service.dart';
import 'config/runtime_config.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  
  await RuntimeConfig.load();
  
  // Firebase (FCM) : optionnel. Si google-services.json est absent sur Android, l'app démarre sans push.
  try {
    await Firebase.initializeApp();
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
    await NotificationService().initialize();
  } catch (e) {
    AppLogger.warning('Firebase not configured (push disabled): $e');
  }

  await setupDependencies();
  
  AppLogger.info('App initialized successfully');

  runApp(
    const ProviderScope(
      child: CourierApp(),
    ),
  );
}
