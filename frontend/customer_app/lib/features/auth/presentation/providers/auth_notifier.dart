import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/errors/failures.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/login_usecase.dart';
import '../../domain/usecases/register_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import '../../domain/usecases/forgot_password_usecase.dart';
import '../../domain/usecases/verify_otp_usecase.dart';
import '../../domain/usecases/reset_password_usecase.dart';
import '../../domain/usecases/get_current_user_usecase.dart';
import '../../domain/usecases/get_cached_user_usecase.dart';
import '../../domain/usecases/check_login_status_usecase.dart';
import '../../data/models/verify_otp_request.dart';
import 'auth_state.dart';

/// Notifier pour gérer l'état d'authentification
/// 
/// Responsabilités:
/// - Exécuter les use cases
/// - Mettre à jour l'état (AuthState)
/// - Gérer les erreurs
/// - Notifier l'UI des changements
class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository _authRepository;
  final LoginUseCase _loginUseCase;
  final RegisterUseCase _registerUseCase;
  final LogoutUseCase _logoutUseCase;
  final ForgotPasswordUseCase _forgotPasswordUseCase;
  final VerifyOtpUseCase _verifyOtpUseCase;
  final ResetPasswordUseCase _resetPasswordUseCase;
  final GetCurrentUserUseCase _getCurrentUserUseCase;
  final GetCachedUserUseCase _getCachedUserUseCase;
  final CheckLoginStatusUseCase _checkLoginStatusUseCase;

  AuthNotifier({
    required AuthRepository authRepository,
    required LoginUseCase loginUseCase,
    required RegisterUseCase registerUseCase,
    required LogoutUseCase logoutUseCase,
    required ForgotPasswordUseCase forgotPasswordUseCase,
    required VerifyOtpUseCase verifyOtpUseCase,
    required ResetPasswordUseCase resetPasswordUseCase,
    required GetCurrentUserUseCase getCurrentUserUseCase,
    required GetCachedUserUseCase getCachedUserUseCase,
    required CheckLoginStatusUseCase checkLoginStatusUseCase,
  })  : _authRepository = authRepository,
        _loginUseCase = loginUseCase,
        _registerUseCase = registerUseCase,
        _logoutUseCase = logoutUseCase,
        _forgotPasswordUseCase = forgotPasswordUseCase,
        _verifyOtpUseCase = verifyOtpUseCase,
        _resetPasswordUseCase = resetPasswordUseCase,
        _getCurrentUserUseCase = getCurrentUserUseCase,
        _getCachedUserUseCase = getCachedUserUseCase,
        _checkLoginStatusUseCase = checkLoginStatusUseCase,
        super(const AuthState.initial());

  /// Récupère l'utilisateur actuellement connecté (si existe)
  User? get currentUser {
    return state.maybeMap(
      authenticated: (state) => state.user,
      orElse: () => null,
    );
  }

  /// Vérifie si l'utilisateur est connecté
  bool get isAuthenticated {
    return state.maybeWhen(
      authenticated: (_) => true,
      orElse: () => false,
    );
  }

  /// Vérifie l'état d'authentification au démarrage de l'app
  /// 
  /// Essaie de restaurer la session depuis le cache
  Future<void> checkAuthStatus() async {
    state = const AuthState.loading();

    final isLoggedIn = await _checkLoginStatusUseCase.call();

    if (!isLoggedIn) {
      state = const AuthState.unauthenticated();
      return;
    }

    // Essayer de récupérer l'utilisateur depuis le cache
    final result = await _getCachedUserUseCase.call();

    result.fold(
      (failure) {
        // Si pas de cache, essayer depuis l'API
        _getCurrentUser();
      },
      (user) {
        state = AuthState.authenticated(user: user);
      },
    );
  }

  /// Connexion avec email et mot de passe (envoie OTP)
  Future<void> login({
    required String email,
    required String password,
  }) async {
    state = const AuthState.loading();

    final result = await _loginUseCase.call(
      email: email,
      password: password,
    );

    result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
      },
      (otpResult) {
        state = AuthState.otpSent(otpResult: otpResult);
      },
    );
  }

  /// Vérification du code OTP pour authentification (login)
  /// 
  /// @returns true si succès, false si erreur
  Future<bool> verifyLoginOtp({
    required String email,
    required String otpCode,
  }) async {
    state = const AuthState.loading();

    final result = await _authRepository.verifyOtp(
      VerifyOtpRequest(email: email, otp: otpCode, type: 'login'),
    );

    return result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
        return false;
      },
      (user) {
        state = AuthState.authenticated(user: user);
        return true;
      },
    );
  }

  /// Inscription d'un nouveau client
  Future<void> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phoneNumber,
    required String password,
  }) async {
    state = const AuthState.loading();

    final result = await _registerUseCase.call(
      firstName: firstName,
      lastName: lastName,
      email: email,
      phoneNumber: phoneNumber,
      password: password,
    );

    result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
      },
      (_) {
        // Inscription réussie, rediriger vers login
        state = const AuthState.registered();
      },
    );
  }

  /// Déconnexion
  Future<void> logout() async {
    state = const AuthState.loading();

    final result = await _logoutUseCase.call();

    result.fold(
      (failure) {
        // Même en cas d'erreur, on déconnecte l'utilisateur localement
        state = const AuthState.unauthenticated();
      },
      (_) {
        state = const AuthState.unauthenticated();
      },
    );
  }

  /// Demande de réinitialisation de mot de passe (envoie OTP)
  /// 
  /// @returns true si succès, false si erreur
  Future<bool> forgotPassword({required String email}) async {
    state = const AuthState.loading();

    final result = await _forgotPasswordUseCase.call(email: email);

    return result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
        return false;
      },
      (_) {
        // Retour à l'état unauthenticated après envoi OTP
        state = const AuthState.unauthenticated();
        return true;
      },
    );
  }
  
  /// Renvoyer OTP
  /// 
  /// @returns true si succès, false si erreur
  Future<bool> resendOtp({required String email}) async {
    final result = await _authRepository.resendOtp(email);

    return result.fold(
      (failure) {
        return false;
      },
      (_) {
        return true;
      },
    );
  }

  /// Vérification du code OTP
  /// 
  /// @returns true si OTP valide, false si invalide
  Future<bool> verifyOtp({
    required String email,
    required String otp,
  }) async {
    state = const AuthState.loading();

    final result = await _verifyOtpUseCase.call(
      email: email,
      otp: otp,
    );

    return result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
        return false;
      },
      (_) {
        state = const AuthState.unauthenticated();
        return true;
      },
    );
  }

  /// Réinitialisation du mot de passe avec OTP validé
  /// 
  /// @returns true si succès, false si erreur
  Future<bool> resetPassword({
    required String email,
    required String otp,
    required String newPassword,
  }) async {
    state = const AuthState.loading();

    final result = await _resetPasswordUseCase.call(
      email: email,
      otp: otp,
      newPassword: newPassword,
    );

    return result.fold(
      (failure) {
        state = AuthState.error(message: _mapFailureToMessage(failure));
        return false;
      },
      (_) {
        state = const AuthState.unauthenticated();
        return true;
      },
    );
  }

  /// Récupération du profil utilisateur actuel
  /// 
  /// Utilisé pour rafraîchir les données du profil
  Future<void> refreshUser() async {
    await _getCurrentUser();
  }

  /// Récupère le profil depuis l'API
  Future<void> _getCurrentUser() async {
    final result = await _getCurrentUserUseCase.call();

    result.fold(
      (failure) {
        // Si erreur, déconnecter l'utilisateur
        state = const AuthState.unauthenticated();
      },
      (user) {
        state = AuthState.authenticated(user: user);
      },
    );
  }

  /// Convertit un Failure en message d'erreur lisible
  String _mapFailureToMessage(Failure failure) {
    if (failure is NetworkFailure) {
      return 'Pas de connexion internet';
    } else if (failure is ServerFailure) {
      return failure.message.isNotEmpty
          ? failure.message
          : 'Erreur serveur. Réessayez plus tard.';
    } else if (failure is AuthFailure) {
      return failure.message.isNotEmpty
          ? failure.message
          : 'Email ou mot de passe incorrect';
    } else if (failure is ValidationFailure) {
      return failure.message.isNotEmpty
          ? failure.message
          : 'Données invalides';
    } else if (failure is NotFoundFailure) {
      return failure.message.isNotEmpty
          ? failure.message
          : 'Ressource non trouvée';
    } else if (failure is CacheFailure) {
      return 'Erreur de cache local';
    } else {
      return 'Une erreur est survenue';
    }
  }

  /// Réinitialise l'état à unauthenticated
  /// 
  /// Utilisé après traitement d'une erreur
  void clearError() {
    state = const AuthState.unauthenticated();
  }
}
