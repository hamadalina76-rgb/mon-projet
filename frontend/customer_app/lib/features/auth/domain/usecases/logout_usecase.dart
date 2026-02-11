import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/auth_repository.dart';

/// Use Case pour la déconnexion
/// 
/// Responsabilité unique: Déconnecter l'utilisateur
class LogoutUseCase {
  final AuthRepository repository;

  LogoutUseCase(this.repository);

  /// Exécute la déconnexion
  /// 
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur (rare car on déconnecte toujours localement)
  Future<Either<Failure, Unit>> call() async {
    return await repository.logout();
  }
}
