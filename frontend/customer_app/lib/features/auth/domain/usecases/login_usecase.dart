import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/login_result.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/login_request.dart';

/// Use Case pour la connexion
/// 
/// Responsabilité: Gérer la connexion avec support pour OTP (PENDING) ou auth directe (ACTIVE)
class LoginUseCase {
  final AuthRepository repository;

  LoginUseCase(this.repository);

  /// Exécute la connexion
  /// 
  /// @param email Email de l'utilisateur
  /// @param password Mot de passe
  /// @returns Right(LoginResult.requiresOtp) si compte PENDING (OTP requis)
  /// @returns Right(LoginResult.authenticated) si compte ACTIVE (connexion directe)
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, LoginResult>> call({
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
