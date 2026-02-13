// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'auth_response.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

AuthResponse _$AuthResponseFromJson(Map<String, dynamic> json) {
  return _AuthResponse.fromJson(json);
}

/// @nodoc
mixin _$AuthResponse {
  @JsonKey(name: 'access_token')
  String get token => throw _privateConstructorUsedError;
  @JsonKey(name: 'refresh_token')
  String get refreshToken => throw _privateConstructorUsedError;
  UserModel get user => throw _privateConstructorUsedError;
  int? get expiresIn => throw _privateConstructorUsedError;
  @JsonKey(name: 'token_type')
  String? get tokenType => throw _privateConstructorUsedError;
  bool? get isNewUser => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $AuthResponseCopyWith<AuthResponse> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AuthResponseCopyWith<$Res> {
  factory $AuthResponseCopyWith(
          AuthResponse value, $Res Function(AuthResponse) then) =
      _$AuthResponseCopyWithImpl<$Res, AuthResponse>;
  @useResult
  $Res call(
      {@JsonKey(name: 'access_token') String token,
      @JsonKey(name: 'refresh_token') String refreshToken,
      UserModel user,
      int? expiresIn,
      @JsonKey(name: 'token_type') String? tokenType,
      bool? isNewUser});

  $UserModelCopyWith<$Res> get user;
}

/// @nodoc
class _$AuthResponseCopyWithImpl<$Res, $Val extends AuthResponse>
    implements $AuthResponseCopyWith<$Res> {
  _$AuthResponseCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? token = null,
    Object? refreshToken = null,
    Object? user = null,
    Object? expiresIn = freezed,
    Object? tokenType = freezed,
    Object? isNewUser = freezed,
  }) {
    return _then(_value.copyWith(
      token: null == token
          ? _value.token
          : token // ignore: cast_nullable_to_non_nullable
              as String,
      refreshToken: null == refreshToken
          ? _value.refreshToken
          : refreshToken // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as UserModel,
      expiresIn: freezed == expiresIn
          ? _value.expiresIn
          : expiresIn // ignore: cast_nullable_to_non_nullable
              as int?,
      tokenType: freezed == tokenType
          ? _value.tokenType
          : tokenType // ignore: cast_nullable_to_non_nullable
              as String?,
      isNewUser: freezed == isNewUser
          ? _value.isNewUser
          : isNewUser // ignore: cast_nullable_to_non_nullable
              as bool?,
    ) as $Val);
  }

  @override
  @pragma('vm:prefer-inline')
  $UserModelCopyWith<$Res> get user {
    return $UserModelCopyWith<$Res>(_value.user, (value) {
      return _then(_value.copyWith(user: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$AuthResponseImplCopyWith<$Res>
    implements $AuthResponseCopyWith<$Res> {
  factory _$$AuthResponseImplCopyWith(
          _$AuthResponseImpl value, $Res Function(_$AuthResponseImpl) then) =
      __$$AuthResponseImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {@JsonKey(name: 'access_token') String token,
      @JsonKey(name: 'refresh_token') String refreshToken,
      UserModel user,
      int? expiresIn,
      @JsonKey(name: 'token_type') String? tokenType,
      bool? isNewUser});

  @override
  $UserModelCopyWith<$Res> get user;
}

/// @nodoc
class __$$AuthResponseImplCopyWithImpl<$Res>
    extends _$AuthResponseCopyWithImpl<$Res, _$AuthResponseImpl>
    implements _$$AuthResponseImplCopyWith<$Res> {
  __$$AuthResponseImplCopyWithImpl(
      _$AuthResponseImpl _value, $Res Function(_$AuthResponseImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? token = null,
    Object? refreshToken = null,
    Object? user = null,
    Object? expiresIn = freezed,
    Object? tokenType = freezed,
    Object? isNewUser = freezed,
  }) {
    return _then(_$AuthResponseImpl(
      token: null == token
          ? _value.token
          : token // ignore: cast_nullable_to_non_nullable
              as String,
      refreshToken: null == refreshToken
          ? _value.refreshToken
          : refreshToken // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as UserModel,
      expiresIn: freezed == expiresIn
          ? _value.expiresIn
          : expiresIn // ignore: cast_nullable_to_non_nullable
              as int?,
      tokenType: freezed == tokenType
          ? _value.tokenType
          : tokenType // ignore: cast_nullable_to_non_nullable
              as String?,
      isNewUser: freezed == isNewUser
          ? _value.isNewUser
          : isNewUser // ignore: cast_nullable_to_non_nullable
              as bool?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$AuthResponseImpl implements _AuthResponse {
  const _$AuthResponseImpl(
      {@JsonKey(name: 'access_token') required this.token,
      @JsonKey(name: 'refresh_token') required this.refreshToken,
      required this.user,
      this.expiresIn,
      @JsonKey(name: 'token_type') this.tokenType,
      this.isNewUser});

  factory _$AuthResponseImpl.fromJson(Map<String, dynamic> json) =>
      _$$AuthResponseImplFromJson(json);

  @override
  @JsonKey(name: 'access_token')
  final String token;
  @override
  @JsonKey(name: 'refresh_token')
  final String refreshToken;
  @override
  final UserModel user;
  @override
  final int? expiresIn;
  @override
  @JsonKey(name: 'token_type')
  final String? tokenType;
  @override
  final bool? isNewUser;

  @override
  String toString() {
    return 'AuthResponse(token: $token, refreshToken: $refreshToken, user: $user, expiresIn: $expiresIn, tokenType: $tokenType, isNewUser: $isNewUser)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AuthResponseImpl &&
            (identical(other.token, token) || other.token == token) &&
            (identical(other.refreshToken, refreshToken) ||
                other.refreshToken == refreshToken) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.expiresIn, expiresIn) ||
                other.expiresIn == expiresIn) &&
            (identical(other.tokenType, tokenType) ||
                other.tokenType == tokenType) &&
            (identical(other.isNewUser, isNewUser) ||
                other.isNewUser == isNewUser));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType, token, refreshToken, user, expiresIn, tokenType, isNewUser);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$AuthResponseImplCopyWith<_$AuthResponseImpl> get copyWith =>
      __$$AuthResponseImplCopyWithImpl<_$AuthResponseImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AuthResponseImplToJson(
      this,
    );
  }
}

abstract class _AuthResponse implements AuthResponse {
  const factory _AuthResponse(
      {@JsonKey(name: 'access_token') required final String token,
      @JsonKey(name: 'refresh_token') required final String refreshToken,
      required final UserModel user,
      final int? expiresIn,
      @JsonKey(name: 'token_type') final String? tokenType,
      final bool? isNewUser}) = _$AuthResponseImpl;

  factory _AuthResponse.fromJson(Map<String, dynamic> json) =
      _$AuthResponseImpl.fromJson;

  @override
  @JsonKey(name: 'access_token')
  String get token;
  @override
  @JsonKey(name: 'refresh_token')
  String get refreshToken;
  @override
  UserModel get user;
  @override
  int? get expiresIn;
  @override
  @JsonKey(name: 'token_type')
  String? get tokenType;
  @override
  bool? get isNewUser;
  @override
  @JsonKey(ignore: true)
  _$$AuthResponseImplCopyWith<_$AuthResponseImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
