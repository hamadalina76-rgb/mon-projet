import 'package:firebase_core/firebase_core.dart';
import '../core/utils/logger.dart';

class FirebaseService {
  Future<void> initialize() async {
    try {
      await Firebase.initializeApp();
      AppLogger.info('Firebase initialized');
    } catch (e) {
      AppLogger.error('Firebase initialization failed', e);
    }
  }
}
