// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'login_result.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

/// @nodoc
mixin _$LoginResult {
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(OtpResult otpResult) requiresOtp,
    required TResult Function(User user) authenticated,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(OtpResult otpResult)? requiresOtp,
    TResult? Function(User user)? authenticated,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(OtpResult otpResult)? requiresOtp,
    TResult Function(User user)? authenticated,
    required TResult orElse(),
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RequiresOtp value) requiresOtp,
    required TResult Function(Authenticated value) authenticated,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RequiresOtp value)? requiresOtp,
    TResult? Function(Authenticated value)? authenticated,
  }) =>
      throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RequiresOtp value)? requiresOtp,
    TResult Function(Authenticated value)? authenticated,
    required TResult orElse(),
  }) =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $LoginResultCopyWith<$Res> {
  factory $LoginResultCopyWith(
          LoginResult value, $Res Function(LoginResult) then) =
      _$LoginResultCopyWithImpl<$Res, LoginResult>;
}

/// @nodoc
class _$LoginResultCopyWithImpl<$Res, $Val extends LoginResult>
    implements $LoginResultCopyWith<$Res> {
  _$LoginResultCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;
}

/// @nodoc
abstract class _$$RequiresOtpImplCopyWith<$Res> {
  factory _$$RequiresOtpImplCopyWith(
          _$RequiresOtpImpl value, $Res Function(_$RequiresOtpImpl) then) =
      __$$RequiresOtpImplCopyWithImpl<$Res>;
  @useResult
  $Res call({OtpResult otpResult});

  $OtpResultCopyWith<$Res> get otpResult;
}

/// @nodoc
class __$$RequiresOtpImplCopyWithImpl<$Res>
    extends _$LoginResultCopyWithImpl<$Res, _$RequiresOtpImpl>
    implements _$$RequiresOtpImplCopyWith<$Res> {
  __$$RequiresOtpImplCopyWithImpl(
      _$RequiresOtpImpl _value, $Res Function(_$RequiresOtpImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? otpResult = null,
  }) {
    return _then(_$RequiresOtpImpl(
      otpResult: null == otpResult
          ? _value.otpResult
          : otpResult // ignore: cast_nullable_to_non_nullable
              as OtpResult,
    ));
  }

  @override
  @pragma('vm:prefer-inline')
  $OtpResultCopyWith<$Res> get otpResult {
    return $OtpResultCopyWith<$Res>(_value.otpResult, (value) {
      return _then(_value.copyWith(otpResult: value));
    });
  }
}

/// @nodoc

class _$RequiresOtpImpl implements RequiresOtp {
  const _$RequiresOtpImpl({required this.otpResult});

  @override
  final OtpResult otpResult;

  @override
  String toString() {
    return 'LoginResult.requiresOtp(otpResult: $otpResult)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RequiresOtpImpl &&
            (identical(other.otpResult, otpResult) ||
                other.otpResult == otpResult));
  }

  @override
  int get hashCode => Object.hash(runtimeType, otpResult);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$RequiresOtpImplCopyWith<_$RequiresOtpImpl> get copyWith =>
      __$$RequiresOtpImplCopyWithImpl<_$RequiresOtpImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(OtpResult otpResult) requiresOtp,
    required TResult Function(User user) authenticated,
  }) {
    return requiresOtp(otpResult);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(OtpResult otpResult)? requiresOtp,
    TResult? Function(User user)? authenticated,
  }) {
    return requiresOtp?.call(otpResult);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(OtpResult otpResult)? requiresOtp,
    TResult Function(User user)? authenticated,
    required TResult orElse(),
  }) {
    if (requiresOtp != null) {
      return requiresOtp(otpResult);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RequiresOtp value) requiresOtp,
    required TResult Function(Authenticated value) authenticated,
  }) {
    return requiresOtp(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RequiresOtp value)? requiresOtp,
    TResult? Function(Authenticated value)? authenticated,
  }) {
    return requiresOtp?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RequiresOtp value)? requiresOtp,
    TResult Function(Authenticated value)? authenticated,
    required TResult orElse(),
  }) {
    if (requiresOtp != null) {
      return requiresOtp(this);
    }
    return orElse();
  }
}

abstract class RequiresOtp implements LoginResult {
  const factory RequiresOtp({required final OtpResult otpResult}) =
      _$RequiresOtpImpl;

  OtpResult get otpResult;
  @JsonKey(ignore: true)
  _$$RequiresOtpImplCopyWith<_$RequiresOtpImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$AuthenticatedImplCopyWith<$Res> {
  factory _$$AuthenticatedImplCopyWith(
          _$AuthenticatedImpl value, $Res Function(_$AuthenticatedImpl) then) =
      __$$AuthenticatedImplCopyWithImpl<$Res>;
  @useResult
  $Res call({User user});
}

/// @nodoc
class __$$AuthenticatedImplCopyWithImpl<$Res>
    extends _$LoginResultCopyWithImpl<$Res, _$AuthenticatedImpl>
    implements _$$AuthenticatedImplCopyWith<$Res> {
  __$$AuthenticatedImplCopyWithImpl(
      _$AuthenticatedImpl _value, $Res Function(_$AuthenticatedImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? user = null,
  }) {
    return _then(_$AuthenticatedImpl(
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as User,
    ));
  }
}

/// @nodoc

class _$AuthenticatedImpl implements Authenticated {
  const _$AuthenticatedImpl({required this.user});

  @override
  final User user;

  @override
  String toString() {
    return 'LoginResult.authenticated(user: $user)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AuthenticatedImpl &&
            (identical(other.user, user) || other.user == user));
  }

  @override
  int get hashCode => Object.hash(runtimeType, user);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$AuthenticatedImplCopyWith<_$AuthenticatedImpl> get copyWith =>
      __$$AuthenticatedImplCopyWithImpl<_$AuthenticatedImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(OtpResult otpResult) requiresOtp,
    required TResult Function(User user) authenticated,
  }) {
    return authenticated(user);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(OtpResult otpResult)? requiresOtp,
    TResult? Function(User user)? authenticated,
  }) {
    return authenticated?.call(user);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(OtpResult otpResult)? requiresOtp,
    TResult Function(User user)? authenticated,
    required TResult orElse(),
  }) {
    if (authenticated != null) {
      return authenticated(user);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RequiresOtp value) requiresOtp,
    required TResult Function(Authenticated value) authenticated,
  }) {
    return authenticated(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RequiresOtp value)? requiresOtp,
    TResult? Function(Authenticated value)? authenticated,
  }) {
    return authenticated?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RequiresOtp value)? requiresOtp,
    TResult Function(Authenticated value)? authenticated,
    required TResult orElse(),
  }) {
    if (authenticated != null) {
      return authenticated(this);
    }
    return orElse();
  }
}

abstract class Authenticated implements LoginResult {
  const factory Authenticated({required final User user}) = _$AuthenticatedImpl;

  User get user;
  @JsonKey(ignore: true)
  _$$AuthenticatedImplCopyWith<_$AuthenticatedImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
