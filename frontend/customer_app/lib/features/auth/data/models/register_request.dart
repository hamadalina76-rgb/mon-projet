/// Modèle de requête pour l'inscription d'un nouvel utilisateur
/// 
/// UTILITÉ:
/// - Encapsule toutes les informations nécessaires pour créer un compte
/// - Envoyé au backend via POST /api/auth/register
/// - Déclenche l'envoi d'un OTP par email pour vérification
/// 
/// NOTES:
/// - Après inscription, l'utilisateur doit vérifier son email via OTP
/// - Le téléphone doit respecter le format marocain
import 'package:freezed_annotation/freezed_annotation.dart';

part 'register_request.freezed.dart';
part 'register_request.g.dart';

@freezed
class RegisterRequest with _$RegisterRequest {
  const factory RegisterRequest({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
    required String phone,
  }) = _RegisterRequest;

  factory RegisterRequest.fromJson(Map<String, dynamic> json) =>
      _$RegisterRequestFromJson(json);
}
