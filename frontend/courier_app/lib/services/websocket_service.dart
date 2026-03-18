import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/io.dart';
import 'package:web_socket_channel/status.dart' as ws_status;
import '../core/constants/app_constants.dart';
import '../config/runtime_config.dart';
import '../core/utils/logger.dart';

enum WebSocketConnectionStatus {
  connected,
  connecting,
  disconnected,
  error,
}

class WebSocketConnectionState {
  final WebSocketConnectionStatus status;
  final String? errorMessage;
  final int reconnectAttempt;

  const WebSocketConnectionState({
    required this.status,
    this.errorMessage,
    required this.reconnectAttempt,
  });
}

/// Simple WebSocket client with JWT support for the courier app.
class WebSocketService {
  static String? _ownerInstanceId;

  final Future<String?> Function()? _accessTokenResolver;
  final Future<String?> Function()? _tokenRefresher;
  final String? _wsBaseUrlOverride;
  final Future<WebSocket> Function(
    String url, {
    Map<String, dynamic>? headers,
    CompressionOptions compression,
  })? _socketConnector;

  WebSocketService({
    Future<String?> Function()? accessTokenResolver,
    Future<String?> Function()? tokenRefresher,
    String? wsBaseUrlOverride,
    Future<WebSocket> Function(
      String url, {
      Map<String, dynamic>? headers,
      CompressionOptions compression,
    })? socketConnector,
  })  : _accessTokenResolver = accessTokenResolver,
        _tokenRefresher = tokenRefresher,
        _wsBaseUrlOverride = wsBaseUrlOverride,
        _socketConnector = socketConnector;

  WebSocketChannel? _channel;
  bool _isConnected = false;
  bool _isRefreshingToken = false;
  StreamSubscription? _subscription;
  Timer? _reconnectTimer;
  Timer? _stabilityTimer;
  final StreamController<bool> _connectionController =
      StreamController<bool>.broadcast();
  final StreamController<WebSocketConnectionState> _connectionStateController =
      StreamController<WebSocketConnectionState>.broadcast();
  final StreamController<Map<String, dynamic>> _messagesController =
      StreamController<Map<String, dynamic>>.broadcast();

  String? _lastPath;
  String? _lastJwt;
  int _reconnectAttempts = 0;
  bool _shouldReconnect = false;
  bool _authRefreshAttemptedForCurrentAttempt = false;
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

    _emitConnectionState(WebSocketConnectionStatus.connecting);
    final base = (_wsBaseUrlOverride ?? RuntimeConfig.wsUrl).replaceFirst(RegExp(r'/$'), '');
    final fullUrl = '$base$path';

    try {
      final uri = Uri.parse(fullUrl);
      final token = await _resolveAccessToken(jwt);
      final headers = <String, dynamic>{};
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
        _lastJwt = token;
      }

      AppLogger.info('Connecting WebSocket to $fullUrl');

      // Some gateway/proxy chains mis-handle permessage-deflate negotiation
      // and trigger close code 1002 (Protocol error). Force no compression.
      final connector = _socketConnector ??
          (String url, {Map<String, dynamic>? headers, CompressionOptions compression = CompressionOptions.compressionDefault}) {
            return WebSocket.connect(url, headers: headers, compression: compression);
          };
      final socket = await connector(
        uri.toString(),
        headers: headers,
        compression: CompressionOptions.compressionOff,
      );
      socket.pingInterval = const Duration(seconds: AppConstants.webSocketHeartbeatIntervalSeconds);
      _channel = IOWebSocketChannel(socket);
      _setConnected(true);
      AppLogger.info('WebSocket connected');
      _startStabilityTimer();
      _authRefreshAttemptedForCurrentAttempt = false;
      _emitConnectionState(WebSocketConnectionStatus.connected);

