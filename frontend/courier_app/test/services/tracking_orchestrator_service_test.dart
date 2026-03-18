import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:courier_app/core/constants/app_constants.dart';
import 'package:courier_app/services/background_location_service.dart';
import 'package:courier_app/services/connectivity_service.dart';
import 'package:courier_app/services/local_notification_service.dart';
import 'package:courier_app/services/location_queue_service.dart';
import 'package:courier_app/services/tracking_background_runtime_service.dart';
import 'package:courier_app/services/tracking_orchestrator_service.dart';
import 'package:courier_app/services/tracking_transport.dart';

class FakeBackgroundLocationService extends BackgroundLocationService {
  FakeBackgroundLocationService();

  bool started = false;
  bool stopped = false;
  bool highAccuracy = false;

  Future<void> Function(Map<String, dynamic> payload)? onPositionPayload;
  Future<void> Function(int batteryLevel)? onBatteryLow;

  @override
  Future<void> startTracking({
    required bool highAccuracy,
    required Future<void> Function(Map<String, dynamic> payload) onPositionPayload,
    Future<void> Function(int batteryLevel)? onBatteryLow,
  }) async {
    started = true;
    this.highAccuracy = highAccuracy;
    this.onPositionPayload = onPositionPayload;
    this.onBatteryLow = onBatteryLow;
  }

  @override
  Future<void> stopTracking() async {
    stopped = true;
  }

  @override
  Future<void> setHighAccuracy(bool highAccuracy) async {
    this.highAccuracy = highAccuracy;
  }

  Future<void> emitPosition(Map<String, dynamic> payload) async {
    await onPositionPayload?.call(payload);
  }

  Future<void> emitBatteryLow(int level) async {
    await onBatteryLow?.call(level);
  }
}

class FakeTrackingTransport implements TrackingTransport {
  final StreamController<bool> _controller = StreamController<bool>.broadcast();

  bool connected = false;
  bool allowSend = true;
  final List<Map<String, dynamic>> sentPayloads = [];

  @override
  Future<void> connect({required String path, required String jwt}) async {
    connected = true;
    _controller.add(true);
  }

  @override
  Future<void> disconnect() async {
    connected = false;
    _controller.add(false);
  }

  @override
  bool get isConnected => connected;

  @override
  bool send(Map<String, dynamic> payload) {
    if (!connected || !allowSend) {
      return false;
    }
    sentPayloads.add(payload);
    return true;
  }

  @override
  Stream<bool> get connectionChanges => _controller.stream;

  void emitConnection(bool value) {
    connected = value;
    _controller.add(value);
  }
}

class FakeConnectivityService extends ConnectivityService {
  final StreamController<bool> _controller = StreamController<bool>.broadcast();
  bool online;

  FakeConnectivityService({required this.online});

  @override
  Future<void> initialize() async {}

  @override
  Stream<bool> get connectionChanges => _controller.stream;

  @override
  Future<bool> isConnected() async => online;

  void emit(bool value) {
    online = value;
    _controller.add(value);
  }
}

class FakeLocationQueueService extends LocationQueueService {
  final List<QueuedLocationPayload> _items = [];
  int _nextKey = 1;

  @override
  Future<void> initialize() async {}

  @override
  int get size => _items.length;

  @override
  Future<void> enqueue(Map<String, dynamic> payload, {int maxSize = 1000}) async {
    if (_items.length >= maxSize) {
      _items.removeAt(0);
    }
    _items.add(QueuedLocationPayload(key: _nextKey++, payload: payload));
  }

  @override
  Future<List<QueuedLocationPayload>> peek({int maxItems = 100}) async {
    return _items.take(maxItems).toList();
  }

  @override
  Future<void> removeByKeys(List<dynamic> keys) async {
    _items.removeWhere((item) => keys.contains(item.key));
  }
}

class FakeLocalNotificationService extends LocalNotificationService {
  bool initialized = false;
  bool trackingShown = false;
  bool trackingCancelled = false;
  final List<int> lowBatteryLevels = [];

  @override
  Future<void> initialize() async {
    initialized = true;
  }

  @override
  Future<void> showTrackingActiveNotification() async {
    trackingShown = true;
  }

  @override
  Future<void> cancelTrackingActiveNotification() async {
    trackingCancelled = true;
  }

  @override
  Future<void> showLowBatteryNotification(int batteryLevel) async {
    lowBatteryLevels.add(batteryLevel);
  }
}

class FakeTrackingBackgroundRuntimeService extends TrackingBackgroundRuntimeService {
  bool started = false;
  bool stopped = false;

  @override
  Future<void> start() async {
    started = true;
  }

