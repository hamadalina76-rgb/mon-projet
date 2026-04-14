import 'dart:async';
import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../config/runtime_config.dart';

class RealtimeOrderEvent {
  final String destination;
  final String type;
  final String title;
  final String message;
  final String? orderId;
  final Map<String, dynamic> raw;

  const RealtimeOrderEvent({
    required this.destination,
    required this.type,
    required this.title,
    required this.message,
    required this.raw,
    this.orderId,
  });
}

class WebsocketService {
  final Logger _logger;
  final FlutterSecureStorage _secureStorage;

  final StreamController<RealtimeOrderEvent> _eventsController =
      StreamController<RealtimeOrderEvent>.broadcast();

  StompClient? _client;
  String? _activeUserId;
  bool _manualDisconnect = false;
  Timer? _reconnectTimer;
  int _reconnectAttempt = 0;

  WebsocketService({
    required Logger logger,
    required FlutterSecureStorage secureStorage,
  }) : _logger = logger,
       _secureStorage = secureStorage;

  Stream<RealtimeOrderEvent> get events => _eventsController.stream;

  Future<void> connectForUser(String userId) async {
    final normalizedUserId = userId.trim();
    if (normalizedUserId.isEmpty) return;

    if (_activeUserId == normalizedUserId && (_client?.connected ?? false)) {
      return;
    }

    _manualDisconnect = false;
    _activeUserId = normalizedUserId;
    _reconnectTimer?.cancel();

    final token = await _secureStorage.read(key: 'auth_token');
    final headers = <String, String>{};
    if (token != null && token.trim().isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }

    _client?.deactivate();
    _client = StompClient(
      config: StompConfig.sockJS(
        url: _resolveSockJsUrl(),
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        onConnect: (_) {
          _reconnectAttempt = 0;
          _subscribeToTopics(normalizedUserId);
        },
        onDisconnect: (_) {
          if (!_manualDisconnect) {
            _scheduleReconnect();
          }
        },
        onStompError: (frame) {
          _logger.w('STOMP error: ${frame.body}');
          _scheduleReconnect();
        },
        onWebSocketError: (dynamic error) {
          _logger.w('WebSocket error: $error');
          _scheduleReconnect();
        },
        heartbeatIncoming: const Duration(seconds: 20),
        heartbeatOutgoing: const Duration(seconds: 20),
        reconnectDelay: const Duration(milliseconds: 0),
      ),
    );

    _client?.activate();
  }

  Future<void> disconnect() async {
    _manualDisconnect = true;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    _reconnectAttempt = 0;
    _activeUserId = null;
    _client?.deactivate();
    _client = null;
  }

  Future<void> dispose() async {
    await disconnect();
    await _eventsController.close();
  }

  void _subscribeToTopics(String userId) {
    _subscribe('/topic/user/$userId/notifications');
    _subscribe('/topic/orders/$userId');
  }

  void _subscribe(String destination) {
    _client?.subscribe(
      destination: destination,
      callback: (frame) {
        final body = frame.body;
        if (body == null || body.trim().isEmpty) return;

        final payload = _decodePayload(body);
        final event = RealtimeOrderEvent(
          destination: destination,
          type: payload['type']?.toString() ?? 'ORDER_UPDATE',
          title: payload['title']?.toString() ?? 'Order update',
          message:
              payload['message']?.toString() ??
              payload['body']?.toString() ??
              'You have a new update',
          orderId: _extractOrderId(payload),
          raw: payload,
        );

        _eventsController.add(event);
      },
    );
  }

  Map<String, dynamic> _decodePayload(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) {
        final payload = Map<String, dynamic>.from(decoded);
        final data = payload['data'];
        if (data is Map) {
          payload.addAll(Map<String, dynamic>.from(data));
        }
        return payload;
      }

      if (decoded is Map) {
        final payload = Map<String, dynamic>.from(decoded);
        final data = payload['data'];
        if (data is Map) {
          payload.addAll(Map<String, dynamic>.from(data));
        }
        return payload;
      }
    } catch (_) {
      // Fall through and keep raw body message.
    }

    return <String, dynamic>{'message': body};
  }

  String? _extractOrderId(Map<String, dynamic> payload) {
    final orderId =
        payload['orderId']?.toString() ??
        payload['order_id']?.toString() ??
        payload['id']?.toString();

    if (orderId == null || orderId.trim().isEmpty) {
      return null;
    }

    return orderId.trim();
  }

  String _resolveSockJsUrl() {
    var base = RuntimeConfig.wsUrl.trim();
    if (base.isEmpty) {
      base = RuntimeConfig.apiBaseUrl.trim();
    }

    if (base.startsWith('ws://')) {
      base = 'http://${base.substring(5)}';
    } else if (base.startsWith('wss://')) {
      base = 'https://${base.substring(6)}';
    }

    if (base.endsWith('/')) {
      base = base.substring(0, base.length - 1);
    }

    return '$base/ws/notifications';
  }

  void _scheduleReconnect() {
    final userId = _activeUserId;
    if (_manualDisconnect || userId == null || userId.trim().isEmpty) {
      return;
    }

    _reconnectTimer?.cancel();
    _reconnectAttempt += 1;
    final waitSeconds = _reconnectAttempt.clamp(1, 6);

    _reconnectTimer = Timer(Duration(seconds: waitSeconds), () {
      connectForUser(userId);
    });
  }
}
