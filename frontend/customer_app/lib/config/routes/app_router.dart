import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/verify_otp_screen.dart';
import '../../features/auth/presentation/screens/reset_password_screen.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import '../../features/location/presentation/screens/enable_location_screen.dart';
import '../../features/location/presentation/screens/confirm_location_screen.dart';
import '../../features/profile/presentation/screens/profile_screen.dart';
import '../../features/explore/presentation/screens/explore_screen.dart';
import '../../features/search/presentation/screens/search_screen.dart';
import '../../features/orders/presentation/screens/orders_screen.dart';
import '../../features/main/presentation/screens/main_scaffold.dart';
import '../../features/profile/presentation/screens/settings_screen.dart';
import '../../features/profile/presentation/screens/edit_profile_screen.dart';
import '../../features/profile/presentation/screens/change_password_screen.dart';
import '../../features/profile/presentation/screens/address_type_selector_screen.dart';
import '../../features/profile/presentation/screens/address_details_screen.dart';
import '../../features/profile/presentation/screens/addresses_screen.dart';
import '../../features/location/data/models/saved_location.dart';
import '../../features/profile/data/models/address_model.dart';
import '../../features/partners/presentation/screens/nearby_partners_screen.dart';
import '../../features/cart/presentation/cart_screen.dart';
import '../../features/cart/presentation/screens/checkout_screen.dart';
import '../../features/orders/presentation/screens/order_confirmation_screen.dart';
import '../../features/orders/presentation/screens/order_tracking_screen.dart';
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
        OtpVerificationType verificationType =
            OtpVerificationType.forgotPassword;

        if (extra is Map) {
          email = extra['email'] as String? ?? '';
          verificationType =
              extra['type'] as OtpVerificationType? ??
              OtpVerificationType.forgotPassword;
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

        return ResetPasswordScreen(email: email, otp: otp);
      },
    ),

    // Post-login - Location Permission
    GoRoute(
      path: RouteNames.enableLocation,
      builder: (context, state) => const EnableLocationScreen(),
    ),

    // Post-GPS - Confirm location on map
    GoRoute(
      path: RouteNames.confirmLocation,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? {};
        return ConfirmLocationScreen(
          latitude: (extra['latitude'] as num?)?.toDouble() ?? 36.8065,
          longitude: (extra['longitude'] as num?)?.toDouble() ?? 10.1815,
          initialAddress: extra['initialAddress'] as String? ?? '',
        );
      },
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
    GoRoute(
      path: RouteNames.changePassword,
      builder: (context, state) => const ChangePasswordScreen(),
    ),

    // Address management
    GoRoute(
      path: RouteNames.addresses,
      builder: (context, state) => const AddressesScreen(),
    ),
    GoRoute(
      path: RouteNames.addressTypeSelector,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? {};
        return AddressTypeSelectorScreen(
          customerId: extra['customerId'] as String? ?? '',
          fromLocation: extra['fromLocation'] as SavedLocation?,
          redirectOnSuccess: extra['redirectOnSuccess'] as String?,
        );
      },
    ),
    GoRoute(
      path: RouteNames.addressDetails,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? {};
        return AddressDetailsScreen(
          type: extra['type'] as AddressType? ?? AddressType.home,
          customerId: extra['customerId'] as String? ?? '',
          fromLocation: extra['fromLocation'] as SavedLocation?,
          existing: extra['existing'] as AddressModel?,
          redirectOnSuccess: extra['redirectOnSuccess'] as String?,
        );
      },
    ),

    // Partners
    GoRoute(
      path: RouteNames.nearbyPartners,
      builder: (context, state) => const NearbyPartnersScreen(),
    ),

    GoRoute(
      path: RouteNames.cart,
      builder: (context, state) => const CartScreen(),
    ),
    GoRoute(
      path: RouteNames.checkout,
      builder: (context, state) => const CheckoutScreen(),
    ),
    GoRoute(
      path: RouteNames.orderConfirmation,
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>? ?? {};
        final orderId = extra['orderId']?.toString() ?? '';
        final orderNumber = extra['orderNumber']?.toString();
        final partnerName = extra['partnerName']?.toString();
        final estimatedDeliveryRaw = extra['estimatedDeliveryTime']?.toString();
        final estimatedDeliveryTime = estimatedDeliveryRaw == null
            ? null
            : DateTime.tryParse(estimatedDeliveryRaw);

        return OrderConfirmationScreen(
          orderId: orderId,
          orderNumber: orderNumber,
          partnerName: partnerName,
          estimatedDeliveryTime: estimatedDeliveryTime,
        );
      },
    ),
    GoRoute(
      path: RouteNames.orderTrackingTemplate,
      builder: (context, state) {
        final orderId = state.pathParameters['orderId'] ?? '';
        return OrderTrackingScreen(orderId: orderId);
      },
    ),

    // Main App Screens with Bottom Navigation
    ShellRoute(
      navigatorKey: _shellNavigatorKey,
      builder: (context, state, child) {
        return MainScaffold(currentPath: state.uri.path, child: child);
      },
      routes: [
        GoRoute(
          path: RouteNames.explore,
          pageBuilder: (context, state) =>
            const NoTransitionPage(child: ExploreScreen()),
        ),
        GoRoute(
          path: RouteNames.search,
          builder: (context, state) {
            final extra = state.extra;
            String heroTag = 'nearby-search-hero';
            double? lat;
            double? lng;
            String? initialQuery;

            if (extra is Map<String, dynamic>) {
              heroTag = extra['heroTag'] as String? ?? heroTag;
              lat = (extra['lat'] as num?)?.toDouble();
              lng = (extra['lng'] as num?)?.toDouble();
              initialQuery = extra['query'] as String?;
            }

            return SearchScreen(
              heroTag: heroTag,
              initialLat: lat,
              initialLng: lng,
              initialQuery: initialQuery,
            );
          },
        ),
        GoRoute(
          path: RouteNames.orders,
          pageBuilder: (context, state) =>
            const NoTransitionPage(child: OrdersScreen()),
        ),
        GoRoute(
          path: RouteNames.profile,
          pageBuilder: (context, state) =>
            const NoTransitionPage(child: ProfileScreen()),
        ),
      ],
    ),
  ],
);
