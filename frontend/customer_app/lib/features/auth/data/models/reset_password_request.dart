/// Modèle de requête pour définir un nouveau mot de passe
/// 
/// UTILITÉ:
/// - Finalise le processus de réinitialisation de mot de passe
/// - Envoyé au backend via POST /api/auth/reset-password
/// - Utilisé après vérification OTP réussie
/// 
/// FLUX COMPLET DE RÉINITIALISATION:
/// 1. ForgotPasswordScreen → envoi email
/// 2. VerifyOtpScreen → vérification OTP
/// 3. ResetPasswordScreen → ce modèle
/// 4. LoginScreen → connexion avec nouveau mot de passe
/// 
/// VALIDATION MOT DE PASSE:
/// - Minimum 8 caractères
/// - Au moins 1 majuscule
/// - Au moins 1 chiffre
/// - Au moins 1 caractère spécial recommandé
/// 
/// NOTES:
/// - L'OTP doit être encore valide (< 10 minutes)
/// - L'ancien mot de passe est immédiatement invalidé
import 'package:freezed_annotation/freezed_annotation.dart';

part 'reset_password_request.freezed.dart';
part 'reset_password_request.g.dart';

@freezed
class ResetPasswordRequest with _$ResetPasswordRequest {
  const factory ResetPasswordRequest({
    required String email,
    required String otp,
    required String newPassword,
  }) = _ResetPasswordRequest;

  factory ResetPasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ResetPasswordRequestFromJson(json);
}
