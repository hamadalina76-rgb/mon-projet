import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../../core/api/api_endpoints.dart';
import '../models/category_dto.dart';
import '../models/partner_nearby_dto.dart';

class PartnerApiService {
  final Dio _dio;

  static const String _baseUrl = 'http://192.168.1.15:8080';

  PartnerApiService({Dio? dio})
      : _dio =
            dio ??
            Dio(
              BaseOptions(
                baseUrl: _baseUrl,
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
        queryParameters: {
          'lat': lat,
          'lng': lng,
          'page': page,
          'size': size,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        return NearbyPartnersPage.fromJson(response.data as Map<String, dynamic>);
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
}
