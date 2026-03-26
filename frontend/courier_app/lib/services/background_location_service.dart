import 'dart:async';

import 'package:battery_plus/battery_plus.dart';
import 'package:geolocator/geolocator.dart';

import '../core/constants/app_constants.dart';
import '../core/utils/logger.dart';

class BackgroundLocationService {
  static const Duration _positionFixTimeout = Duration(seconds: 15);

  final Battery _battery;

  StreamSubscription<Position>? _positionStreamSubscription;
  Timer? _tickTimer;
  bool _isTracking = false;
  bool _highAccuracy = false;
  bool _lowBatteryNotified = false;
  DateTime? _lastFallbackAttemptAt;

  DateTime? _lastSentAt;
  DateTime? _lastMovementAt;
  Position? _latestPosition;
  Position? _lastMovementAnchor;

  Future<void> Function(Map<String, dynamic> payload)? _onPositionPayload;
  Future<void> Function(int batteryLevel)? _onBatteryLow;

  BackgroundLocationService({Battery? battery}) : _battery = battery ?? Battery();

  Future<void> startTracking({
    required bool highAccuracy,
    required Future<void> Function(Map<String, dynamic> payload) onPositionPayload,
    Future<void> Function(int batteryLevel)? onBatteryLow,
  }) async {
    if (_isTracking) {
      await setHighAccuracy(highAccuracy);
      return;
    }

    _highAccuracy = highAccuracy;
    _onPositionPayload = onPositionPayload;
    _onBatteryLow = onBatteryLow;
    _isTracking = true;

    await _startPositionStream();
    await _primeInitialPosition();
    _tickTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      _emitIfDue();
    });

    AppLogger.info('Location tracking started (highAccuracy=$_highAccuracy)');
  }

  Future<void> stopTracking() async {
    await _positionStreamSubscription?.cancel();
    _positionStreamSubscription = null;
    _tickTimer?.cancel();
    _tickTimer = null;

    _isTracking = false;
    _lastSentAt = null;
    _lastMovementAt = null;
    _lastMovementAnchor = null;
    _latestPosition = null;
    _lastFallbackAttemptAt = null;
    _lowBatteryNotified = false;

    AppLogger.info('Location tracking stopped');
  }

  Future<void> setHighAccuracy(bool highAccuracy) async {
    if (_highAccuracy == highAccuracy || !_isTracking) {
      _highAccuracy = highAccuracy;
      return;
    }

    _highAccuracy = highAccuracy;
    await _positionStreamSubscription?.cancel();
    await _startPositionStream();
    AppLogger.info('Updated tracking accuracy mode. highAccuracy=$_highAccuracy');
  }

  bool get isTracking => _isTracking;

  Future<void> _startPositionStream() async {
    final settings = LocationSettings(
      accuracy: _highAccuracy ? LocationAccuracy.high : LocationAccuracy.medium,
      distanceFilter: AppConstants.distanceFilter.toInt(),
    );

    _positionStreamSubscription = Geolocator.getPositionStream(
      locationSettings: settings,
    ).listen((position) {
      _latestPosition = position;
      _trackMovement(position);
    }, onError: (Object error) {
      AppLogger.warning('Geolocator stream error: $error');
    });
  }

  Future<void> _primeInitialPosition() async {
    try {
      final lastKnown = await Geolocator.getLastKnownPosition();
      if (lastKnown != null) {
        _latestPosition = lastKnown;
        _trackMovement(lastKnown);
        AppLogger.info('Primed tracking with last known GPS position');
      }
    } catch (e) {
      AppLogger.warning('Unable to read last known position: $e');
    }

    if (_latestPosition != null) {
      return;
    }

    try {
      final current = await Geolocator.getCurrentPosition(
        locationSettings: LocationSettings(
          accuracy: _highAccuracy ? LocationAccuracy.high : LocationAccuracy.medium,
          timeLimit: _positionFixTimeout,
        ),
      );
      _latestPosition = current;
      _trackMovement(current);
      AppLogger.info('Primed tracking with current GPS position');
    } catch (e) {
      AppLogger.warning('Unable to prime current position on tracking start: $e');
    }
  }

  void _trackMovement(Position position) {
    if (_lastMovementAnchor == null) {
      _lastMovementAnchor = position;
      _lastMovementAt = DateTime.now();
      return;
    }

    final movedMeters = Geolocator.distanceBetween(
      _lastMovementAnchor!.latitude,
      _lastMovementAnchor!.longitude,
      position.latitude,
      position.longitude,
    );

    if (movedMeters >= AppConstants.distanceFilter) {
      _lastMovementAt = DateTime.now();
      _lastMovementAnchor = position;
    }
  }

  Future<void> _emitIfDue() async {
    if (!_isTracking || _onPositionPayload == null) {
      return;
    }

    final now = DateTime.now();
    final isIdle = _lastMovementAt != null &&
        now.difference(_lastMovementAt!).inSeconds >= AppConstants.idleDetectionSeconds;
    final targetInterval = isIdle
        ? AppConstants.idleUpdateInterval
        : AppConstants.movingUpdateInterval;

    if (_lastSentAt != null && now.difference(_lastSentAt!).inSeconds < targetInterval) {
      return;
    }

    final position = await _resolvePosition();
    if (position == null) {
      return;
    }

    final batteryLevel = await _safeBatteryLevel();
    if (batteryLevel != null) {
      await _handleBatteryGuardrail(batteryLevel);
    }

    final payload = <String, dynamic>{
      'type': 'POSITION_UPDATE',
      'payload': <String, dynamic>{
        'lat': position.latitude,
        'lng': position.longitude,
        'accuracy': position.accuracy,
        'speed': position.speed,
        'heading': position.heading,
        'batteryLevel': batteryLevel,
        'timestamp': now.toUtc().toIso8601String(),
      },
    };

    _lastSentAt = now;
    try {
      await _onPositionPayload!(payload);
    } catch (e) {
      // Never let timer callback exceptions crash tracking loop.
      AppLogger.error('Failed to dispatch position payload', e);
    }
  }

  Future<Position?> _resolvePosition() async {
    if (_latestPosition != null) {
      return _latestPosition;
    }

    // Reuse cached platform location first to avoid unnecessary active GPS fixes.
    try {
      final lastKnown = await Geolocator.getLastKnownPosition();
      if (lastKnown != null) {
        _latestPosition = lastKnown;
        _trackMovement(lastKnown);
        return lastKnown;
      }
    } catch (e) {
      AppLogger.warning('Unable to read fallback last known position: $e');
    }

    // Avoid repeatedly triggering OS location resolution prompts on emulator/device.
    final now = DateTime.now();
    if (_lastFallbackAttemptAt != null &&
        now.difference(_lastFallbackAttemptAt!).inSeconds < 15) {
      return null;
    }
    _lastFallbackAttemptAt = now;

    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      AppLogger.warning('Location service disabled; waiting for stream position');
      return null;
    }

    try {
      return await Geolocator.getCurrentPosition(
        locationSettings: LocationSettings(
          accuracy: _highAccuracy ? LocationAccuracy.high : LocationAccuracy.medium,
          timeLimit: _positionFixTimeout,
        ),
      );
    } catch (e) {
      AppLogger.warning('Unable to get fallback position: $e');
      return null;
    }
  }

  Future<void> _handleBatteryGuardrail(int batteryLevel) async {
    if (_onBatteryLow == null) {
      return;
    }

    if (batteryLevel < AppConstants.lowBatteryThreshold && !_lowBatteryNotified) {
      _lowBatteryNotified = true;
      await _onBatteryLow!(batteryLevel);
    }

    if (batteryLevel >= AppConstants.lowBatteryThreshold + 2) {
      _lowBatteryNotified = false;
    }
  }

  Future<int?> _safeBatteryLevel() async {
    try {
      return await _battery.batteryLevel;
    } catch (e) {
      AppLogger.warning('Failed to read battery level: $e');
      return null;
    }
  }
}

