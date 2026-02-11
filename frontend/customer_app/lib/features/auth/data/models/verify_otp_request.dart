/// Modèle de requête pour vérification du code OTP
/// 
/// UTILITÉ:
/// - Vérifie le code OTP reçu par email
/// - Envoyé au backend via POST /api/auth/verify-otp
/// - Utilisé dans 2 scénarios:
///   1. Vérification email après inscription
///   2. Validation avant réinitialisation mot de passe
/// 
/// SCÉNARIOS D'UTILISATION:
/// 
/// 1. APRÈS INSCRIPTION:
///    - Vérifie l'email du nouvel utilisateur
///    - Active le compte après vérification réussie
/// 
/// 2. RÉINITIALISATION MOT DE PASSE:
///    - Confirme l'identité de l'utilisateur
///    - Autorise la définition d'un nouveau mot de passe
/// 
/// NOTES:
/// - OTP valide pendant 10 minutes
/// - Maximum 3 tentatives incorrectes
/// - Peut être renvoyé après 60 secondes

import 'package:freezed_annotation/freezed_annotation.dart';

part 'verify_otp_request.freezed.dart';
part 'verify_otp_request.g.dart';

@freezed
class VerifyOtpRequest with _$VerifyOtpRequest {
  const factory VerifyOtpRequest({
    required String email,
    required String otp,
  }) = _VerifyOtpRequest;

  factory VerifyOtpRequest.fromJson(Map<String, dynamic> json) =>
      _$VerifyOtpRequestFromJson(json);
}
