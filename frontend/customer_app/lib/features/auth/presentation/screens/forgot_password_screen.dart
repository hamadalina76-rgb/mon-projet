import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/localization_extension.dart';
import '../widgets/auth_text_field.dart';

/// Écran de récupération de mot de passe
/// Permet de demander un code OTP par email
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    final success = await ref.read(authNotifierProvider.notifier).forgotPassword(
          email: _emailController.text.trim(),
        );

    setState(() => _isLoading = false);

    if (!mounted) return;

    if (success) {
      // Navigation vers écran OTP
      context.go(
        RouteNames.verifyOtp,
        extra: _emailController.text.trim(),
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
          onPressed: () => context.go(RouteNames.login),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding)),
          child: Form(
            key: _formKey,
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
                    Icons.lock_reset,
                    size: ResponsiveUtils.getResponsiveSize(context, 40),
                    color: AppColors.primary,
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

                // Titre
                Text(
                  context.tr('forgot_password_title'),
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 28),
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

                // Description
                Text(
                  context.tr('forgot_password_desc'),
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 40)),

                // Email field
                AuthTextField(
                  label: context.tr('email_address'),
                  hintText: context.tr('email_hint'),
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  prefixIcon: Icons.email_outlined,
                  enabled: !_isLoading,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _handleSubmit(),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return context.tr('please_enter_email');
                    }
                    if (!value.contains('@')) {
                      return context.tr('invalid_email');
                    }
                    return null;
                  },
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

                // Bouton Submit
                SizedBox(
                  height: MediaQuery.of(context).size.height * 0.065,
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _handleSubmit,
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
                            context.tr('send_code'),
                            style: TextStyle(
                              fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

                // Retour à la connexion
                Wrap(
                  alignment: WrapAlignment.center,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: ResponsiveUtils.getResponsiveSpacing(context, 4),
                  children: [
                    Text(
                      context.tr('remember_password'),
                      style: TextStyle(
                        fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                        color: AppColors.textSecondary,
                      ),
                    ),
                    TextButton(
                      onPressed: () => context.go(RouteNames.login),
                      style: TextButton.styleFrom(
                        padding: EdgeInsets.symmetric(
                          horizontal: ResponsiveUtils.getResponsiveSpacing(context, 4),
                        ),
                        minimumSize: const Size(0, 0),
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      ),
                      child: Text(
                        context.tr('sign_in'),
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
      ),
    );
  }
}
