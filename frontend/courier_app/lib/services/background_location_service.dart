import 'dart:async';
import 'package:geolocator/geolocator.dart';
import '../core/utils/logger.dart';

class BackgroundLocationService {
  StreamSubscription<Position>? _positionStreamSubscription;
  bool _isTracking = false;
  
  Future<void> startTracking() async {
    if (_isTracking) return;
    
    _positionStreamSubscription = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 10,
      ),
    ).listen((Position position) {
      AppLogger.info('Location: ${position.latitude}, ${position.longitude}');
      // TODO: Send to backend
    });
    
    _isTracking = true;
    AppLogger.info('Location tracking started');
  }
  
  Future<void> stopTracking() async {
    await _positionStreamSubscription?.cancel();
    _isTracking = false;
    AppLogger.info('Location tracking stopped');
  }
  
  Future<Position> getCurrentLocation() async {
    return await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
      ),
    );
  }
  
  bool get isTracking => _isTracking;
}
