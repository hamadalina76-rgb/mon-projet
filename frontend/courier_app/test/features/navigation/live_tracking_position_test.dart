import 'package:flutter_test/flutter_test.dart';

import 'package:courier_app/features/navigation/presentation/models/live_tracking_position.dart';

void main() {
  group('LiveTrackingPosition', () {
    test('parses payload and computes speed/heading', () {
      final parsed = LiveTrackingPosition.fromPayload({
        'type': 'POSITION_UPDATE',
        'payload': {
          'lat': 36.81,
          'lng': 10.17,
          'accuracy': 3.2,
          'speed': 8.0,
          'heading': 370,
          'batteryLevel': 72,
          'timestamp': '2026-03-18T08:30:00Z',
        },
      });

      expect(parsed, isNotNull);
      expect(parsed!.speedKmh, closeTo(28.8, 0.001));
      expect(parsed.headingDegrees, 10);
      expect(parsed.precisionLabel, 'Excellent');
      expect(parsed.batteryLevel, 72);
    });

    test('classifies GPS precision bands', () {
      final good = LiveTrackingPosition.fromPayload({
        'payload': {
          'lat': 1,
          'lng': 1,
          'accuracy': 5,
          'speed': 0,
          'heading': 0,
        },
      });

      final weak = LiveTrackingPosition.fromPayload({
        'payload': {
          'lat': 1,
          'lng': 1,
          'accuracy': 21,
          'speed': 0,
          'heading': 0,
        },
      });

      expect(good?.precisionLabel, 'Bon');
      expect(weak?.precisionLabel, 'Faible');
    });
  });
}
