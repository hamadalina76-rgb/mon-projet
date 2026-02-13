// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'otp_result.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$OtpResult {
  String get message => throw _privateConstructorUsedError;
  String get email => throw _privateConstructorUsedError;
  bool get otpSent => throw _privateConstructorUsedError;
  int get expirationMinutes => throw _privateConstructorUsedError;

  @JsonKey(ignore: true)
  $OtpResultCopyWith<OtpResult> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $OtpResultCopyWith<$Res> {
  factory $OtpResultCopyWith(OtpResult value, $Res Function(OtpResult) then) =
      _$OtpResultCopyWithImpl<$Res, OtpResult>;
  @useResult
  $Res call(
      {String message, String email, bool otpSent, int expirationMinutes});
}

/// @nodoc
class _$OtpResultCopyWithImpl<$Res, $Val extends OtpResult>
    implements $OtpResultCopyWith<$Res> {
  _$OtpResultCopyWithImpl(this._value, this._then);

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
abstract class _$$OtpResultImplCopyWith<$Res>
    implements $OtpResultCopyWith<$Res> {
  factory _$$OtpResultImplCopyWith(
          _$OtpResultImpl value, $Res Function(_$OtpResultImpl) then) =
      __$$OtpResultImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String message, String email, bool otpSent, int expirationMinutes});
}

/// @nodoc
class __$$OtpResultImplCopyWithImpl<$Res>
    extends _$OtpResultCopyWithImpl<$Res, _$OtpResultImpl>
    implements _$$OtpResultImplCopyWith<$Res> {
  __$$OtpResultImplCopyWithImpl(
      _$OtpResultImpl _value, $Res Function(_$OtpResultImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? message = null,
    Object? email = null,
    Object? otpSent = null,
    Object? expirationMinutes = null,
  }) {
    return _then(_$OtpResultImpl(
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

class _$OtpResultImpl implements _OtpResult {
  const _$OtpResultImpl(
      {required this.message,
      required this.email,
      required this.otpSent,
      required this.expirationMinutes});

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
    return 'OtpResult(message: $message, email: $email, otpSent: $otpSent, expirationMinutes: $expirationMinutes)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$OtpResultImpl &&
            (identical(other.message, message) || other.message == message) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.otpSent, otpSent) || other.otpSent == otpSent) &&
            (identical(other.expirationMinutes, expirationMinutes) ||
                other.expirationMinutes == expirationMinutes));
  }

  @override
  int get hashCode =>
      Object.hash(runtimeType, message, email, otpSent, expirationMinutes);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$OtpResultImplCopyWith<_$OtpResultImpl> get copyWith =>
      __$$OtpResultImplCopyWithImpl<_$OtpResultImpl>(this, _$identity);
}

abstract class _OtpResult implements OtpResult {
  const factory _OtpResult(
      {required final String message,
      required final String email,
      required final bool otpSent,
      required final int expirationMinutes}) = _$OtpResultImpl;

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
  _$$OtpResultImplCopyWith<_$OtpResultImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
