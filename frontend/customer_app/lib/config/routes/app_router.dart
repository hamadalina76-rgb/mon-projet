import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/verify_otp_screen.dart';
import '../../features/auth/presentation/screens/reset_password_screen.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import 'route_names.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final appRouter = GoRouter(
  initialLocation: RouteNames.home,
  navigatorKey: _rootNavigatorKey,
  routes: [
    // Home / Onboarding
    GoRoute(
      path: RouteNames.home,
      builder: (context, state) => const HomeScreen(),
    ),

    // Authentication Flow
    GoRoute(
      path: RouteNames.login,
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: RouteNames.forgotPassword,
      builder: (context, state) => const ForgotPasswordScreen(),
    ),
    GoRoute(
      path: RouteNames.verifyOtp,
      builder: (context, state) {
        final email = state.extra as String?;
        return VerifyOtpScreen(email: email ?? '');
      },
    ),
    GoRoute(
      path: RouteNames.resetPassword,
      builder: (context, state) {
        final params = state.extra as Map<String, String>?;
        return ResetPasswordScreen(
          email: params?['email'] ?? '',
          otp: params?['otp'] ?? '',
        );
      },
    ),
  ],
);
