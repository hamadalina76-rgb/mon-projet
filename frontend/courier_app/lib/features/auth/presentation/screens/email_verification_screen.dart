import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'dart:async';

import '../../../../core/localization/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';
import '../widgets/otp_input.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../data/datasources/auth_remote_datasource.dart';
import '../../data/datasources/auth_local_datasource.dart';
import '../../../../config/di/injection_container.dart' show getIt;
import '../../../../services/notification_service.dart';
import '../../domain/entities/courier.dart';

class EmailVerificationScreen extends StatefulWidget {
  final String email;

  const EmailVerificationScreen({
    super.key,
    required this.email,
  });

  @override
  State<EmailVerificationScreen> createState() => _EmailVerificationScreenState();
}

class _EmailVerificationScreenState extends State<EmailVerificationScreen> {
  bool _isLoading = false;
  String _otp = '';
  int _remainingSeconds = 180; // 3 minutes = 180 seconds
  Timer? _timer;
  int _resendCountdown = 0; // Countdown for resend button
  Timer? _resendTimer;

  @override
  void initState() {
    super.initState();
    _startTimer();
    _startResendCountdown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _resendTimer?.cancel();
    super.dispose();
  }

  void _startTimer() {
    _timer?.cancel();
    _remainingSeconds = 180;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_remainingSeconds > 0) {
        setState(() => _remainingSeconds--);
      } else {
        timer.cancel();
        _showExpiredDialog();
      }
    });
  }

  void _startResendCountdown() {
    _resendTimer?.cancel();
    _resendCountdown = 30;
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_resendCountdown > 0) {
        setState(() => _resendCountdown--);
      } else {
        timer.cancel();
      }
    });
  }

  void _showExpiredDialog() {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context)!;
    
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12.r),
        ),
        title: Row(
          children: [
            Icon(Icons.timer_off, color: AppColors.error),
            SizedBox(width: 8.w),
            Text(l10n.translate('email_verification_code_expired')),
          ],
        ),
        content: Text(l10n.translate('email_verification_code_expired_desc')),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.go('/login');
            },
            child: Text(l10n.translate('back')),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _handleResend();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
            ),
            child: Text(l10n.translate('resend_code')),
          ),
        ],
      ),
    );
  }

  String get _timerDisplay {
    final minutes = _remainingSeconds ~/ 60;
    final seconds = _remainingSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  Future<void> _handleVerify() async {
    final l10n = AppLocalizations.of(context)!;

    if (_otp.length != 6) {
      _showSnackBar(
        message: l10n.translate('email_verification_enter_full_code'),
        isError: true,
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      // Get repository instance
      final authRepository = AuthRepositoryImpl(
        remoteDataSource: getIt<AuthRemoteDataSource>(),
        localDataSource: getIt<AuthLocalDataSource>(),
      );

      // Verify OTP and get initial courier
      final courier = await authRepository.verifyOtp(
        email: widget.email,
        otpCode: _otp,
      );
      print('📋 OTP verified. Initial courier.documentsVerified = ${courier.documentsVerified}');

      // Fetch fresh profile for status and documentsVerified
      var profileForNav = courier;
      try {
        final freshProfile = await authRepository.fetchCourierProfile();
        profileForNav = freshProfile;
        print('🔄 Fetched fresh profile. status=${freshProfile.status}, documentsVerified=${freshProfile.documentsVerified}');
      } catch (e) {
        print('⚠️ Could not fetch fresh profile: $e. Using OTP response.');
      }

      if (!mounted) return;

      _timer?.cancel();
      _showSnackBar(
        message: l10n.translate('email_verified_success'),
        isError: false,
      );
      
      // Register FCM token so courier receives push (e.g. account approved/blocked)
      _registerPushTokenIfPossible(profileForNav);
      // Navigate by documentation then home (no separate pending/rejected screens)
      await Future.delayed(const Duration(milliseconds: 800));
      if (mounted) {
        if (profileForNav.documentsVerified == false) {
          context.go('/documentation');
        } else {
          context.go('/home');
        }
      }
    } catch (e) {
      if (!mounted) return;
      print('❌ OTP verification error: $e');
      String errorMessage = l10n.translate('email_verification_invalid_code');
      
      // Try to extract error message from exception
      if (e.toString().contains('Exception:')) {
        errorMessage = e.toString().replaceAll('Exception:', '').trim();
      }
      
      _showSnackBar(
        message: errorMessage,
        isError: true,
      );
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _handleResend() async {
    final l10n = AppLocalizations.of(context)!;

    if (_resendCountdown > 0) {
      _showSnackBar(
        message: l10n.translateWithParams('email_verification_wait_resend', {
          'seconds': _resendCountdown.toString(),
        }),
        isError: true,
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      // Get repository instance
      final authRepository = AuthRepositoryImpl(
        remoteDataSource: getIt<AuthRemoteDataSource>(),
        localDataSource: getIt<AuthLocalDataSource>(),
      );

      // Resend OTP
      await authRepository.resendOtp(email: widget.email);

      if (!mounted) return;

      // Restart timers
      _startTimer();
      _startResendCountdown();
      
      _showSnackBar(
        message: l10n.translate('email_verification_code_sent'),
        isError: false,
      );
    } catch (e) {
      if (!mounted) return;
      _showSnackBar(
        message: l10n.translate('email_verification_resend_failed'),
        isError: true,
      );
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _registerPushTokenIfPossible(Courier courier) {
    final userId = int.tryParse(courier.userId ?? '') ?? int.tryParse(courier.id);
    if (userId != null) {
      NotificationService().registerWithBackend(userId);
    }
  }

  void _showSnackBar({required String message, required bool isError}) {
    if (!mounted) return;
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            Icon(
              isError ? Icons.error_outline : Icons.check_circle_outline,
              color: Colors.white,
            ),
            SizedBox(width: 12.w),
            Expanded(
              child: Text(
                message,
                style: TextStyle(
                  fontSize: 15.sp,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
        backgroundColor: isError ? AppColors.error : AppColors.success,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12.r),
        ),
        margin: EdgeInsets.all(16.w),
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 14.h),
        duration: Duration(seconds: isError ? 4 : 2),
        action: SnackBarAction(
          label: AppLocalizations.of(context)!.translate('ok'),
          textColor: Colors.white,
          onPressed: () {
            if (mounted) {
              ScaffoldMessenger.of(context).hideCurrentSnackBar();
            }
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Colors.black, size: 28.sp),
          onPressed: () => context.go('/login'),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.all(24.w),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(height: 32.h),
              
              // Email icon
              Container(
                width: 100.w,
                height: 100.h,
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.email_outlined,
                  size: 50.sp,
                  color: AppColors.primary,
                ),
              ),
              
              SizedBox(height: 32.h),
              
              // Title
              Text(
                l10n.translate('verify_your_email'),
                style: TextStyle(
                  fontSize: 28.sp,
                  fontWeight: FontWeight.bold,
                  color: AppColors.text,
                ),
                textAlign: TextAlign.center,
              ),
              
              SizedBox(height: 16.h),
              
              // Description
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: TextStyle(
                    fontSize: 16.sp,
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                  children: [
                    TextSpan(text: '${l10n.translate('email_verification_sent_to')}\n'),
                    TextSpan(
                      text: widget.email,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        color: AppColors.text,
                      ),
                    ),
                  ],
                ),
              ),
              
              SizedBox(height: 24.h),
              
              // Timer
              Container(
                padding: EdgeInsets.symmetric(
                  horizontal: 16.w,
                  vertical: 12.h,
                ),
                decoration: BoxDecoration(
                  color: _remainingSeconds < 120 
                      ? AppColors.error.withOpacity(0.1)
                      : AppColors.primary.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.timer,
                      size: 20.sp,
                      color: _remainingSeconds < 120 ? AppColors.error : AppColors.primary,
                    ),
                    SizedBox(width: 8.w),
                    Text(
                      '${l10n.translate('email_verification_expires_in')} $_timerDisplay',
                      style: TextStyle(
                        fontSize: 15.sp,
                        fontWeight: FontWeight.w600,
                        color: _remainingSeconds < 120 ? AppColors.error : AppColors.primary,
                      ),
                    ),
                  ],
                ),
              ),
              
              SizedBox(height: 32.h),
              
              // OTP Input
              OtpInput(
                onCompleted: (otp) {
                  setState(() => _otp = otp);
                  _handleVerify();
                },
              ),
              
              SizedBox(height: 32.h),
              
              // Verify button
              SizedBox(
                height: 56.h,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _handleVerify,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28.r),
                    ),
                    elevation: 2,
                    shadowColor: AppColors.primary.withOpacity(0.3),
                  ),
                  child: _isLoading
                      ? SizedBox(
                          width: 24.w,
                          height: 24.h,
                          child: const CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : Text(
                          l10n.translate('verify_email'),
                          style: TextStyle(
                            fontSize: 18.sp,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),
              
              SizedBox(height: 24.h),
              
              // Resend code
              Wrap(
                alignment: WrapAlignment.center,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 4.w,
                children: [
                  Text(
                    l10n.translate('didnt_receive_code'),
                    style: TextStyle(
                      fontSize: 14.sp,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  TextButton(
                    onPressed: (_isLoading || _resendCountdown > 0) ? null : _handleResend,
                    style: TextButton.styleFrom(
                      padding: EdgeInsets.symmetric(horizontal: 4.w),
                      minimumSize: const Size(0, 0),
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: Text(
                      _resendCountdown > 0 
                          ? l10n.translateWithParams('resend_countdown', {'seconds': _resendCountdown.toString()})
                          : l10n.translate('resend'),
                      style: TextStyle(
                        fontSize: 14.sp,
                        fontWeight: FontWeight.w600,
                        color: (_resendCountdown > 0) 
                            ? AppColors.textSecondary 
                            : AppColors.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
