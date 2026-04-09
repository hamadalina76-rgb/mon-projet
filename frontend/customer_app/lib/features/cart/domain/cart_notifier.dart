import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/cart_item_model.dart';
import '../data/repositories/cart_repository.dart';

class CartState {
  final List<CartItemModel> items;
  final bool isLoading;
  final bool hasPendingSync;

  const CartState({
    this.items = const <CartItemModel>[],
    this.isLoading = false,
    this.hasPendingSync = false,
  });

  double get subtotal => items.fold(0, (sum, item) => sum + item.lineTotal);

  double get total => subtotal;

  int get itemCount => items.fold(0, (sum, item) => sum + item.quantity);

  String? get partnerId => items.isNotEmpty ? items.first.partnerId : null;

  CartState copyWith({
    List<CartItemModel>? items,
    bool? isLoading,
    bool? hasPendingSync,
  }) {
    return CartState(
      items: items ?? this.items,
      isLoading: isLoading ?? this.isLoading,
      hasPendingSync: hasPendingSync ?? this.hasPendingSync,
    );
  }
}

class CartNotifier extends StateNotifier<CartState> {
  final CartRepository _repository;
  Timer? _debounce;
  bool _syncInFlight = false;

  CartNotifier({required CartRepository repository})
      : _repository = repository,
        super(const CartState(isLoading: true)) {
    hydrateFromServer();
  }

  List<CartItemModel> get items => state.items;
  double get subtotal => state.subtotal;
  double get total => state.total;
  int get itemCount => state.itemCount;
  bool get isLoading => state.isLoading;

  Future<void> hydrateFromServer() async {
    state = state.copyWith(isLoading: true);
    try {
      final serverItems = await _repository.fetchServerCart();
      state = state.copyWith(items: serverItems, isLoading: false);
    } catch (_) {
      state = state.copyWith(isLoading: false);
    }
  }

  Future<bool> addItem(
    CartItemModel newItem, {
    BuildContext? context,
  }) async {
    if (_hasDifferentPartner(newItem.partnerId)) {
      final currentPartner = state.items.first.partnerName;
      final accepted = context != null
          ? await _confirmPartnerReplacement(
              context: context,
              currentPartnerName: currentPartner,
              nextPartnerName: newItem.partnerName,
            )
          : false;

      if (!accepted) {
        return false;
      }

      state = state.copyWith(items: const <CartItemModel>[]);
    }

    final next = List<CartItemModel>.from(state.items);
    final index = next.indexWhere((item) => item.uniqueKey == newItem.uniqueKey);

    if (index >= 0) {
      final existing = next[index];
      next[index] = existing.copyWith(quantity: existing.quantity + newItem.quantity);
    } else {
      next.add(newItem);
    }

    await _commitAndSync(next);
    return true;
  }

  Future<void> removeItem(String itemKey) async {
    final next = List<CartItemModel>.from(state.items)
      ..removeWhere((item) => item.uniqueKey == itemKey);
    await _commitAndSync(next);
  }

  Future<void> updateQuantity({
    required String itemKey,
    required int quantity,
  }) async {
    final next = List<CartItemModel>.from(state.items);
    final index = next.indexWhere((item) => item.uniqueKey == itemKey);
    if (index < 0) return;

    if (quantity <= 0) {
      next.removeAt(index);
    } else {
      next[index] = next[index].copyWith(quantity: quantity);
    }

    await _commitAndSync(next);
  }

  Future<void> updateItemCustomization({
    required String itemKey,
    required List<String> selectedOptions,
    String? kitchenNote,
  }) async {
    final next = List<CartItemModel>.from(state.items);
    final index = next.indexWhere((item) => item.uniqueKey == itemKey);
    if (index < 0) return;

    final current = next[index];
    final updated = current.copyWith(
      selectedOptions: selectedOptions
          .map(CartItemSelectedOption.freeText)
          .toList(),
      kitchenNote: kitchenNote,
      clearKitchenNote: (kitchenNote == null || kitchenNote.trim().isEmpty),
    );

    // If customization changes the merge key, remove and re-add through add flow.
    next.removeAt(index);
    final mergeIndex = next.indexWhere((item) => item.uniqueKey == updated.uniqueKey);
    if (mergeIndex >= 0) {
      final merged = next[mergeIndex];
      next[mergeIndex] = merged.copyWith(quantity: merged.quantity + updated.quantity);
    } else {
      next.add(updated);
    }

    await _commitAndSync(next);
  }

