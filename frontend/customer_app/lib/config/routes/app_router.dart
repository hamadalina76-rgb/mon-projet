import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/verify_otp_screen.dart';
import '../../features/auth/presentation/screens/reset_password_screen.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import '../../features/home/presentation/screens/accueil_screen.dart';
import '../../features/location/presentation/screens/enable_location_screen.dart';
import '../../features/profile/presentation/screens/profile_screen.dart';
import '../../features/explore/presentation/screens/explore_screen.dart';
import '../../features/search/presentation/screens/search_screen.dart';
import '../../features/orders/presentation/screens/orders_screen.dart';
import '../../features/main/presentation/screens/main_scaffold.dart';
import '../../features/profile/presentation/screens/settings_screen.dart';
import '../../features/profile/presentation/screens/edit_profile_screen.dart';
import 'route_names.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorKey = GlobalKey<NavigatorState>();

final appRouter = GoRouter(
  initialLocation: RouteNames.home,
  navigatorKey: _rootNavigatorKey,
  routes: [
    // Home / Onboarding
    GoRoute(
      path: RouteNames.home,
      builder: (context, state) => const HomeScreen(),
    ),
    GoRoute(
      path: RouteNames.accueil,
      builder: (context, state) => const AccueilScreen(),
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
        final extra = state.extra;
        String email = '';
        OtpVerificationType verificationType = OtpVerificationType.forgotPassword;
        
        if (extra is Map) {
          email = extra['email'] as String? ?? '';
          verificationType = extra['type'] as OtpVerificationType? ?? OtpVerificationType.forgotPassword;
        } else if (extra is String) {
          email = extra;
        }
        
        return VerifyOtpScreen(
          email: email,
          verificationType: verificationType,
        );
      },
    ),
    GoRoute(
      path: RouteNames.resetPassword,
      builder: (context, state) {
        final extra = state.extra;
        String email = '';
        String otp = '';
        
        if (extra is Map) {
          email = extra['email'] as String? ?? '';
          otp = extra['otp'] as String? ?? '';
        }
        
        return ResetPasswordScreen(
          email: email,
          otp: otp,
        );
      },
    ),
    
    // Post-login - Location Permission
    GoRoute(
      path: RouteNames.enableLocation,
      builder: (context, state) => const EnableLocationScreen(),
    ),
    
    // Settings & Profile Management (outside bottom nav)
    GoRoute(
      path: RouteNames.settings,
      builder: (context, state) => const SettingsScreen(),
    ),
    GoRoute(
      path: RouteNames.editProfile,
      builder: (context, state) => const EditProfileScreen(),
    ),
    
    // Main App Screens with Bottom Navigation
    ShellRoute(
      navigatorKey: _shellNavigatorKey,
      builder: (context, state, child) {
        return MainScaffold(
          currentPath: state.uri.path,
          child: child,
        );
      },
      routes: [
        GoRoute(
          path: RouteNames.explore,
          pageBuilder: (context, state) => NoTransitionPage(
            child: const ExploreScreen(),
          ),
        ),
        GoRoute(
          path: RouteNames.search,
          pageBuilder: (context, state) => NoTransitionPage(
            child: const SearchScreen(),
          ),
        ),
        GoRoute(
          path: RouteNames.orders,
          pageBuilder: (context, state) => NoTransitionPage(
            child: const OrdersScreen(),
          ),
        ),
        GoRoute(
          path: RouteNames.profile,
          pageBuilder: (context, state) => NoTransitionPage(
            child: const ProfileScreen(),
          ),
        ),
      ],
    ),
  ],
);
