import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../entities/user.dart';
import '../entities/otp_result.dart';
import '../entities/login_result.dart';
import '../../data/models/login_request.dart';
import '../../data/models/register_request.dart';
import '../../data/models/forgot_password_request.dart';
import '../../data/models/verify_otp_request.dart';
import '../../data/models/reset_password_request.dart';
import '../../data/models/social_login_request.dart';

/// Interface du repository d'authentification (Domain Layer)
/// 
/// Définit le contrat entre la couche domain et data.
/// Retourne Either<Failure, Success> pour gérer les erreurs de manière fonctionnelle.
abstract class AuthRepository {
  /// Connexion avec email et mot de passe
  /// 
  /// Retourne LoginResult qui peut être:
  /// - requiresOtp: Compte PENDING, nécessite vérification OTP
  /// - authenticated: Compte ACTIVE, connexion directe avec tokens
  /// 
  /// @returns Right(LoginResult) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, LoginResult>> login(LoginRequest request);

  /// Vérification du code OTP pour authentification
  /// 
  /// @returns Right(User) si OTP valide et login réussi
  /// @returns Left(Failure) si OTP invalide
  Future<Either<Failure, User>> verifyOtp(VerifyOtpRequest request);
  
  /// Renvoyer OTP
  /// 
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> resendOtp(String email);

  /// Inscription d'un nouvel utilisateur
  /// Retourne Unit en cas de succès (inscription réussie)
  Future<Either<Failure, Unit>> register(RegisterRequest request);

  /// Demande de réinitialisation de mot de passe
  /// Envoie un OTP par email
  /// 
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> forgotPassword(ForgotPasswordRequest request);

  /// Réinitialisation du mot de passe avec OTP validé
  /// 
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> resetPassword(ResetPasswordRequest request);

  /// Déconnexion
  /// 
  /// @returns Right(Unit) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, Unit>> logout();

  /// Récupération du profil utilisateur actuel
  /// 
  /// @returns Right(User) si succès
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, User>> getCurrentUser();

  /// Récupération de l'utilisateur depuis le cache local
  /// Utilisé pour restaurer la session au démarrage
  /// 
  /// @returns Right(User) si user en cache
  /// @returns Left(CacheFailure) si aucun cache
  Future<Either<Failure, User>> getCachedUser();

  /// Vérifie si l'utilisateur est connecté
  /// 
  /// @returns true si un token existe
  Future<bool> isLoggedIn();

  /// Connexion via fournisseur social (Google/Facebook)
  /// 
  /// @returns Right(User) si authentification réussie
  /// @returns Left(Failure) si erreur
  Future<Either<Failure, User>> socialLogin(SocialLoginRequest request);
}
