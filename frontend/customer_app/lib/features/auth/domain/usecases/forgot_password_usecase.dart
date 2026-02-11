import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/forgot_password_request.dart';

/// Use Case pour la demande de réinitialisation du mot de passe
/// 
/// Responsabilité unique: Envoyer un OTP par email
class ForgotPasswordUseCase {
  final AuthRepository repository;

  ForgotPasswordUseCase(this.repository);

  /// Exécute la demande de réinitialisation
  /// 
  /// @param email Email de l'utilisateur
  /// @returns Right(Unit) si OTP envoyé
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> call({
    required String email,
  }) async {
    final request = ForgotPasswordRequest(email: email);
    return await repository.forgotPassword(request);
  }
}
