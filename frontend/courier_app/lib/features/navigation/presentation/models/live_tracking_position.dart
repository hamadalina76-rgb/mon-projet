import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';

enum GpsPrecisionLevel {
  excellent,
  good,
  weak,
}

class LiveTrackingPosition {
  final double latitude;
  final double longitude;
  final double accuracyMeters;
  final double speedMetersPerSecond;
  final double headingDegrees;
  final int? batteryLevel;
  final DateTime timestamp;

  const LiveTrackingPosition({
    required this.latitude,
    required this.longitude,
    required this.accuracyMeters,
    required this.speedMetersPerSecond,
    required this.headingDegrees,
    required this.batteryLevel,
    required this.timestamp,
  });

  double get speedKmh => (speedMetersPerSecond.clamp(0.0, 120.0)) * 3.6;

  GpsPrecisionLevel get precisionLevel {
    if (accuracyMeters < 5) {
      return GpsPrecisionLevel.excellent;
    }
    if (accuracyMeters <= 15) {
      return GpsPrecisionLevel.good;
    }
    return GpsPrecisionLevel.weak;
  }

  String get precisionLabel {
    switch (precisionLevel) {
      case GpsPrecisionLevel.excellent:
        return 'Excellent';
      case GpsPrecisionLevel.good:
        return 'Bon';
      case GpsPrecisionLevel.weak:
        return 'Faible';
    }
  }

  Color get precisionColor {
    switch (precisionLevel) {
      case GpsPrecisionLevel.excellent:
        return AppColors.success;
      case GpsPrecisionLevel.good:
        return AppColors.info;
      case GpsPrecisionLevel.weak:
        return AppColors.warning;
    }
  }

  String get formattedSpeed => '${speedKmh.toStringAsFixed(1)} km/h';

  bool get hasBatteryLevel => batteryLevel != null;

  static LiveTrackingPosition? fromPayload(Map<String, dynamic> rawPayload) {
    final dynamic payload = rawPayload['payload'];
    if (payload is! Map) {
      return null;
    }

    final lat = _toDouble(payload['lat']);
    final lng = _toDouble(payload['lng']);

    if (lat == null || lng == null) {
      return null;
    }

    final timestampRaw = payload['timestamp']?.toString();

    return LiveTrackingPosition(
      latitude: lat,
      longitude: lng,
      accuracyMeters: (_toDouble(payload['accuracy']) ?? 0).clamp(0.0, 5000.0),
      speedMetersPerSecond: (_toDouble(payload['speed']) ?? 0).clamp(0.0, 120.0),
      headingDegrees: _normalizeHeading(_toDouble(payload['heading']) ?? 0),
      batteryLevel: _toInt(payload['batteryLevel']),
      timestamp: timestampRaw == null
          ? DateTime.now().toUtc()
          : DateTime.tryParse(timestampRaw)?.toUtc() ?? DateTime.now().toUtc(),
    );
  }

  static double _normalizeHeading(double heading) {
    final normalized = heading % 360;
    return normalized < 0 ? normalized + 360 : normalized;
  }

  static double? _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value);
    }
    return null;
  }

  static int? _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.round();
    }
    if (value is String) {
      return int.tryParse(value);
    }
    return null;
  }
}
