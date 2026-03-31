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

  String _normalizeBaseUrl(String baseUrl) {
    var normalized = baseUrl.trim();
    if (normalized.endsWith('/')) {
      normalized = normalized.substring(0, normalized.length - 1);
    }
    while (normalized.contains('/api/api')) {
      normalized = normalized.replaceAll('/api/api', '/api');
    }
    return normalized;
  }

  String _normalizeRequestPath(String path) {
    var normalized = path;
    while (normalized.contains('/api/api')) {
      normalized = normalized.replaceAll('/api/api', '/api');
    }
    return normalized;
  }

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
    // RuntimeConfig is the primary source for automatic device/cloud routing.
    // Keep .env as a fallback only when runtime config is absent.
    final envBaseUrl = dotenv.env['API_BASE_URL']?.trim();
    final runtimeBaseUrl = RuntimeConfig.apiBaseUrl.trim();
    final selectedBaseUrl = runtimeBaseUrl.isNotEmpty
        ? runtimeBaseUrl
        : (envBaseUrl ?? '');
    final baseUrl = _normalizeBaseUrl(selectedBaseUrl);
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
    // Normaliser les chemins entrants pour éviter /api/api/...
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          options.path = _normalizeRequestPath(options.path);
          handler.next(options);
        },
      ),
    );

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
          final uri = error.requestOptions.uri.toString();
          final message = _getErrorMessage(error);
          final rawError = error.error?.toString();

          _logger.e('❌ API Error [${error.type.name}][$statusCode] $uri');
          _logger.e('📦 Error Data: ${error.response?.data}');
          _logger.e('🔍 Error Message: $message');
          if (rawError != null && rawError.trim().isNotEmpty) {
            _logger.e('🧩 Transport Error: $rawError');
          }

          // Transformer DioException en erreur plus lisible
          if (error.type == DioExceptionType.connectionTimeout ||
              error.type == DioExceptionType.receiveTimeout ||
              error.type == DioExceptionType.sendTimeout) {
            _logger.e('⏱️ Timeout: Vérifiez votre connexion Internet');
          } else if (error.type == DioExceptionType.connectionError) {
            _logger.e('🌐 Erreur de connexion: Backend inaccessible');
          } else if (error.type == DioExceptionType.unknown) {
            final lower = (rawError ?? '').toLowerCase();
            if (lower.contains('cleartext')) {
              _logger.e('📵 HTTP bloqué par Android (cleartext non autorisé)');
            } else if (lower.contains('failed host lookup') ||
                lower.contains('connection refused') ||
                lower.contains('network is unreachable') ||
                lower.contains('no route to host')) {
              _logger.e('🌐 Hôte API inaccessible depuis cet appareil');
            }
          }

          return handler.next(error);
        },
      ),
    );

    _logger.i('✅ Intercepteurs configurés');
  }

  /// Extraire un message d'erreur lisible
  String _getErrorMessage(DioException error) {
    final data = error.response?.data;
    if (error.response?.data is Map) {
      final data = error.response!.data as Map<String, dynamic>;
      return data['message'] ?? data['error'] ?? 'Erreur inconnue';
    }

    if (data is String && data.trim().isNotEmpty) {
      return data;
    }

    final message = error.message;
    if (message != null && message.trim().isNotEmpty) {
      return message;
    }

    final transport = error.error?.toString();
    if (transport != null && transport.trim().isNotEmpty) {
      return transport;
    }

    return 'Erreur reseau inconnue';
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
