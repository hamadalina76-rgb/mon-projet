import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/api/api_client.dart';
import '../../data/datasources/address_api_service.dart';
import '../../data/models/address_model.dart';

// ─── Service provider ─────────────────────────────────────────────────────────

final addressApiServiceProvider = Provider<AddressApiService>((ref) {
  // Use the authenticated Dio from ApiClient so the API Gateway
  // receives the Bearer token on every request.
  return AddressApiService(dio: ApiClient().dio);
});

// ─── Notifier ────────────────────────────────────────────────────────────────

class AddressNotifier extends AsyncNotifier<List<AddressModel>> {
  late AddressApiService _service;
  String? _currentCustomerId;

  @override
  Future<List<AddressModel>> build() async {
    _service = ref.watch(addressApiServiceProvider);
    return [];
  }

  // ── Fetch all addresses for a customer ───────────────────────────────────

  Future<void> fetchAddresses(String customerId) async {
    _currentCustomerId = customerId;
    state = const AsyncLoading();
    state = await AsyncValue.guard(
        () => _service.fetchAddresses(customerId));
  }

  // ── Create a new address ─────────────────────────────────────────────────

  Future<AddressModel?> createAddress(
      String customerId, AddressRequest request) async {
    try {
      final created = await _service.createAddress(customerId, request);
      debugPrint('[AddressProvider] createAddress OK → id=${created.id}');
      // Append to current list
      state = state.whenData((list) => [...list, created]);
      return created;
    } on DioException {
      rethrow;
    } catch (e, st) {
      debugPrint('[AddressProvider] createAddress unexpected error: $e');
      debugPrint('[AddressProvider] stacktrace: $st');
      rethrow;
    }
  }

  // ── Update an existing address ───────────────────────────────────────────

  Future<AddressModel?> updateAddress(
      int addressId, AddressRequest request) async {
    try {
      final updated = await _service.updateAddress(addressId, request);
      state = state.whenData((list) => [
            for (final a in list)
              if (a.id == addressId) updated else a
          ]);
      return updated;
    } on DioException {
      rethrow;
    }
  }

  // ── Delete an address ────────────────────────────────────────────────────

  Future<void> deleteAddress(int addressId) async {
    await _service.deleteAddress(addressId);
    state = state.whenData(
        (list) => list.where((a) => a.id != addressId).toList());
  }
}

// ─── Provider ────────────────────────────────────────────────────────────────

final addressNotifierProvider =
    AsyncNotifierProvider<AddressNotifier, List<AddressModel>>(
        AddressNotifier.new);
