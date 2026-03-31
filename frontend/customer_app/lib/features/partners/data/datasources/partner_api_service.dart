import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'dart:convert';

import '../../../../core/api/api_endpoints.dart';
import '../../../../config/runtime_config.dart';
import '../models/category_dto.dart';
import '../models/partner_nearby_dto.dart';

class PartnerApiService {
  final Dio _dio;

  PartnerApiService({Dio? dio})
    : _dio =
          dio ??
          Dio(
            BaseOptions(
              baseUrl: RuntimeConfig.apiBaseUrl,
              connectTimeout: const Duration(seconds: 10),
              receiveTimeout: const Duration(seconds: 15),
              headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
              },
            ),
          );

  Future<List<CategoryDto>> fetchCategories() async {
    final url = '${_dio.options.baseUrl}${ApiEndpoints.CATEGORIES}';
    debugPrint('[Categories] GET $url');

    try {
      final response = await _dio.get(ApiEndpoints.CATEGORIES);
      if (response.statusCode == 200 && response.data is List) {
        final list = (response.data as List)
            .whereType<Map<String, dynamic>>()
            .map(CategoryDto.fromJson)
            .toList();
        return list;
      }
      return [];
    } on DioException catch (e) {
      debugPrint('[Categories] DioException: ${e.message}');
      debugPrint('[Categories] status: ${e.response?.statusCode}');
      debugPrint('[Categories] body: ${e.response?.data}');
      rethrow;
    } catch (e) {
      debugPrint('[Categories] error: $e');
      rethrow;
    }
  }

  Future<NearbyPartnersPage> fetchNearbyPartners({
    required double lat,
    required double lng,
    int page = 0,
    int size = 10,
  }) async {
    try {
      final response = await _dio.get(
        ApiEndpoints.NEARBY_PARTNERS,
        queryParameters: {'lat': lat, 'lng': lng, 'page': page, 'size': size},
      );

      if (response.statusCode == 200 && response.data != null) {
        return NearbyPartnersPage.fromJson(
          response.data as Map<String, dynamic>,
        );
      }

      return NearbyPartnersPage(
        content: const [],
        pageNumber: page,
        pageSize: size,
        totalElements: 0,
      );
    } on DioException catch (e) {
      debugPrint('[PartnerApi] fetchNearbyPartners error: $e');
      rethrow;
    }
  }

  Future<NearbyPartnersPage> searchPartners({
    required String query,
    required double lat,
    required double lng,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _dio.get(
        ApiEndpoints.SEARCH_PARTNERS,
        queryParameters: {
          'query': query,
          'lat': lat,
          'lng': lng,
          'page': page,
          'size': size,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        if (response.data is Map<String, dynamic>) {
          return NearbyPartnersPage.fromJson(
            response.data as Map<String, dynamic>,
          );
        }

        if (response.data is List) {
          final list = (response.data as List)
              .whereType<Map<String, dynamic>>()
              .map(PartnerNearbyDto.fromJson)
              .toList();
          return NearbyPartnersPage(
            content: list,
            pageNumber: page,
            pageSize: size,
            totalElements: list.length,
          );
        }
      }

      return NearbyPartnersPage(
        content: const [],
        pageNumber: page,
        pageSize: size,
        totalElements: 0,
      );
    } on DioException catch (e) {
      debugPrint('[PartnerApi] searchPartners error: $e');
      rethrow;
    }
  }

  Future<List<String>> fetchTrendingSearches({
    required double lat,
    required double lng,
    int limit = 5,
  }) async {
    try {
      final response = await _dio.get(
        '${ApiEndpoints.SEARCH_PARTNERS}/trending',
        queryParameters: {'lat': lat, 'lng': lng, 'limit': limit},
      );

      if (response.statusCode == 200 && response.data != null) {
        if (response.data is List) {
          return (response.data as List)
              .map((e) => e.toString())
              .where((e) => e.trim().isNotEmpty)
              .take(limit)
              .toList();
        }

        if (response.data is Map<String, dynamic>) {
          final list = (response.data['content'] ?? response.data['items']);
          if (list is List) {
            return list
                .map((e) => e.toString())
                .where((e) => e.trim().isNotEmpty)
                .take(limit)
                .toList();
          }
        }
      }
    } on DioException catch (e) {
      debugPrint('[PartnerApi] fetchTrendingSearches error: $e');
    }

    // Fallback: derive trends from nearby partners names.
    final nearby = await fetchNearbyPartners(lat: lat, lng: lng, size: 20);
    final trends = <String>[];
    final seen = <String>{};
    for (final partner in nearby.content) {
      final name = partner.displayName.trim();
      if (name.isEmpty) continue;
      final key = name.toLowerCase();
      if (seen.add(key)) trends.add(name);
      if (trends.length >= limit) break;
    }
    return trends;
  }

  Future<PartnerNearbyDto> fetchPartnerById(String partnerId) async {
    final response = await _dio.get(ApiEndpoints.partnerById(partnerId));
    final body = (response.data is Map<String, dynamic>)
        ? Map<String, dynamic>.from(response.data as Map<String, dynamic>)
        : <String, dynamic>{};

    try {
      final opening = await _dio.get(
        '${ApiEndpoints.PARTNER_BASE}/$partnerId/opening-hours',
      );
      if (opening.statusCode == 200 && opening.data is List) {
        body['openingHours'] = opening.data;
      } else if (opening.data is String) {
        body['openingHours'] = jsonDecode(opening.data as String);
      }
    } catch (_) {
      // Keep partner details usable even if opening-hours endpoint fails.
    }

    return PartnerNearbyDto.fromJson(body);
  }

  Future<List<PartnerMenuSectionDto>> fetchPartnerMenu(String partnerId) async {
    try {
      final response = await _dio.get(ApiEndpoints.partnerMenu(partnerId));

      if (response.data is! Map<String, dynamic>) {
        return await _fetchMenuViaCategoryFallback(partnerId);
      }

      final root = response.data as Map<String, dynamic>;
      final raw = root['categories'];
      if (raw is! List) {
        return await _fetchMenuViaCategoryFallback(partnerId);
      }

      final sections = raw
          .whereType<Map<String, dynamic>>()
          .map(PartnerMenuSectionDto.fromJson)
          .where((s) => s.products.isNotEmpty)
          .toList();

      if (sections.isNotEmpty) return sections;
      return await _fetchMenuViaCategoryFallback(partnerId);
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      debugPrint(
        '[PartnerApi] fetchPartnerMenu failed ($status): ${e.message}. Fallback to categories/products.',
      );
      return await _fetchMenuViaCategoryFallback(partnerId);
    } catch (e) {
      debugPrint('[PartnerApi] fetchPartnerMenu unexpected error: $e');
      return await _fetchMenuViaCategoryFallback(partnerId);
    }
  }

  Future<MenuProductDto> fetchMenuProductDetails(
    String partnerId,
    String productId,
  ) async {
    try {
      final response = await _dio.get(
        '${ApiEndpoints.partnerMenu(partnerId)}/products/$productId',
      );

      if (response.data is! Map<String, dynamic>) {
        throw StateError('Unexpected product details response format');
      }

      return MenuProductDto.fromJson(
        Map<String, dynamic>.from(response.data as Map<String, dynamic>),
      );
    } on DioException catch (e) {
      debugPrint(
        '[PartnerApi] fetchMenuProductDetails failed (${e.response?.statusCode}): ${e.message}',
      );
      rethrow;
    }
  }

  Future<List<MenuProductOptionGroupDto>> fetchMenuProductOptionGroups(
    String partnerId,
    String productId,
  ) async {
    try {
      final response = await _dio.get(
        '${ApiEndpoints.partnerMenu(partnerId)}/products/$productId/option-groups',
      );

      if (response.data is! List) return const [];

      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(MenuProductOptionGroupDto.fromJson)
          .toList()
        ..sort((a, b) => a.position.compareTo(b.position));
    } on DioException catch (e) {
      debugPrint(
        '[PartnerApi] fetchMenuProductOptionGroups failed (${e.response?.statusCode}): ${e.message}',
      );
      rethrow;
    }
  }

  Future<List<PartnerMenuSectionDto>> _fetchMenuViaCategoryFallback(
    String partnerId,
  ) async {
    try {
      final categoriesResponse = await _dio.get(
        '${ApiEndpoints.PARTNER_BASE}/$partnerId/menu/categories',
      );

      if (categoriesResponse.data is! List) return const [];

      final categories = (categoriesResponse.data as List)
          .whereType<Map<String, dynamic>>()
          .toList();

      final sections = <PartnerMenuSectionDto>[];

      for (final category in categories) {
        final catId = category['id']?.toString();
        if (catId == null || catId.isEmpty) continue;

        try {
          final productsResponse = await _dio.get(
            '${ApiEndpoints.PARTNER_BASE}/$partnerId/menu/categories/$catId/products',
          );

          final products = (productsResponse.data is List)
              ? (productsResponse.data as List)
                    .whereType<Map<String, dynamic>>()
                    .map(MenuProductDto.fromJson)
                    .where((p) => p.available)
                    .toList()
              : <MenuProductDto>[];

          if (products.isEmpty) continue;

          sections.add(
            PartnerMenuSectionDto(
              id: catId,
              name: category['name']?.toString() ?? 'Menu',
              products: products,
            ),
          );
        } on DioException catch (e) {
          debugPrint(
            '[PartnerApi] category $catId products failed (${e.response?.statusCode}): ${e.message}',
          );
        }
      }

      return sections;
    } catch (e) {
      debugPrint('[PartnerApi] fallback menu failed: $e');
      return const [];
    }
  }

  Future<List<PartnerReviewDto>> fetchPartnerReviews(String partnerId) async {
    try {
      final response = await _dio.get(ApiEndpoints.partnerReviews(partnerId));
      final data = response.data;

      if (data is List) {
        return data
            .whereType<Map<String, dynamic>>()
            .map(PartnerReviewDto.fromJson)
            .toList();
      }

      if (data is Map<String, dynamic>) {
        final content = data['content'];
        if (content is List) {
          return content
              .whereType<Map<String, dynamic>>()
              .map(PartnerReviewDto.fromJson)
              .toList();
        }
      }
    } on DioException catch (e) {
      debugPrint('[PartnerApi] fetchPartnerReviews skipped: ${e.message}');
    }

    return const [];
  }

  Future<List<FavoriteEntryDto>> fetchFavorites(String userId) async {
    final response = await _dio.get(
      ApiEndpoints.FAVORITES,
      queryParameters: {'userId': userId},
    );

    if (response.data is! List) return const [];

    return (response.data as List)
        .whereType<Map<String, dynamic>>()
        .map(FavoriteEntryDto.fromJson)
        .where((entry) => entry.partnerId.isNotEmpty)
        .toList();
  }

  Future<Set<String>> fetchFavoritePartnerIds(String customerId) async {
    final favorites = await fetchFavorites(customerId);
    return favorites.map((f) => f.partnerId).toSet();
  }

  Future<bool> addFavorite(String customerId, String partnerId) async {
    final response = await _dio.post(
      ApiEndpoints.FAVORITES,
      data: {'partnerId': partnerId},
    );
    return response.statusCode == 201;
  }

  Future<void> removeFavorite(String customerId, String partnerId) async {
    await _dio.delete(ApiEndpoints.favoriteByPartner(partnerId));
  }
}

