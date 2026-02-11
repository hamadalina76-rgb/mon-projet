/// Exceptions personnalisées pour l'API
/// 
/// Utilisées pour transformer les DioException en erreurs métier

/// Exception de base pour toutes les erreurs API
class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic data;

  const ApiException({
    required this.message,
    this.statusCode,
    this.data,
  });

  @override
  String toString() => 'ApiException: $message (code: $statusCode)';
}

/// Erreur d'authentification (401, 403)
class AuthException extends ApiException {
  const AuthException({
    String message = 'Authentification échouée',
    int? statusCode,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Erreur de connexion réseau
class NetworkException extends ApiException {
  const NetworkException({
    String message = 'Pas de connexion Internet',
    int? statusCode,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Erreur serveur (500, 502, 503)
class ServerException extends ApiException {
  const ServerException({
    String message = 'Erreur serveur',
    int? statusCode,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Ressource non trouvée (404)
class NotFoundException extends ApiException {
  const NotFoundException({
    String message = 'Ressource non trouvée',
    int? statusCode = 404,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Erreur de validation (400)
class ValidationException extends ApiException {
  const ValidationException({
    String message = 'Données invalides',
    int? statusCode = 400,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Timeout de la requête
class TimeoutException extends ApiException {
  const TimeoutException({
    String message = 'Délai d\'attente dépassé',
    int? statusCode,
    dynamic data,
  }) : super(message: message, statusCode: statusCode, data: data);
}

/// Erreur de cache/stockage local
class CacheException implements Exception {
  final String message;

  const CacheException([this.message = 'Erreur de cache']);

  @override
  String toString() => 'CacheException: $message';
}

