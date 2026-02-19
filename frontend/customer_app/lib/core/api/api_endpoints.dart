/// Endpoints de l'API SpeedLine
/// 
/// Organisation:
/// - Auth: Authentification et gestion des tokens
/// - User: Profil et adresses utilisateur
/// - Partners: Restaurants/Partenaires
/// - Orders: Commandes
/// - Cart: Panier
/// - Payment: Paiements
/// - Notifications: Notifications push
class ApiEndpoints {
  // ==================== AUTHENTICATION ====================
  static const String AUTH_BASE = '/v1/auth';
  static const String AUTH_LOGIN = '$AUTH_BASE/login';
  static const String AUTH_REGISTER = '$AUTH_BASE/register';
  static const String AUTH_VERIFY_OTP = '$AUTH_BASE/verify-otp';
  static const String AUTH_RESEND_OTP = '$AUTH_BASE/resend-otp';
  static const String AUTH_FORGOT_PASSWORD = '$AUTH_BASE/forgot-password';
  static const String AUTH_RESET_PASSWORD = '$AUTH_BASE/reset-password';
  static const String AUTH_REFRESH_TOKEN = '$AUTH_BASE/refresh-token';
  static const String AUTH_LOGOUT = '$AUTH_BASE/logout';
  static const String AUTH_CURRENT_USER = '$AUTH_BASE/current_user';
  static const String AUTH_SOCIAL_LOGIN = '$AUTH_BASE/social-login';
  static const String AUTH_GOOGLE_LOGIN = '$AUTH_BASE/google';
  static const String AUTH_FACEBOOK_LOGIN = '$AUTH_BASE/facebook';

  // ==================== USER ====================
  static const String USER_BASE = '/users';
  static const String USER_PROFILE = '$USER_BASE/profile';
  static const String USER_UPDATE_PROFILE = '$USER_BASE/profile';
  static const String USER_ADDRESSES = '$USER_BASE/addresses';
  static String userAddressById(String id) => '$USER_ADDRESSES/$id';

  // ==================== CUSTOMERS ====================
  static const String CUSTOMER_BASE = '/customers';
  static String customerById(String id) => '$CUSTOMER_BASE/$id';
  static String customerAddresses(String id) => '$CUSTOMER_BASE/$id/addresses';
  static String customerFavorites(String id) => '$CUSTOMER_BASE/$id/favorites';
  static String addFavoritePartner(String customerId, String partnerId) => 
      '$CUSTOMER_BASE/$customerId/favorites/$partnerId';

  // ==================== PARTNERS/RESTAURANTS ====================
  static const String PARTNER_BASE = '/partners';
  static String partnerById(String id) => '$PARTNER_BASE/$id';
  static String partnerMenu(String id) => '$PARTNER_BASE/$id/menu';
  static const String SEARCH_PARTNERS = '$PARTNER_BASE/search';
  static const String NEARBY_PARTNERS = '$PARTNER_BASE/nearby';

  // ==================== PRODUCTS ====================
  static const String PRODUCT_BASE = '/products';
  static String productById(String id) => '$PRODUCT_BASE/$id';

  // ==================== ORDERS ====================
  static const String ORDER_BASE = '/orders';
  static String orderById(String id) => '$ORDER_BASE/$id';
  static String cancelOrder(String id) => '$ORDER_BASE/$id/cancel';
  static String trackOrder(String id) => '$ORDER_BASE/$id/track';
  static String rateOrder(String id) => '$ORDER_BASE/$id/rate';

  // ==================== CART ====================
  static const String CART_BASE = '/cart';
  static const String CART_ADD_ITEM = '$CART_BASE/items';
  static String updateCartItem(String id) => '$CART_BASE/items/$id';
  static String removeCartItem(String id) => '$CART_BASE/items/$id';
  static const String CART_CLEAR = '$CART_BASE/clear';

  // ==================== PAYMENT ====================
  static const String PAYMENT_BASE = '/payments';
  static const String PAYMENT_CREATE_INTENT = '$PAYMENT_BASE/create-intent';
  static const String PAYMENT_CONFIRM = '$PAYMENT_BASE/confirm';
  static const String PAYMENT_METHODS = '$PAYMENT_BASE/methods';
  static String paymentById(String id) => '$PAYMENT_BASE/$id';

  // ==================== WALLETS ====================
  static const String WALLET_BASE = '/wallets';
  static String walletBalance(String userId) => '$WALLET_BASE/$userId/balance';
  static String walletTransactions(String userId) => '$WALLET_BASE/$userId/transactions';

  // ==================== NOTIFICATIONS ====================
  static const String NOTIFICATION_BASE = '/notifications';
  static String markAsRead(String id) => '$NOTIFICATION_BASE/$id/read';
  static const String REGISTER_PUSH_TOKEN = '/push-tokens';
  static String deletePushToken(String id) => '/push-tokens/$id';
  static const String REGISTER_DEVICE = '$NOTIFICATION_BASE/register-device';

  // ==================== DELIVERY ====================
  static const String DELIVERY_BASE = '/deliveries';
  static String deliveryById(String id) => '$DELIVERY_BASE/$id';
  static String trackDelivery(String id) => '$DELIVERY_BASE/$id/track';

  // ==================== LOCATIONS ====================
  static const String LOCATION_BASE = '/locations';
  static const String GEOCODE = '$LOCATION_BASE/geocode';
  static const String REVERSE_GEOCODE = '$LOCATION_BASE/reverse-geocode';
  static const String ZONES = '/zones';

  // ==================== PROMOTIONS ====================
  static const String PROMOTION_BASE = '/promotions';
  static String applyPromotion(String code) => '$PROMOTION_BASE/apply/$code';
  static const String ACTIVE_PROMOTIONS = '$PROMOTION_BASE/active';

  // ==================== REVIEWS ====================
  static const String REVIEW_BASE = '/reviews';
  static String partnerReviews(String partnerId) => '$REVIEW_BASE/partner/$partnerId';
  static String orderReview(String orderId) => '$REVIEW_BASE/order/$orderId';
}
