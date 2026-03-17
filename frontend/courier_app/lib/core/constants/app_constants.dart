class AppConstants {
  static const String appName = 'SpeedLine Courier';
  static const String appVersion = '1.0.0';
  
  // Timeouts
  static const int connectionTimeout = 30;
  static const int receiveTimeout = 30;
  
  // Location
  static const int movingUpdateInterval = 5; // seconds
  static const int idleUpdateInterval = 10; // seconds
  static const int idleDetectionSeconds = 120; // 2 minutes
  static const double distanceFilter = 10; // meters

  // Battery
  static const int lowBatteryThreshold = 15; // percent

  // WebSocket reconnect
  static const int reconnectBaseDelaySeconds = 2;
  static const int reconnectMaxDelaySeconds = 30;

  // Local persistence
  static const String trackingOnlinePrefKey = 'tracking_online';
}
