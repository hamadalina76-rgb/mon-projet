import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import '../../../../config/di/injection_container.dart';
import '../../../../core/error/exceptions.dart';
import '../../domain/exceptions/auth_exceptions.dart';
import 'auth_local_datasource.dart';

/// Remote data source for authentication
/// Handles all API calls related to authentication
abstract class AuthRemoteDataSource {
  Future<Map<String, dynamic>> login({required String email, required String password});
  Future<Map<String, dynamic>> register({required Map<String, dynamic> data});
  Future<Map<String, dynamic>> verifyPhone({required String phone, required String code});
  Future<Map<String, dynamic>> verifyOtp({required String email, required String otpCode});
  Future<void> resendOtp({required String email});
  Future<void> logout();
  Future<Map<String, dynamic>> refreshToken({required String refreshToken});
  Future<Map<String, dynamic>> getCourierProfile();
  Future<Map<String, dynamic>> updateProfile({required Map<String, dynamic> data});
  Future<Map<String, dynamic>> uploadProfilePhoto({required String filePath});
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  });
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final Dio dio;

  AuthRemoteDataSourceImpl({required this.dio});

  @override
  Future<Map<String, dynamic>> login({required String email, required String password}) async {
    final response = await dio.post('/api/v1/auth/login', data: {
      'email': email,
      'password': password,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> register({required Map<String, dynamic> data}) async {
    final response = await dio.post('/api/v1/auth/register', data: data);
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> verifyPhone({required String phone, required String code}) async {
    final response = await dio.post('/api/v1/auth/verify-phone', data: {
      'phone': phone,
      'code': code,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> verifyOtp({required String email, required String otpCode}) async {
    print('🔐 Verifying OTP: email=$email, code=$otpCode');
    try {
      final response = await dio.post(
        '/api/v1/auth/verify-otp',
        data: {
          'email': email,
          'otpCode': otpCode,
          'type': 'login',
        },
        options: Options(responseType: ResponseType.plain),
      ).timeout(const Duration(seconds: 15));

      final data = response.data;
      print('✅ OTP verification raw response (status=${response.statusCode}): $data');

      if (data is Map<String, dynamic>) return data;
      if (data is String) {
        final parsed = _tryParseJson(data);
        if (parsed is Map<String, dynamic>) return parsed;
      }

      // Unknown shape, return an empty map to let repository handle it
      return <String, dynamic>{'raw': data};
    } on DioException catch (e) {
        final resp = e.response;
        String rawDataRepr = '';
        try {
          if (resp?.data == null) {
            rawDataRepr = 'null';
          } else if (resp!.data is List<int> || resp.data is Uint8List) {
            rawDataRepr = base64.encode(List<int>.from(resp.data));
          } else {
            rawDataRepr = resp.data.toString();
          }
        } catch (_) {
          rawDataRepr = '<<unprintable>>';
        }

          print('❌ DioException during verifyOtp: dioType=${e.type}, message=${e.message}, '
            'error=${e.error}, statusCode=${resp?.statusCode}, headers=${resp?.headers?.map}, '
            'dataType=${resp?.data.runtimeType}, rawData=$rawDataRepr, '
            'request=${e.requestOptions.uri}');

          try {
          print('➡️ Request details: method=${e.requestOptions.method}, '
            'path=${e.requestOptions.path}, headers=${e.requestOptions.headers}, '
            'data=${e.requestOptions.data}');
          } catch (_) {}

        String message = e.message ?? 'Network error';
        try {
          final serverData = _tryParseJson(resp?.data);
          if (serverData is Map<String, dynamic> && serverData['message'] != null) {
            message = serverData['message'].toString();
          }
        } catch (err) {
          // ignore parse errors
        }
        throw AuthenticationException(message);
    }
  }

  // Helper to try parsing JSON without throwing FormatException to caller
  // Accepts String or already-decoded data and returns parsed JSON or null
  dynamic _tryParseJson(dynamic source) {
    try {
      if (source == null) return null;
      if (source is Map<String, dynamic> || source is List) return source;
      if (source is String) {
        final s = source.trim();
        if (s.isEmpty) return null;
        if (s.startsWith('{') || s.startsWith('[')) {
          return jsonDecode(s);
        }
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  @override
  Future<void> resendOtp({required String email}) async {
    await dio.post('/api/v1/auth/resend-otp', data: {
      'email': email,
    });
  }

  @override
  Future<void> logout() async {
    await dio.post('/api/v1/auth/logout');
  }

  @override
  Future<Map<String, dynamic>> refreshToken({required String refreshToken}) async {
    final response = await dio.post('/api/v1/auth/refresh', data: {
      'refreshToken': refreshToken,
    });
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> getCourierProfile() async {
    final response = await dio.get('/couriers/profile');
    return response.data;
  }

  @override
  Future<Map<String, dynamic>> uploadProfilePhoto({required String filePath}) async {
    // 1) Déterminer userId et courierId à partir des données locales / JWT
    String? userId;
    String? courierId;

    try {
      final local = getIt<AuthLocalDataSource>();
      final courierData = await local.getCourierData();
      if (courierData != null) {
        userId = (courierData['userId'] ?? courierData['user_id'])?.toString();
        courierId = (courierData['id'] ?? courierData['courierId'])?.toString();
      }

      // Fallback: extraire userId depuis le token JWT si nécessaire
      if (userId == null || userId.isEmpty) {
        final token = await local.getAccessToken();
        if (token != null && token.isNotEmpty) {
          try {
            final parts = token.split('.');
            if (parts.length >= 2) {
              final payload = utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
              final Map<String, dynamic> claims = jsonDecode(payload);
              userId = (claims['userId'] ?? claims['user_id'] ?? claims['sub'])?.toString();
            }
          } catch (_) {
            // ignore parsing errors, we'll fail below if userId is null
          }
        }
      }
    } catch (e) {
      print('⚠️ Could not determine user/courier id for uploadProfilePhoto: $e');
    }

    if (userId == null || userId.isEmpty) {
      throw Exception('Cannot determine user id for profile photo upload');
    }

    // 2) Uploader la vraie image vers /users/{userId}/profile-picture (user-service)
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(
        filePath,
        filename: filePath.split('/').last,
      ),
    });

    final userResp = await dio.post(
      '/users/$userId/profile-picture',
      data: formData,
      options: Options(
        headers: {'Content-Type': 'multipart/form-data'},
      ),
    );

    final dynamic userData = userResp.data;
    String? profileUrl;
    if (userData is Map<String, dynamic>) {
      profileUrl = userData['profilePicture']?.toString();
    }

    if (profileUrl == null || profileUrl.isEmpty) {
      // Même si l'URL est manquante, retourner la réponse brute pour debug
      throw Exception('Profile picture upload did not return a profilePicture URL');
    }

    // 3) Lier cette URL au livreur via /couriers/{courierId}/documents (PROFILE_PHOTO)
    // On peut ne pas avoir courierId côté mobile (ex: profil non encore créé) → on s'arrête ici.
    if (courierId == null || courierId.isEmpty) {
      // Dans ce cas, l'image est stockée côté user, mais non liée au courier.
      // On renvoie quand même la réponse user-service pour que l'appelant puisse éventuellement rafraîchir.
      return <String, dynamic>{'profilePicture': profileUrl};
    }

    final docResp = await dio.post(
      '/couriers/$courierId/documents',
      data: {
        'documentType': 'PROFILE_PHOTO',
        'documentUrl': profileUrl,
      },
    );

    // docResp.data doit contenir le CourierDTO complet
    if (docResp.data is Map<String, dynamic>) {
      return docResp.data as Map<String, dynamic>;
    }
    throw Exception('Unexpected response type for uploadProfilePhoto: ${docResp.data.runtimeType}');
  }

  @override
  Future<Map<String, dynamic>> updateProfile({required Map<String, dynamic> data}) async {
    // Update using PUT /couriers/{id} with JSON payload.
    // Determine courier id from local storage or JWT claims.
    String? courierId;
    final headers = <String, String>{'Content-Type': 'application/json'};
    try {
      final local = getIt<AuthLocalDataSource>();
      final courierData = await local.getCourierData();
      if (courierData != null) {
        courierId = (courierData['id'] ?? courierData['userId'] ?? courierData['user_id'])?.toString();
      }

      final token = await local.getAccessToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      } else if (courierData != null) {
        final alt = (courierData['accessToken'] ?? courierData['access_token'] ?? courierData['token']);
        if (alt != null) headers['Authorization'] = 'Bearer ${alt.toString()}';
      }

      if (courierId == null) {
        // Try extract from JWT claims
        if (token != null) {
          final parts = token.split('.');
          if (parts.length >= 2) {
            final payload = utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
            final Map<String, dynamic> claims = jsonDecode(payload);
            courierId = (claims['userId'] ?? claims['user_id'] ?? claims['sub'])?.toString();
          }
        }
      }
    } catch (e) {
      print('⚠️ Could not determine courier id for updateProfile: $e');
    }

    if (courierId == null || courierId.isEmpty) {
      throw Exception('Cannot determine courier id for profile update');
    }

    try {
      final response = await dio.put(
        '/couriers/$courierId',
        data: data,
        options: Options(headers: headers),
      );
      return response.data;
    } on DioException catch (e) {
      final resp = e.response;
      final statusCode = resp?.statusCode ?? 0;
      if (statusCode == 400 && resp?.data != null) {
        try {
          final data = resp!.data is Map ? resp.data as Map<String, dynamic> : null;
          if (data != null) {
            final msg = data['message']?.toString() ?? 'Erreur de validation';
            final details = data['details'] is Map
                ? Map<String, dynamic>.from(data['details'] as Map)
                : <String, dynamic>{};
            throw ApiValidationException(
              statusCode: statusCode,
              message: msg,
              details: details,
            );
          }
        } catch (e2) {
          if (e2 is ApiValidationException) rethrow;
        }
      }
      String body = '';
      try {
        if (resp?.data != null) body = resp!.data is String ? resp.data : resp.data.toString();
      } catch (_) {
        body = '<unprintable response body>';
      }
      print('❌ updateProfile failed: status=$statusCode, body=$body, error=${e.message}');
      throw Exception('Profile update failed: status=$statusCode, body=$body');
    }
  }

  @override
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  }) async {
    final formData = FormData();
    
    // Add document data fields
    documentData.forEach((key, value) {
      formData.fields.add(MapEntry(key, value.toString()));
    });
    
    // Add file uploads
    for (var entry in filePaths.entries) {
      formData.files.add(
        MapEntry(
          entry.key,
          await MultipartFile.fromFile(
            entry.value,
            filename: entry.value.split('/').last,
          ),
        ),
      );
    }
    
    // Use PUT /couriers/current_user - API Gateway adds X-User-Id header from JWT
    // Backend finds courier by userId and updates documentation
    // Attempt to include X-User-Id header when calling user-service directly
    String? xUserId;
    try {
      final local = getIt<AuthLocalDataSource>();
      final courierData = await local.getCourierData();
      if (courierData != null) {
        // Prefer the locally stored courier userId to avoid relying on stale JWTs
        xUserId = (courierData['userId'] ?? courierData['id'])?.toString();
        print('🌐 API: using local courier userId for X-User-Id: $xUserId');
      }
      // If still null, try extracting userId from JWT access token
      if (xUserId == null) {
        final token = await local.getAccessToken();
        if (token != null) {
          final parts = token.split('.');
          if (parts.length >= 2) {
            final payload = utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
            final Map<String, dynamic> claims = jsonDecode(payload);
            print('🌐 API: decoded token claims: $claims');
            xUserId = (claims['userId'] ?? claims['user_id'] ?? claims['sub'])?.toString();
          }
        }
      }
    } catch (e) {
      print('⚠️ Could not determine X-User-Id header: $e');
    }

    final headers = {'Content-Type': 'multipart/form-data'};
    if (xUserId != null) headers['X-User-Id'] = xUserId;
    // Include Authorization header when available
    try {
      final local = getIt<AuthLocalDataSource>();
      final token = await local.getAccessToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
        print('🌐 API: Authorization header added for upload');
      } else {
        // Try to find token in saved courier data as a fallback
        final courierData = await local.getCourierData();
        String? altToken;
        if (courierData != null) {
          altToken = (courierData['accessToken'] ?? courierData['access_token'] ?? courierData['token'])?.toString();
        }
        if (altToken != null && altToken.isNotEmpty) {
          headers['Authorization'] = 'Bearer $altToken';
          print('🌐 API: Authorization header added for upload (from courierData)');
        } else {
          print('! No token found for request: /couriers/me');
        }
      }
    } catch (e) {
      print('⚠️ Failed to attach Authorization token: $e');
    }

    await dio.put(
      '/couriers/current_user',
      data: formData,
      options: Options(
        headers: headers,
      ),
    );
  }
}
