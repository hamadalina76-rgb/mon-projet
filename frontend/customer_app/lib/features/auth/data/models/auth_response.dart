/// Modèle de réponse du backend pour toutes les opérations d'authentification
/// 
/// UTILITÉ:
/// - Encapsule la réponse complète après login/register réussi
/// - Contient les tokens JWT et les informations utilisateur
/// - Renvoyé par les endpoints: /login, /register, /refresh-token
/// 
/// ENDPOINTS QUI RETOURNENT AuthResponse:
/// - POST /api/auth/login
/// - POST /api/auth/register (après vérification OTP)
/// - POST /api/auth/refresh-token
/// - POST /api/auth/social/google
/// - POST /api/auth/social/facebook
/// 
/// SÉCURITÉ:
/// - Le token est envoyé dans les headers: Authorization: Bearer <token>
/// - Le refreshToken est utilisé uniquement pour renouveler le token
/// - Les tokens sont stockés dans FlutterSecureStorage (chiffré)
/// 
/// NOTES:
/// - Après réception, rediriger vers HomeScreen
/// - Écouter l'expiration du token (401) et refresh automatiquement
import 'package:freezed_annotation/freezed_annotation.dart';
import 'user_model.dart';

part 'auth_response.freezed.dart';
part 'auth_response.g.dart';

@freezed
class AuthResponse with _$AuthResponse {
  const factory AuthResponse({
    required String token,
    required String refreshToken,
    required UserModel user,
  }) = _AuthResponse;

  factory AuthResponse.fromJson(Map<String, dynamic> json) =>
      _$AuthResponseFromJson(json);
}
