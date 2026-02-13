import 'package:freezed_annotation/freezed_annotation.dart';

part 'otp_result.freezed.dart';

/// Domain entity for OTP result
@freezed
class OtpResult with _$OtpResult {
  const factory OtpResult({
    required String message,
    required String email,
    required bool otpSent,
    required int expirationMinutes,
  }) = _OtpResult;
}
