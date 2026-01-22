import '../entities/courier.dart';

abstract class AuthRepository {
  Future<Courier> login({required String email, required String password});
  Future<Courier> register({required Map<String, dynamic> data});
  Future<void> logout();
  Future<bool> isLoggedIn();
  Future<Courier?> getCurrentCourier();
  Future<Courier> verifyPhone({required String phone, required String code});
  Future<Courier> updateProfile({required Map<String, dynamic> data});
}
