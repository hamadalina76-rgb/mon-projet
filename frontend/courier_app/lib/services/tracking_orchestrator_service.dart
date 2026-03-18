import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

import '../core/constants/app_constants.dart';
import '../core/utils/logger.dart';
import 'background_location_service.dart';
import 'connectivity_service.dart';
import 'local_notification_service.dart';
import 'location_queue_service.dart';
import 'tracking_background_runtime_service.dart';
import 'tracking_transport.dart';

class TrackingOrchestratorService {
  final BackgroundLocationService _backgroundLocationService;
  final TrackingTransport _transport;
  final ConnectivityService _connectivityService;
  final LocationQueueService _queueService;
  final LocalNotificationService _localNotificationService;
  final TrackingBackgroundRuntimeService _backgroundRuntimeService;

  StreamSubscription<bool>? _connectivitySub;
  StreamSubscription<bool>? _transportSub;
  bool _isFlushingQueue = false;

  bool _isOnline = false;

  TrackingOrchestratorService({
    required BackgroundLocationService backgroundLocationService,
    required TrackingTransport transport,
    required ConnectivityService connectivityService,
    required LocationQueueService queueService,
    required LocalNotificationService localNotificationService,
    required TrackingBackgroundRuntimeService backgroundRuntimeService,
  })  : _backgroundLocationService = backgroundLocationService,
        _transport = transport,
        _connectivityService = connectivityService,
        _queueService = queueService,
        _localNotificationService = localNotificationService,
        _backgroundRuntimeService = backgroundRuntimeService;

  bool get isOnline => _isOnline;

  Future<void> start({
    required String jwt,
    required bool highAccuracy,
    void Function(int queueSize)? onQueueChanged,
    void Function(Map<String, dynamic> payload)? onPositionCollected,
  }) async {
    if (_isOnline) {
      return;
    }

    await _localNotificationService.initialize();
    await _queueService.initialize();
    await _connectivityService.initialize();
    await _backgroundRuntimeService.start();

    await _localNotificationService.showTrackingActiveNotification();
    await _transport.connect(path: '/ws/location', jwt: jwt);

    _connectivitySub = _connectivityService.connectionChanges.listen((connected) async {
      if (connected) {
        await _flushQueue(onQueueChanged: onQueueChanged);
      }
    });

    _transportSub = _transport.connectionChanges.listen((connected) async {
      if (connected) {
        await _flushQueue(onQueueChanged: onQueueChanged);
      }
    });

    await _backgroundLocationService.startTracking(
      highAccuracy: highAccuracy,
      onPositionPayload: (payload) async {
        onPositionCollected?.call(payload);
        await _sendOrQueue(payload, onQueueChanged: onQueueChanged);
      },
      onBatteryLow: (level) async {
        await _localNotificationService.showLowBatteryNotification(level);
      },
    );

    _isOnline = true;
    await _persistOnline(true);
    onQueueChanged?.call(_queueService.size);
    AppLogger.info('Tracking orchestrator started');
  }

  Future<void> stop() async {
    await _connectivitySub?.cancel();
    await _transportSub?.cancel();
    _connectivitySub = null;
    _transportSub = null;

    await _backgroundLocationService.stopTracking();
    await _transport.disconnect();
    await _backgroundRuntimeService.stop();
    await _localNotificationService.cancelTrackingActiveNotification();

    _isOnline = false;
    await _persistOnline(false);
    AppLogger.info('Tracking orchestrator stopped');
  }

  Future<void> updateTrackingMode({required bool highAccuracy}) async {
    await _backgroundLocationService.setHighAccuracy(highAccuracy);
  }

  Future<void> _sendOrQueue(
    Map<String, dynamic> payload, {
    void Function(int queueSize)? onQueueChanged,
  }) async {
    final hasNetwork = await _connectivityService.isConnected();
    final sent = hasNetwork && _transport.isConnected && _transport.send(payload);

    if (sent) {
      AppLogger.info('Tracking payload sent via WebSocket');
      await _flushQueue(onQueueChanged: onQueueChanged);
      return;
    }

    AppLogger.warning('Tracking payload queued offline (network/ws unavailable)');
    await _queueService.enqueue(payload, maxSize: AppConstants.trackingQueueMaxPoints);
    onQueueChanged?.call(_queueService.size);
  }

  Future<void> _flushQueue({void Function(int queueSize)? onQueueChanged}) async {
    if (_isFlushingQueue) {
      return;
    }
    _isFlushingQueue = true;
    try {
      if (!_transport.isConnected) {
        return;
      }

      final pending = await _queueService.peek(maxItems: AppConstants.trackingQueueFlushBatchSize);
      if (pending.isEmpty) {
        onQueueChanged?.call(0);
        return;
      }

      final keysToRemove = <dynamic>[];
      for (final item in pending) {
        final sent = _transport.send(item.payload);
        if (!sent) {
          break;
        }
        keysToRemove.add(item.key);
      }

      if (keysToRemove.isNotEmpty) {
        await _queueService.removeByKeys(keysToRemove);
        onQueueChanged?.call(_queueService.size);
      }
    } finally {
      _isFlushingQueue = false;
    }
  }

  Future<void> _persistOnline(bool online) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(AppConstants.trackingOnlinePrefKey, online);
  }
}