class FavoriteEntryDto {
  final String id;
  final String customerId;
  final String partnerId;
  final DateTime? createdAt;

  const FavoriteEntryDto({
    required this.id,
    required this.customerId,
    required this.partnerId,
    this.createdAt,
  });

  factory FavoriteEntryDto.fromJson(Map<String, dynamic> json) {
    DateTime? createdAt;
    final rawCreatedAt = json['createdAt'];
    if (rawCreatedAt != null) {
      try {
        createdAt = DateTime.parse(rawCreatedAt.toString());
      } catch (_) {
        createdAt = null;
      }
    }

    return FavoriteEntryDto(
      id: json['id']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      partnerId: json['partnerId']?.toString() ?? '',
      createdAt: createdAt,
    );
  }
}

class PartnerMenuSectionDto {
  final String id;
  final String name;
  final List<MenuProductDto> products;

  const PartnerMenuSectionDto({
    required this.id,
    required this.name,
    required this.products,
  });

  factory PartnerMenuSectionDto.fromJson(Map<String, dynamic> json) {
    final category = (json['category'] is Map<String, dynamic>)
        ? json['category'] as Map<String, dynamic>
        : const <String, dynamic>{};
    final productsRaw = json['products'];

    return PartnerMenuSectionDto(
      id: category['id']?.toString() ?? '',
      name: category['name']?.toString() ?? 'Menu',
      products: (productsRaw is List)
          ? productsRaw
                .whereType<Map<String, dynamic>>()
                .map(MenuProductDto.fromJson)
                .where((p) => p.available)
                .toList()
          : const [],
    );
  }
}

