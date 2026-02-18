import 'package:flutter_facebook_auth/flutter_facebook_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:logger/logger.dart';
import '../models/social_login_request.dart';

/// Service pour gérer l'authentification sociale via SDK natifs
/// 
/// Responsabilités:
/// - Connexion via Google Sign-In SDK
/// - Connexion via Facebook Login SDK
/// - Récupération des access tokens pour envoyer au backend
class SocialAuthService {
  final Logger _logger;
  late final GoogleSignIn _googleSignIn;
  late final FacebookAuth _facebookAuth;
  
  SocialAuthService({Logger? logger}) : _logger = logger ?? Logger() {
    _googleSignIn = GoogleSignIn(
      scopes: [
        'email',
        'profile',
      ],
    );
    _facebookAuth = FacebookAuth.instance;
  }

  /// Connexion avec Google
  /// 
  /// @returns SocialLoginRequest avec l'access token Google
  /// @throws SocialAuthException si connexion échoue
  Future<SocialLoginRequest> signInWithGoogle() async {
    try {
      _logger.i('🔐 Initiating Google Sign-In...');
      
      // Déconnexion préalable pour forcer le choix de compte
      await _googleSignIn.signOut();
      
      final GoogleSignInAccount? account = await _googleSignIn.signIn();
      
      if (account == null) {
        _logger.w('Google Sign-In cancelled by user');
        throw SocialAuthCancelledException('Connexion Google annulée');
      }
      
      _logger.i('✅ Google account selected: ${account.email}');
      
      // Récupérer l'authentification pour obtenir les tokens
      final GoogleSignInAuthentication auth = await account.authentication;
      
      // Préférer l'ID token si disponible, sinon utiliser l'access token
      final String? token = auth.idToken ?? auth.accessToken;
      
      if (token == null) {
        _logger.e('Failed to get Google token');
        throw SocialAuthException('Impossible d\'obtenir le token Google');
      }
      
      _logger.i('✅ Google token obtained successfully');
      
      return SocialLoginRequest(
        accessToken: token,
        provider: SocialProvider.GOOGLE,
      );
    } catch (e) {
      if (e is SocialAuthException) rethrow;
      _logger.e('Google Sign-In error: $e');
      throw SocialAuthException('Erreur lors de la connexion Google: $e');
    }
  }

  /// Connexion avec Facebook
  /// 
  /// @returns SocialLoginRequest avec l'access token Facebook
  /// @throws SocialAuthException si connexion échoue
  Future<SocialLoginRequest> signInWithFacebook() async {
    try {
      _logger.i('🔐 Initiating Facebook Login...');
      
      // Déconnexion préalable
      await _facebookAuth.logOut();
      
      final LoginResult result = await _facebookAuth.login(
        permissions: ['email', 'public_profile'],
      );
      
      switch (result.status) {
        case LoginStatus.success:
          final AccessToken? accessToken = result.accessToken;
          
          if (accessToken == null) {
            _logger.e('Facebook login succeeded but no access token');
            throw SocialAuthException('Token Facebook non disponible');
          }
          
          _logger.i('✅ Facebook login successful');
          
          return SocialLoginRequest(
            accessToken: accessToken.tokenString,
            provider: SocialProvider.FACEBOOK,
          );
          
        case LoginStatus.cancelled:
          _logger.w('Facebook login cancelled by user');
          throw SocialAuthCancelledException('Connexion Facebook annulée');
          
        case LoginStatus.failed:
          _logger.e('Facebook login failed: ${result.message}');
          throw SocialAuthException('Échec de la connexion Facebook: ${result.message}');
          
        case LoginStatus.operationInProgress:
          _logger.w('Facebook login operation already in progress');
          throw SocialAuthException('Une connexion Facebook est déjà en cours');
      }
    } catch (e) {
      if (e is SocialAuthException) rethrow;
      _logger.e('Facebook Login error: $e');
      throw SocialAuthException('Erreur lors de la connexion Facebook: $e');
    }
  }

  /// Déconnexion de tous les fournisseurs
  Future<void> signOutAll() async {
    try {
      await Future.wait([
        _googleSignIn.signOut(),
        _facebookAuth.logOut(),
      ]);
      _logger.i('✅ Signed out from all social providers');
    } catch (e) {
      _logger.w('Error signing out from social providers: $e');
    }
  }

  /// Vérifier si l'utilisateur est connecté via Google
  Future<bool> isSignedInWithGoogle() async {
    return await _googleSignIn.isSignedIn();
  }

  /// Vérifier si l'utilisateur est connecté via Facebook
  Future<bool> isSignedInWithFacebook() async {
    final accessToken = await _facebookAuth.accessToken;
    return accessToken != null;
  }
}

/// Exception pour les erreurs d'authentification sociale
class SocialAuthException implements Exception {
  final String message;
  SocialAuthException(this.message);
  
  @override
  String toString() => message;
}

/// Exception pour l'annulation par l'utilisateur
class SocialAuthCancelledException extends SocialAuthException {
  SocialAuthCancelledException(super.message);
}
