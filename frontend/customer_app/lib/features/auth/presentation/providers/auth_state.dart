import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/user.dart';

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

  /// Utilisateur authentifié avec succès
  const factory AuthState.authenticated({
    required User user,
  }) = _Authenticated;

  /// Utilisateur non authentifié
  const factory AuthState.unauthenticated() = _Unauthenticated;

  /// Erreur d'authentification
  const factory AuthState.error({
    required String message,
  }) = _Error;
}
