import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/di/injection_container.dart';
import '../../../../providers/location_websocket_provider.dart';
import '../../domain/entities/order.dart';
import '../../domain/repositories/orders_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class OrdersState {
  final DeliveryOffer? incomingOffer;
  final bool isResponding;
  final String? error;
  final String? successMessage;

  const OrdersState({
    this.incomingOffer,
    this.isResponding = false,
    this.error,
    this.successMessage,
  });

  static const initial = OrdersState();

  OrdersState copyWith({
    DeliveryOffer? incomingOffer,
    bool clearOffer = false,
    bool? isResponding,
    String? error,
    bool clearError = false,
    String? successMessage,
    bool clearSuccess = false,
  }) {
    return OrdersState(
      incomingOffer: clearOffer ? null : (incomingOffer ?? this.incomingOffer),
      isResponding: isResponding ?? this.isResponding,
      error: clearError ? null : (error ?? this.error),
      successMessage: clearSuccess ? null : (successMessage ?? this.successMessage),
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class OrdersNotifier extends Notifier<OrdersState> {
  StreamSubscription<Map<String, dynamic>>? _wsSub;

  @override
  OrdersState build() {
    _subscribeToWsMessages();
    ref.onDispose(() => _wsSub?.cancel());
    return OrdersState.initial;
  }

  void _subscribeToWsMessages() {
    _wsSub?.cancel();
    final ws = ref.read(webSocketServiceProvider);
    _wsSub = ws.messages.listen((msg) {
      if (msg['type'] == 'DELIVERY_OFFER') {
        final payload = msg['payload'];
        if (payload is Map<String, dynamic>) {
          state = state.copyWith(
            incomingOffer: DeliveryOffer.fromJson(payload),
            clearError: true,
          );
        }
      }
    });
  }

  Future<void> acceptOffer(int courierId) async {
    final offer = state.incomingOffer;
    if (offer == null) return;
    state = state.copyWith(isResponding: true, clearError: true);
    try {
      final repo = getIt<OrdersRepository>();
      await repo.acceptOffer(offer.orderId, courierId);
      state = state.copyWith(
        clearOffer: true,
        isResponding: false,
        successMessage: 'Commande acceptée ✓',
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isResponding: false,
        error: 'Erreur lors de l\'acceptation: $e',
      );
    }
  }

  Future<void> declineOffer(int courierId, {String reason = 'DECLINED'}) async {
    final offer = state.incomingOffer;
    if (offer == null) return;
    state = state.copyWith(isResponding: true, clearError: true);
    try {
      final repo = getIt<OrdersRepository>();
      await repo.declineOffer(offer.orderId, courierId, reason);
      state = state.copyWith(
        clearOffer: true,
        isResponding: false,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isResponding: false,
        error: 'Erreur lors du refus: $e',
      );
    }
  }

  void dismissOffer() {
    state = state.copyWith(clearOffer: true, clearError: true, clearSuccess: true);
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final ordersProvider = NotifierProvider<OrdersNotifier, OrdersState>(
  OrdersNotifier.new,
);
