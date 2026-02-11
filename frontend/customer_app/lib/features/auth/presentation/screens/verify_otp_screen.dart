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

/// Écran de vérification du code OTP
class VerifyOtpScreen extends ConsumerStatefulWidget {
  final String email;

  const VerifyOtpScreen({
    super.key,
    required this.email,
  });

  @override
  ConsumerState<VerifyOtpScreen> createState() => _VerifyOtpScreenState();
}

class _VerifyOtpScreenState extends ConsumerState<VerifyOtpScreen> {
  bool _isLoading = false;
  String _otp = '';

  Future<void> _handleVerify() async {
    if (_otp.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(context.tr('please_enter_6_digit_code')),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    final success = await ref.read(authNotifierProvider.notifier).verifyOtp(
          email: widget.email,
          otp: _otp,
        );

    setState(() => _isLoading = false);

    if (!mounted) return;

    if (success) {
      // Navigation vers écran reset password
      context.go(
        RouteNames.resetPassword,
        extra: {
          'email': widget.email,
          'otp': _otp,
        },
      );
    }
  }

  Future<void> _handleResend() async {
    setState(() => _isLoading = true);

    final success = await ref.read(authNotifierProvider.notifier).forgotPassword(
          email: widget.email,
        );

    setState(() => _isLoading = false);

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(context.tr('code_resent_success')),
          backgroundColor: AppColors.success,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
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
                  Icons.mail_outlined,
                  size: ResponsiveUtils.getResponsiveSize(context, 40),
                  color: AppColors.primary,
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

              // Titre
              Text(
                context.tr('verify_code'),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 28),
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

              // Description
              RichText(
                text: TextSpan(
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                  children: [
                    TextSpan(
                      text: '${context.tr('verify_code_desc')}\n',
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

              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 40)),

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
                    elevation: 0,
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
                          context.tr('verify_code'),
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
                    onPressed: _isLoading ? null : _handleResend,
                    style: TextButton.styleFrom(
                      padding: EdgeInsets.symmetric(
                        horizontal: ResponsiveUtils.getResponsiveSpacing(context, 4),
                      ),
                      minimumSize: const Size(0, 0),
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: Text(
                      context.tr('resend'),
                      style: TextStyle(
                        fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
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
