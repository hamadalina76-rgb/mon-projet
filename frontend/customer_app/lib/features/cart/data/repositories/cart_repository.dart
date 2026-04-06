import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../../core/api/api_endpoints.dart';
import '../../../../core/utils/media_url.dart';
import '../models/cart_item_model.dart';

class DeliveryFeeInfo {
  final double deliveryFee;
  final double freeDeliveryThreshold;

  const DeliveryFeeInfo({
    required this.deliveryFee,
    required this.freeDeliveryThreshold,
  });
}

class PromoValidationResult {
  final bool isValid;
  final double discount;
  final String message;

  const PromoValidationResult({
    required this.isValid,
    required this.discount,
    required this.message,
  });
}

class PartnerCartInfo {
  final String partnerId;
  final String partnerName;
  final String partnerLogoUrl;
  final double minimumOrder;
  final bool isOpen;
  final double serviceFeeValue;
  final bool serviceFeeIsPercentage;
  final int? deliveryRadius;
  final double? distanceKm;
  final double? latitude;
  final double? longitude;

  const PartnerCartInfo({
    required this.partnerId,
    required this.partnerName,
    required this.partnerLogoUrl,
    required this.minimumOrder,
    required this.isOpen,
    this.serviceFeeValue = 0,
    this.serviceFeeIsPercentage = false,
    this.deliveryRadius,
    this.distanceKm,
    this.latitude,
    this.longitude,
  });
}

class CartRepository {
  final Dio _dio;

  CartRepository({required Dio dio}) : _dio = dio;

  Future<List<CartItemModel>> fetchServerCart() async {
    final response = await _dio.get(ApiEndpoints.CART_BASE);
    final data = response.data;

    dynamic rawItems;
    if (data is Map<String, dynamic>) {
      rawItems = data['items'] ?? data['cartItems'];
    } else if (data is List) {
      rawItems = data;
    }

    if (rawItems is! List) return const [];

    return rawItems
        .whereType<Map>()
        .map((raw) => CartItemModel.fromJson(Map<String, dynamic>.from(raw)))
        .toList();
  }

  Future<void> patchCart(List<CartItemModel> items) async {
    await _dio.patch(
      ApiEndpoints.CART_BASE,
      data: {'items': items.map((e) => e.toJson()).toList()},
    );
  }

  Future<PartnerCartInfo?> fetchPartnerInfo(String partnerId) async {
    try {
      final response = await _dio.get(ApiEndpoints.partnerById(partnerId));
      if (response.data is! Map<String, dynamic>) return null;
      final json = response.data as Map<String, dynamic>;

      final name = (json['brandName']?.toString().trim().isNotEmpty ?? false)
          ? json['brandName'].toString().trim()
          : (json['businessName']?.toString().trim().isNotEmpty ?? false)
          ? json['businessName'].toString().trim()
          : 'Partenaire';

      final minimumOrder = _toDouble(json['minimumOrder']) ?? 0;
      final logo = resolveMediaUrl(json['logo']?.toString());

      bool isOpen = _toBool(json['isOpen']) ?? true;
      if (json.containsKey('acceptsOrders')) {
        isOpen = isOpen && (_toBool(json['acceptsOrders']) ?? true);
      }
      if (json.containsKey('isActive')) {
        isOpen = isOpen && (_toBool(json['isActive']) ?? true);
      }
      final status = json['status']?.toString().toUpperCase();
      if (status != null && status.isNotEmpty) {
        isOpen = isOpen && status == 'ACTIVE';
      }

      final serviceFee =
          _toDouble(json['serviceFee']) ?? _toDouble(json['service_fee']);
      final commissionRate =
          _toDouble(json['commissionRate']) ??
          _toDouble(json['commission_rate']);

      final normalizedServiceFee = (serviceFee != null && serviceFee >= 0)
          ? serviceFee
          : null;
      final normalizedCommissionRate =
          (commissionRate != null && commissionRate >= 0)
          ? commissionRate
          : null;

      return PartnerCartInfo(
        partnerId: partnerId,
        partnerName: name,
        partnerLogoUrl: logo,
        minimumOrder: minimumOrder,
        isOpen: isOpen,
        serviceFeeValue: normalizedServiceFee ?? normalizedCommissionRate ?? 0,
        serviceFeeIsPercentage:
            normalizedServiceFee == null && normalizedCommissionRate != null,
        deliveryRadius:
            _toInt(json['deliveryRadius']) ?? _toInt(json['delivery_radius']),
        distanceKm:
            _toDouble(json['distanceKm']) ?? _toDouble(json['distance_km']),
        latitude: _toDouble(json['latitude']),
        longitude: _toDouble(json['longitude']),
      );
    } catch (e) {
      debugPrint('[CartRepository] fetchPartnerInfo failed: $e');
      return null;
    }
  }

