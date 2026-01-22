import 'package:flutter_riverpod/flutter_riverpod.dart';

enum AppLifecycleState {
  resumed,
  inactive,
  paused,
  detached,
}

class AppStateNotifier extends StateNotifier<AppLifecycleState> {
  AppStateNotifier() : super(AppLifecycleState.resumed);

  void updateState(AppLifecycleState newState) {
    state = newState;
  }

  void onResumed() => state = AppLifecycleState.resumed;
  void onInactive() => state = AppLifecycleState.inactive;
  void onPaused() => state = AppLifecycleState.paused;
  void onDetached() => state = AppLifecycleState.detached;
}

final appLifecycleProvider = StateNotifierProvider<AppStateNotifier, AppLifecycleState>((ref) {
  return AppStateNotifier();
});

// App initialization state
class AppState {
  final bool isInitialized;
  final bool isLoading;
  final String? error;

  AppState({
    this.isInitialized = false,
    this.isLoading = false,
    this.error,
  });

  AppState copyWith({
    bool? isInitialized,
    bool? isLoading,
    String? error,
  }) {
    return AppState(
      isInitialized: isInitialized ?? this.isInitialized,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

class AppStateNotifierProvider extends StateNotifier<AppState> {
  AppStateNotifierProvider() : super(AppState());

  Future<void> initialize() async {
    state = state.copyWith(isLoading: true);
    try {
      // Initialize app services here
      await Future.delayed(const Duration(seconds: 1));
      state = state.copyWith(isInitialized: true, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, error: e.toString());
    }
  }
}

final appStateProvider = StateNotifierProvider<AppStateNotifierProvider, AppState>((ref) {
  return AppStateNotifierProvider();
});
