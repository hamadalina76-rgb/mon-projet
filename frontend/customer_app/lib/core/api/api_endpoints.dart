class ApiEndpoints {
  // Auth
  static const String login = '/auth/login';
  static const String register = '/auth/register';
  static const String logout = '/auth/logout';
  static const String refreshToken = '/auth/refresh';
  static const String verifyOtp = '/auth/verify-otp';

  // User
  static const String profile = '/users/profile';
  static const String updateProfile = '/users/profile';
  static const String addresses = '/users/addresses';

  // Restaurants
  static const String restaurants = '/restaurants';
  static String restaurantById(String id) => '/restaurants/$id';
  static String restaurantMenu(String id) => '/restaurants/$id/menu';
  static const String searchRestaurants = '/restaurants/search';
  static const String nearbyRestaurants = '/restaurants/nearby';

  // Orders
  static const String orders = '/orders';
  static String orderById(String id) => '/orders/$id';
  static String cancelOrder(String id) => '/orders/$id/cancel';
  static String trackOrder(String id) => '/orders/$id/track';

  // Cart
  static const String cart = '/cart';
  static const String addToCart = '/cart/items';
  static String updateCartItem(String id) => '/cart/items/$id';
  static String removeCartItem(String id) => '/cart/items/$id';
  static const String clearCart = '/cart/clear';

  // Payment
  static const String createPaymentIntent = '/payments/create-intent';
  static const String confirmPayment = '/payments/confirm';
  static const String paymentMethods = '/payments/methods';

  // Notifications
  static const String notifications = '/notifications';
  static String markAsRead(String id) => '/notifications/$id/read';
  static const String registerDevice = '/notifications/register-device';
}