class MenuProductDto {
  final String id;
  final String? categoryId;
  final String? categoryName;
  final String name;
  final String description;
  final String imageUrl;
  final double price;
  final double? originalPrice;
  final double? discount;
  final double? rating;
  final int? preparationTime;
  final bool available;
  final bool popular;
  final List<MenuProductOptionGroupDto> optionGroups;

  const MenuProductDto({
    required this.id,
    this.categoryId,
    this.categoryName,
    required this.name,
    required this.description,
    required this.imageUrl,
    required this.price,
    this.originalPrice,
    this.discount,
    this.rating,
    this.preparationTime,
    this.available = true,
    this.popular = false,
    this.optionGroups = const [],
  });

  factory MenuProductDto.fromJson(Map<String, dynamic> json) {
    final category = json['category'] is Map<String, dynamic>
        ? json['category'] as Map<String, dynamic>
        : const <String, dynamic>{};

    return MenuProductDto(
      id: json['id']?.toString() ?? '',
      categoryId: json['categoryId']?.toString() ?? category['id']?.toString(),
      categoryName:
          json['categoryName']?.toString() ?? category['name']?.toString(),
      name: json['name']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      imageUrl: json['imageUrl']?.toString() ?? '',
      price: _parseDouble(json['price']) ?? 0,
      originalPrice: _parseDouble(json['originalPrice']),
      discount: _parseDouble(json['discountPercentage']),
      rating: _parseDouble(
        json['rating'] ?? json['averageRating'] ?? json['productRating'],
      ),
      preparationTime: _parseInt(
        json['preparationTime'] ??
            json['preparationTimeMin'] ??
            json['preparationTimeMinutes'] ??
            json['estimatedPrepTime'],
      ),
      available: _parseBool(json['isAvailable'], defaultValue: true),
      popular: _parseBool(json['isPopular']),
      optionGroups: _parseOptionGroups(json['optionGroups']),
    );
  }

