import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;

  AuthRepositoryImpl(this.remoteDataSource);

  @override
  Future<User?> getCurrentUser() async => null;

  @override
  Future<void> login(String email, String password) async {
    await remoteDataSource.login(email, password);
  }

  @override
  Future<void> register(String name, String email, String password) async {}

  @override
  Future<void> logout() async {}
}
