import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/user.dart';
import '../../domain/entities/otp_result.dart';

part 'auth_state.freezed.dart';

/// État d'authentification de l'application
/// 
/// Utilise Freezed pour l'immutabilité et le pattern matching
@freezed
class AuthState with _$AuthState {
  /// État initial au démarrage de l'app
  const factory AuthState.initial() = _Initial;

  /// En cours de chargement (login, register, etc.)
  const factory AuthState.loading() = _Loading;

  /// OTP envoyé, en attente de vérification
  const factory AuthState.otpSent({
    required OtpResult otpResult,
  }) = _OtpSent;

  /// Utilisateur authentifié avec succès
  const factory AuthState.authenticated({
    required User user,
  }) = _Authenticated;

  /// Inscription réussie (doit se connecter ensuite)
  const factory AuthState.registered() = _Registered;

  /// Utilisateur non authentifié
  const factory AuthState.unauthenticated() = _Unauthenticated;

  /// Erreur d'authentification
  const factory AuthState.error({
    required String message,
  }) = _Error;
}
