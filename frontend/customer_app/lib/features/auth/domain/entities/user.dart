import 'package:equatable/equatable.dart';

class User extends Equatable {
  final String id;
  final String email;
  final String firstName;
  final String lastName;
  final String phone;
  final String role;
  final String? profilePicture;

  const User({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.phone,
    required this.role,
    this.profilePicture,
  });

  String get fullName => '$firstName $lastName';

  @override
  List<Object?> get props => [id, email, firstName, lastName, phone, role, profilePicture];

  @override
  bool get stringify => true;
}
