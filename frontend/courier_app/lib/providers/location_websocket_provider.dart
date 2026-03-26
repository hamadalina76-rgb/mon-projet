import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/di/injection_container.dart';
import '../core/storage/secure_storage.dart';
import '../core/utils/logger.dart';
import '../features/auth/data/datasources/auth_local_datasource.dart';
import '../features/auth/data/datasources/auth_remote_datasource.dart';
import '../services/websocket_service.dart';

enum LocationWebSocketStatus {
  connected,
  connecting,
  disconnected,
  error,
}

class LocationWebSocketState {
  final LocationWebSocketStatus status;
  final String? lastError;
  final int queuedCount;

  const LocationWebSocketState({
    required this.status,
    required this.queuedCount,
    this.lastError,
  });

  LocationWebSocketState copyWith({
    LocationWebSocketStatus? status,
    String? lastError,
    bool clearError = false,
    int? queuedCount,
  }) {
    return LocationWebSocketState(
      status: status ?? this.status,
      queuedCount: queuedCount ?? this.queuedCount,
      lastError: clearError ? null : (lastError ?? this.lastError),
    );
  }

  static const LocationWebSocketState initial = LocationWebSocketState(
    status: LocationWebSocketStatus.disconnected,
    queuedCount: 0,
    lastError: null,
  );
}

final webSocketServiceProvider = Provider<WebSocketService>((ref) {
  return WebSocketService(
    accessTokenResolver: _resolveLatestAccessToken,
    tokenRefresher: _refreshAccessToken,
  );
});

class LocationWebSocketController extends Notifier<LocationWebSocketState> {
  StreamSubscription<WebSocketConnectionState>? _wsStateSub;
  final StreamController<Map<String, dynamic>> _positionStreamController =
      StreamController<Map<String, dynamic>>.broadcast();
  Map<String, dynamic>? _lastPublishedPosition;

  @override
  LocationWebSocketState build() {
    final webSocketService = ref.read(webSocketServiceProvider);

    _wsStateSub = webSocketService.connectionStateChanges.listen((event) {
      switch (event.status) {
        case WebSocketConnectionStatus.connected:
          state = state.copyWith(
            status: LocationWebSocketStatus.connected,
            clearError: true,
          );
          break;
        case WebSocketConnectionStatus.connecting:
          state = state.copyWith(status: LocationWebSocketStatus.connecting);
          break;
        case WebSocketConnectionStatus.disconnected:
          state = state.copyWith(status: LocationWebSocketStatus.disconnected);
          break;
        case WebSocketConnectionStatus.error:
          state = state.copyWith(
            status: LocationWebSocketStatus.error,
            lastError: event.errorMessage,
          );
          break;
      }
    });

    ref.onDispose(() async {
      await _wsStateSub?.cancel();
      await _positionStreamController.close();
    });

    return LocationWebSocketState.initial;
  }

  void setQueuedCount(int count) {
    state = state.copyWith(queuedCount: count);
  }

  void publishPosition(Map<String, dynamic> payload) {
    _lastPublishedPosition = payload;
    if (!_positionStreamController.isClosed) {
      _positionStreamController.add(payload);
    }
  }

  Stream<Map<String, dynamic>> get positionStream => _positionStreamController.stream;

  Map<String, dynamic>? get lastPublishedPosition => _lastPublishedPosition;
}

final locationWebSocketProvider =
    NotifierProvider<LocationWebSocketController, LocationWebSocketState>(
  LocationWebSocketController.new,
);

final locationWebSocketPositionStreamProvider = StreamProvider<Map<String, dynamic>>((ref) {
  final controller = ref.watch(locationWebSocketProvider.notifier);
  return controller.positionStream;
});

Future<String?> _resolveLatestAccessToken() {
  return SecureStorage.read('access_token');
}

Future<String?> _refreshAccessToken() async {
  try {
    final local = getIt<AuthLocalDataSource>();
    final remote = getIt<AuthRemoteDataSource>();

    final refreshToken = await local.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return null;
    }

    final response = await remote.refreshToken(refreshToken: refreshToken);
    final accessToken =
        (response['accessToken'] ?? response['access_token'] ?? response['token'])?.toString();
    final nextRefreshToken =
        (response['refreshToken'] ?? response['refresh_token'])?.toString() ?? refreshToken;

    if (accessToken == null || accessToken.isEmpty) {
      return null;
    }

    await local.saveTokens(accessToken: accessToken, refreshToken: nextRefreshToken);
    await SecureStorage.write('access_token', accessToken);
    await SecureStorage.write('refresh_token', nextRefreshToken);
    return accessToken;
  } catch (e) {
    AppLogger.warning('WebSocket token refresh failed: $e');
    return null;
  }
}
