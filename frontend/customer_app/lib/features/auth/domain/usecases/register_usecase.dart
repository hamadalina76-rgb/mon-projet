import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/user.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/register_request.dart';

/// Use Case pour l'inscription
/// 
/// Responsabilité unique: Inscrire un nouveau client
class RegisterUseCase {
  final AuthRepository repository;

  RegisterUseCase(this.repository);

  /// Exécute l'inscription
  /// 
  /// @param firstName Prénom
  /// @param lastName Nom
  /// @param email Email
  /// @param phone Téléphone
  /// @param password Mot de passe
  /// @returns Right(User) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, User>> call({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
  }) async {
    final request = RegisterRequest(
      firstName: firstName,
      lastName: lastName,
      email: email,
      phone: phone,
      password: password,
    );

    return await repository.register(request);
  }
}
