import 'package:web_socket_channel/web_socket_channel.dart';
import '../core/utils/logger.dart';

class WebSocketService {
  WebSocketChannel? _channel;
  bool _isConnected = false;
  
  Future<void> connect(String url) async {
    try {
      _channel = WebSocketChannel.connect(Uri.parse(url));
      _isConnected = true;
      AppLogger.info('WebSocket connected');
      
      _channel!.stream.listen(
        (message) {
          AppLogger.info('WebSocket message: $message');
          // TODO: Handle messages
        },
        onError: (error) {
          AppLogger.error('WebSocket error', error);
          _isConnected = false;
        },
        onDone: () {
          AppLogger.info('WebSocket disconnected');
          _isConnected = false;
        },
      );
    } catch (e) {
      AppLogger.error('WebSocket connection failed', e);
      _isConnected = false;
    }
  }
  
  void send(Map<String, dynamic> data) {
    if (_isConnected && _channel != null) {
      _channel!.sink.add(data.toString());
    }
  }
  
  Future<void> disconnect() async {
    await _channel?.sink.close();
    _isConnected = false;
    AppLogger.info('WebSocket disconnected');
  }
  
  bool get isConnected => _isConnected;
}
