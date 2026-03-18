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

  const TrackingState({
    required this.isOnline,
    required this.isBusy,
    this.blockingMessage,
    required this.queuedCount,
    required this.permissionBlocked,
  });

  TrackingState copyWith({
    bool? isOnline,
    bool? isBusy,
    String? blockingMessage,
    bool clearBlockingMessage = false,
    int? queuedCount,
    bool? permissionBlocked,
  }) {
    return TrackingState(
      isOnline: isOnline ?? this.isOnline,
      isBusy: isBusy ?? this.isBusy,
      blockingMessage: clearBlockingMessage ? null : (blockingMessage ?? this.blockingMessage),
      queuedCount: queuedCount ?? this.queuedCount,
      permissionBlocked: permissionBlocked ?? this.permissionBlocked,
    );
  }

  static const TrackingState initial = TrackingState(
    isOnline: false,
    isBusy: false,
    blockingMessage: null,
    queuedCount: 0,
    permissionBlocked: false,
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
    if (state.isBusy) {
      return;
    }

    if (!online) {
      state = state.copyWith(isBusy: true, clearBlockingMessage: true);
      await _orchestrator.stop();
      ref.read(locationWebSocketProvider.notifier).setQueuedCount(0);
      state = state.copyWith(isBusy: false, isOnline: false);
      return;
    }

    state = state.copyWith(isBusy: true, clearBlockingMessage: true);

    final permissionResult = await PermissionUtils.requestTrackingPermissionWithDialog(context);
    if (!permissionResult.granted) {
      state = state.copyWith(
        isBusy: false,
        isOnline: false,
        blockingMessage: permissionResult.message,
        permissionBlocked: true,
      );
      return;
    }

    final jwt = await SecureStorage.read('access_token');
    if (jwt == null || jwt.isEmpty) {
      state = state.copyWith(
        isBusy: false,
        isOnline: false,
        blockingMessage: 'Session invalide. Veuillez vous reconnecter.',
        permissionBlocked: true,
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
      },
    );

    state = state.copyWith(
      isBusy: false,
      isOnline: true,
      permissionBlocked: false,
    );
  }

  Future<void> updateDeliveryMode(bool inDelivery) async {
    await _orchestrator.updateTrackingMode(highAccuracy: inDelivery);
  }
}

final trackingProvider = NotifierProvider<TrackingController, TrackingState>(
  TrackingController.new,
);
