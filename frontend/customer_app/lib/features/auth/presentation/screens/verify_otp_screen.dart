import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/localization_extension.dart';
import '../widgets/otp_input.dart';
import 'dart:async';

/// Type de vérification OTP
enum OtpVerificationType {
  login,
  forgotPassword,
}

/// Écran de vérification du code OTP
class VerifyOtpScreen extends ConsumerStatefulWidget {
  final String email;
  final OtpVerificationType verificationType;

  const VerifyOtpScreen({
    super.key,
    required this.email,
    this.verificationType = OtpVerificationType.forgotPassword,
  });

  @override
  ConsumerState<VerifyOtpScreen> createState() => _VerifyOtpScreenState();
}

class _VerifyOtpScreenState extends ConsumerState<VerifyOtpScreen> {
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
    
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        ),
        title: Row(
          children: [
            Icon(Icons.timer_off, color: AppColors.error),
            SizedBox(width: 8),
            Text(context.tr('code_expired')),
          ],
        ),
        content: Text(context.tr('code_expired_message')),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.pop(context);
            },
            child: Text(context.tr('back')),
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
            child: Text(context.tr('resend_code')),
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
    if (_otp.length != 6) {
      _showSnackBar(
        message: context.tr('enter_6_digit_code'),
        isError: true,
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      bool success = false;

      if (widget.verificationType == OtpVerificationType.login) {
        // Flow de connexion
        success = await ref.read(authNotifierProvider.notifier).verifyLoginOtp(
          email: widget.email,
          otpCode: _otp,
        );

        if (!mounted) return;

        if (success) {
          _timer?.cancel();
          _showSnackBar(
            message: context.tr('login_success'),
            isError: false,
          );
          
          // Navigation vers enableLocation après un court délai pour afficher le snackbar
          await Future.delayed(const Duration(milliseconds: 800));
          if (mounted) {
            context.go(RouteNames.enableLocation);
          }
        } else {
          _showSnackBar(
            message: context.tr('invalid_or_expired_code'),
            isError: true,
          );
        }
      } else {
        // Flow de mot de passe oublié
        success = await ref.read(authNotifierProvider.notifier).verifyOtp(
          email: widget.email,
          otp: _otp,
        );

        if (!mounted) return;

        if (success) {
          _timer?.cancel();
          context.go(
            RouteNames.resetPassword,
            extra: {
              'email': widget.email,
              'otp': _otp,
            },
          );
        } else {
          _showSnackBar(
            message: context.tr('invalid_or_expired_code'),
            isError: true,
          );
        }
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _handleResend() async {
    if (_resendCountdown > 0) {
      _showSnackBar(
        message: '${context.tr('wait')} $_resendCountdown ${context.tr('seconds')}',
        isError: true,
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      bool success = false;

      if (widget.verificationType == OtpVerificationType.login) {
        // Renvoyer OTP pour connexion
        success = await ref.read(authNotifierProvider.notifier).resendOtp(
          email: widget.email,
        );
        
        if (mounted && success) {
          // Redémarrer les timers
          _startTimer();
          _startResendCountdown();
          
          _showSnackBar(
            message: context.tr('code_resent_success'),
            isError: false,
          );
        } else if (mounted && !success) {
          _showSnackBar(
            message: context.tr('generic_error'),
            isError: true,
          );
        }
      } else {
        // Renvoyer OTP pour mot de passe oublié
        success = await ref.read(authNotifierProvider.notifier).forgotPassword(
          email: widget.email,
        );

        if (!mounted) return;

        if (success) {
          _startTimer();
          _startResendCountdown();
          _showSnackBar(
            message: context.tr('code_resent_success'),
            isError: false,
          );
        }
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
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
            SizedBox(width: 12),
            Expanded(
              child: Text(
                message,
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
        backgroundColor: isError ? AppColors.error : AppColors.success,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        margin: EdgeInsets.all(16),
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        duration: Duration(seconds: isError ? 4 : 2),
        action: SnackBarAction(
          label: 'OK',
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
    final isLogin = widget.verificationType == OtpVerificationType.login;
    
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 20)),

              // Icône
              Container(
                width: ResponsiveUtils.getResponsiveSize(context, 80),
                height: ResponsiveUtils.getResponsiveSize(context, 80),
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  isLogin ? Icons.lock_outlined : Icons.mail_outlined,
                  size: ResponsiveUtils.getResponsiveSize(context, 40),
                  color: AppColors.primary,
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

              // Titre
              Text(
                isLogin ? context.tr('verify_identity') : context.tr('verify_code'),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 28),
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

              // Description
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                  children: [
                    TextSpan(
                      text: isLogin
                          ? '${context.tr('otp_sent_to')}\n'
                          : '${context.tr('verify_code_desc')}\n',
                    ),
                    TextSpan(
                      text: widget.email,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ],
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

              // Timer
              Container(
                padding: EdgeInsets.symmetric(
                  horizontal: ResponsiveUtils.getResponsiveSpacing(context, 16),
                  vertical: ResponsiveUtils.getResponsiveSpacing(context, 12),
                ),
                decoration: BoxDecoration(
                  color: _remainingSeconds < 120 
                      ? AppColors.error.withOpacity(0.1)
                      : AppColors.primary.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.timer,
                      size: ResponsiveUtils.getResponsiveSize(context, 20),
                      color: _remainingSeconds < 120 ? AppColors.error : AppColors.primary,
                    ),
                    SizedBox(width: 8),
                    Text(
                      '${context.tr('expires_in')}: $_timerDisplay',
                      style: TextStyle(
                        fontSize: ResponsiveUtils.getResponsiveFontSize(context, 15),
                        fontWeight: FontWeight.w600,
                        color: _remainingSeconds < 120 ? AppColors.error : AppColors.primary,
                      ),
                    ),
                  ],
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

              // OTP Input
              OtpInput(
                onCompleted: (otp) {
                  setState(() => _otp = otp);
                  _handleVerify();
                },
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

              // Bouton Verify
              SizedBox(
                height: ResponsiveUtils.getResponsiveButtonHeight(context),
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _handleVerify,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    elevation: 2,
                    shadowColor: AppColors.primary.withOpacity(0.3),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppConstants.borderRadiusMedium),
                    ),
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor:
                                AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : Text(
                          isLogin ? context.tr('verify_and_login') : context.tr('verify_code'),
                          style: TextStyle(
                            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

              // Resend code
              Wrap(
                alignment: WrapAlignment.center,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: ResponsiveUtils.getResponsiveSpacing(context, 4),
                children: [
                  Text(
                    context.tr('didnt_receive_code'),
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      color: AppColors.textSecondary,
                    ),
                  ),
                  TextButton(
                    onPressed: (_isLoading || _resendCountdown > 0) ? null : _handleResend,
                    style: TextButton.styleFrom(
                      padding: EdgeInsets.symmetric(
                        horizontal: ResponsiveUtils.getResponsiveSpacing(context, 4),
                      ),
                      minimumSize: const Size(0, 0),
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: Text(
                      _resendCountdown > 0 
                          ? '${context.tr('resend')} ($_resendCountdown${context.tr('sec')})'
                          : context.tr('resend'),
                      style: TextStyle(
                        fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
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
