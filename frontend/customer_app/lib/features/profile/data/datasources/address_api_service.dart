import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/api/api_endpoints.dart';
import '../../../../config/runtime_config.dart';
import '../models/address_model.dart';

/// Service API bas-niveau pour les adresses de livraison.
/// Communique avec user-service via l'API Gateway (port 8080).
class AddressApiService {
  final Dio _dio;

  AddressApiService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: RuntimeConfig.apiBaseUrl,
              connectTimeout: const Duration(seconds: 10),
              receiveTimeout: const Duration(seconds: 15),
              headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
              },
            ));

  // ─── GET /customers/{id}/addresses ─────────────────────────────────────────

  Future<List<AddressModel>> fetchAddresses(String userId) async {
    try {
      final response = await _dio.get(
        ApiEndpoints.customerAddressesByUserId(userId),
      );
      if (response.statusCode == 200 && response.data != null) {
        final list = response.data as List<dynamic>;
        return list
            .map((e) => AddressModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } on DioException catch (e) {
      debugPrint('[AddressApi] fetchAddresses error: $e');
      rethrow;
    }
  }

  // ─── POST /customers/{id}/addresses ────────────────────────────────────────

  Future<AddressModel> createAddress(
      String userId, AddressRequest request) async {
    try {
      final body = request.toJson();
      debugPrint('[AddressApi] createAddress → POST ${ApiEndpoints.customerAddressesByUserId(userId)}');
      debugPrint('[AddressApi] request body: $body');
      final response = await _dio.post(
        ApiEndpoints.customerAddressesByUserId(userId),
        data: body,
      );
      debugPrint('[AddressApi] createAddress status: ${response.statusCode}');
      debugPrint('[AddressApi] createAddress response: ${response.data}');
      if (response.statusCode == 201 || response.statusCode == 200) {
        return AddressModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: RequestOptions(path: ApiEndpoints.customerAddressesByUserId(userId)),
        response: response,
        message: 'Unexpected status: ${response.statusCode}',
      );
    } on DioException catch (e) {
      debugPrint('[AddressApi] createAddress DioException: $e');
      debugPrint('[AddressApi] response status: ${e.response?.statusCode}');
      debugPrint('[AddressApi] response body: ${e.response?.data}');
      rethrow;
    } catch (e, st) {
      debugPrint('[AddressApi] createAddress unexpected error: $e');
      debugPrint('[AddressApi] stacktrace: $st');
      rethrow;
    }
  }

  // ─── PUT /addresses/{id} ───────────────────────────────────────────────────

  Future<AddressModel> updateAddress(int addressId, AddressRequest request) async {
    try {
      final response = await _dio.put(
        ApiEndpoints.userAddressById(addressId.toString()),
        data: request.toJson(),
      );
      if (response.statusCode == 200) {
        return AddressModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Unexpected status: ${response.statusCode}',
      );
    } on DioException catch (e) {
      debugPrint('[AddressApi] updateAddress error: $e');
      rethrow;
    }
  }

  // ─── DELETE /addresses/{id} ────────────────────────────────────────────────

  Future<void> deleteAddress(int addressId) async {
    try {
      final response = await _dio.delete(
        ApiEndpoints.userAddressById(addressId.toString()),
      );
      // validateStatus in ApiClient lets 4xx through without throwing —
      // explicitly reject anything outside 2xx so the provider does NOT
      // remove the address from the local state when the backend refuses.
      final status = response.statusCode ?? 0;
      if (status < 200 || status >= 300) {
        final data = response.data;
        final message = data is Map ? (data['message'] ?? 'Delete failed') : 'Delete failed';
        throw DioException(
          requestOptions: response.requestOptions,
          response: response,
          message: message.toString(),
        );
      }
    } on DioException catch (e) {
      debugPrint('[AddressApi] deleteAddress error: $e');
      rethrow;
    }
  }

  /// Extrait le message d'erreur du corps JSON d'une DioException (si dispo).
  static String extractErrorMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String? ??
          data['error'] as String? ??
          e.message ??
          'Erreur inconnue';
    }
    return e.message ?? 'Erreur inconnue';
  }
}
