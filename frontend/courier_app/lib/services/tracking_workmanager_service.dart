import 'package:flutter/widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';

import '../core/constants/app_constants.dart';
import '../core/utils/logger.dart';
import 'tracking_background_runtime_service.dart';

const String kTrackingKeepAliveTask = 'tracking_keepalive_task';

@pragma('vm:entry-point')
void trackingWorkmanagerDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    WidgetsFlutterBinding.ensureInitialized();

    final prefs = await SharedPreferences.getInstance();
    final shouldBeOnline = prefs.getBool(AppConstants.trackingOnlinePrefKey) ?? false;

    if (shouldBeOnline) {
      final runtime = TrackingBackgroundRuntimeService();
      await runtime.start();
      AppLogger.info('WorkManager keepalive ensured background runtime');
    }

    return Future.value(true);
  });
}

class TrackingWorkmanagerService {
  Future<void> initialize() async {
    await Workmanager().initialize(trackingWorkmanagerDispatcher);

    await Workmanager().registerPeriodicTask(
      kTrackingKeepAliveTask,
      kTrackingKeepAliveTask,
      frequency: const Duration(minutes: 15),
      existingWorkPolicy: ExistingPeriodicWorkPolicy.update,
      constraints: Constraints(networkType: NetworkType.connected),
    );
  }
}
