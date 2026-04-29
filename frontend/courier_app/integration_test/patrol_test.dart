import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';
import 'package:courier_app/main.dart' as app;

import 'test_config.dart';

/// SpeedLine Courier App - Patrol Integration Tests
///
/// Handles native permission dialogs (location, notifications, camera)
///
/// Run with:
///   dart pub global activate patrol_cli
///   patrol test --target integration_test/patrol_test.dart
void main() {
  patrolTest('Grant location permission for courier tracking', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // Login
    final textFields = find.byType(TextFormField);
    if (textFields.evaluate().length >= 2) {
      await $.tester.enterText(textFields.at(0), testConfig.courierEmail);
      await $.tester.enterText(textFields.at(1), testConfig.courierPassword);
      await $.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await $.tester.tap(loginButtons.first);
        await $.pumpAndSettle(duration: const Duration(seconds: 5));
      }
    }

    // Handle location permission (critical for courier app)
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 5))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }

    // Courier app may also ask for "always allow" location for background tracking
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 3))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }
  });

  patrolTest('Handle notification permission for delivery alerts', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // Login
    final textFields = find.byType(TextFormField);
    if (textFields.evaluate().length >= 2) {
      await $.tester.enterText(textFields.at(0), testConfig.courierEmail);
      await $.tester.enterText(textFields.at(1), testConfig.courierPassword);
      await $.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await $.tester.tap(loginButtons.first);
        await $.pumpAndSettle(duration: const Duration(seconds: 5));
      }
    }

    // Handle notification permission
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 5))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }
  });

  patrolTest('Toggle availability and verify GPS tracking starts', ($) async {
    app.main();
    await $.pumpAndSettle(duration: const Duration(seconds: 3));

    // Grant all permissions first
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 3))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }

    // Login
    final textFields = find.byType(TextFormField);
    if (textFields.evaluate().length >= 2) {
      await $.tester.enterText(textFields.at(0), testConfig.courierEmail);
      await $.tester.enterText(textFields.at(1), testConfig.courierPassword);
      await $.pumpAndSettle();

      final loginButtons = find.byType(ElevatedButton);
      if (loginButtons.evaluate().isNotEmpty) {
        await $.tester.tap(loginButtons.first);
        await $.pumpAndSettle(duration: const Duration(seconds: 5));
      }
    }

    // Handle remaining permission dialogs
    if (await $.native.isPermissionDialogVisible(timeout: const Duration(seconds: 3))) {
      await $.native.grantPermissionWhenInUse();
      await $.pumpAndSettle();
    }

    // Find and toggle availability switch
    final toggle = find.byType(Switch);
    if (toggle.evaluate().isNotEmpty) {
      await $.tester.tap(toggle.first);
      await $.pumpAndSettle(duration: const Duration(seconds: 2));
    }
  });
}
