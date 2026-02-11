import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/user.dart';
import '../repositories/auth_repository.dart';

/// Use Case pour récupérer le profil utilisateur actuel
/// 
/// Responsabilité unique: Récupérer les données du user connecté
class GetCurrentUserUseCase {
  final AuthRepository repository;

  GetCurrentUserUseCase(this.repository);

  /// Exécute la récupération du profil
  /// 
  /// @returns Right(User) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, User>> call() async {
    return await repository.getCurrentUser();
  }
}
