/// Modèle de requête pour l'authentification (Login)
/// 
/// UTILITÉ:
/// - Encapsule les données nécessaires pour se connecter à l'application
/// - Envoyé au backend via POST /api/auth/login
/// 
/// NOTES:
/// - Utilise @freezed pour immutabilité et génération automatique
/// - Sérialisable en JSON via json_serializable
/// - Le mot de passe n'est jamais stocké localement
import 'package:freezed_annotation/freezed_annotation.dart';

part 'login_request.freezed.dart';
part 'login_request.g.dart';

@freezed
class LoginRequest with _$LoginRequest {
  const factory LoginRequest({
    required String email,
    required String password,
  }) = _LoginRequest;

  factory LoginRequest.fromJson(Map<String, dynamic> json) =>
      _$LoginRequestFromJson(json);
}
