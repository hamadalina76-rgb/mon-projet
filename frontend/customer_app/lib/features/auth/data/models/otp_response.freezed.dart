// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'otp_response.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

OtpResponse _$OtpResponseFromJson(Map<String, dynamic> json) {
  return _OtpResponse.fromJson(json);
}

/// @nodoc
mixin _$OtpResponse {
  String get message => throw _privateConstructorUsedError;
  String get email => throw _privateConstructorUsedError;
  bool get otpSent => throw _privateConstructorUsedError;
  int get expirationMinutes => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $OtpResponseCopyWith<OtpResponse> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $OtpResponseCopyWith<$Res> {
  factory $OtpResponseCopyWith(
          OtpResponse value, $Res Function(OtpResponse) then) =
      _$OtpResponseCopyWithImpl<$Res, OtpResponse>;
  @useResult
  $Res call(
      {String message, String email, bool otpSent, int expirationMinutes});
}

/// @nodoc
class _$OtpResponseCopyWithImpl<$Res, $Val extends OtpResponse>
    implements $OtpResponseCopyWith<$Res> {
  _$OtpResponseCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? message = null,
    Object? email = null,
    Object? otpSent = null,
    Object? expirationMinutes = null,
  }) {
    return _then(_value.copyWith(
      message: null == message
          ? _value.message
          : message // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      otpSent: null == otpSent
          ? _value.otpSent
          : otpSent // ignore: cast_nullable_to_non_nullable
              as bool,
      expirationMinutes: null == expirationMinutes
          ? _value.expirationMinutes
          : expirationMinutes // ignore: cast_nullable_to_non_nullable
              as int,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$OtpResponseImplCopyWith<$Res>
    implements $OtpResponseCopyWith<$Res> {
  factory _$$OtpResponseImplCopyWith(
          _$OtpResponseImpl value, $Res Function(_$OtpResponseImpl) then) =
      __$$OtpResponseImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String message, String email, bool otpSent, int expirationMinutes});
}

/// @nodoc
class __$$OtpResponseImplCopyWithImpl<$Res>
    extends _$OtpResponseCopyWithImpl<$Res, _$OtpResponseImpl>
    implements _$$OtpResponseImplCopyWith<$Res> {
  __$$OtpResponseImplCopyWithImpl(
      _$OtpResponseImpl _value, $Res Function(_$OtpResponseImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? message = null,
    Object? email = null,
    Object? otpSent = null,
    Object? expirationMinutes = null,
  }) {
    return _then(_$OtpResponseImpl(
      message: null == message
          ? _value.message
          : message // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      otpSent: null == otpSent
          ? _value.otpSent
          : otpSent // ignore: cast_nullable_to_non_nullable
              as bool,
      expirationMinutes: null == expirationMinutes
          ? _value.expirationMinutes
          : expirationMinutes // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$OtpResponseImpl implements _OtpResponse {
  const _$OtpResponseImpl(
      {required this.message,
      required this.email,
      required this.otpSent,
      required this.expirationMinutes});

  factory _$OtpResponseImpl.fromJson(Map<String, dynamic> json) =>
      _$$OtpResponseImplFromJson(json);

  @override
  final String message;
  @override
  final String email;
  @override
  final bool otpSent;
  @override
  final int expirationMinutes;

  @override
  String toString() {
    return 'OtpResponse(message: $message, email: $email, otpSent: $otpSent, expirationMinutes: $expirationMinutes)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$OtpResponseImpl &&
            (identical(other.message, message) || other.message == message) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.otpSent, otpSent) || other.otpSent == otpSent) &&
            (identical(other.expirationMinutes, expirationMinutes) ||
                other.expirationMinutes == expirationMinutes));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, message, email, otpSent, expirationMinutes);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$OtpResponseImplCopyWith<_$OtpResponseImpl> get copyWith =>
      __$$OtpResponseImplCopyWithImpl<_$OtpResponseImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$OtpResponseImplToJson(
      this,
    );
  }
}

abstract class _OtpResponse implements OtpResponse {
  const factory _OtpResponse(
      {required final String message,
      required final String email,
      required final bool otpSent,
      required final int expirationMinutes}) = _$OtpResponseImpl;

  factory _OtpResponse.fromJson(Map<String, dynamic> json) =
      _$OtpResponseImpl.fromJson;

  @override
  String get message;
  @override
  String get email;
  @override
  bool get otpSent;
  @override
  int get expirationMinutes;
  @override
  @JsonKey(ignore: true)
  _$$OtpResponseImplCopyWith<_$OtpResponseImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
