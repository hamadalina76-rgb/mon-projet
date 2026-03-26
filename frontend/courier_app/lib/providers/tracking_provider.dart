import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'location_websocket_provider.dart';
import '../core/storage/secure_storage.dart';
import '../core/utils/permission_utils.dart';
import '../services/background_location_service.dart';
import '../services/connectivity_service.dart';
import '../services/local_notification_service.dart';
import '../services/location_queue_service.dart';
import '../services/tracking_background_runtime_service.dart';
import '../services/tracking_orchestrator_service.dart';
import '../services/tracking_transport.dart';
import '../services/websocket_tracking_transport.dart';

class TrackingState {
  final bool isOnline;
  final bool isBusy;
  final String? blockingMessage;
  final int queuedCount;
  final bool permissionBlocked;
  final DateTime? shiftStartedAt;
  final int? batteryLevel;

  const TrackingState({
    required this.isOnline,
    required this.isBusy,
    this.blockingMessage,
    required this.queuedCount,
    required this.permissionBlocked,
    this.shiftStartedAt,
    this.batteryLevel,
  });

  TrackingState copyWith({
    bool? isOnline,
    bool? isBusy,
    String? blockingMessage,
    bool clearBlockingMessage = false,
    int? queuedCount,
    bool? permissionBlocked,
    DateTime? shiftStartedAt,
    bool clearShiftStartedAt = false,
    int? batteryLevel,
    bool clearBatteryLevel = false,
  }) {
    return TrackingState(
      isOnline: isOnline ?? this.isOnline,
      isBusy: isBusy ?? this.isBusy,
      blockingMessage: clearBlockingMessage ? null : (blockingMessage ?? this.blockingMessage),
      queuedCount: queuedCount ?? this.queuedCount,
      permissionBlocked: permissionBlocked ?? this.permissionBlocked,
      shiftStartedAt:
          clearShiftStartedAt ? null : (shiftStartedAt ?? this.shiftStartedAt),
      batteryLevel: clearBatteryLevel ? null : (batteryLevel ?? this.batteryLevel),
    );
  }

  static const TrackingState initial = TrackingState(
    isOnline: false,
    isBusy: false,
    blockingMessage: null,
    queuedCount: 0,
    permissionBlocked: false,
    shiftStartedAt: null,
    batteryLevel: null,
  );
}

final trackingTransportProvider = Provider<TrackingTransport>((ref) {
  return WebSocketTrackingTransport(ref.read(webSocketServiceProvider));
});

final trackingOrchestratorProvider = Provider<TrackingOrchestratorService>((ref) {
  return TrackingOrchestratorService(
    backgroundLocationService: BackgroundLocationService(),
    transport: ref.read(trackingTransportProvider),
    connectivityService: ConnectivityService(),
    queueService: LocationQueueService(),
    localNotificationService: LocalNotificationService(),
    backgroundRuntimeService: TrackingBackgroundRuntimeService(),
  );
});

class TrackingController extends Notifier<TrackingState> {
  late final TrackingOrchestratorService _orchestrator;

  @override
  TrackingState build() {
    _orchestrator = ref.read(trackingOrchestratorProvider);
    return TrackingState.initial;
  }

  Future<void> setOnline(
    BuildContext context,
    bool online, {
    required bool inDelivery,
  }) async {
    if (state.isBusy && online) {
      return;
    }

    state = state.copyWith(isBusy: true, clearBlockingMessage: true);

    try {
      if (!online) {
        await _orchestrator.stop();
        ref.read(locationWebSocketProvider.notifier).setQueuedCount(0);
        state = state.copyWith(
          isOnline: false,
          clearShiftStartedAt: true,
          clearBatteryLevel: true,
        );
        return;
      }

      final permissionResult = await PermissionUtils.requestTrackingPermissionWithDialog(context);
      if (!permissionResult.granted) {
        state = state.copyWith(
          isOnline: false,
          blockingMessage: permissionResult.message,
          permissionBlocked: true,
        );
        return;
      }

      final jwt = await SecureStorage.read('access_token');
      if (jwt == null || jwt.isEmpty) {
        state = state.copyWith(
          isOnline: false,
          blockingMessage: 'Session invalide. Veuillez vous reconnecter.',
          permissionBlocked: false,
        );
        return;
      }

      await _orchestrator.start(
        jwt: jwt,
        highAccuracy: inDelivery,
        onQueueChanged: (queueSize) {
          ref.read(locationWebSocketProvider.notifier).setQueuedCount(queueSize);
          state = state.copyWith(queuedCount: queueSize);
        },
        onPositionCollected: (payload) {
          ref.read(locationWebSocketProvider.notifier).publishPosition(payload);
          final body = payload['payload'];
          if (body is Map) {
            final rawBattery = body['batteryLevel'];
            final battery = rawBattery is num
                ? rawBattery.round()
                : int.tryParse(rawBattery?.toString() ?? '');
            if (battery != null) {
              state = state.copyWith(batteryLevel: battery);
            }
          }
        },
      );

      state = state.copyWith(
        isOnline: true,
        permissionBlocked: false,
        shiftStartedAt: DateTime.now(),
      );
    } catch (e) {
      state = state.copyWith(
        isOnline: false,
        blockingMessage: 'Impossible de changer le statut: $e',
      );
    } finally {
      state = state.copyWith(isBusy: false);
    }
  }

  Future<void> updateDeliveryMode(bool inDelivery) async {
    await _orchestrator.updateTrackingMode(highAccuracy: inDelivery);
  }
}

final trackingProvider = NotifierProvider<TrackingController, TrackingState>(
  TrackingController.new,
);
