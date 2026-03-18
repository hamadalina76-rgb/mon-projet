import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:courier_app/providers/location_websocket_provider.dart';
import 'package:courier_app/services/websocket_service.dart';

class FakeWebSocketService extends WebSocketService {
  final StreamController<WebSocketConnectionState> _stateController =
      StreamController<WebSocketConnectionState>.broadcast();

  @override
  Stream<WebSocketConnectionState> get connectionStateChanges =>
      _stateController.stream;

  void emit(WebSocketConnectionStatus status, {String? errorMessage}) {
    _stateController.add(
      WebSocketConnectionState(
        status: status,
        errorMessage: errorMessage,
        reconnectAttempt: 0,
      ),
    );
  }

  Future<void> disposeFake() async {
    await _stateController.close();
  }
}

void main() {
  test('LocationWebSocketProvider maps websocket states correctly', () async {
    final fakeWs = FakeWebSocketService();
    final container = ProviderContainer(
      overrides: [
        webSocketServiceProvider.overrideWithValue(fakeWs),
      ],
    );
    addTearDown(() async {
      await fakeWs.disposeFake();
      container.dispose();
    });

    expect(container.read(locationWebSocketProvider).status,
        LocationWebSocketStatus.disconnected);

    fakeWs.emit(WebSocketConnectionStatus.connecting);
    await Future<void>.delayed(const Duration(milliseconds: 5));
    expect(container.read(locationWebSocketProvider).status,
        LocationWebSocketStatus.connecting);

    fakeWs.emit(WebSocketConnectionStatus.connected);
    await Future<void>.delayed(const Duration(milliseconds: 5));
    expect(container.read(locationWebSocketProvider).status,
        LocationWebSocketStatus.connected);

    fakeWs.emit(WebSocketConnectionStatus.error, errorMessage: 'auth failed');
    await Future<void>.delayed(const Duration(milliseconds: 5));
    final current = container.read(locationWebSocketProvider);
    expect(current.status, LocationWebSocketStatus.error);
    expect(current.lastError, 'auth failed');
  });

  test('LocationWebSocketProvider exposes collected position stream', () async {
    final fakeWs = FakeWebSocketService();
    final container = ProviderContainer(
      overrides: [
        webSocketServiceProvider.overrideWithValue(fakeWs),
      ],
    );
    addTearDown(() async {
      await fakeWs.disposeFake();
      container.dispose();
    });

    final notifier = container.read(locationWebSocketProvider.notifier);
    final future = notifier.positionStream.first;

    notifier.publishPosition({
      'type': 'POSITION_UPDATE',
      'payload': {'lat': 1.0, 'lng': 2.0},
    });

    final payload = await future;
    expect(payload['type'], 'POSITION_UPDATE');
  });
}
