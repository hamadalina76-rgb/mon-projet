import 'package:logger/logger.dart';

class AnalyticsService {
  final Logger _logger;

  AnalyticsService(this._logger);

  Future<void> initialize() async {
    _logger.i('Analytics service initialized');
  }

  void logEvent(String eventName, {Map<String, dynamic>? parameters}) {
    _logger.i('Event: $eventName - Parameters: $parameters');
    // Firebase Analytics would go here when uncommented
  }

  void logScreenView(String screenName) {
    _logger.i('Screen View: $screenName');
  }

  void setUserId(String userId) {
    _logger.i('User ID set: $userId');
  }

  void setUserProperty(String name, String value) {
    _logger.i('User Property: $name = $value');
  }

  // Customer-specific events
  void logRestaurantView(String restaurantId, String restaurantName) {
    logEvent('restaurant_view', parameters: {
      'restaurant_id': restaurantId,
      'restaurant_name': restaurantName,
    });
  }

  void logAddToCart(String productId, double price) {
    logEvent('add_to_cart', parameters: {
      'product_id': productId,
      'price': price,
    });
  }

  void logOrderPlaced(String orderId, double amount) {
    logEvent('order_placed', parameters: {
      'order_id': orderId,
      'amount': amount,
    });
  }

  void logSearch(String searchTerm) {
    logEvent('search', parameters: {
      'search_term': searchTerm,
    });
  }
}
