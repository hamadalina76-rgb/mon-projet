import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';
import '../api_endpoints.dart';

/// Intercepteur pour gérer l'authentification JWT
/// 
/// Fonctionnalités:
/// - Injection automatique du token Bearer dans les headers
/// - Rafraîchissement du token en cas d'expiration (401)
/// - Retry automatique après refresh token
/// - Logging des requêtes/réponses
class AuthInterceptor extends Interceptor {
  static const String _accessTokenKey = 'auth_token';
  static const String _refreshTokenKey = 'auth_refresh_token';
  static const String _legacyRefreshTokenKey = 'refresh_token';

  final FlutterSecureStorage _secureStorage;
  final Logger _logger;
  final Dio _dio;

  AuthInterceptor({
    required FlutterSecureStorage secureStorage,
    required Logger logger,
    required Dio dio,
  })  : _secureStorage = secureStorage,
        _logger = logger,
        _dio = dio;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // Skip token injection pour certains endpoints publics
    final publicEndpoints = [
      ApiEndpoints.AUTH_LOGIN,
      ApiEndpoints.AUTH_REGISTER,
      ApiEndpoints.AUTH_FORGOT_PASSWORD,
      ApiEndpoints.AUTH_VERIFY_OTP,
      ApiEndpoints.AUTH_RESET_PASSWORD,
    ];

    final isPublicEndpoint = publicEndpoints.any(
      (endpoint) => options.path.contains(endpoint),
    );

    // Injecter le token pour les endpoints protégés
    if (!isPublicEndpoint) {
      final token = await _secureStorage.read(key: _accessTokenKey);
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
        _logger.d('🔐 Token injecté pour ${options.path}');
      } else {
        _logger.w('⚠️ Aucun token disponible pour ${options.path}');
      }
    }

    _logger.d('📤 REQUEST: ${options.method} ${options.uri}');
    _logger.d('📋 Headers: ${options.headers}');
    if (options.data != null) {
      _logger.d('📦 Body: ${options.data}');
    }

    return handler.next(options);
  }

  @override
  void onResponse(
    Response response,
    ResponseInterceptorHandler handler,
  ) {
    _logger.d('📥 RESPONSE: ${response.statusCode} ${response.requestOptions.uri}');
    _logger.d('📦 Data: ${response.data}');
    return handler.next(response);
  }

  @override
  void onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    _logger.e('❌ ERROR: ${err.response?.statusCode} ${err.requestOptions.uri}');
    _logger.e('📦 Error Data: ${err.response?.data}');
    _logger.e('🔍 Error Message: ${err.message}');

    // Gérer l'erreur 401 (Unauthorized)
    if (err.response?.statusCode == 401) {
      _logger.w('🔄 Token expiré, tentative de rafraîchissement...');

      // Éviter boucle infinie si c'est déjà un refresh token qui échoue
      if (err.requestOptions.path.contains(ApiEndpoints.AUTH_REFRESH_TOKEN)) {
        _logger.e('❌ Refresh token invalide, déconnexion nécessaire');
        await _clearTokens();
        return handler.next(err);
      }

      // Tenter de rafraîchir le token
      try {
        final success = await _refreshToken();

        if (success) {
          _logger.i('✅ Token rafraîchi avec succès, retry de la requête...');

          // Retry la requête originale avec le nouveau token
          final response = await _retry(err.requestOptions);
          return handler.resolve(response);
        } else {
          _logger.e('❌ Échec du rafraîchissement du token');
          await _clearTokens();
        }
      } catch (e) {
        _logger.e('❌ Exception lors du refresh token: $e');
        await _clearTokens();
      }
    }

    // Gérer les autres erreurs
    if (err.response?.statusCode == 403) {
      _logger.e('🚫 Accès interdit (403)');
    } else if (err.response?.statusCode == 404) {
      _logger.e('🔍 Ressource non trouvée (404)');
    } else if (err.response?.statusCode == 500) {
      _logger.e('💥 Erreur serveur (500)');
    }

    return handler.next(err);
  }

  /// Rafraîchir le token JWT
  Future<bool> _refreshToken() async {
    try {
      final refreshToken = await _readRefreshToken();

      if (refreshToken == null) {
        _logger.w('⚠️ Aucun refresh token disponible');
        return false;
      }

      _logger.d('🔄 Appel API refresh token...');

      // Créer une nouvelle instance Dio pour éviter les intercepteurs
      final dio = Dio(BaseOptions(
        baseUrl: _dio.options.baseUrl,
        headers: {'Content-Type': 'application/json'},
      ));

      final response = await dio.post(
        ApiEndpoints.AUTH_REFRESH_TOKEN,
        data: {'refreshToken': refreshToken},
      );

      if (response.statusCode == 200 && response.data is Map) {
        final data = response.data as Map;
        final newToken = _asNonEmptyString(
          data['access_token'] ?? data['accessToken'] ?? data['token'],
        );
        final newRefreshToken = _asNonEmptyString(
          data['refresh_token'] ?? data['refreshToken'],
        );

        if (newToken != null && newRefreshToken != null) {
          await _secureStorage.write(key: _accessTokenKey, value: newToken);
          await _secureStorage.write(key: _refreshTokenKey, value: newRefreshToken);
          // Kept for backward compatibility with older storage readers.
          await _secureStorage.write(
            key: _legacyRefreshTokenKey,
            value: newRefreshToken,
          );
          _logger.i('✅ Nouveaux tokens sauvegardés');
          return true;
        }
      }

      return false;
    } catch (e) {
      _logger.e('❌ Erreur refresh token: $e');
      return false;
    }
  }

  /// Retry une requête avec le nouveau token
  Future<Response<dynamic>> _retry(RequestOptions requestOptions) async {
    // Récupérer le nouveau token
    final token = await _secureStorage.read(key: _accessTokenKey);

    // Mettre à jour le header Authorization
    final options = Options(
      method: requestOptions.method,
      headers: {
        ...requestOptions.headers,
        'Authorization': 'Bearer $token',
      },
    );

    // Relancer la requête
    return _dio.request(
      requestOptions.path,
      data: requestOptions.data,
      queryParameters: requestOptions.queryParameters,
      options: options,
    );
  }

  Future<String?> _readRefreshToken() async {
    final primary = await _secureStorage.read(key: _refreshTokenKey);
    if (primary != null && primary.trim().isNotEmpty) {
      return primary;
    }

    final legacy = await _secureStorage.read(key: _legacyRefreshTokenKey);
    if (legacy != null && legacy.trim().isNotEmpty) {
      return legacy;
    }

    return null;
  }

  String? _asNonEmptyString(dynamic value) {
    if (value is! String) {
      return null;
    }
    final trimmed = value.trim();
    return trimmed.isEmpty ? null : trimmed;
  }

  /// Nettoyer les tokens (lors d'une déconnexion ou erreur auth)
  Future<void> _clearTokens() async {
    await _secureStorage.delete(key: _accessTokenKey);
    await _secureStorage.delete(key: _refreshTokenKey);
    await _secureStorage.delete(key: _legacyRefreshTokenKey);
    _logger.i('🧹 Tokens supprimés');
  }
}
