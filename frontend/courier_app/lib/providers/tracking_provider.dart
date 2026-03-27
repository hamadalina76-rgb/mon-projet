import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/di/injection_container.dart';
import 'location_websocket_provider.dart';
import '../core/storage/secure_storage.dart';
import '../core/utils/permission_utils.dart';
import '../features/auth/domain/entities/courier.dart';
import '../features/auth/domain/repositories/auth_repository.dart';
import 'current_courier_provider.dart';
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
  final bool hasActiveDelivery;
  final DateTime? activeDeliveryStartedAt;
  final String? blockingMessage;
  final int queuedCount;
  final bool permissionBlocked;
  final DateTime? shiftStartedAt;
  final int? batteryLevel;

  const TrackingState({
    required this.isOnline,
    required this.isBusy,
    required this.hasActiveDelivery,
    this.activeDeliveryStartedAt,
    this.blockingMessage,
    required this.queuedCount,
    required this.permissionBlocked,
    this.shiftStartedAt,
    this.batteryLevel,
  });

  TrackingState copyWith({
    bool? isOnline,
    bool? isBusy,
    bool? hasActiveDelivery,
    DateTime? activeDeliveryStartedAt,
    bool clearActiveDeliveryStartedAt = false,
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
      hasActiveDelivery: hasActiveDelivery ?? this.hasActiveDelivery,
      activeDeliveryStartedAt: clearActiveDeliveryStartedAt
          ? null
          : (activeDeliveryStartedAt ?? this.activeDeliveryStartedAt),
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
    hasActiveDelivery: false,
    activeDeliveryStartedAt: null,
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
  final ConnectivityService _connectivityService = ConnectivityService();
  bool _bootstrapScheduled = false;
  Timer? _activeDeliveryPollingTimer;
  String? _courierId;

  @override
  TrackingState build() {
    _orchestrator = ref.read(trackingOrchestratorProvider);
    ref.onDispose(() {
      _stopActiveDeliveryPolling();
      unawaited(_connectivityService.dispose());
    });
    if (!_bootstrapScheduled) {
      _bootstrapScheduled = true;
      unawaited(_bootstrapFromPersistedState());
    }
    return TrackingState.initial;
  }

  Future<void> _bootstrapFromPersistedState() async {
    final shouldResume = await _orchestrator.shouldResumeOnlineSession();
    if (!shouldResume || !getIt.isRegistered<AuthRepository>()) {
      return;
    }

    try {
      final authRepository = getIt<AuthRepository>();
      final courier = await authRepository.fetchCourierProfile();
      _courierId = courier.id;
      if (!_isCourierEligible(courier) || !courier.isOnline) {
        return;
      }

      final jwt = await SecureStorage.read('access_token');
      if (jwt == null || jwt.isEmpty) {
        return;
      }

      await _startTrackingPipeline(jwt: jwt, highAccuracy: false);
      state = state.copyWith(
        isOnline: true,
        shiftStartedAt: DateTime.now(),
        permissionBlocked: false,
      );
      await _refreshActiveDeliveryStatus();
      _startActiveDeliveryPolling();
    } catch (_) {
      // Keep default offline state when startup restore fails.
    }
  }

  void _startActiveDeliveryPolling() {
    _activeDeliveryPollingTimer?.cancel();
    _activeDeliveryPollingTimer = Timer.periodic(const Duration(seconds: 15), (_) {
      if (!state.isOnline) {
        return;
      }
      unawaited(_refreshActiveDeliveryStatus());
    });
  }

  void _stopActiveDeliveryPolling() {
    _activeDeliveryPollingTimer?.cancel();
    _activeDeliveryPollingTimer = null;
  }

  Future<void> _refreshActiveDeliveryStatus() async {
    if (!getIt.isRegistered<AuthRepository>() || !state.isOnline) {
      return;
    }

    final authRepository = getIt<AuthRepository>();
    _courierId ??= (await authRepository.fetchCourierProfile()).id;
    if (_courierId == null || _courierId!.isEmpty) {
      return;
    }

    final hasActiveDelivery = await authRepository.hasActiveDelivery(
      courierId: _courierId!,
    );
    final wasActive = state.hasActiveDelivery;
    state = state.copyWith(
      hasActiveDelivery: hasActiveDelivery,
      activeDeliveryStartedAt: hasActiveDelivery && !wasActive
          ? DateTime.now()
          : null,
      clearActiveDeliveryStartedAt: !hasActiveDelivery,
    );
  }

  bool _isCourierEligible(Courier courier) {
    return courier.canAccessApp && courier.documentsVerified && courier.isEmailVerified;
  }

  Future<void> _startTrackingPipeline({
    required String jwt,
    required bool highAccuracy,
  }) async {
    await _orchestrator.start(
      jwt: jwt,
      highAccuracy: highAccuracy,
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
  }

  Future<void> setOnline(
    BuildContext context,
    bool online, {
    required bool inDelivery,
  }) async {
    if (state.isBusy) {
      return;
    }

    state = state.copyWith(isBusy: true, clearBlockingMessage: true);

    try {
      if (!getIt.isRegistered<AuthRepository>()) {
        state = state.copyWith(
          isOnline: false,
          blockingMessage: 'Session invalide. Veuillez vous reconnecter.',
          permissionBlocked: false,
        );
        return;
      }

      final authRepository = getIt<AuthRepository>();
      final courier = await authRepository.fetchCourierProfile();
      final courierId = courier.id;
      _courierId = courierId;

      if (!online) {
        final hasActiveDelivery = await authRepository.hasActiveDelivery(
          courierId: courierId,
        );
        if (hasActiveDelivery) {
          state = state.copyWith(
            isOnline: true,
            hasActiveDelivery: true,
            activeDeliveryStartedAt: state.activeDeliveryStartedAt ?? DateTime.now(),
            blockingMessage: 'Vous avez une livraison en cours. Terminez-la avant de passer hors ligne.',
            permissionBlocked: false,
          );
          return;
        }

        _stopActiveDeliveryPolling();

        await _orchestrator.stop();
        ref.read(locationWebSocketProvider.notifier).setQueuedCount(0);

        try {
          await authRepository.updateAvailability(
            courierId: courierId,
            isOnline: false,
            isAvailable: false,
          );
          ref.invalidate(currentCourierProvider);
          state = state.copyWith(
            isOnline: false,
            hasActiveDelivery: false,
            clearActiveDeliveryStartedAt: true,
            clearShiftStartedAt: true,
            clearBatteryLevel: true,
            permissionBlocked: false,
          );
          return;
        } catch (e) {
          final jwt = await SecureStorage.read('access_token');
          if (jwt != null && jwt.isNotEmpty) {
            await _startTrackingPipeline(jwt: jwt, highAccuracy: false);
          }
          state = state.copyWith(
            isOnline: true,
            blockingMessage: 'Impossible de passer hors ligne: $e',
            permissionBlocked: false,
          );
          _startActiveDeliveryPolling();
          return;
        }
      }

      if (!_isCourierEligible(courier)) {
        state = state.copyWith(
          isOnline: false,
          blockingMessage: 'Compte non approuve ou documents incomplets.',
          permissionBlocked: false,
        );
        return;
      }

      final hasInternet = await _connectivityService.isConnected();
      if (!hasInternet) {
        state = state.copyWith(
          isOnline: false,
          blockingMessage: 'Connexion internet requise pour passer en ligne.',
          permissionBlocked: false,
        );
        return;
      }

      if (!context.mounted) {
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

      await _startTrackingPipeline(jwt: jwt, highAccuracy: inDelivery);

      try {
        await authRepository.updateAvailability(
          courierId: courierId,
          isOnline: true,
          isAvailable: true,
        );
        ref.invalidate(currentCourierProvider);
        await _refreshActiveDeliveryStatus();
        _startActiveDeliveryPolling();
      } catch (e) {
        await _orchestrator.stop();
        ref.read(locationWebSocketProvider.notifier).setQueuedCount(0);
        _stopActiveDeliveryPolling();
        state = state.copyWith(
          isOnline: false,
          hasActiveDelivery: false,
          clearActiveDeliveryStartedAt: true,
          blockingMessage: 'Impossible de synchroniser le statut en ligne: $e',
          permissionBlocked: false,
          clearShiftStartedAt: true,
          clearBatteryLevel: true,
        );
        return;
      }

      state = state.copyWith(
        isOnline: true,
        permissionBlocked: false,
        shiftStartedAt: state.shiftStartedAt ?? DateTime.now(),
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