  Future<DeliveryFeeInfo> fetchDeliveryFee(String partnerId) async {
    final response = await _dio.get(
      '${ApiEndpoints.PARTNER_BASE}/$partnerId/delivery-fee',
    );

    if (response.data is! Map<String, dynamic>) {
      throw StateError('Réponse delivery-fee invalide');
    }

    final json = response.data as Map<String, dynamic>;
    return DeliveryFeeInfo(
      deliveryFee: _toDouble(json['deliveryFee']) ?? 0,
      freeDeliveryThreshold: _toDouble(json['freeDeliveryThreshold']) ?? 0,
    );
  }

  Future<PromoValidationResult> validatePromo({
    required String code,
    required String partnerId,
    required double subtotal,
  }) async {
    final endpoints = <String>[
      '/api/promo/validate',
      '${ApiEndpoints.PROMOTION_BASE}/validate',
      '/promotions/validate',
    ];

    for (final endpoint in endpoints) {
      try {
        final response = await _dio.post(
          endpoint,
          data: {'code': code, 'partnerId': partnerId, 'subtotal': subtotal},
        );

        final json = response.data is Map<String, dynamic>
            ? response.data as Map<String, dynamic>
            : const <String, dynamic>{};

        return PromoValidationResult(
          isValid: true,
          discount: _toDouble(json['discount']) ?? 0,
          message: json['message']?.toString() ?? 'Code promo appliqué',
        );
      } on DioException catch (e) {
        final status = e.response?.statusCode ?? 0;
        final message = _extractErrorMessage(e);

        if (status == 400 || status == 404 || status == 422) {
          return PromoValidationResult(
            isValid: false,
            discount: 0,
            message: message.isNotEmpty ? message : 'Code promo invalide',
          );
        }

        // Try next endpoint only when resource path is unknown.
        if (status != 404) {
          rethrow;
        }
      }
    }

    return const PromoValidationResult(
      isValid: false,
      discount: 0,
      message: 'Service promo indisponible',
    );
  }

  Future<void> placeOrder({
    required List<CartItemModel> cartItems,
    String? promoCode,
    String? addressId,
    required String paymentMethod,
  }) async {
    await _dio.post(
      ApiEndpoints.ORDER_BASE,
      data: {
        'cartItems': cartItems
            .map(
              (item) => {
                'productId': item.productId,
                'partnerId': item.partnerId,
                'quantity': item.quantity,
                'unitPrice': item.unitPrice,
                'selectedOptions': item.selectedOptions,
                'kitchenNote': item.kitchenNote,
              },
            )
            .toList(),
        'promoCode': promoCode,
        'addressId': addressId,
        'paymentMethod': paymentMethod,
      },
    );
  }

  String _extractErrorMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      final message = data['message'] ?? data['error'];
      if (message != null) return message.toString();
    }
    return e.message ?? '';
  }

  double? _toDouble(dynamic value) {
    if (value is num) return value.toDouble();
    if (value is String) return double.tryParse(value);
    return null;
  }

  int? _toInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) {
      final asInt = int.tryParse(value);
      if (asInt != null) return asInt;
      final asDouble = double.tryParse(value);
      if (asDouble != null) return asDouble.toInt();
    }
    return null;
  }

  bool? _toBool(dynamic value) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      final normalized = value.trim().toLowerCase();
      if (normalized == 'true' || normalized == '1') return true;
      if (normalized == 'false' || normalized == '0') return false;
    }
    return null;
  }
}
