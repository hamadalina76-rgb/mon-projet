# Patrol Setup Guide for SpeedLine Mobile Apps

## Overview

Mobile integration tests now live **inside the actual Flutter app directories**:
- `frontend/customer_app/integration_test/` - standard Flutter integration tests + Patrol
- `frontend/courier_app/integration_test/` - standard Flutter integration tests + Patrol

Two types of test files per app:
- `app_test.dart` - Standard `integration_test` (no native interaction, runs everywhere)
- `patrol_test.dart` - Patrol tests (native permission dialogs, requires Patrol CLI)

## Prerequisites

1. Flutter SDK >= 3.0.0
2. For Patrol tests: `dart pub global activate patrol_cli`
3. Android SDK (for Android) or Xcode (for iOS)

## Running Tests

### Standard integration tests (no Patrol CLI needed)

```bash
# Customer App
cd frontend/customer_app
flutter pub get
flutter test integration_test/app_test.dart

# Courier App
cd frontend/courier_app
flutter pub get
flutter test integration_test/app_test.dart
```

### Patrol tests (handles native dialogs)

```bash
# Customer App
cd frontend/customer_app
flutter pub get
patrol test --target integration_test/patrol_test.dart

# Courier App
cd frontend/courier_app
flutter pub get
patrol test --target integration_test/patrol_test.dart
```

### On a specific device

```bash
flutter test integration_test/app_test.dart -d <device_id>
patrol test --target integration_test/patrol_test.dart --device <device_id>
```

## Test Accounts

All mobile tests use credentials from `integration_test/test_config.dart`:
- Customer: `customer@speedline-test.com` / `TestCustomer123!`
- Courier: `courier@speedline-test.com` / `TestCourier123!`

Seed these with: `cd tests/seed && npm install && node seed-test-data.js`

## API URL for Mobile

The test config uses `http://10.0.2.2:8080` which maps to the host machine's
`localhost:8080` from inside an Android emulator. For iOS simulator, use `http://localhost:8080`.

## CI/CD

The `.gitlab-ci-tests.yml` includes mobile test jobs that run `flutter test integration_test/app_test.dart`.
Patrol tests require a device/emulator and are better suited for local development or a device farm.
