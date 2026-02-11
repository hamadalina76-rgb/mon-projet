import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/user.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/login_request.dart';

/// Use Case pour la connexion
/// 
/// Responsabilité unique: Connecter un utilisateur avec email/password
class LoginUseCase {
  final AuthRepository repository;

  LoginUseCase(this.repository);

  /// Exécute la connexion
  /// 
  /// @param email Email de l'utilisateur
  /// @param password Mot de passe
  /// @returns Right(User) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, User>> call({
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
