import 'package:hive_flutter/hive_flutter.dart';

import '../core/constants/app_constants.dart';
import '../core/utils/logger.dart';

class QueuedLocationPayload {
  final dynamic key;
  final Map<String, dynamic> payload;

  const QueuedLocationPayload({required this.key, required this.payload});
}

class LocationQueueService {
  static const String _boxName = 'tracking_location_queue';
  Box<dynamic>? _box;

  Future<void> initialize() async {
    _box ??= await Hive.openBox<dynamic>(_boxName);
  }

  int get size => _box?.length ?? 0;

  Future<void> enqueue(
    Map<String, dynamic> payload, {
    int maxSize = AppConstants.trackingQueueMaxPoints,
  }) async {
    await initialize();
    if (_box!.length >= maxSize) {
      final firstKey = _box!.keys.isNotEmpty ? _box!.keys.first : null;
      if (firstKey != null) {
        await _box!.delete(firstKey);
      }
    }
    await _box!.add(payload);
    AppLogger.info('Queued offline location payload. Queue size=${_box!.length}');
  }

  Future<List<QueuedLocationPayload>> peek({int maxItems = AppConstants.trackingQueueFlushBatchSize}) async {
    await initialize();
    final items = <QueuedLocationPayload>[];
    for (final key in _box!.keys.take(maxItems)) {
      final raw = _box!.get(key);
      if (raw is Map) {
        items.add(
          QueuedLocationPayload(
            key: key,
            payload: Map<String, dynamic>.from(raw),
          ),
        );
      }
    }
    return items;
  }

  Future<void> removeByKeys(List<dynamic> keys) async {
    if (keys.isEmpty) {
      return;
    }
    await initialize();
    await _box!.deleteAll(keys);
    AppLogger.info('Removed ${keys.length} queued payload(s). Queue size=${_box!.length}');
  }
}
