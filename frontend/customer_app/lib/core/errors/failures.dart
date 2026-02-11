import 'package:equatable/equatable.dart';

/// Failures pour la couche domain (Clean Architecture)
/// 
/// Contrairement aux Exceptions (data layer),
/// les Failures sont utilisées dans le domain avec Either<Failure, Success>
abstract class Failure extends Equatable {
  final String message;

  const Failure(this.message);

  @override
  List<Object> get props => [message];

  @override
  bool get stringify => true;
}

/// Échec réseau
class NetworkFailure extends Failure {
  const NetworkFailure([String message = 'Pas de connexion Internet']) 
      : super(message);
}

/// Échec serveur
class ServerFailure extends Failure {
  const ServerFailure([String message = 'Erreur serveur']) 
      : super(message);
}

/// Échec d'authentification
class AuthFailure extends Failure {
  const AuthFailure([String message = 'Authentification échouée']) 
      : super(message);
}

/// Échec de validation
class ValidationFailure extends Failure {
  const ValidationFailure(String message) : super(message);
}

/// Ressource non trouvée
class NotFoundFailure extends Failure {
  const NotFoundFailure([String message = 'Ressource non trouvée']) 
      : super(message);
}

/// Échec de cache
class CacheFailure extends Failure {
  const CacheFailure([String message = 'Erreur de cache']) 
      : super(message);
}

/// Échec inconnu
class UnknownFailure extends Failure {
  const UnknownFailure([String message = 'Erreur inconnue']) 
      : super(message);
}