  static List<MenuProductOptionGroupDto> _parseOptionGroups(dynamic value) {
    if (value is! List) return const [];
    final groups =
        value
            .whereType<Map<String, dynamic>>()
            .map(MenuProductOptionGroupDto.fromJson)
            .toList()
          ..sort((a, b) => a.position.compareTo(b.position));
    return groups;
  }

  static double? _parseDouble(dynamic value) {
    if (value is num) return value.toDouble();
    if (value is String) return double.tryParse(value);
    return null;
  }

  static int? _parseInt(dynamic value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static bool _parseBool(dynamic value, {bool defaultValue = false}) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      final normalized = value.trim().toLowerCase();
      return normalized == 'true' || normalized == '1' || normalized == 'yes';
    }
    return defaultValue;
  }

  bool get hasDiscount {
    if (discount != null && discount! > 0) return true;
    if (originalPrice != null && originalPrice! > price) return true;
    return false;
  }
}

class MenuProductOptionGroupDto {
  final String id;
  final String productId;
  final String name;
  final String type;
  final bool isRequired;
  final int minSelection;
  final int maxSelection;
  final int position;
  final List<MenuProductOptionDto> options;

  const MenuProductOptionGroupDto({
    required this.id,
    required this.productId,
    required this.name,
    required this.type,
    required this.isRequired,
    required this.minSelection,
    required this.maxSelection,
    required this.position,
    required this.options,
  });

