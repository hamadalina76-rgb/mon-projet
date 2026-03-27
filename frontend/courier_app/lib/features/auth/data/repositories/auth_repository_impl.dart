import '../../domain/entities/courier.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/exceptions/auth_exceptions.dart';
import '../datasources/auth_local_datasource.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/courier_model.dart';
import '../models/login_response.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final AuthLocalDataSource localDataSource;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.localDataSource,
  });

  @override
  Future<Courier> login({required String email, required String password}) async {
    final response = await remoteDataSource.login(email: email, password: password);
    
    // Check if OTP verification is required
    if (response['otpSent'] == true) {
      throw EmailVerificationRequiredException(
        email: response['email'] ?? email,
        message: response['message'] ?? 'OTP sent to your email. Please verify to complete login.',
      );
    }
    
    final loginResponse = LoginResponse.fromJson(response);
    
    await localDataSource.saveTokens(
      accessToken: loginResponse.accessToken,
      refreshToken: loginResponse.refreshToken,
    );
    
    // Fetch full courier profile from user-service to get documentsVerified status
    try {
      final courierProfile = await remoteDataSource.getCourierProfile();
      final courier = CourierModel.fromJson(courierProfile);
      await localDataSource.saveCourierData(courier.toJson());
      return courier;
    } catch (e) {
      print('⚠️ Failed to fetch courier profile, using login response data: $e');
      // Fallback to data from login response if profile fetch fails
      await localDataSource.saveCourierData(loginResponse.courier.toJson());
      return loginResponse.courier;
    }
  }

  @override
  Future<void> register({required Map<String, dynamic> data}) async {
    // Just call the registration endpoint - it returns a success message
    final response = await remoteDataSource.register(data: data);
    print('✅ Registration successful: $response');
    // No need to parse tokens or save data - user will login after registration
  }

  @override
  Future<void> logout() async {
    await remoteDataSource.logout();
    await localDataSource.clearTokens();
  }

  @override
  Future<bool> isLoggedIn() async {
    final token = await localDataSource.getAccessToken();
    return token != null && token.isNotEmpty;
  }

  @override
  Future<Courier?> getCurrentCourier() async {
    final data = await localDataSource.getCourierData();
    if (data == null) return null;
    return CourierModel.fromJson(data);
  }

  @override
  Future<Courier> fetchCourierProfile() async {
    final response = await remoteDataSource.getCourierProfile();
    final courier = CourierModel.fromJson(response);
    // persist locally for offline access
    await localDataSource.saveCourierData(courier.toJson());
    return courier;
  }

  @override
  Future<Courier> verifyPhone({required String phone, required String code}) async {
    final response = await remoteDataSource.verifyPhone(phone: phone, code: code);
    final courier = CourierModel.fromJson(response['courier']);
    await localDataSource.saveCourierData(courier.toJson());
    return courier;
  }

  @override
  Future<Courier> verifyOtp({required String email, required String otpCode}) async {
    print('📧 Repository: Verifying OTP for email=$email');
    final response = await remoteDataSource.verifyOtp(email: email, otpCode: otpCode);
    print('📦 Repository: OTP verification response keys: ${response.keys}');
    
    // Save tokens from OTP verification response (accept multiple key styles)
    String? accessToken;
    String? refreshToken;
    if (response.containsKey('accessToken')) accessToken = response['accessToken']?.toString();
    if (response.containsKey('access_token')) accessToken ??= response['access_token']?.toString();
    if (response.containsKey('token')) accessToken ??= response['token']?.toString();

    if (response.containsKey('refreshToken')) refreshToken = response['refreshToken']?.toString();
    if (response.containsKey('refresh_token')) refreshToken ??= response['refresh_token']?.toString();

    if (accessToken != null && refreshToken != null) {
      print('💾 Repository: Saving tokens (access token length=${accessToken.length})');
      await localDataSource.saveTokens(
        accessToken: accessToken,
        refreshToken: refreshToken,
      );
    } else {
      print('⚠️ Repository: No access/refresh tokens found in OTP response. Available keys: ${response.keys}');
    }
    
    // Get courier data from response
    final courierData = response['courier'] ?? response['user'];
    if (courierData == null) {
      print('❌ Repository: No courier data in response');
      throw Exception('No courier data in OTP verification response');
    }
    
    print('👤 Repository: Parsing courier data');
    final courier = CourierModel.fromJson(courierData);
    await localDataSource.saveCourierData(courier.toJson());
    print('✅ Repository: OTP verification complete, isEmailVerified=${courier.isEmailVerified}');
    return courier;
  }

  @override
  Future<void> resendOtp({required String email}) async {
    await remoteDataSource.resendOtp(email: email);
  }

  @override
  Future<Courier> updateProfile({required Map<String, dynamic> data}) async {
    final response = await remoteDataSource.updateProfile(data: data);
    final courier = CourierModel.fromJson(response);
    await localDataSource.saveCourierData(courier.toJson());
    return courier;
  }

  @override
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  }) async {
    await remoteDataSource.uploadDocumentation(
      documentData: documentData,
      filePaths: filePaths,
    );
  }

  @override
  Future<Courier> uploadProfilePhoto({required String filePath}) async {
    final response = await remoteDataSource.uploadProfilePhoto(filePath: filePath);
    final courier = CourierModel.fromJson(response);
    await localDataSource.saveCourierData(courier.toJson());
    return courier;
  }

  @override
  Future<Courier> updateAvailability({
    required String courierId,
    required bool isOnline,
    required bool isAvailable,
  }) async {
    final response = await remoteDataSource.updateCourierAvailability(
      courierId: courierId,
      isOnline: isOnline,
      isAvailable: isAvailable,
    );
    Courier courier;
    try {
      courier = CourierModel.fromJson(response);
      await localDataSource.saveCourierData((courier as CourierModel).toJson());
    } catch (_) {
      courier = await fetchCourierProfile();
    }
    return courier;
  }

  @override
  Future<bool> hasActiveDelivery({required String courierId}) {
    return remoteDataSource.hasActiveDelivery(courierId: courierId);
  }
}
