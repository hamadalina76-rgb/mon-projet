import 'package:logger/logger.dart';

class FirebaseService {
  final Logger _logger;

  FirebaseService(this._logger);

  Future<void> initialize() async {
    _logger.i('Firebase service initialized (currently disabled for web)');
    // When Firebase is enabled:
    // await Firebase.initializeApp();
  }

  Future<String?> getMessagingToken() async {
    _logger.i('Getting FCM token (currently disabled)');
    // await FirebaseMessaging.instance.getToken();
    return null;
  }

  void subscribeToTopic(String topic) {
    _logger.i('Subscribe to topic: $topic (currently disabled)');
    // await FirebaseMessaging.instance.subscribeToTopic(topic);
  }

  void unsubscribeFromTopic(String topic) {
    _logger.i('Unsubscribe from topic: $topic (currently disabled)');
    // await FirebaseMessaging.instance.unsubscribeFromTopic(topic);
  }
}
