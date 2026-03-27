import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../config/di/injection_container.dart';
import '../../../../services/notification_service.dart';
import '../../../auth/domain/repositories/auth_repository.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    );
    
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _animationController,
        curve: const Interval(0.0, 0.6, curve: Curves.easeIn),
      ),
    );
    
    _scaleAnimation = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(
        parent: _animationController,
        curve: const Interval(0.0, 0.6, curve: Curves.easeOutBack),
      ),
    );
    
    _animationController.forward();
    
    // After animation: if already logged in, fetch profile and redirect by status; else onboarding
    Future.delayed(const Duration(milliseconds: 2500), () async {
      if (!mounted) return;
      if (!getIt.isRegistered<AuthRepository>()) {
        context.go('/onboarding');
        return;
      }
      final authRepository = getIt<AuthRepository>();
      final loggedIn = await authRepository.isLoggedIn();
      if (!mounted) return;
      if (!loggedIn) {
        context.go('/onboarding');
        return;
      }
      try {
        final profile = await authRepository.fetchCourierProfile();
        if (!mounted) return;
        final userId = int.tryParse(profile.userId ?? '') ?? int.tryParse(profile.id);
        if (userId != null) NotificationService().registerWithBackend(userId);
        if (!profile.isEmailVerified) {
          context.go('/login');
        } else if (profile.isBlocked) {
          context.go('/rejected');
        } else if (!profile.documentsVerified) {
          context.go('/documentation');
        } else {
          context.go('/home');
        }
      } catch (_) {
        if (mounted) context.go('/onboarding');
      }
    });
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: AnimatedBuilder(
          animation: _animationController,
          builder: (context, child) {
            return Opacity(
              opacity: _fadeAnimation.value,
              child: Transform.scale(
                scale: _scaleAnimation.value,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Logo
                    Image.asset(
                      'assets/images/speedline_logo_anniversary.png',
                      height: 120.h,
                      filterQuality: FilterQuality.high,
                      isAntiAlias: true,
                      errorBuilder: (context, error, stackTrace) {
                        return Column(
                          children: [
                            Icon(
                              Icons.delivery_dining,
                              size: 80.sp,
                              color: AppColors.primary,
                            ),
                            SizedBox(height: 16.h),
                            Text(
                              l10n.translate('app_name'),
                              style: TextStyle(
                                fontSize: 32.sp,
                                fontWeight: FontWeight.bold,
                                color: AppColors.primary,
                              ),
                            ),
                            Text(
                              '4 ans',
                              style: TextStyle(
                                fontSize: 20.sp,
                                fontWeight: FontWeight.w600,
                                fontStyle: FontStyle.italic,
                                color: Colors.black87,
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                    
                    SizedBox(height: 24.h),
                    
                    // Tagline
                    Text(
                      l10n.translate('courier_app'),
                      style: TextStyle(
                        fontSize: 18.sp,
                        color: AppColors.textSecondary,
                        letterSpacing: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