      _subscription = _channel!.stream.listen(
        (message) {
          AppLogger.info('WebSocket message: $message');
          _parseIncomingMessage(message);
        },
        onError: (error) async {
          AppLogger.error('WebSocket error', error);
          final handledAuthError = await _tryRefreshJwtForAuthFailure(error: error);
          if (handledAuthError && _lastPath != null) {
            await connect(path: _lastPath!, jwt: _lastJwt);
            return;
          }
          _emitConnectionState(WebSocketConnectionStatus.error, errorMessage: error.toString());
          _handleDisconnected();
        },
        onDone: () async {
          final closeCode = _channel?.closeCode;
          final closeReason = _channel?.closeReason;
          AppLogger.info('WebSocket disconnected (code=$closeCode, reason=$closeReason)');
          final handledAuthError = await _tryRefreshJwtForAuthFailure(
            closeCode: closeCode,
            closeReason: closeReason,
          );
          if (handledAuthError && _lastPath != null) {
            await connect(path: _lastPath!, jwt: _lastJwt);
            return;
          }
          _handleDisconnected();
        },
        cancelOnError: true,
      );
    } catch (e) {
      AppLogger.error('WebSocket connection failed', e);
      final handledAuthError = await _tryRefreshJwtForAuthFailure(error: e);
      if (handledAuthError && _lastPath != null) {
        await connect(path: _lastPath!, jwt: _lastJwt);
        return;
      }
      _emitConnectionState(WebSocketConnectionStatus.error, errorMessage: e.toString());
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

  Future<void> disconnect({String reason = 'courier_offline'}) async {
    if (_ownerInstanceId == _instanceId) {
      _ownerInstanceId = null;
    }
    _shouldReconnect = false;
    _reconnectTimer?.cancel();
    _stabilityTimer?.cancel();
    await _subscription?.cancel();
    await _channel?.sink.close(ws_status.normalClosure, reason);
    _subscription = null;
    _channel = null;
    _setConnected(false);
    _authRefreshAttemptedForCurrentAttempt = false;
    _emitConnectionState(WebSocketConnectionStatus.disconnected);
    AppLogger.info('WebSocket disconnected');
  }

  bool get isConnected => _isConnected;

  Stream<bool> get connectionChanges => _connectionController.stream;

  Stream<WebSocketConnectionState> get connectionStateChanges => _connectionStateController.stream;

  Stream<Map<String, dynamic>> get messages => _messagesController.stream;

  WebSocketConnectionState get currentConnectionState => WebSocketConnectionState(
        status: _isConnected
            ? WebSocketConnectionStatus.connected
            : WebSocketConnectionStatus.disconnected,
        reconnectAttempt: _reconnectAttempts,
      );

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
    _authRefreshAttemptedForCurrentAttempt = false;
    _emitConnectionState(WebSocketConnectionStatus.disconnected);
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
    final delaySeconds = reconnectDelayForAttempt(_reconnectAttempts);
    _reconnectAttempts++;

    AppLogger.warning('Scheduling WebSocket reconnect in ${delaySeconds}s');
    _reconnectTimer = Timer(Duration(seconds: delaySeconds), () async {
      await connect(path: _lastPath!, jwt: _lastJwt);
    });
  }

  static int reconnectDelayForAttempt(int attempt) {
    final baseDelay = AppConstants.reconnectBaseDelaySeconds;
    final maxDelay = AppConstants.reconnectMaxDelaySeconds;
    final exponential = baseDelay * (1 << attempt);
    return exponential.clamp(baseDelay, maxDelay);
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

  void _emitConnectionState(WebSocketConnectionStatus status, {String? errorMessage}) {
    _connectionStateController.add(
      WebSocketConnectionState(
        status: status,
        errorMessage: errorMessage,
        reconnectAttempt: _reconnectAttempts,
      ),
    );
  }

  Future<String?> _resolveAccessToken(String? fallbackJwt) async {
    if (_accessTokenResolver != null) {
      final resolved = await _accessTokenResolver.call();
      if (resolved != null && resolved.isNotEmpty) {
        return resolved;
      }
    }
    return fallbackJwt;
  }

  Future<bool> _tryRefreshJwtForAuthFailure({
    Object? error,
    int? closeCode,
    String? closeReason,
  }) async {
    if (_tokenRefresher == null || _isRefreshingToken || !_shouldReconnect) {
      return false;
    }

    if (_authRefreshAttemptedForCurrentAttempt) {
      return false;
    }

    final authFailure = _isAuthFailure(error: error, closeCode: closeCode, closeReason: closeReason);
    if (!authFailure) {
      return false;
    }

    _isRefreshingToken = true;
    try {
      final refreshed = await _tokenRefresher.call();
      if (refreshed != null && refreshed.isNotEmpty) {
        _lastJwt = refreshed;
        _authRefreshAttemptedForCurrentAttempt = true;
        AppLogger.info('JWT refreshed for WebSocket reconnect');
        return true;
      }
      return false;
    } catch (e) {
      AppLogger.warning('WebSocket JWT refresh failed: $e');
      return false;
    } finally {
      _isRefreshingToken = false;
    }
  }

  bool _isAuthFailure({Object? error, int? closeCode, String? closeReason}) {
    if (closeCode == 1008 || closeCode == 4401 || closeCode == 4403) {
      return true;
    }

    final source = '${error ?? ''} ${closeReason ?? ''}'.toLowerCase();
    return source.contains('401') ||
        source.contains('403') ||
        source.contains('unauthorized') ||
        source.contains('forbidden') ||
        source.contains('jwt') ||
        source.contains('token');
  }

  void _parseIncomingMessage(dynamic message) {
    if (message is! String) {
      return;
    }
    try {
      final decoded = jsonDecode(message);
      if (decoded is Map<String, dynamic>) {
        _messagesController.add(decoded);
      } else if (decoded is Map) {
        _messagesController.add(Map<String, dynamic>.from(decoded));
      }
    } catch (_) {
      // Ignore non-JSON payloads; some backends can emit plain text pings.
    }
  }
}
