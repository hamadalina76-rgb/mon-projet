import '../entities/user.dart';

abstract class AuthRepository {
  Future<User?> getCurrentUser();
  Future<void> login(String email, String password);
  Future<void> register(String name, String email, String password);
  Future<void> logout();
}
