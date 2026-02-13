import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/otp_result.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/login_request.dart';

/// Use Case pour la connexion (envoie OTP)
/// 
/// Responsabilité unique: Envoyer OTP à l'utilisateur après vérification des credentials
class LoginUseCase {
  final AuthRepository repository;

  LoginUseCase(this.repository);

  /// Exécute la connexion et envoie l'OTP
  /// 
  /// @param email Email de l'utilisateur
  /// @param password Mot de passe
  /// @returns Right(OtpResult) si succès (OTP envoyé)
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, OtpResult>> call({
    required String email,
    required String password,
  }) async {
    final request = LoginRequest(
      email: email,
      password: password,
    );

    return await repository.login(request);
  }
}
