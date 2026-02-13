import 'package:freezed_annotation/freezed_annotation.dart';

part 'otp_response.freezed.dart';
part 'otp_response.g.dart';

@freezed
class OtpResponse with _$OtpResponse {
  const factory OtpResponse({
    required String message,
    required String email,
    required bool otpSent,
    required int expirationMinutes,
  }) = _OtpResponse;

  factory OtpResponse.fromJson(Map<String, dynamic> json) =>
      _$OtpResponseFromJson(json);
}
