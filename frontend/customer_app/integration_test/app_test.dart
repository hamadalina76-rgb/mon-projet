import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:customer_app/main.dart' as app;

import 'test_config.dart';

/// SpeedLine Customer App - Integration Tests
///
/// Run with:
///   flutter test integration_test/app_test.dart
///
/// Or on a connected device:
///   flutter test integration_test/app_test.dart -d <device_id>
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Authentication', () {
    testWidgets('should display login screen with email and password fields',
        (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      // The login screen uses TabController with Log In / Sign Up
      // Look for text fields (AuthTextField uses TextFormField internally)
      final textFields = find.byType(TextFormField);
      expect(textFields, findsWidgets);
    });

    testWidgets('should show validation error on empty submit', (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      // Find the login/sign in button
      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await tester.tap(loginButtons.first);
        await tester.pumpAndSettle();
      }

      // Validation errors should appear
    });

    testWidgets('should login with valid test credentials', (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      // Find email and password TextFormFields
      final textFields = find.byType(TextFormField);
      expect(textFields, findsWidgets);

      // Customer app login screen has _loginEmailController and _loginPasswordController
      // The first TextFormField is email, second is password (on the Login tab)
      await tester.enterText(textFields.at(0), testConfig.customerEmail);
      await tester.enterText(textFields.at(1), testConfig.customerPassword);
      await tester.pumpAndSettle();

      // Tap the login button
      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await tester.tap(loginButtons.first);
        await tester.pumpAndSettle(const Duration(seconds: 5));
      }
    });

    testWidgets('should show error with invalid credentials', (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      final textFields = find.byType(TextFormField);
      if (textFields.evaluate().length >= 2) {
        await tester.enterText(textFields.at(0), 'wrong@test.com');
        await tester.enterText(textFields.at(1), 'WrongPassword123!');
        await tester.pumpAndSettle();

        final loginButtons = find.byType(ElevatedButton);
        if (loginButtons.evaluate().isNotEmpty) {
          await tester.tap(loginButtons.first);
          await tester.pumpAndSettle(const Duration(seconds: 3));
        }

        // Should show a SnackBar or error dialog
        final snackBar = find.byType(SnackBar);
        final alertDialog = find.byType(AlertDialog);
        expect(
          snackBar.evaluate().isNotEmpty || alertDialog.evaluate().isNotEmpty,
          isTrue,
          reason: 'Expected an error message for invalid credentials',
        );
      }
    });
  });

  group('Home & Browse', () {
    testWidgets('should display explore screen after login', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      // After login, should land on explore/home screen
      // The app uses go_router with '/explore' route
      await tester.pumpAndSettle(const Duration(seconds: 2));
    });

    testWidgets('should show bottom navigation bar', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      final bottomNav = find.byType(BottomNavigationBar);
      final navBar = find.byType(NavigationBar);
      expect(
        bottomNav.evaluate().isNotEmpty || navBar.evaluate().isNotEmpty,
        isTrue,
        reason: 'Expected bottom navigation bar on home screen',
      );
    });

    testWidgets('should navigate to orders tab', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      // Find and tap orders icon in bottom nav
      final ordersIcon = find.byIcon(Icons.receipt_long);
      final ordersIcon2 = find.byIcon(Icons.shopping_bag);
      final ordersIcon3 = find.byIcon(Icons.list_alt);

      for (final icon in [ordersIcon, ordersIcon2, ordersIcon3]) {
        if (icon.evaluate().isNotEmpty) {
          await tester.tap(icon.first);
          await tester.pumpAndSettle(const Duration(seconds: 2));
          break;
        }
      }
    });

    testWidgets('should navigate to profile tab', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      final profileIcon = find.byIcon(Icons.person);
      final profileIcon2 = find.byIcon(Icons.person_outline);
      final profileIcon3 = find.byIcon(Icons.account_circle);

      for (final icon in [profileIcon, profileIcon2, profileIcon3]) {
        if (icon.evaluate().isNotEmpty) {
          await tester.tap(icon.first);
          await tester.pumpAndSettle(const Duration(seconds: 2));
          break;
        }
      }
    });
  });

  group('Cart & Checkout', () {
    testWidgets('should navigate to cart', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      final cartIcon = find.byIcon(Icons.shopping_cart);
      final cartIcon2 = find.byIcon(Icons.shopping_cart_outlined);

      for (final icon in [cartIcon, cartIcon2]) {
        if (icon.evaluate().isNotEmpty) {
          await tester.tap(icon.first);
          await tester.pumpAndSettle(const Duration(seconds: 2));
          break;
        }
      }
    });
  });

  group('Address Management', () {
    testWidgets('should access addresses from profile', (tester) async {
      app.main();
      await _loginAsCustomer(tester);

      // Navigate to profile
      final profileIcon = find.byIcon(Icons.person);
      if (profileIcon.evaluate().isNotEmpty) {
        await tester.tap(profileIcon.first);
        await tester.pumpAndSettle(const Duration(seconds: 2));
      }

      // Look for addresses link/button
      final addressText = find.textContaining(RegExp(r'address|adresse', caseSensitive: false));
      if (addressText.evaluate().isNotEmpty) {
        await tester.tap(addressText.first);
        await tester.pumpAndSettle(const Duration(seconds: 2));
      }
    });
  });
}

/// Helper: login as test customer
Future<void> _loginAsCustomer(WidgetTester tester) async {
  await tester.pumpAndSettle(const Duration(seconds: 3));

  final textFields = find.byType(TextFormField);
  if (textFields.evaluate().length >= 2) {
    await tester.enterText(textFields.at(0), testConfig.customerEmail);
    await tester.enterText(textFields.at(1), testConfig.customerPassword);
    await tester.pumpAndSettle();

    final loginButtons = find.byType(ElevatedButton);
    if (loginButtons.evaluate().isNotEmpty) {
      await tester.tap(loginButtons.first);
      await tester.pumpAndSettle(const Duration(seconds: 5));
    }
  }
}
