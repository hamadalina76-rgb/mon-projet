/// Modèle de requête pour réinitialisation de mot de passe oublié
/// 
/// UTILITÉ:
/// - Initie le processus de récupération de mot de passe
/// - Envoyé au backend via POST /api/auth/forgot-password
/// - Déclenche l'envoi d'un code OTP par email
///
/// FLUX:
/// 1. ForgotPasswordScreen: saisie email
/// 2. Envoi ForgotPasswordRequest
/// 3. Backend envoie OTP par email
/// 4. VerifyOtpScreen: saisie OTP
/// 5. ResetPasswordScreen: nouveau mot de passe
/// 
/// NOTES:
/// - L'OTP est valide pendant 10 minutes
/// - Peut être renvoyé après 60 secondes
import 'package:freezed_annotation/freezed_annotation.dart';

part 'forgot_password_request.freezed.dart';
part 'forgot_password_request.g.dart';

@freezed
class ForgotPasswordRequest with _$ForgotPasswordRequest {
  const factory ForgotPasswordRequest({
    required String email,
  }) = _ForgotPasswordRequest;

  factory ForgotPasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ForgotPasswordRequestFromJson(json);
}