  factory MenuProductOptionGroupDto.fromJson(Map<String, dynamic> json) {
    final type = json['type']?.toString().toUpperCase() ?? 'MULTIPLE';
    final options = _parseOptions(json['options']);
    final minSelection = _parseInt(json['minSelection']) ?? 0;
    final defaultMaxSelection = type == 'MULTIPLE'
        ? (options.length > 1 ? options.length : 3)
        : 1;
    final maxSelection = _parseInt(json['maxSelection']) ?? defaultMaxSelection;
    final resolvedMinSelection = minSelection < 0 ? 0 : minSelection;
    final resolvedMaxSelection = maxSelection < 1 ? 1 : maxSelection;
    final minBoundMaxSelection = resolvedMaxSelection < resolvedMinSelection
        ? resolvedMinSelection
        : resolvedMaxSelection;
    final normalizedMaxSelection =
        type == 'MULTIPLE' && options.length > 1 && minBoundMaxSelection <= 1
        ? options.length
        : minBoundMaxSelection;

    return MenuProductOptionGroupDto(
      id: json['id']?.toString() ?? '',
      productId: json['productId']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      type: type,
      isRequired: _parseBool(json['isRequired']),
      minSelection: resolvedMinSelection,
      maxSelection: normalizedMaxSelection,
      position: _parseInt(json['position']) ?? 0,
      options: options,
    );
  }

  bool get isSingle => type == 'SINGLE';

  String get selectionRuleLabel {
    if (isSingle) return 'Pick one';
    if (minSelection > 0) {
      return 'Pick $minSelection-$maxSelection';
    }
    return 'Up to $maxSelection';
  }

  static List<MenuProductOptionDto> _parseOptions(dynamic value) {
    if (value is! List) return const [];
    final options =
        value
            .whereType<Map<String, dynamic>>()
            .map(MenuProductOptionDto.fromJson)
            .toList()
          ..sort((a, b) => a.position.compareTo(b.position));
    return options;
  }

  static int? _parseInt(dynamic value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static bool _parseBool(dynamic value, {bool defaultValue = false}) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      final normalized = value.trim().toLowerCase();
      return normalized == 'true' || normalized == '1' || normalized == 'yes';
    }
    return defaultValue;
  }
}

class MenuProductOptionDto {
  final String id;
  final String groupId;
  final String name;
  final double priceModifier;
  final bool isDefault;
  final bool isAvailable;
  final int position;

  const MenuProductOptionDto({
    required this.id,
    required this.groupId,
    required this.name,
    required this.priceModifier,
    required this.isDefault,
    required this.isAvailable,
    required this.position,
  });

  factory MenuProductOptionDto.fromJson(Map<String, dynamic> json) {
    return MenuProductOptionDto(
      id: json['id']?.toString() ?? '',
      groupId: json['groupId']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      priceModifier: (json['priceModifier'] as num?)?.toDouble() ?? 0,
      isDefault: _parseBool(json['isDefault']),
      isAvailable: _parseBool(json['isAvailable'], defaultValue: true),
      position: _parseInt(json['position']) ?? 0,
    );
  }

  static int? _parseInt(dynamic value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static bool _parseBool(dynamic value, {bool defaultValue = false}) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      final normalized = value.trim().toLowerCase();
      return normalized == 'true' || normalized == '1' || normalized == 'yes';
    }
    return defaultValue;
  }
}

class PartnerReviewDto {
  final String id;
  final int rating;
  final String comment;
  final String author;
  final DateTime? createdAt;

  const PartnerReviewDto({
    required this.id,
    required this.rating,
    required this.comment,
    required this.author,
    this.createdAt,
  });

  factory PartnerReviewDto.fromJson(Map<String, dynamic> json) {
    DateTime? created;
    final rawCreated = json['createdAt'];
    if (rawCreated != null) {
      try {
        created = DateTime.parse(rawCreated.toString());
      } catch (_) {}
    }

    final customerId = json['customerId']?.toString();
    final fallbackAuthor = customerId != null
        ? 'Client #$customerId'
        : 'Client';

    return PartnerReviewDto(
      id: json['id']?.toString() ?? '',
      rating: (json['rating'] as num?)?.toInt() ?? 0,
      comment: json['comment']?.toString() ?? '',
      author: json['customerName']?.toString() ?? fallbackAuthor,
      createdAt: created,
    );
  }
}
