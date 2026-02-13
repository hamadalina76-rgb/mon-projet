import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/user.dart';

part 'user_model.freezed.dart';
part 'user_model.g.dart';

@freezed
class UserModel with _$UserModel {
  const UserModel._();

  const factory UserModel({
    required int id,
    required String email,
    required String firstName,
    required String lastName,
    String? phone,
    @JsonKey(name: 'phoneNumber') String? phoneNumber,
    required String role,
    String? profilePicture,
  }) = _UserModel;

  factory UserModel.fromJson(Map<String, dynamic> json) =>
      _$UserModelFromJson(json);

  // Convertir UserModel vers User entity
  User toEntity() {
    return User(
      id: id.toString(),
      email: email,
      firstName: firstName,
      lastName: lastName,
      phone: phoneNumber ?? phone ?? '',
      role: role,
    );
  }

  // Créer UserModel depuis User entity
  factory UserModel.fromEntity(User user) {
    return UserModel(
      id: int.parse(user.id),
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      phoneNumber: user.phone,
      role: user.role,
    );
  }
}
