// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'otp_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$OtpResponseImpl _$$OtpResponseImplFromJson(Map<String, dynamic> json) =>
    _$OtpResponseImpl(
      message: json['message'] as String,
      email: json['email'] as String,
      otpSent: json['otpSent'] as bool,
      expirationMinutes: (json['expirationMinutes'] as num).toInt(),
    );

Map<String, dynamic> _$$OtpResponseImplToJson(_$OtpResponseImpl instance) =>
    <String, dynamic>{
      'message': instance.message,
      'email': instance.email,
      'otpSent': instance.otpSent,
      'expirationMinutes': instance.expirationMinutes,
    };
