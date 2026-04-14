import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/dependency_injection/injection.dart';
import '../../../../core/websocket/websocket_service.dart';

final websocketServiceProvider = Provider<WebsocketService>((ref) {
  final service = WebsocketService(
    logger: ref.watch(loggerProvider),
    secureStorage: ref.watch(secureStorageProvider),
  );

  ref.onDispose(() {
    unawaited(service.dispose());
  });

  return service;
});

final orderNotificationStreamProvider =
    StreamProvider.autoDispose<RealtimeOrderEvent>((ref) async* {
      final user = ref.watch(currentUserProvider);
      final websocketService = ref.watch(websocketServiceProvider);

      if (user == null) {
        await websocketService.disconnect();
        return;
      }

      await websocketService.connectForUser(user.id);

      ref.onDispose(() {
        unawaited(websocketService.disconnect());
      });

      yield* websocketService.events;
    });
