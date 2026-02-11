import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/reset_password_request.dart';

/// Use Case pour la réinitialisation du mot de passe
/// 
/// Responsabilité unique: Réinitialiser le mot de passe avec un OTP validé
class ResetPasswordUseCase {
  final AuthRepository repository;

  ResetPasswordUseCase(this.repository);

  /// Exécute la réinitialisation
  /// 
  /// @param email Email de l'utilisateur
  /// @param otp Code OTP validé
  /// @param newPassword Nouveau mot de passe
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> call({
    required String email,
    required String otp,
    required String newPassword,
  }) async {
    final request = ResetPasswordRequest(
      email: email,
      otp: otp,
      newPassword: newPassword,
    );

    return await repository.resetPassword(request);
  }
}
