import '../entities/courier.dart';

abstract class AuthRepository {
  Future<Courier> login({required String email, required String password});
  Future<void> register({required Map<String, dynamic> data});
  Future<Courier> verifyOtp({required String email, required String otpCode});
  Future<void> resendOtp({required String email});
  Future<void> logout();
  Future<bool> isLoggedIn();
  Future<Courier?> getCurrentCourier();
  /// Fetch the latest courier profile from the backend and persist locally
  Future<Courier> fetchCourierProfile();
  Future<Courier> verifyPhone({required String phone, required String code});
  Future<Courier> updateProfile({required Map<String, dynamic> data});
  Future<void> uploadDocumentation({
    required Map<String, dynamic> documentData,
    required Map<String, String> filePaths,
  });
  Future<Courier> uploadProfilePhoto({required String filePath});
  Future<Courier> updateAvailability({
    required String courierId,
    required bool isOnline,
    required bool isAvailable,
  });
  Future<Map<String, dynamic>> declareUnavailability({
    required String reason,
    int? estimatedDurationMinutes,
    String? comment,
    DateTime? startsAt,
    DateTime? endsAt,
  });
  Future<List<Map<String, dynamic>>> getMyUnavailabilityDeclarations({String? state});
  Future<Map<String, dynamic>> markAsAvailableNow();
  Future<Map<String, dynamic>?> getMyFixedSchedule();
  Future<List<Map<String, dynamic>>> getMyExceptionalSchedules({required String from, required String to});
  Future<List<Map<String, dynamic>>> getMyEffectiveWeek({required String from});
  Future<bool> hasActiveDelivery({required String courierId});
}
