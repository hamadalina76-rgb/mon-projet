import 'dart:io';
import 'package:dio/dio.dart';
import '../api_endpoints.dart';
import '../api_client.dart';

/// Service API pour les opérations utilisateur
class UserApiService {
  final ApiClient _apiClient;

  UserApiService({ApiClient? apiClient})
    : _apiClient = apiClient ?? ApiClient();

  /// Met à jour le profil utilisateur
  Future<Map<String, dynamic>> updateUserProfile({
    required String userId,
    String? firstName,
    String? lastName,
    String? email,
    String? phoneNumber,
  }) async {
    try {
      final Map<String, dynamic> data = {};

      if (firstName != null) data['firstName'] = firstName;
      if (lastName != null) data['lastName'] = lastName;
      if (email != null) data['email'] = email;
      if (phoneNumber != null) data['phoneNumber'] = phoneNumber;

      final response = await _apiClient.dio.put(
        '${ApiEndpoints.USER_BASE}/$userId',
        data: data,
      );

      if (response.statusCode == 200) {
        return response.data as Map<String, dynamic>;
      } else {
        throw Exception('Failed to update profile: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw Exception('Error updating profile: ${e.message}');
    }
  }

  /// Upload une photo de profil
  Future<Map<String, dynamic>> uploadProfilePicture({
    required String userId,
    required File imageFile,
  }) async {
    try {
      String fileName = imageFile.path.split('/').last;

      FormData formData = FormData.fromMap({
        'file': await MultipartFile.fromFile(
          imageFile.path,
          filename: fileName,
        ),
      });

      final response = await _apiClient.dio.post(
        '${ApiEndpoints.USER_BASE}/$userId/profile-picture',
        data: formData,
        options: Options(contentType: 'multipart/form-data'),
      );

      if (response.statusCode == 200) {
        return response.data as Map<String, dynamic>;
      } else {
        throw Exception('Failed to upload picture: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw Exception('Error uploading picture: ${e.message}');
    }
  }

  /// Récupère les informations utilisateur
  Future<Map<String, dynamic>> getUserInfo(String userId) async {
    try {
      final response = await _apiClient.dio.get(
        '${ApiEndpoints.USER_BASE}/$userId',
      );

      if (response.statusCode == 200) {
        return response.data as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get user info: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw Exception('Error getting user info: ${e.message}');
    }
  }
}
