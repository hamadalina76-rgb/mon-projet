import 'package:freezed_annotation/freezed_annotation.dart';
import 'user.dart';
import 'otp_result.dart';

part 'login_result.freezed.dart';

/// Résultat d'une tentative de connexion
/// 
/// Peut être:
/// - requiresOtp: Compte PENDING, OTP requis pour première vérification
/// - authenticated: Compte ACTIVE, connexion directe avec tokens
@freezed
class LoginResult with _$LoginResult {
  /// OTP envoyé (compte PENDING - première connexion)
  const factory LoginResult.requiresOtp({
    required OtpResult otpResult,
  }) = RequiresOtp;

  /// Authentification réussie (compte ACTIVE)
  const factory LoginResult.authenticated({
    required User user,
  }) = Authenticated;
}
