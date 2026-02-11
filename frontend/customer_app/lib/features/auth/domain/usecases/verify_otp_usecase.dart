import 'package:dartz/dartz.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/auth_repository.dart';
import '../../data/models/verify_otp_request.dart';

/// Use Case pour la vérification d'un code OTP
/// 
/// Responsabilité unique: Vérifier que le code OTP est valide
class VerifyOtpUseCase {
  final AuthRepository repository;

  VerifyOtpUseCase(this.repository);

  /// Exécute la vérification
  /// 
  /// @param email Email de l'utilisateur
  /// @param otp Code OTP reçu
  /// @returns Right(Unit) si OTP valide
  /// @returns Left(Failure) si OTP invalide ou expiré
  Future<Either<Failure, Unit>> call({
    required String email,
    required String otp,
  }) async {
    final request = VerifyOtpRequest(email: email, otp: otp);
    return await repository.verifyOtp(request);
  }
}
