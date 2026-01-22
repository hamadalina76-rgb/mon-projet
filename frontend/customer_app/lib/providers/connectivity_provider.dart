import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import '../services/connectivity_service.dart';

class ConnectivityNotifier extends StateNotifier<ConnectivityResult> {
  final ConnectivityService _connectivityService;

  ConnectivityNotifier(this._connectivityService) : super(ConnectivityResult.none) {
    _init();
  }

  void _init() async {
    state = await _connectivityService.getConnectivityStatus();
    
    _connectivityService.onConnectivityChanged.listen((result) {
      state = result;
    });
  }

  bool get isConnected => state != ConnectivityResult.none;
  bool get isWifi => state == ConnectivityResult.wifi;
  bool get isMobile => state == ConnectivityResult.mobile;
}

final connectivityProvider = StateNotifierProvider<ConnectivityNotifier, ConnectivityResult>((ref) {
  final service = ref.watch(connectivityServiceProvider);
  return ConnectivityNotifier(service);
});

final isConnectedProvider = Provider<bool>((ref) {
  final connectivity = ref.watch(connectivityProvider);
  return connectivity != ConnectivityResult.none;
});
