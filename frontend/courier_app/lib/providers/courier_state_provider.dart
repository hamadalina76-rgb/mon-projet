import 'package:flutter_riverpod/flutter_riverpod.dart';

enum CourierStatus {
  offline,
  available,
  onDelivery,
  paused,
}

class CourierState {
  final CourierStatus status;
  final bool isOnline;
  
  const CourierState({
    required this.status,
    required this.isOnline,
  });
  
  CourierState copyWith({
    CourierStatus? status,
    bool? isOnline,
  }) {
    return CourierState(
      status: status ?? this.status,
      isOnline: isOnline ?? this.isOnline,
    );
  }
}

// Simple provider for now
final courierStateProvider = Provider<CourierState>((ref) {
  return const CourierState(
    status: CourierStatus.offline,
    isOnline: false,
  );
});
