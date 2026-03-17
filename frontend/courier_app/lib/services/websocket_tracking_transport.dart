import 'tracking_transport.dart';
import 'websocket_service.dart';

class WebSocketTrackingTransport implements TrackingTransport {
  final WebSocketService _webSocketService;

  WebSocketTrackingTransport(this._webSocketService);

  @override
  Future<void> connect({required String path, required String jwt}) {
    return _webSocketService.connect(path: path, jwt: jwt);
  }

  @override
  Future<void> disconnect() {
    return _webSocketService.disconnect();
  }

  @override
  bool get isConnected => _webSocketService.isConnected;

  @override
  bool send(Map<String, dynamic> payload) {
    return _webSocketService.send(payload);
  }

  @override
  Stream<bool> get connectionChanges => _webSocketService.connectionChanges;
}
