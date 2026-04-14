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
  static const String AUTH_BASE = '/api/v1/auth';
  static const String AUTH_LOGIN = '$AUTH_BASE/login';
  static const String AUTH_REGISTER = '$AUTH_BASE/register';
  static const String AUTH_VERIFY_OTP = '$AUTH_BASE/verify-otp';
  static const String AUTH_RESEND_OTP = '$AUTH_BASE/resend-otp';
  static const String AUTH_FORGOT_PASSWORD = '$AUTH_BASE/forgot-password';
  static const String AUTH_RESET_PASSWORD = '$AUTH_BASE/reset-password';
    static const String AUTH_REFRESH_TOKEN = '$AUTH_BASE/refresh';
  static const String AUTH_LOGOUT = '$AUTH_BASE/logout';
  static const String AUTH_CURRENT_USER = '$AUTH_BASE/current_user';
  static const String AUTH_SOCIAL_LOGIN = '$AUTH_BASE/social-login';
  static const String AUTH_GOOGLE_LOGIN = '$AUTH_BASE/google';
  static const String AUTH_FACEBOOK_LOGIN = '$AUTH_BASE/facebook';

  // ==================== USER ====================
  static const String USER_BASE = '/api/users';
  static const String USER_PROFILE = '$USER_BASE/profile';
  static const String USER_UPDATE_PROFILE = '$USER_BASE/profile';
  static const String USER_ADDRESSES = '$USER_BASE/addresses';
  // /api/addresses/{id} → gateway StripPrefix=1 → /addresses/{id} → AddressController
  static String userAddressById(String id) => '/api/addresses/$id';

  // ==================== CUSTOMERS ====================
  static const String CUSTOMER_BASE = '/api/customers';
  static String customerById(String id) => '$CUSTOMER_BASE/$id';
  static String customerAddresses(String id) => '$CUSTOMER_BASE/$id/addresses';
  static String customerAddressesByUserId(String userId) =>
      '$CUSTOMER_BASE/by-user/$userId/addresses';
  // Legacy favorites routes (kept for backward compatibility during migration)
  static String customerFavorites(String id) => '$CUSTOMER_BASE/$id/favorites';
  static String addFavoritePartner(String customerId, String partnerId) =>
      '$CUSTOMER_BASE/$customerId/favorites/$partnerId';
  static String removeFavoritePartner(String customerId, String partnerId) =>
      '$CUSTOMER_BASE/$customerId/favorites/$partnerId';

  // ==================== FAVORITES ====================
  static const String FAVORITES = '/api/favorites';
  static String favoriteByPartner(String partnerId) => '$FAVORITES/$partnerId';

  // ==================== PARTNERS/RESTAURANTS ====================
  static const String PARTNER_BASE = '/api/partners';
  static String partnerById(String id) => '$PARTNER_BASE/$id';
  static String partnerMenu(String id) => '$PARTNER_BASE/$id/menu';
  static const String SEARCH_PARTNERS = '$PARTNER_BASE/search';
  static const String NEARBY_PARTNERS = '$PARTNER_BASE/nearby';

  // ==================== CATEGORIES ====================
  static const String CATEGORIES = '/api/v1/categories';
  static String categoryById(int id) => '$CATEGORIES/$id';

  // ==================== PRODUCTS ====================
  static const String PRODUCT_BASE = '/api/products';
  static String productById(String id) => '$PRODUCT_BASE/$id';

  // ==================== ORDERS ====================
  static const String ORDER_BASE = '/api/orders';
    static String customerOrders(String customerId) =>
            '$ORDER_BASE/customers/$customerId/orders';
  static String orderById(String id) => '$ORDER_BASE/$id';
  static String cancelOrder(String id) => '$ORDER_BASE/$id/cancel';
  static String trackOrder(String id) => '$ORDER_BASE/$id/track';
  static String rateOrder(String id) => '$ORDER_BASE/$id/rate';

  // ==================== CART ====================
  static const String CART_BASE = '/api/cart';
  static const String CART_ADD_ITEM = '$CART_BASE/items';
  static String updateCartItem(String id) => '$CART_BASE/items/$id';
  static String removeCartItem(String id) => '$CART_BASE/items/$id';
  static const String CART_CLEAR = '$CART_BASE/clear';

  // ==================== PAYMENT ====================
  static const String PAYMENT_BASE = '/api/payments';
  static const String PAYMENT_CREATE_INTENT = '$PAYMENT_BASE/create-intent';
  static const String PAYMENT_CONFIRM = '$PAYMENT_BASE/confirm';
  static const String PAYMENT_METHODS = '$PAYMENT_BASE/methods';
  static String paymentById(String id) => '$PAYMENT_BASE/$id';

  // ==================== WALLETS ====================
  static const String WALLET_BASE = '/api/wallets';
  static String walletBalance(String userId) => '$WALLET_BASE/$userId/balance';
  static String walletTransactions(String userId) =>
      '$WALLET_BASE/$userId/transactions';

  // ==================== NOTIFICATIONS ====================
  static const String NOTIFICATION_BASE = '/api/notifications';
  static String markAsRead(String id) => '$NOTIFICATION_BASE/$id/read';
  static const String REGISTER_PUSH_TOKEN = '/api/push-tokens';
  static String deletePushToken(String id) => '/api/push-tokens/$id';
  static const String REGISTER_DEVICE = '$NOTIFICATION_BASE/register-device';

  // ==================== DELIVERY ====================
  static const String DELIVERY_BASE = '/api/deliveries';
  static String deliveryById(String id) => '$DELIVERY_BASE/$id';
  static String trackDelivery(String id) => '$DELIVERY_BASE/$id/track';

  // ==================== LOCATIONS ====================
  static const String LOCATION_BASE = '/api/locations';
  static const String GEOCODE = '$LOCATION_BASE/geocode';
  static const String REVERSE_GEOCODE = '$LOCATION_BASE/reverse-geocode';
  static const String ZONES = '/api/zones';

  // ==================== PROMOTIONS ====================
  static const String PROMOTION_BASE = '/api/promotions';
  static String applyPromotion(String code) => '$PROMOTION_BASE/apply/$code';
  static const String ACTIVE_PROMOTIONS = '$PROMOTION_BASE/active';

  // ==================== REVIEWS ====================
  static const String REVIEW_BASE = '/api/reviews';
  static String partnerReviews(String partnerId) =>
      '$REVIEW_BASE/partner/$partnerId';
  static String orderReview(String orderId) => '$REVIEW_BASE/order/$orderId';
}
