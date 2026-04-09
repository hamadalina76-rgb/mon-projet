import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'dart:convert';

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

class PartnerOpeningHour {
  final String dayOfWeek;
  final String? openTime;
  final String? closeTime;
  final bool isClosed;
  final bool is24Hours;

  const PartnerOpeningHour({
    required this.dayOfWeek,
    this.openTime,
    this.closeTime,
    this.isClosed = false,
    this.is24Hours = false,
  });

  factory PartnerOpeningHour.fromJson(Map<String, dynamic> json) {
    final raw = json['dayOfWeek'] ?? json['day'];
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    final dayAliases = <String, String>{
      'MONDAY': 'MONDAY',
      'MON': 'MONDAY',
      'LUNDI': 'MONDAY',
      'LUN': 'MONDAY',
      'TUESDAY': 'TUESDAY',
      'TUE': 'TUESDAY',
      'MARDI': 'TUESDAY',
      'MAR': 'TUESDAY',
      'WEDNESDAY': 'WEDNESDAY',
      'WED': 'WEDNESDAY',
      'MERCREDI': 'WEDNESDAY',
      'MER': 'WEDNESDAY',
      'THURSDAY': 'THURSDAY',
      'THU': 'THURSDAY',
      'JEUDI': 'THURSDAY',
      'JEU': 'THURSDAY',
      'FRIDAY': 'FRIDAY',
      'FRI': 'FRIDAY',
      'VENDREDI': 'FRIDAY',
      'VEN': 'FRIDAY',
      'SATURDAY': 'SATURDAY',
      'SAT': 'SATURDAY',
      'SAMEDI': 'SATURDAY',
      'SAM': 'SATURDAY',
      'SUNDAY': 'SUNDAY',
      'SUN': 'SUNDAY',
      'DIMANCHE': 'SUNDAY',
      'DIM': 'SUNDAY',
    };

    final normalizedDay = (raw is int && raw >= 1 && raw <= 7)
        ? dayNames[raw - 1]
        : dayAliases[(raw?.toString() ?? '').trim().toUpperCase()] ?? '';

    String? openTime = json['openTime']?.toString();
    String? closeTime = json['closeTime']?.toString();

    if ((openTime == null || closeTime == null) && json['slots'] is List) {
      final slots = (json['slots'] as List)
          .whereType<Map>()
          .map((raw) => Map<String, dynamic>.from(raw))
          .toList();
      if (slots.isNotEmpty) {
        final first = slots.first;
        openTime =
            openTime ?? first['open']?.toString() ?? first['openTime']?.toString();
        closeTime =
            closeTime ?? first['close']?.toString() ?? first['closeTime']?.toString();
      }
    }

    return PartnerOpeningHour(
      dayOfWeek: normalizedDay,
      openTime: openTime,
      closeTime: closeTime,
      isClosed: json['isClosed'] as bool? ?? false,
      is24Hours: json['is24Hours'] as bool? ?? false,
    );
  }
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
  final List<PartnerOpeningHour> openingHours;

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
    this.openingHours = const <PartnerOpeningHour>[],
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
      final json = Map<String, dynamic>.from(response.data as Map<String, dynamic>);

      final embeddedHours =
          _extractOpeningHoursPayload(json['openingHours']) ??
          _extractOpeningHoursPayload(json['openingHoursDisplay']);
      if (embeddedHours is List) {
        json['openingHours'] = embeddedHours;
      }

      try {
        final opening = await _dio.get(
          '${ApiEndpoints.PARTNER_BASE}/$partnerId/opening-hours',
        );
        if (opening.statusCode == 200) {
          final openingPayload = _extractOpeningHoursPayload(opening.data);
          if (openingPayload is List) {
            json['openingHours'] = openingPayload;
          }
        }
      } catch (_) {
        // Keep partner info usable if opening hours endpoint is unavailable.
      }

      final name = (json['brandName']?.toString().trim().isNotEmpty ?? false)
          ? json['brandName'].toString().trim()
          : (json['businessName']?.toString().trim().isNotEmpty ?? false)
          ? json['businessName'].toString().trim()
          : 'Partenaire';

      final minimumOrder = _toDouble(json['minimumOrder']) ?? 0;
      final logo = resolveMediaUrl(json['logo']?.toString());

      final openingHours = _parseOpeningHours(json['openingHours']);

      final acceptsOrders = _toBool(json['acceptsOrders']) ?? true;
      final isActive = _toBool(json['isActive']) ?? true;
      final status = json['status']?.toString().toUpperCase();
      final statusAllowsOrders =
          status == null || status.isEmpty || status == 'ACTIVE';
      final availabilityAllowsOrders =
          acceptsOrders && isActive && statusAllowsOrders;

