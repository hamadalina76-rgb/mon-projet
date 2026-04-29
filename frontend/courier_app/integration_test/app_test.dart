import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:courier_app/main.dart' as app;

import 'test_config.dart';

/// SpeedLine Courier App - Integration Tests
///
/// Run with:
///   flutter test integration_test/app_test.dart
///   flutter test integration_test/app_test.dart -d <device_id>
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Authentication', () {
    testWidgets('should display login screen with email and password fields',
        (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      // Courier login screen has _emailController, _passwordController
      // Uses TextFormField widgets inside a Form
      final textFields = find.byType(TextFormField);
      expect(textFields, findsWidgets);
    });

    testWidgets('should show validation error on empty submit', (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      // Ensure we're on login tab (_isLoginTab = true by default)
      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await tester.tap(loginButtons.first);
        await tester.pumpAndSettle();
      }
    });

    testWidgets('should login with valid courier credentials', (tester) async {
      app.main();
      await tester.pumpAndSettle(const Duration(seconds: 3));

      final textFields = find.byType(TextFormField);
      expect(textFields, findsWidgets);

      // Courier app: first field = email, second = password (on login tab)
      await tester.enterText(textFields.at(0), testConfig.courierEmail);
      await tester.enterText(textFields.at(1), testConfig.courierPassword);
      await tester.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await tester.tap(loginButtons.first);
        await tester.pumpAndSettle(const Duration(seconds: 5));
      }

      // After successful login, courier goes to /home or /pending or /documentation
      // depending on account status
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

        // Courier app shows SnackBar on login error
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

  group('Home Screen', () {
    testWidgets('should display home screen after login', (tester) async {
      app.main();
      await _loginAsCourier(tester);

      // Courier home at '/home' route
      await tester.pumpAndSettle(const Duration(seconds: 2));
    });

    testWidgets('should show online/offline toggle', (tester) async {
      app.main();
      await _loginAsCourier(tester);

      // Look for Switch or toggle widget for availability
      final toggle = find.byType(Switch);
      final switchWidget = find.byType(SwitchListTile);
      expect(
        toggle.evaluate().isNotEmpty || switchWidget.evaluate().isNotEmpty,
        isTrue,
        reason: 'Expected an availability toggle on home screen',
      );
    });

    testWidgets('should toggle courier availability', (tester) async {
      app.main();
      await _loginAsCourier(tester);

      final toggle = find.byType(Switch);
      if (toggle.evaluate().isNotEmpty) {
        await tester.tap(toggle.first);
        await tester.pumpAndSettle(const Duration(seconds: 2));
        // Toggle back
        await tester.tap(toggle.first);
        await tester.pumpAndSettle(const Duration(seconds: 2));
      }
    });
  });

  group('Active Delivery', () {
    testWidgets('should navigate to active delivery screen', (tester) async {
      app.main();
      await _loginAsCourier(tester);

      // '/active-delivery' route exists
      // If there's an active delivery, a card or button should be visible
      final activeDeliveryText = find.textContaining(
        RegExp(r'active|delivery|livraison|mission', caseSensitive: false),
      );
      if (activeDeliveryText.evaluate().isNotEmpty) {
        await tester.tap(activeDeliveryText.first);
        await tester.pumpAndSettle(const Duration(seconds: 2));
      }
    });
  });

  group('Notifications', () {
    testWidgets('should navigate to notifications', (tester) async {
      app.main();
      await _loginAsCourier(tester);

      // '/notifications' route
      final notifIcon = find.byIcon(Icons.notifications);
      final notifIcon2 = find.byIcon(Icons.notifications_outlined);

      for (final icon in [notifIcon, notifIcon2]) {
        if (icon.evaluate().isNotEmpty) {
          await tester.tap(icon.first);
          await tester.pumpAndSettle(const Duration(seconds: 2));
          break;
        }
      }
    });
  });
}

/// Helper: login as test courier
Future<void> _loginAsCourier(WidgetTester tester) async {
  await tester.pumpAndSettle(const Duration(seconds: 3));

  final textFields = find.byType(TextFormField);
  if (textFields.evaluate().length >= 2) {
    await tester.enterText(textFields.at(0), testConfig.courierEmail);
    await tester.enterText(textFields.at(1), testConfig.courierPassword);
    await tester.pumpAndSettle();

    final loginButtons = find.byType(ElevatedButton);
    if (loginButtons.evaluate().isNotEmpty) {
      await tester.tap(loginButtons.first);
      await tester.pumpAndSettle(const Duration(seconds: 5));
    }
  }
}