  Future<void> replaceItem({
    required String itemKey,
    required CartItemModel updatedItem,
  }) async {
    final next = List<CartItemModel>.from(state.items);
    final index = next.indexWhere((item) => item.uniqueKey == itemKey);
    if (index < 0) return;

    next.removeAt(index);

    final mergeIndex = next.indexWhere(
      (item) => item.uniqueKey == updatedItem.uniqueKey,
    );

    if (mergeIndex >= 0) {
      final merged = next[mergeIndex];
      next[mergeIndex] = merged.copyWith(
        quantity: merged.quantity + updatedItem.quantity,
      );
    } else {
      final insertIndex = index <= next.length ? index : next.length;
      next.insert(insertIndex, updatedItem);
    }

    await _commitAndSync(next);
  }

  Future<void> clearCart() async {
    await _commitAndSync(const <CartItemModel>[]);
  }

  Future<void> syncFromServerOnLogin() async {
    try {
      final localSnapshot = List<CartItemModel>.from(state.items);
      final serverItems = await _repository.fetchServerCart();

      if (serverItems.isNotEmpty) {
        state = state.copyWith(items: serverItems, hasPendingSync: false);
        return;
      }

      if (localSnapshot.isNotEmpty) {
        state = state.copyWith(items: localSnapshot);
        _scheduleDebouncedSync();
        return;
      }

      state = state.copyWith(items: const <CartItemModel>[], hasPendingSync: false);
    } catch (_) {
      // Keep local cart as source of truth.
    }
  }

  Future<void> retryPendingSyncIfNeeded() async {
    if (!state.hasPendingSync) return;
    await _syncNow();
  }

  Future<void> placeOrder({
    String? promoCode,
    String? addressId,
    Map<String, dynamic>? deliveryAddressDetails,
    String paymentMethod = 'CASH',
    DateTime? scheduledDeliveryTime,
  }) async {
    if (state.items.isEmpty) return;

    await _repository.placeOrder(
      cartItems: state.items,
      promoCode: promoCode,
      addressId: addressId,
      deliveryAddressDetails: deliveryAddressDetails,
      paymentMethod: paymentMethod,
      scheduledDeliveryTime: scheduledDeliveryTime,
    );

    await clearCart();
  }

  bool _hasDifferentPartner(String partnerId) {
    return state.items.isNotEmpty && state.items.first.partnerId != partnerId;
  }

  Future<bool> _confirmPartnerReplacement({
    required BuildContext context,
    required String currentPartnerName,
    required String nextPartnerName,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Changer de partenaire ?'),
          content: Text(
            'Votre panier contient des articles de $currentPartnerName. '
            'Vider le panier et commander chez $nextPartnerName ?',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Annuler'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Confirmer'),
            ),
          ],
        );
      },
    );
    return result ?? false;
  }

  Future<void> _commitAndSync(List<CartItemModel> nextItems) async {
    state = state.copyWith(items: nextItems, isLoading: false);
    _scheduleDebouncedSync();
  }

  void _scheduleDebouncedSync() {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () {
      _syncNow();
    });
  }

  Future<void> _syncNow() async {
    if (_syncInFlight) return;

    _syncInFlight = true;
    try {
      await _repository.patchCart(state.items);
      state = state.copyWith(hasPendingSync: false);
    } catch (e) {
      debugPrint('[CartNotifier] PATCH /cart failed: $e');
      state = state.copyWith(hasPendingSync: true);
    } finally {
      _syncInFlight = false;
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }
}