      final backendOpen =
          _toBool(json['isOpen']) ?? _toBool(json['isCurrentlyOpen']) ?? true;
      final scheduleOpen = _computeIsOpenFromOpeningHours(openingHours);
      final isOpen =
          availabilityAllowsOrders && (scheduleOpen ?? backendOpen);

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
        openingHours: openingHours,
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
    Map<String, dynamic>? deliveryAddressDetails,
    required String paymentMethod,
    DateTime? scheduledDeliveryTime,
  }) async {
    final payload = <String, dynamic>{
      'cartItems': cartItems
          .map(
            (item) => {
              'productId': item.productId,
              'partnerId': item.partnerId,
              'quantity': item.quantity,
              'unitPrice': item.unitPrice,
              'selectedOptions': item.selectedOptionsPayload,
              'kitchenNote': item.kitchenNote,
            },
          )
          .toList(),
      'promoCode': promoCode,
      'addressId': addressId,
      'paymentMethod': paymentMethod,
      'isScheduled': scheduledDeliveryTime != null,
      if (deliveryAddressDetails != null)
        'deliveryAddressDetails': deliveryAddressDetails,
    };

    if (scheduledDeliveryTime != null) {
      payload['scheduledDate'] = _formatDate(scheduledDeliveryTime);
      payload['scheduledTime'] = _formatTime(scheduledDeliveryTime);
    }

    await _dio.post(
      ApiEndpoints.ORDER_BASE,
      data: payload,
    );
  }

  dynamic _extractOpeningHoursPayload(dynamic raw) {
    if (raw == null) return null;

    if (raw is String) {
      try {
        return _extractOpeningHoursPayload(jsonDecode(raw));
      } catch (_) {
        return null;
      }
    }

    if (raw is List) {
      return raw;
    }

    if (raw is Map) {
      final map = Map<String, dynamic>.from(raw);
      for (final key in const <String>[
        'openingHours',
        'hours',
        'data',
        'content',
      ]) {
        final extracted = _extractOpeningHoursPayload(map[key]);
        if (extracted is List) {
          return extracted;
        }
      }
    }

    return null;
  }

  List<PartnerOpeningHour> _parseOpeningHours(dynamic rawHours) {
    final source = _extractOpeningHoursPayload(rawHours);
    if (source is! List) return const <PartnerOpeningHour>[];

    return source
        .whereType<Map>()
        .expand((raw) {
          final map = Map<String, dynamic>.from(raw);
          final base = PartnerOpeningHour.fromJson(map);
          if (base.dayOfWeek.isEmpty) {
            return const <PartnerOpeningHour>[];
          }

          if (base.isClosed || base.is24Hours) {
            return <PartnerOpeningHour>[base];
          }

          final rawSlots = map['slots'];
          if (rawSlots is List) {
            final expanded = rawSlots
                .whereType<Map>()
                .map((slot) => Map<String, dynamic>.from(slot))
                .map((slot) {
                  final open =
                      slot['open']?.toString() ?? slot['openTime']?.toString();
                  final close =
                      slot['close']?.toString() ?? slot['closeTime']?.toString();
                  if (open == null || close == null) {
                    return null;
                  }

                  return PartnerOpeningHour(
                    dayOfWeek: base.dayOfWeek,
                    openTime: open,
                    closeTime: close,
                  );
                })
                .whereType<PartnerOpeningHour>()
                .toList();

            if (expanded.isNotEmpty) {
              return expanded;
            }
          }

          if (base.openTime != null && base.closeTime != null) {
            return <PartnerOpeningHour>[base];
          }

          return const <PartnerOpeningHour>[];
        })
        .where((h) => h.dayOfWeek.isNotEmpty)
        .toList();
  }

  bool? _computeIsOpenFromOpeningHours(List<PartnerOpeningHour> openingHours) {
    if (openingHours.isEmpty) return null;

    const dayNames = <String>[
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    final now = DateTime.now();
    final nowMinutes = (now.hour * 60) + now.minute;
    final todayName = dayNames[now.weekday - 1];
    final yesterdayName = dayNames[(now.weekday + 5) % 7];

    for (final hour in openingHours) {
      if (hour.dayOfWeek.toUpperCase() != todayName || hour.isClosed) {
        continue;
      }

      if (hour.is24Hours) {
        return true;
      }

      final openMinutes = _parseMinutes(hour.openTime);
      final closeMinutes = _parseMinutes(hour.closeTime);
      if (openMinutes == null || closeMinutes == null) {
        return true;
      }

      if (closeMinutes > openMinutes) {
        if (nowMinutes >= openMinutes && nowMinutes < closeMinutes) {
          return true;
        }
      } else {
        // Overnight slot, e.g. 19:00 -> 02:00 (today's evening segment).
        if (nowMinutes >= openMinutes) {
          return true;
        }
      }
    }

    for (final hour in openingHours) {
      if (hour.dayOfWeek.toUpperCase() != yesterdayName || hour.isClosed) {
        continue;
      }

      if (hour.is24Hours) {
        return true;
      }

      final openMinutes = _parseMinutes(hour.openTime);
      final closeMinutes = _parseMinutes(hour.closeTime);
      if (openMinutes == null || closeMinutes == null) {
        continue;
      }

      // Overnight slot from yesterday contributes to today's early hours.
      if (closeMinutes <= openMinutes && nowMinutes < closeMinutes) {
        return true;
      }
    }

    return false;
  }

  int? _parseMinutes(String? value) {
    if (value == null || value.trim().isEmpty) return null;

    final parts = value.trim().split(':');
    if (parts.length < 2) return null;

    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null) return null;
    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;

    return (hour * 60) + minute;
  }

  String _formatDate(DateTime value) {
    final y = value.year.toString().padLeft(4, '0');
    final m = value.month.toString().padLeft(2, '0');
    final d = value.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  String _formatTime(DateTime value) {
    final h = value.hour.toString().padLeft(2, '0');
    final m = value.minute.toString().padLeft(2, '0');
    return '$h:$m:00';
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
