import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/splash/presentation/screens/splash_screen.dart';
import '../../features/onboarding/presentation/screens/onboarding_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/email_verification_screen.dart';
import '../../features/auth/presentation/screens/documentation_screen.dart';
import '../../features/auth/presentation/screens/driving_license_screen.dart';
import '../../features/auth/presentation/screens/payout_details_screen.dart';
import '../../features/auth/presentation/screens/pending_approval_screen.dart';
import '../../features/auth/presentation/screens/rejected_screen.dart';
import '../../features/home/presentation/screens/main_navigation_screen.dart';
import '../../features/deliveries/presentation/screens/active_delivery_screen.dart';

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
        builder: (context, state) {
          final email = state.uri.queryParameters['email'] ?? '';
          return EmailVerificationScreen(email: email);
        },
      ),
      GoRoute(
        path: '/documentation',
        builder: (context, state) {
          final readOnly = state.uri.queryParameters['readOnly'] == 'true';
          return DocumentationScreen(readOnly: readOnly);
        },
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
        path: '/pending',
        builder: (context, state) => const PendingApprovalScreen(),
      ),
      GoRoute(
        path: '/rejected',
        builder: (context, state) => const RejectedScreen(),
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const MainNavigationScreen(),
      ),
      GoRoute(
        path: '/active-delivery',
        builder: (context, state) => const ActiveDeliveryScreen(),
      ),
    ],
  );
});
