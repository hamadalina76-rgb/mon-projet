import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../../../../core/api/api_endpoints.dart';
import '../../../../core/errors/exceptions.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';
import '../models/forgot_password_request.dart';
import '../models/verify_otp_request.dart';
import '../models/reset_password_request.dart';
import '../models/social_login_request.dart';
import '../models/auth_response.dart';
import '../models/user_model.dart';
import '../models/otp_response.dart';

/// DataSource distant pour l'authentification
/// 
/// Responsabilités:
/// - Communication avec l'API backend via Dio
/// - Conversion des réponses JSON en modèles
/// - Gestion des erreurs HTTP
/// - Ne gère PAS le cache local (voir AuthLocalDataSource)
abstract class AuthRemoteDataSource {
  /// Connexion avec email et mot de passe
  /// 
  /// Retourne OtpResponse si compte PENDING (nécessite vérification OTP)
  /// Retourne AuthResponse si compte ACTIVE (connexion directe)
  /// 
  /// @throws AuthException si credentials invalides
  /// @throws ServerException si erreur serveur
  /// @throws NetworkException si pas de connexion
  Future<dynamic> login(LoginRequest request);

  /// Vérification du code OTP pour authentification
  /// 
  /// @throws AuthException si OTP invalide ou expiré
  /// @throws ServerException si erreur serveur
  Future<AuthResponse> verifyOtp(VerifyOtpRequest request);
  
  /// Renvoyer OTP
  /// 
  /// @throws NotFoundException si email n'existe pas
  /// @throws ServerException si erreur serveur
  Future<void> resendOtp(String email);

  /// Inscription d'un nouveau client
  /// 
  /// @throws ValidationException si données invalides
  /// @throws ServerException si erreur serveur
  Future<void> register(RegisterRequest request);

  /// Demande de réinitialisation de mot de passe
  /// Envoie un OTP par email
  /// 
  /// @throws NotFoundException si email n'existe pas
  /// @throws ServerException si erreur serveur
  Future<void> forgotPassword(ForgotPasswordRequest request);

  /// Réinitialisation du mot de passe avec OTP validé
  /// 
  /// @throws AuthException si OTP invalide
  /// @throws ValidationException si mot de passe invalide
  Future<void> resetPassword(ResetPasswordRequest request);

  /// Déconnexion (invalide le refresh token côté serveur)
  /// 
  /// @throws ServerException si erreur serveur
  Future<void> logout();

  /// Rafraîchissement du token JWT
  /// 
  /// @throws AuthException si refresh token invalide
  /// @throws ServerException si erreur serveur
  Future<AuthResponse> refreshToken(String refreshToken);

  /// Récupération du profil utilisateur actuel
  /// 
  /// @throws AuthException si token invalide
  /// @throws ServerException si erreur serveur
  Future<UserModel> getCurrentUser();

  /// Connexion via fournisseur social (Google/Facebook)
  /// 
  /// @throws AuthException si token social invalide
  /// @throws ServerException si erreur serveur
  Future<AuthResponse> socialLogin(SocialLoginRequest request);
}

