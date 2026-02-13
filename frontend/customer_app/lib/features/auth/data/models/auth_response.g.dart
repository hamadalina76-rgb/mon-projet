// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'auth_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$AuthResponseImpl _$$AuthResponseImplFromJson(Map<String, dynamic> json) =>
    _$AuthResponseImpl(
      token: json['access_token'] as String,
      refreshToken: json['refresh_token'] as String,
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>),
      expiresIn: (json['expiresIn'] as num?)?.toInt(),
      tokenType: json['token_type'] as String?,
      isNewUser: json['isNewUser'] as bool?,
    );

Map<String, dynamic> _$$AuthResponseImplToJson(_$AuthResponseImpl instance) =>
    <String, dynamic>{
      'access_token': instance.token,
      'refresh_token': instance.refreshToken,
      'user': instance.user,
      'expiresIn': instance.expiresIn,
      'token_type': instance.tokenType,
      'isNewUser': instance.isNewUser,
    };
