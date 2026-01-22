import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/entities/user.dart';

class AuthState {
  final User? user;
  final bool isLoading;

  AuthState({this.user, this.isLoading = false});
}

class AuthNotifier extends StateNotifier<AuthState> {
  AuthNotifier() : super(AuthState());
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier();
});
