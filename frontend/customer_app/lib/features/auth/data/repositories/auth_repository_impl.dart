import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../../../../core/errors/exceptions.dart';
import '../../domain/entities/user.dart';
import '../../domain/entities/otp_result.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../datasources/auth_local_datasource.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';
import '../models/forgot_password_request.dart';
import '../models/verify_otp_request.dart';
import '../models/reset_password_request.dart';
import '../models/social_login_request.dart';

/// Implémentation du repository d'authentification (Data Layer)
/// 
/// Responsabilités:
/// - Orchestrer les appels entre datasources remote et local
/// - Convertir les Exceptions en Failures
/// - Gérer le cache et la synchronisation
/// - Retourner des Either<Failure, Success>
class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final AuthLocalDataSource localDataSource;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.localDataSource,
  });

  @override
  Future<Either<Failure, OtpResult>> login(LoginRequest request) async {
    try {
      // 1. Appel API (envoie OTP)
      final otpResponse = await remoteDataSource.login(request);

      // 2. Convertir en entity
      final result = OtpResult(
        message: otpResponse.message,
        email: otpResponse.email,
        otpSent: otpResponse.otpSent,
        expirationMinutes: otpResponse.expirationMinutes,
      );

      return Right(result);
    } on AuthException catch (e) {
      return Left(AuthFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, User>> verifyOtp(VerifyOtpRequest request) async {
    try {
      // 1. Appel API (vérifie OTP)
      final authResponse = await remoteDataSource.verifyOtp(request);

      // 2. Sauvegarder les tokens
      await localDataSource.saveTokens(
        token: authResponse.token,
        refreshToken: authResponse.refreshToken,
      );

      // 3. Sauvegarder l'utilisateur en cache
      await localDataSource.saveUser(authResponse.user);

      // 4. Convertir UserModel → User (Entity)
      final user = _mapUserModelToEntity(authResponse.user);

      return Right(user);
    } on AuthException catch (e) {
      return Left(AuthFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } on CacheException catch (e) {
      return Left(CacheFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, Unit>> register(RegisterRequest request) async {
    try {
      // 1. Appel API (inscription uniquement, pas de login automatique)
      await remoteDataSource.register(request);

      // 2. Retourner succès
      return const Right(unit);
    } on ValidationException catch (e) {
      return Left(ValidationFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } on CacheException catch (e) {
      return Left(CacheFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, Unit>> resendOtp(String email) async {
    try {
      await remoteDataSource.resendOtp(email);
      return const Right(unit);
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, Unit>> forgotPassword(
    ForgotPasswordRequest request,
  ) async {
    try {
      await remoteDataSource.forgotPassword(request);
      return const Right(unit);
    } on NotFoundException catch (e) {
      return Left(NotFoundFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, Unit>> resetPassword(
    ResetPasswordRequest request,
  ) async {
    try {
      await remoteDataSource.resetPassword(request);
      return const Right(unit);
    } on AuthException catch (e) {
      return Left(AuthFailure(e.message));
    } on ValidationException catch (e) {
      return Left(ValidationFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, Unit>> logout() async {
    try {
      // 1. Appel API (invalide le refresh token côté serveur)
      await remoteDataSource.logout();

      // 2. Supprimer toutes les données locales
      await localDataSource.clearAll();

      return const Right(unit);
    } on CacheException catch (e) {
      // Même si le cache échoue, on considère la déconnexion réussie
      return Left(CacheFailure(e.message));
    } catch (e) {
      // Même en cas d'erreur serveur, on déconnecte localement
      try {
        await localDataSource.clearAll();
      } catch (_) {}
      return const Right(unit);
    }
  }

  @override
  Future<Either<Failure, User>> getCurrentUser() async {
    try {
      // 1. Récupérer depuis l'API
      final userModel = await remoteDataSource.getCurrentUser();

      // 2. Mettre à jour le cache
      await localDataSource.saveUser(userModel);

      // 3. Convertir en entity
      final user = _mapUserModelToEntity(userModel);

      return Right(user);
    } on AuthException catch (e) {
      return Left(AuthFailure(e.message));
    } on NetworkException catch (_) {
      // En cas d'erreur réseau, essayer le cache
      return getCachedUser();
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (_) {
      // En cas de timeout, essayer le cache
      return getCachedUser();
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }

  @override
  Future<Either<Failure, User>> getCachedUser() async {
    try {
      final userModel = await localDataSource.getCachedUser();
      final user = _mapUserModelToEntity(userModel);
      return Right(user);
    } on CacheException catch (e) {
      return Left(CacheFailure(e.message));
    } catch (e) {
      return Left(CacheFailure('Aucun utilisateur en cache'));
    }
  }

  @override
  Future<bool> isLoggedIn() async {
    return await localDataSource.isLoggedIn();
  }

  /// Convertit un UserModel (data) en User (domain entity)
  User _mapUserModelToEntity(userModel) {
    return userModel.toEntity();
  }

  @override
  Future<Either<Failure, User>> socialLogin(SocialLoginRequest request) async {
    try {
      // 1. Appel API (connexion sociale)
      final authResponse = await remoteDataSource.socialLogin(request);

      // 2. Sauvegarder les tokens
      await localDataSource.saveTokens(
        token: authResponse.token,
        refreshToken: authResponse.refreshToken,
      );

      // 3. Sauvegarder l'utilisateur en cache
      await localDataSource.saveUser(authResponse.user);

      // 4. Convertir UserModel → User (Entity)
      final user = _mapUserModelToEntity(authResponse.user);

      return Right(user);
    } on AuthException catch (e) {
      return Left(AuthFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on TimeoutException catch (e) {
      return Left(NetworkFailure(e.message));
    } on CacheException catch (e) {
      return Left(CacheFailure(e.message));
    } catch (e) {
      return Left(UnknownFailure('Erreur inattendue: $e'));
    }
  }
}