/// Implémentation de AuthRemoteDataSource avec Dio
class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final ApiClient apiClient;

  AuthRemoteDataSourceImpl({required this.apiClient});

  @override
  Future<dynamic> login(LoginRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_LOGIN,
        data: request.toJson(),
      );

      if (response.statusCode == 200 && response.data != null) {
        // Check response type by examining the data structure
        // If it has 'access_token', it's an AuthResponse (ACTIVE account)
        // If it has 'otpSent', it's an OtpResponse (PENDING account)
        final data = response.data as Map<String, dynamic>;
        
        if (data.containsKey('access_token') || data.containsKey('accessToken')) {
          // Direct authentication for ACTIVE accounts
          return AuthResponse.fromJson(data);
        } else if (data.containsKey('otpSent')) {
          // OTP required for PENDING accounts
          return OtpResponse.fromJson(data);
        } else {
          throw ServerException(
            message: 'Format de réponse invalide',
            statusCode: response.statusCode,
          );
        }
      } else {
        throw ServerException(
          message: 'Réponse serveur invalide',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<AuthResponse> verifyOtp(VerifyOtpRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_VERIFY_OTP,
        data: {
          'email': request.email,
          // Send both keys to support backends expecting either field
          'otpCode': request.otp,
          'otp': request.otp,
          'type': request.type ?? 'login',
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        try {
          return AuthResponse.fromJson(response.data);
        } catch (e) {
          // Parsing failed - try to extract a meaningful message from the server
          final data = response.data;
          String serverMessage = 'Réponse serveur invalide';
          if (data is Map<String, dynamic>) {
            serverMessage = data['message'] ?? data['error'] ?? serverMessage;
          } else if (data is String && data.isNotEmpty) {
            serverMessage = data;
          }
          throw AuthException(message: serverMessage);
        }
      } else {
        throw ServerException(
          message: 'Réponse serveur invalide',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<void> register(RegisterRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_REGISTER,
        data: request.toJson(),
      );

      if (response.statusCode != 201) {
        throw ServerException(
          message: 'Inscription échouée',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }
  
  @override
  Future<void> resendOtp(String email) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_RESEND_OTP,
        data: {'email': email},
      );

      if (response.statusCode != 200) {
        throw ServerException(
          message: 'Échec du renvoi de l\'OTP',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<void> forgotPassword(ForgotPasswordRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_FORGOT_PASSWORD,
        data: request.toJson(),
      );

      if (response.statusCode != 200) {
        throw ServerException(
          message: 'Échec de l\'envoi du code OTP',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<void> resetPassword(ResetPasswordRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_RESET_PASSWORD,
        data: request.toJson(),
      );

      if (response.statusCode != 200) {
        throw ServerException(
          message: 'Réinitialisation du mot de passe échouée',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<void> logout() async {
    try {
      // Note: AuthInterceptor ajoutera automatiquement le token
      final response = await apiClient.dio.post(ApiEndpoints.AUTH_LOGOUT);

      if (response.statusCode != 200 && response.statusCode != 204) {
        throw ServerException(
          message: 'Déconnexion échouée',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      // On ignore les erreurs de déconnexion côté serveur
      // Car on déconnecte quand même l'utilisateur localement
      if (e.response?.statusCode != 401) {
        throw _handleDioException(e);
      }
    }
  }

  @override
  Future<AuthResponse> refreshToken(String refreshToken) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_REFRESH_TOKEN,
        data: {'refreshToken': refreshToken},
      );

      if (response.statusCode == 200 && response.data != null) {
        return AuthResponse.fromJson(response.data);
      } else {
        throw ServerException(
          message: 'Rafraîchissement du token échoué',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<UserModel> getCurrentUser() async {
    try {
      final response = await apiClient.dio.get(ApiEndpoints.AUTH_CURRENT_USER);

      if (response.statusCode == 200 && response.data != null) {
        return UserModel.fromJson(response.data);
      } else {
        throw ServerException(
          message: 'Récupération du profil échouée',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  @override
  Future<AuthResponse> socialLogin(SocialLoginRequest request) async {
    try {
      final response = await apiClient.dio.post(
        ApiEndpoints.AUTH_SOCIAL_LOGIN,
        data: request.toJson(),
      );

      if (response.statusCode == 200 && response.data != null) {
        return AuthResponse.fromJson(response.data);
      } else {
        throw ServerException(
          message: 'Connexion sociale échouée',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      throw _handleDioException(e);
    }
  }

  /// Convertit une DioException en exception personnalisée
  ApiException _handleDioException(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return TimeoutException(
          message: 'La requête a pris trop de temps',
        );

      case DioExceptionType.connectionError:
        return NetworkException(
          message: 'Pas de connexion internet',
        );

      case DioExceptionType.badResponse:
        final statusCode = error.response?.statusCode;
        final data = error.response?.data;

        // Message d'erreur du serveur
        String errorMessage = 'Une erreur est survenue';
        if (data is Map<String, dynamic>) {
          errorMessage = data['message'] ?? 
                        data['error'] ?? 
                        data['detail'] ?? 
                        errorMessage;
        }

        switch (statusCode) {
          case 400:
            return ValidationException(
              message: errorMessage,
              data: data is Map ? data['errors'] : null,
            );

          case 401:
            return AuthException(
              message: errorMessage,
              statusCode: 401,
            );

          case 403:
            return AuthException(
              message: 'Accès refusé',
              statusCode: 403,
            );

          case 404:
            return NotFoundException(
              message: errorMessage,
            );

          case 422:
            return ValidationException(
              message: errorMessage,
              data: data is Map ? data['errors'] : null,
            );

          case 500:
          case 502:
          case 503:
            return ServerException(
              message: 'Erreur serveur. Réessayez plus tard.',
              statusCode: statusCode,
            );

          default:
            return ServerException(
              message: errorMessage,
              statusCode: statusCode,
            );
        }

      case DioExceptionType.cancel:
        return NetworkException(
          message: 'Requête annulée',
        );

      default:
        return NetworkException(
          message: error.message ?? 'Erreur réseau inconnue',
        );
    }
  }
}
