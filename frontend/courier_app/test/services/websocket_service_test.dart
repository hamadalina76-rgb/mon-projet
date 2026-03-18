import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:web_socket_channel/status.dart' as ws_status;

import 'package:courier_app/core/constants/app_constants.dart';
import 'package:courier_app/services/websocket_service.dart';

class _WsServerCapture {
  final HttpServer server;
  String? lastAuthorizationHeader;
  String? lastTextMessage;
  int? closeCode;
  String? closeReason;

  _WsServerCapture(this.server);

  String get baseUrl => 'ws://127.0.0.1:${server.port}';
}

Future<_WsServerCapture> _startWsServer() async {
  final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
  final capture = _WsServerCapture(server);

  server.listen((request) async {
    capture.lastAuthorizationHeader = request.headers.value('authorization');
    final socket = await WebSocketTransformer.upgrade(request);
    socket.listen(
      (message) {
        capture.lastTextMessage = message.toString();
      },
      onDone: () {
        capture.closeCode = socket.closeCode;
        capture.closeReason = socket.closeReason;
      },
    );
  });

  return capture;
}

Future<void> _waitFor(bool Function() condition, {Duration timeout = const Duration(seconds: 2)}) async {
  final start = DateTime.now();
  while (!condition()) {
    if (DateTime.now().difference(start) > timeout) {
      fail('Condition not reached within ${timeout.inMilliseconds}ms');
    }
    await Future<void>.delayed(const Duration(milliseconds: 20));
  }
}

void main() {
  test('reconnect backoff follows 1,2,4,8 and caps at 30 seconds', () {
    final delays = List<int>.generate(8, WebSocketService.reconnectDelayForAttempt);
    expect(delays, [1, 2, 4, 8, 16, 30, 30, 30]);
  });

  test('heartbeat interval constant is 25 seconds', () {
    expect(AppConstants.webSocketHeartbeatIntervalSeconds, 25);
  });

  test('send returns false when disconnected', () {
    final service = WebSocketService();
    final sent = service.send({'type': 'POSITION_UPDATE'});
    expect(sent, isFalse);
  });

  test('connect sends JWT in Authorization header and position reaches server', () async {
    final serverCapture = await _startWsServer();
    final service = WebSocketService(wsBaseUrlOverride: serverCapture.baseUrl);
    addTearDown(() async {
      await service.disconnect();
      await serverCapture.server.close(force: true);
    });

    await service.connect(path: '/ws/location', jwt: 'jwt-token-123');
    await _waitFor(() => serverCapture.lastAuthorizationHeader != null);

    expect(serverCapture.lastAuthorizationHeader, 'Bearer jwt-token-123');

    final sent = service.send({
      'type': 'POSITION_UPDATE',
      'payload': {'lat': 36.8, 'lng': 10.1},
    });
    expect(sent, isTrue);

    await _waitFor(() => serverCapture.lastTextMessage != null);
    final decoded = jsonDecode(serverCapture.lastTextMessage!);
    expect(decoded['type'], 'POSITION_UPDATE');
  });

  test('refreshes JWT after auth error and reconnects automatically', () async {
    final serverCapture = await _startWsServer();
    String currentToken = 'expired-token';
    int refreshCalls = 0;
    int connectorCalls = 0;

    final service = WebSocketService(
      wsBaseUrlOverride: serverCapture.baseUrl,
      accessTokenResolver: () async => currentToken,
      tokenRefresher: () async {
        refreshCalls++;
        currentToken = 'fresh-token';
        return currentToken;
      },
      socketConnector: (url, {headers, compression = CompressionOptions.compressionDefault}) async {
        connectorCalls++;
        if (connectorCalls == 1) {
          throw Exception('401 Unauthorized');
        }
        return WebSocket.connect(url, headers: headers, compression: compression);
      },
    );

    addTearDown(() async {
      await service.disconnect();
      await serverCapture.server.close(force: true);
    });

    await service.connect(path: '/ws/location', jwt: 'expired-token');
    await _waitFor(() => service.isConnected);
    await _waitFor(() => serverCapture.lastAuthorizationHeader != null);

    expect(refreshCalls, 1);
    expect(serverCapture.lastAuthorizationHeader, 'Bearer fresh-token');
  });

  test('applies 25s ping interval and closes websocket cleanly when offline', () async {
    final serverCapture = await _startWsServer();
    WebSocket? clientSocket;

    final service = WebSocketService(
      wsBaseUrlOverride: serverCapture.baseUrl,
      socketConnector: (url, {headers, compression = CompressionOptions.compressionDefault}) async {
        clientSocket = await WebSocket.connect(url, headers: headers, compression: compression);
        return clientSocket!;
      },
    );

    addTearDown(() async {
      await service.disconnect();
      await serverCapture.server.close(force: true);
    });

    await service.connect(path: '/ws/location', jwt: 'jwt-token');
    await _waitFor(() => service.isConnected);

    expect(clientSocket?.pingInterval, const Duration(seconds: AppConstants.webSocketHeartbeatIntervalSeconds));

    await service.disconnect(reason: 'courier_offline');
    await _waitFor(() => serverCapture.closeCode != null);

    expect(serverCapture.closeCode, ws_status.normalClosure);
    expect(serverCapture.closeReason, 'courier_offline');
  });
}
