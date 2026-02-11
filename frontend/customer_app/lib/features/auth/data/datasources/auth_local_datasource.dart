import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../../core/errors/exceptions.dart';
import '../models/user_model.dart';

/// DataSource local pour l'authentification
/// 
/// Responsabilités:
/// - Stockage sécurisé des tokens JWT (FlutterSecureStorage)
/// - Cache des données utilisateur (SharedPreferences)
/// - Vérification de l'état de connexion
/// - Nettoyage des données à la déconnexion
/// 
/// Architecture:
/// - Tokens sensibles → FlutterSecureStorage (chiffré)
/// - Données utilisateur → SharedPreferences (rapide)
abstract class AuthLocalDataSource {
  /// Sauvegarde les tokens JWT
  Future<void> saveTokens({
    required String token,
    required String refreshToken,
  });

  /// Récupère le token d'accès
  /// @returns null si aucun token
  Future<String?> getToken();

  /// Récupère le refresh token
  /// @returns null si aucun token
  Future<String?> getRefreshToken();

  /// Supprime tous les tokens
  Future<void> clearTokens();

  /// Sauvegarde les données utilisateur en cache
  Future<void> saveUser(UserModel user);

  /// Récupère les données utilisateur du cache
  /// @throws CacheException si aucune donnée
  Future<UserModel> getCachedUser();

  /// Supprime les données utilisateur du cache
  Future<void> clearUser();

  /// Vérifie si l'utilisateur est connecté
  /// @returns true si un token existe
  Future<bool> isLoggedIn();

  /// Supprime toutes les données d'authentification
  Future<void> clearAll();
}

/// Implémentation de AuthLocalDataSource
class AuthLocalDataSourceImpl implements AuthLocalDataSource {
  final FlutterSecureStorage secureStorage;
  final SharedPreferences sharedPreferences;

  // Clés pour le stockage sécurisé
  static const String _keyToken = 'auth_token';
  static const String _keyRefreshToken = 'auth_refresh_token';

  // Clés pour SharedPreferences
  static const String _keyUser = 'cached_user';
  static const String _keyIsLoggedIn = 'is_logged_in';

  AuthLocalDataSourceImpl({
    required this.secureStorage,
    required this.sharedPreferences,
  });

  @override
  Future<void> saveTokens({
    required String token,
    required String refreshToken,
  }) async {
    try {
      // Stockage sécurisé des tokens
      await Future.wait([
        secureStorage.write(key: _keyToken, value: token),
        secureStorage.write(key: _keyRefreshToken, value: refreshToken),
        sharedPreferences.setBool(_keyIsLoggedIn, true),
      ]);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la sauvegarde des tokens: $e',
      );
    }
  }

  @override
  Future<String?> getToken() async {
    try {
      return await secureStorage.read(key: _keyToken);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la récupération du token: $e',
      );
    }
  }

  @override
  Future<String?> getRefreshToken() async {
    try {
      return await secureStorage.read(key: _keyRefreshToken);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la récupération du refresh token: $e',
      );
    }
  }

  @override
  Future<void> clearTokens() async {
    try {
      await Future.wait([
        secureStorage.delete(key: _keyToken),
        secureStorage.delete(key: _keyRefreshToken),
      ]);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la suppression des tokens: $e',
      );
    }
  }

  @override
  Future<void> saveUser(UserModel user) async {
    try {
      final userJson = jsonEncode(user.toJson());
      await sharedPreferences.setString(_keyUser, userJson);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la sauvegarde de l\'utilisateur: $e',
      );
    }
  }

  @override
  Future<UserModel> getCachedUser() async {
    try {
      final userJson = sharedPreferences.getString(_keyUser);
      
      if (userJson == null) {
        throw CacheException(
          'Aucun utilisateur en cache',
        );
      }

      final userMap = jsonDecode(userJson) as Map<String, dynamic>;
      return UserModel.fromJson(userMap);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la récupération de l\'utilisateur: $e',
      );
    }
  }

  @override
  Future<void> clearUser() async {
    try {
      await sharedPreferences.remove(_keyUser);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la suppression de l\'utilisateur: $e',
      );
    }
  }

  @override
  Future<bool> isLoggedIn() async {
    try {
      // Vérifie si un token existe
      final token = await getToken();
      final hasToken = token != null && token.isNotEmpty;
      
      // Met à jour le flag
      await sharedPreferences.setBool(_keyIsLoggedIn, hasToken);
      
      return hasToken;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<void> clearAll() async {
    try {
      await Future.wait([
        clearTokens(),
        clearUser(),
        sharedPreferences.setBool(_keyIsLoggedIn, false),
      ]);
    } catch (e) {
      throw CacheException(
        'Erreur lors de la suppression des données: $e',
      );
    }
  }
}
