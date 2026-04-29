import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';
import 'package:customer_app/main.dart' as app;

import 'test_config.dart';

/// SpeedLine Customer App - Patrol Integration Tests
///
/// Patrol extends Flutter integration tests with native interaction support:
/// - System dialogs (permissions, location, notifications)
/// - Native OS features
///
/// Run with:
///   dart pub global activate patrol_cli
///   patrol test --target integration_test/patrol_test.dart
void main() {
  patrolTest('Grant location permission and proceed', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // If the app shows a location permission dialog (native)
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 5))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }
  });

  patrolTest('Grant notification permission on login', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // Login
    final textFields = find.byType(TextFormField);
    if (textFields.evaluate().length >= 2) {
      await $.tester.enterText(textFields.at(0), testConfig.customerEmail);
      await $.tester.enterText(textFields.at(1), testConfig.customerPassword);
      await $.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await $.tester.tap(loginButtons.first);
        await $.pumpAndSettle(duration: const Duration(seconds: 5));
      }
    }

    // Handle notification permission dialog
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 5))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }
  });

  patrolTest('Full order flow with native interactions', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // Handle any initial permission dialogs
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 3))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }

    // Login
    final textFields = find.byType(TextFormField);
    if (textFields.evaluate().length >= 2) {
      await $.tester.enterText(textFields.at(0), testConfig.customerEmail);
      await $.tester.enterText(textFields.at(1), testConfig.customerPassword);
      await $.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await $.tester.tap(loginButtons.first);
        await $.pumpAndSettle(duration: const Duration(seconds: 5));
      }
    }

    // Handle post-login permission dialogs
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 3))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }

    // Navigate and interact with the app
    await $.pumpAndSettle(duration: const Duration(seconds: 2));
  });
}
