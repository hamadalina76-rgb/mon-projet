import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/splash/presentation/screens/splash_screen.dart';
import '../../features/onboarding/presentation/screens/onboarding_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/email_verification_screen.dart';
import '../../features/auth/presentation/screens/documentation_screen.dart';
import '../../features/auth/presentation/screens/driving_license_screen.dart';
import '../../features/auth/presentation/screens/payout_details_screen.dart';
import '../../features/home/presentation/screens/main_navigation_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/onboarding',
        builder: (context, state) => const OnboardingScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/email-verification',
        builder: (context, state) => const EmailVerificationScreen(),
      ),
      GoRoute(
        path: '/documentation',
        builder: (context, state) => const DocumentationScreen(),
      ),
      GoRoute(
        path: '/driving-license',
        builder: (context, state) => const DrivingLicenseScreen(),
      ),
      GoRoute(
        path: '/payout-details',
        builder: (context, state) => const PayoutDetailsScreen(),
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const MainNavigationScreen(),
      ),
    ],
  );
});
