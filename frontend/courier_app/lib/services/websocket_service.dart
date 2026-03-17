import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/io.dart';
import '../core/constants/app_constants.dart';
import '../config/runtime_config.dart';
import '../core/utils/logger.dart';

/// Simple WebSocket client with JWT support for the courier app.
class WebSocketService {
  static String? _ownerInstanceId;

  WebSocketChannel? _channel;
  bool _isConnected = false;
  StreamSubscription? _subscription;
  Timer? _reconnectTimer;
  Timer? _stabilityTimer;
  final StreamController<bool> _connectionController =
      StreamController<bool>.broadcast();

  String? _lastPath;
  String? _lastJwt;
  int _reconnectAttempts = 0;
  bool _shouldReconnect = false;
  static const int _stableConnectionSeconds = 15;
  final String _instanceId = '${DateTime.now().microsecondsSinceEpoch}-${Object().hashCode}';

  /// Connect to the WebSocket endpoint using the configured wsUrl and path.
  /// Example: ws://host/ws/location
  Future<void> connect({
    required String path,
    required String? jwt,
  }) async {
    _ownerInstanceId = _instanceId;
    _lastPath = path;
    _lastJwt = jwt;
    _shouldReconnect = true;

    if (_isConnected) {
      return;
    }

    final base = RuntimeConfig.wsUrl.replaceFirst(RegExp(r'/$'), '');
    final fullUrl = '$base$path';

    try {
      final uri = Uri.parse(fullUrl);
      AppLogger.info('Connecting WebSocket to $fullUrl');

      // web_socket_channel for Dart VM (Android) does not support headers directly.
      // We therefore pass the JWT as query parameter. The gateway / backend
      // already valident le token en HTTP; pour WS nous aurons un adaptateur.
      final uriWithToken =
          (jwt != null && jwt.isNotEmpty) ? uri.replace(queryParameters: {...uri.queryParameters, 'token': jwt}) : uri;

      // Some gateway/proxy chains mis-handle permessage-deflate negotiation
      // and trigger close code 1002 (Protocol error). Force no compression.
      final socket = await WebSocket.connect(
        uriWithToken.toString(),
        compression: CompressionOptions.compressionOff,
      );
      socket.pingInterval = const Duration(seconds: 20);
      _channel = IOWebSocketChannel(socket);
      _setConnected(true);
      AppLogger.info('WebSocket connected');
      _startStabilityTimer();

      _subscription = _channel!.stream.listen(
        (message) {
          AppLogger.info('WebSocket message: $message');
          // TODO: Handle messages from tracking endpoint
        },
        onError: (error) {
          AppLogger.error('WebSocket error', error);
          _handleDisconnected();
        },
        onDone: () {
          final closeCode = _channel?.closeCode;
          final closeReason = _channel?.closeReason;
          AppLogger.info('WebSocket disconnected (code=$closeCode, reason=$closeReason)');
          _handleDisconnected();
        },
        cancelOnError: true,
      );
    } catch (e) {
      AppLogger.error('WebSocket connection failed', e);
      _handleDisconnected();
    }
  }

  /// Send a JSON-encodable payload over the WebSocket.
  bool send(Map<String, dynamic> data) {
    if (_isConnected && _channel != null) {
      final jsonPayload = jsonEncode(data);
      _channel!.sink.add(jsonPayload);
      AppLogger.info('WebSocket sent: $jsonPayload');
      return true;
    } else {
      AppLogger.warning('Attempted to send on closed WebSocket');
      return false;
    }
  }

  Future<void> disconnect() async {
    if (_ownerInstanceId == _instanceId) {
      _ownerInstanceId = null;
    }
    _shouldReconnect = false;
    _reconnectTimer?.cancel();
    _stabilityTimer?.cancel();
    await _subscription?.cancel();
    await _channel?.sink.close();
    _subscription = null;
    _channel = null;
    _setConnected(false);
    AppLogger.info('WebSocket disconnected');
  }

  bool get isConnected => _isConnected;

  Stream<bool> get connectionChanges => _connectionController.stream;

  void _handleDisconnected() {
    if (_ownerInstanceId != null && _ownerInstanceId != _instanceId) {
      _shouldReconnect = false;
      return;
    }
    _stabilityTimer?.cancel();
    _subscription?.cancel();
    _subscription = null;
    _channel = null;
    _setConnected(false);
    _scheduleReconnect();
  }

  void _scheduleReconnect() {
    if (!_shouldReconnect || _lastPath == null) {
      return;
    }

    if (_ownerInstanceId != null && _ownerInstanceId != _instanceId) {
      _shouldReconnect = false;
      return;
    }

    _reconnectTimer?.cancel();
    final baseDelay = AppConstants.reconnectBaseDelaySeconds;
    final maxDelay = AppConstants.reconnectMaxDelaySeconds;
    final delaySeconds = (baseDelay * (1 << _reconnectAttempts)).clamp(baseDelay, maxDelay);
    _reconnectAttempts++;

    AppLogger.warning('Scheduling WebSocket reconnect in ${delaySeconds}s');
    _reconnectTimer = Timer(Duration(seconds: delaySeconds), () async {
      await connect(path: _lastPath!, jwt: _lastJwt);
    });
  }

  void _startStabilityTimer() {
    _stabilityTimer?.cancel();
    _stabilityTimer = Timer(const Duration(seconds: _stableConnectionSeconds), () {
      _reconnectAttempts = 0;
      AppLogger.info('WebSocket marked stable; reconnect attempts reset');
    });
  }

  void _setConnected(bool value) {
    if (_isConnected == value) {
      return;
    }
    _isConnected = value;
    _connectionController.add(value);
  }
}