  @override
  Future<void> stop() async {
    stopped = true;
  }
}

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  TrackingOrchestratorService buildOrchestrator({
    required FakeBackgroundLocationService background,
    required FakeTrackingTransport transport,
    required FakeConnectivityService connectivity,
    required FakeLocationQueueService queue,
    required FakeLocalNotificationService notifications,
    required FakeTrackingBackgroundRuntimeService runtime,
  }) {
    return TrackingOrchestratorService(
      backgroundLocationService: background,
      transport: transport,
      connectivityService: connectivity,
      queueService: queue,
      localNotificationService: notifications,
      backgroundRuntimeService: runtime,
    );
  }

  test('queues location payloads when transport/network unavailable and flushes on reconnect', () async {
    final background = FakeBackgroundLocationService();
    final transport = FakeTrackingTransport()
      ..connected = false
      ..allowSend = true;
    final connectivity = FakeConnectivityService(online: false);
    final queue = FakeLocationQueueService();
    final notifications = FakeLocalNotificationService();
    final runtime = FakeTrackingBackgroundRuntimeService();

    final orchestrator = buildOrchestrator(
      background: background,
      transport: transport,
      connectivity: connectivity,
      queue: queue,
      notifications: notifications,
      runtime: runtime,
    );

    await orchestrator.start(jwt: 'jwt', highAccuracy: false);

    const payload = {
      'type': 'POSITION_UPDATE',
      'payload': {'lat': 33.57, 'lng': -7.58}
    };

    await background.emitPosition(payload);
    expect(queue.size, 1);
    expect(transport.sentPayloads, isEmpty);

    connectivity.emit(true);
    transport.emitConnection(true);
    await Future<void>.delayed(const Duration(milliseconds: 20));

    expect(queue.size, 0);
    expect(transport.sentPayloads.length, 1);
    expect(transport.sentPayloads.first['type'], 'POSITION_UPDATE');
  });

  test('start and stop should control runtime, notifications and persisted online status', () async {
    final background = FakeBackgroundLocationService();
    final transport = FakeTrackingTransport()..connected = true;
    final connectivity = FakeConnectivityService(online: true);
    final queue = FakeLocationQueueService();
    final notifications = FakeLocalNotificationService();
    final runtime = FakeTrackingBackgroundRuntimeService();

    final orchestrator = buildOrchestrator(
      background: background,
      transport: transport,
      connectivity: connectivity,
      queue: queue,
      notifications: notifications,
      runtime: runtime,
    );

    await orchestrator.start(jwt: 'jwt', highAccuracy: true);

    expect(orchestrator.isOnline, true);
    expect(background.started, true);
    expect(background.highAccuracy, true);
    expect(runtime.started, true);
    expect(notifications.initialized, true);
    expect(notifications.trackingShown, true);

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool(AppConstants.trackingOnlinePrefKey), true);

    await orchestrator.stop();

    expect(orchestrator.isOnline, false);
    expect(background.stopped, true);
    expect(runtime.stopped, true);
    expect(notifications.trackingCancelled, true);
    expect(prefs.getBool(AppConstants.trackingOnlinePrefKey), false);
  });

  test('forwards low battery callback to notification service', () async {
    final background = FakeBackgroundLocationService();
    final transport = FakeTrackingTransport()..connected = true;
    final connectivity = FakeConnectivityService(online: true);
    final queue = FakeLocationQueueService();
    final notifications = FakeLocalNotificationService();
    final runtime = FakeTrackingBackgroundRuntimeService();

    final orchestrator = buildOrchestrator(
      background: background,
      transport: transport,
      connectivity: connectivity,
      queue: queue,
      notifications: notifications,
      runtime: runtime,
    );

    await orchestrator.start(jwt: 'jwt', highAccuracy: false);
    await background.emitBatteryLow(14);

    expect(notifications.lowBatteryLevels, [14]);
  });

  test('caps offline queue to 50 points when websocket stays disconnected', () async {
    final background = FakeBackgroundLocationService();
    final transport = FakeTrackingTransport()
      ..connected = false
      ..allowSend = false;
    final connectivity = FakeConnectivityService(online: false);
    final queue = FakeLocationQueueService();
    final notifications = FakeLocalNotificationService();
    final runtime = FakeTrackingBackgroundRuntimeService();

    final orchestrator = buildOrchestrator(
      background: background,
      transport: transport,
      connectivity: connectivity,
      queue: queue,
      notifications: notifications,
      runtime: runtime,
    );

    await orchestrator.start(jwt: 'jwt', highAccuracy: false);

    for (var i = 0; i < 60; i++) {
      await background.emitPosition({
        'type': 'POSITION_UPDATE',
        'payload': {'lat': i.toDouble(), 'lng': i.toDouble()},
      });
    }

    expect(queue.size, AppConstants.trackingQueueMaxPoints);
  });
}
