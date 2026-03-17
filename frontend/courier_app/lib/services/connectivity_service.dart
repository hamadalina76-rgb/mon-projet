import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';

class ConnectivityService {
  final Connectivity _connectivity;
  final StreamController<bool> _controller = StreamController<bool>.broadcast();
  StreamSubscription<List<ConnectivityResult>>? _sub;

  ConnectivityService({Connectivity? connectivity}) : _connectivity = connectivity ?? Connectivity();

  Future<void> initialize() async {
    _sub ??= _connectivity.onConnectivityChanged.listen((results) {
      _controller.add(!_isOffline(results));
    });
  }

  Stream<bool> get connectionChanges => _controller.stream;

  Future<bool> isConnected() async {
    final results = await _connectivity.checkConnectivity();
    return !_isOffline(results);
  }

  bool _isOffline(List<ConnectivityResult> results) {
    return results.isEmpty ||
        (results.length == 1 && results.first == ConnectivityResult.none);
  }

  Future<void> dispose() async {
    await _sub?.cancel();
    await _controller.close();
  }
}
