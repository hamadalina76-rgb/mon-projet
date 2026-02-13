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
  /// @param phoneNumber Téléphone
  /// @param password Mot de passe
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> call({
    required String firstName,
    required String lastName,
    required String email,
    required String phoneNumber,
    required String password,
  }) async {
    final request = RegisterRequest(
      firstName: firstName,
      lastName: lastName,
      email: email,
      phoneNumber: phoneNumber,
      password: password,
      role: 'CUSTOMER',
    );

    return await repository.register(request);
  }
}
