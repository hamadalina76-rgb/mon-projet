import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_background_service/flutter_background_service.dart';

import '../core/utils/logger.dart';

@pragma('vm:entry-point')
void trackingBackgroundOnStart(ServiceInstance service) {
  WidgetsFlutterBinding.ensureInitialized();

  if (service is AndroidServiceInstance) {
    service.setForegroundNotificationInfo(
      title: 'SpeedLine Tracking',
      content: 'Tracking actif en arriere-plan',
    );
  }

  service.on('stop').listen((_) {
    service.stopSelf();
  });

  Timer.periodic(const Duration(minutes: 1), (timer) async {
    if (service is AndroidServiceInstance) {
      await service.setForegroundNotificationInfo(
        title: 'SpeedLine Tracking',
        content: 'Suivi de position actif',
      );
    }
  });
}

class TrackingBackgroundRuntimeService {
  final FlutterBackgroundService _service = FlutterBackgroundService();
  bool _configured = false;

  Future<void> initialize() async {
    if (_configured) {
      return;
    }

    await _service.configure(
      androidConfiguration: AndroidConfiguration(
        onStart: trackingBackgroundOnStart,
        autoStart: false,
        autoStartOnBoot: true,
        isForegroundMode: true,
        foregroundServiceNotificationId: 9001,
        initialNotificationTitle: 'SpeedLine Tracking',
        initialNotificationContent: 'Preparation du tracking',
      ),
      iosConfiguration: IosConfiguration(
        autoStart: false,
        onForeground: trackingBackgroundOnStart,
        onBackground: (_) async => true,
      ),
    );

    _configured = true;
  }

  Future<void> start() async {
    await initialize();
    final isRunning = await _service.isRunning();
    if (!isRunning) {
      await _service.startService();
      AppLogger.info('Background runtime started');
    }
  }

  Future<void> stop() async {
    final isRunning = await _service.isRunning();
    if (isRunning) {
      _service.invoke('stop');
      AppLogger.info('Background runtime stop requested');
    }
  }
}
