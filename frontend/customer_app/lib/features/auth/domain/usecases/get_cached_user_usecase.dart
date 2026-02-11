import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/user.dart';
import '../repositories/auth_repository.dart';

/// Use Case pour récupérer l'utilisateur depuis le cache
/// 
/// Responsabilité unique: Restaurer la session au démarrage de l'app
class GetCachedUserUseCase {
  final AuthRepository repository;

  GetCachedUserUseCase(this.repository);

  /// Exécute la récupération depuis le cache
  /// 
  /// @returns Right(User) si user en cache
  /// @returns Left(CacheFailure) si aucun cache
  Future<Either<Failure, User>> call() async {
    return await repository.getCachedUser();
  }
}
