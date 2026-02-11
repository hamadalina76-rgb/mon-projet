import '../repositories/auth_repository.dart';

/// Use Case pour vérifier si l'utilisateur est connecté
/// 
/// Responsabilité unique: Déterminer l'état de connexion
class CheckLoginStatusUseCase {
  final AuthRepository repository;

  CheckLoginStatusUseCase(this.repository);

  /// Vérifie si l'utilisateur est connecté
  /// 
  /// @returns true si un token existe
  Future<bool> call() async {
    return await repository.isLoggedIn();
  }
}
