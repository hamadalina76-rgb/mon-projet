import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';
import 'package:pretty_dio_logger/pretty_dio_logger.dart';
import 'interceptors/auth_interceptor.dart';
import '../../config/runtime_config.dart';

/// Client API Singleton pour gérer toutes les requêtes HTTP
/// 
/// Configuration:
/// - Base URL depuis .env
/// - Timeout: 30 secondes
/// - Intercepteurs: Auth + Logging
/// - Gestion automatique des erreurs et retry
class ApiClient {
  static ApiClient? _instance;
  late final Dio _dio;
  final FlutterSecureStorage _secureStorage;
  final Logger _logger;

  // Singleton pattern
  factory ApiClient({
    FlutterSecureStorage? secureStorage,
    Logger? logger,
  }) {
    if (_instance == null) {
      final storage = secureStorage ?? const FlutterSecureStorage();
      final log = logger ?? Logger();
      _instance = ApiClient._internal(storage, log);
    }
    return _instance!;
  }

  ApiClient._internal(this._secureStorage, this._logger) {
    _initializeDio();
  }

  /// Initialiser Dio avec la configuration
  void _initializeDio() {
    // Récupérer la configuration runtime
    final baseUrl = RuntimeConfig.apiBaseUrl;
    if (baseUrl.isEmpty) {
      throw StateError('API_BASE_URL must be defined in runtime config');
    }
    final timeoutMs = RuntimeConfig.apiTimeoutMs;
    final enableLogging = dotenv.env['LOG_NETWORK']?.toLowerCase() == 'true';

    _logger.i('🚀 Initialisation API Client');
    _logger.i('📍 Base URL: $baseUrl');
    _logger.i('⏱️ Timeout: ${timeoutMs}ms');
    _logger.i('📝 Logging: $enableLogging');

    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: Duration(milliseconds: timeoutMs),
        receiveTimeout: Duration(milliseconds: timeoutMs),
        sendTimeout: Duration(milliseconds: timeoutMs),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        validateStatus: (status) {
          // Treat 401 as an error so AuthInterceptor.onError can handle
          // token refresh and retry automatically. All other < 500 codes
          // are treated as successful responses.
          return status != null && status < 500 && status != 401;
        },
      ),
    );

    _setupInterceptors(enableLogging);
  }

  /// Configurer les intercepteurs
  void _setupInterceptors(bool enableLogging) {
    // 1. Auth Interceptor (priorité haute)
    _dio.interceptors.add(
      AuthInterceptor(
        secureStorage: _secureStorage,
        logger: _logger,
        dio: _dio,
      ),
    );

    // 2. Pretty Logger (uniquement en développement)
    if (enableLogging) {
      _dio.interceptors.add(
        PrettyDioLogger(
          requestHeader: true,
          requestBody: true,
          responseBody: true,
          responseHeader: false,
          error: true,
          compact: true,
          maxWidth: 90,
          logPrint: (object) {
            // Utiliser notre logger au lieu de print
            _logger.d(object);
          },
        ),
      );
    }

    // 3. Error Handler Interceptor
    _dio.interceptors.add(
      InterceptorsWrapper(
        onError: (error, handler) {
          final statusCode = error.response?.statusCode;
          final message = _getErrorMessage(error);

          _logger.e('❌ API Error [$statusCode]: $message');

          // Transformer DioException en erreur plus lisible
          if (error.type == DioExceptionType.connectionTimeout ||
              error.type == DioExceptionType.receiveTimeout ||
              error.type == DioExceptionType.sendTimeout) {
            _logger.e('⏱️ Timeout: Vérifiez votre connexion Internet');
          } else if (error.type == DioExceptionType.connectionError) {
            _logger.e('🌐 Erreur de connexion: Backend inaccessible');
          }

          return handler.next(error);
        },
      ),
    );

    _logger.i('✅ Intercepteurs configurés');
  }

  /// Extraire un message d'erreur lisible
  String _getErrorMessage(DioException error) {
    if (error.response?.data is Map) {
      final data = error.response!.data as Map<String, dynamic>;
      return data['message'] ?? data['error'] ?? 'Erreur inconnue';
    }
    return error.message ?? 'Erreur inconnue';
  }

  /// Sauvegarder les tokens après authentification
  Future<void> saveTokens({
    required String token,
    required String refreshToken,
  }) async {
    await _secureStorage.write(key: 'auth_token', value: token);
    await _secureStorage.write(key: 'refresh_token', value: refreshToken);
    _logger.i('💾 Tokens sauvegardés');
  }

  /// Supprimer les tokens (déconnexion)
  Future<void> clearTokens() async {
    await _secureStorage.delete(key: 'auth_token');
    await _secureStorage.delete(key: 'refresh_token');
    _logger.i('🗑️ Tokens supprimés');
  }

  /// Vérifier si l'utilisateur est authentifié
  Future<bool> hasToken() async {
    final token = await _secureStorage.read(key: 'auth_token');
    return token != null && token.isNotEmpty;
  }

  /// Accès au client Dio
  Dio get dio => _dio;

  /// Reset l'instance (pour les tests)
  static void resetInstance() {
    _instance = null;
  }
}
