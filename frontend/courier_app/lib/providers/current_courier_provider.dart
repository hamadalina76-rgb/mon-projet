import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/di/injection_container.dart';
import '../features/auth/domain/entities/courier.dart';
import '../features/auth/domain/repositories/auth_repository.dart';

final currentCourierProvider = FutureProvider<Courier?>((ref) async {
  if (!getIt.isRegistered<AuthRepository>()) {
    return null;
  }
  return getIt<AuthRepository>().getCurrentCourier();
});
