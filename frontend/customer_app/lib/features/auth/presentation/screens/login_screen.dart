import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../providers/auth_state.dart';
import 'verify_otp_screen.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/localization_extension.dart';
import '../widgets/auth_text_field.dart';
import '../widgets/social_login_button.dart';

/// Écran de connexion moderne SpeedLine
/// 
/// Design basé sur l'image de référence avec onglets Log In / Sign Up
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _formKey = GlobalKey<FormState>();

  // Controllers pour login
  final _loginEmailController = TextEditingController();
  final _loginPasswordController = TextEditingController();

  // Controllers pour register
  final _registerFirstNameController = TextEditingController();
  final _registerLastNameController = TextEditingController();
  final _registerEmailController = TextEditingController();
  final _registerPhoneController = TextEditingController();
  final _registerPasswordController = TextEditingController();
  
  // Password visibility states
  bool _obscureLoginPassword = true;
  bool _obscureRegisterPassword = true;
  
  // Remember me state
  bool _rememberMe = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _loginEmailController.dispose();
    _loginPasswordController.dispose();
    _registerFirstNameController.dispose();
    _registerLastNameController.dispose();
    _registerEmailController.dispose();
    _registerPhoneController.dispose();
    _registerPasswordController.dispose();
    super.dispose();
  }

  void _handleLogin() {
    if (!_formKey.currentState!.validate()) return;

    ref.read(authNotifierProvider.notifier).login(
          email: _loginEmailController.text.trim(),
          password: _loginPasswordController.text,
        );
  }

  void _handleRegister() {
    if (!_formKey.currentState!.validate()) return;

    ref.read(authNotifierProvider.notifier).register(
          firstName: _registerFirstNameController.text.trim(),
          lastName: _registerLastNameController.text.trim(),
          email: _registerEmailController.text.trim(),
          phoneNumber: _registerPhoneController.text.trim(),
          password: _registerPasswordController.text,
        );
  }

  void _handleForgotPassword() {
    context.go(RouteNames.forgotPassword);
  }

  void _handleGoogleLogin() {
    ref.read(authNotifierProvider.notifier).signInWithGoogle();
  }

  void _handleFacebookLogin() {
    ref.read(authNotifierProvider.notifier).signInWithFacebook();
  }

  @override
  Widget build(BuildContext context) {
    // Écoute des changements d'état
    ref.listen<AuthState>(authNotifierProvider, (previous, next) {
      next.maybeWhen(
        error: (message) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  Icon(Icons.error_outline, color: Colors.white),
                  SizedBox(width: 12),
                  Expanded(child: Text(message)),
                ],
              ),
              backgroundColor: AppColors.error,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              margin: EdgeInsets.all(16),
            ),
          );
        },
        otpSent: (otpResult) {
          // Navigation vers écran de vérification OTP pour connexion
          context.push(
            RouteNames.verifyOtp,
            extra: {
              'email': otpResult.email,
              'type': OtpVerificationType.login,
            },
          );
        },
        registered: () {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  Icon(Icons.check_circle_outline, color: Colors.white),
                  SizedBox(width: 12),
                  Expanded(child: Text(context.tr('register_success'))),
                ],
              ),
              backgroundColor: AppColors.success,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              margin: EdgeInsets.all(16),
              duration: const Duration(seconds: 3),
            ),
          );
          // Basculer vers l'onglet login
          _tabController.animateTo(0);
        },
        authenticated: (user) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  Icon(Icons.check_circle_outline, color: Colors.white),
                  SizedBox(width: 12),
                  Expanded(child: Text('${context.tr('welcome_user')} ${user.fullName}!')),
                ],
              ),
              backgroundColor: AppColors.success,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              margin: EdgeInsets.all(16),
            ),
          );
          // Navigate to location permission screen for first-time setup
          context.go(RouteNames.enableLocation);
        },
        orElse: () {},
      );
    });

    final authState = ref.watch(authNotifierProvider);
    final isLoading = authState.maybeWhen(
      loading: () => true,
      orElse: () => false,
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: EdgeInsets.symmetric(
              horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
            ),
            child: Column(
              children: [
                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

                // Logo et tagline
                _buildHeader(context),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 26)),

                // Card principale avec onglets
                _buildAuthCard(context, isLoading),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 20)),

                // Contact support
                _buildContactSupport(context),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final logoSize = ResponsiveUtils.getResponsiveWidth(context, 0.28).clamp(80.0, 140.0);
    
    return Column(
      children: [
        // Logo SpeedLine
        Container(
          width: logoSize,
          height: logoSize,
          child: Image.asset(
            'assets/images/speedline_logo.png',
            errorBuilder: (context, error, stackTrace) {
              // Fallback si logo n'existe pas
              return Center(
                child: Text(
                  '4days',
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 24),
                    fontWeight: FontWeight.bold,
                    color: AppColors.primary,
                  ),
                ),
              );
            },
          ),
        ),

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 10)),

        // Tagline
        Text(
          AppConstants.appTagline,
          style: TextStyle(
            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w500,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildAuthCard(BuildContext context, bool isLoading) {
    final cardHeight = _tabController.index == 0 
        ? ResponsiveUtils.getResponsiveHeight(context, 0.50).clamp(350.0, 420.0)
        : ResponsiveUtils.getResponsiveHeight(context, 0.65).clamp(450.0, 550.0);
    
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusXL),
        boxShadow: [
          BoxShadow(
            color: AppColors.shadow,
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          // Onglets Log In / Sign Up
          _buildTabs(context),

          // Contenu des onglets
          Padding(
            padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, AppConstants.cardPadding)),
            child: Form(
              key: _formKey,
              child: SizedBox(
                height: cardHeight,
                child: TabBarView(
                  controller: _tabController,
                  children: [
                    _buildLoginForm(context, isLoading),
                    _buildRegisterForm(context, isLoading),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabs(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceLight,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
      ),
      margin: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 12)),
      padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 4)),
      child: TabBar(
        controller: _tabController,
        dividerColor: Colors.transparent,
        dividerHeight: 0,
        indicator: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
          boxShadow: [
            BoxShadow(
              color: AppColors.shadow,
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        indicatorSize: TabBarIndicatorSize.tab,
        labelColor: AppColors.primary,
        unselectedLabelColor: AppColors.textSecondary,
        labelStyle: TextStyle(
          fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
          fontWeight: FontWeight.w600,
        ),
        unselectedLabelStyle: TextStyle(
          fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
          fontWeight: FontWeight.w500,
        ),
        labelPadding: EdgeInsets.symmetric(
          vertical: ResponsiveUtils.getResponsiveSpacing(context, 6),
        ),
        tabs: [
          Tab(text: context.tr('log_in')),
          Tab(text: context.tr('sign_up')),
        ],
        onTap: (index) {
          setState(() {}); // Refresh pour ajuster la hauteur
        },
      ),
    );
  }

  Widget _buildLoginForm(BuildContext context, bool isLoading) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Email (removed top spacing)
        AuthTextField(
          label: context.tr('email_address'),
          hintText: context.tr('email_hint'),
          controller: _loginEmailController,
          keyboardType: TextInputType.emailAddress,
          prefixIcon: Icons.email_outlined,
          enabled: !isLoading,
          textInputAction: TextInputAction.next,
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

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

        // Password avec Forgot Password
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  context.tr('password'),
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                TextButton(
                  onPressed: isLoading ? null : _handleForgotPassword,
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(0, 0),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                  child: Text(
                    context.tr('forgot_password'),
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      fontWeight: FontWeight.w600,
                      color: AppColors.primary,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _loginPasswordController,
              obscureText: _obscureLoginPassword,
              enabled: !isLoading,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _handleLogin(),
              style: const TextStyle(
                fontSize: 16,
                color: AppColors.textPrimary,
              ),
              decoration: InputDecoration(
                hintText: '••••••••',
                hintStyle: const TextStyle(
                  color: AppColors.textHint,
                  fontSize: 16,
                
                ),
                prefixIcon: const Icon(
                  Icons.lock_outline,
                  color: AppColors.textHint,
                  size: AppConstants.iconSizeSmall,
                ),
                suffixIcon: IconButton(
                  icon: Icon(
                    _obscureLoginPassword
                        ? Icons.visibility_off_rounded
                        : Icons.visibility_rounded,
                    color: AppColors.textHint,
                    size: AppConstants.iconSizeSmall,
                  ),
                  onPressed: () {
                    setState(() {
                      _obscureLoginPassword = !_obscureLoginPassword;
                    });
                  },
                ),
                filled: true,
                fillColor: AppColors.surface,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 16,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                  borderSide: const BorderSide(
                    color: AppColors.border,
                    width: 1.5,
                  ),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                  borderSide: const BorderSide(
                    color: AppColors.border,
                    width: 1.5,
                  ),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                  borderSide: const BorderSide(
                    color: AppColors.primary,
                    width: 2,
                  ),
                ),
              ),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return context.tr('please_enter_password');
                }
                if (value.length < 6) {
                  return context.tr('password_min_length');
                }
                return null;
              },
            ),
          ],
        ),

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),
        
        // Stay connected checkbox
        Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: _rememberMe,
                onChanged: isLoading ? null : (value) {
                  setState(() {
                    _rememberMe = value ?? false;
                  });
                },
                activeColor: AppColors.primary,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Text(
              context.tr('stay_connected'),
              style: TextStyle(
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 14)),

        // Bouton Sign In
        SizedBox(
          height: ResponsiveUtils.getResponsiveHeight(context, 0.065).clamp(48.0, 60.0),
          child: ElevatedButton(
            onPressed: isLoading ? null : _handleLogin,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
              ),
            ),
            child: isLoading
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                : Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        context.tr('sign_in'),
                        style: TextStyle(
                          fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Icon(Icons.arrow_forward, size: ResponsiveUtils.getResponsiveFontSize(context, 20)),
                    ],
                  ),
          ),
        ),

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 14)),

        // Séparateur
        Row(
          children: [
            const Expanded(child: Divider(color: AppColors.divider)),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(
                context.tr('or_continue_with'),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                  color: AppColors.textSecondary,
                ),
              ),
            ),
            const Expanded(child: Divider(color: AppColors.divider)),
          ],
        ),

        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

        // Boutons sociaux
        Row(
          children: [
            Expanded(
              child: SocialLoginButton(
                provider: SocialProvider.google,
                onPressed: _handleGoogleLogin,
                isLoading: false,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: SocialLoginButton(
                provider: SocialProvider.facebook,
                onPressed: _handleFacebookLogin,
                isLoading: false,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildRegisterForm(BuildContext context, bool isLoading) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),

          // First Name
          AuthTextField(
            label: context.tr('first_name'),
            hintText: context.tr('first_name_hint'),
            controller: _registerFirstNameController,
            prefixIcon: Icons.person_outline,
            enabled: !isLoading,
            textInputAction: TextInputAction.next,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return context.tr('please_enter_first_name');
              }
              return null;
            },
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

          // Last Name
          AuthTextField(
            label: context.tr('last_name'),
            hintText: context.tr('last_name_hint'),
            controller: _registerLastNameController,
            prefixIcon: Icons.person_outline,
            enabled: !isLoading,
            textInputAction: TextInputAction.next,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return context.tr('please_enter_last_name');
              }
              return null;
            },
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

          // Email
          AuthTextField(
            label: context.tr('email_address'),
            hintText: context.tr('email_hint'),
            controller: _registerEmailController,
            keyboardType: TextInputType.emailAddress,
            prefixIcon: Icons.email_outlined,
            enabled: !isLoading,
            textInputAction: TextInputAction.next,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return context.tr('please_enter_valid_email');
              }
              if (!value.contains('@')) {
                return context.tr('invalid_email');
              }
              return null;
            },
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

          // Phone
          AuthTextField(
            label: context.tr('phone_number'),
            hintText: context.tr('phone_hint'),
            controller: _registerPhoneController,
            keyboardType: TextInputType.phone,
            prefixIcon: Icons.phone_outlined,
            enabled: !isLoading,
            textInputAction: TextInputAction.next,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return context.tr('please_enter_phone');
              }
              if (value.length < 8) {
                return context.tr('phone_min_length');
              }
              return null;
            },
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

          // Password
          AuthTextField(
            label: context.tr('password'),
            hintText: '••••••••',
            controller: _registerPasswordController,
            obscureText: true,
            prefixIcon: Icons.lock_outline,
            enabled: !isLoading,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => _handleRegister(),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return context.tr('please_enter_password');
              }
              if (value.length < 8) {
                return context.tr('password_min_length');
              }
              return null;
            },
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

          // Bouton Sign Up
          SizedBox(
            height: ResponsiveUtils.getResponsiveHeight(context, 0.065).clamp(48.0, 60.0),
            child: ElevatedButton(
              onPressed: isLoading ? null : _handleRegister,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                ),
              ),
              child: isLoading
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          context.tr('sign_up'),
                          style: TextStyle(
                            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Icon(Icons.arrow_forward, size: ResponsiveUtils.getResponsiveFontSize(context, 20)),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContactSupport(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          context.tr('having_trouble'),
          style: TextStyle(
            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
            color: AppColors.textSecondary,
          ),
        ),
        TextButton(
          onPressed: () {
            // TODO: Ouvrir support
          },
          style: TextButton.styleFrom(
            padding: EdgeInsets.zero,
            minimumSize: const Size(0, 0),
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          child: Text(
            context.tr('contact_support'),
            style: TextStyle(
              fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
              fontWeight: FontWeight.w600,
              color: AppColors.primary,
            ),
          ),
        ),
      ],
    );
  }
}
