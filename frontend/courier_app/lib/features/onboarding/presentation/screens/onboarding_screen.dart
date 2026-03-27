import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

import '../widgets/onboarding_page.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onPageChanged(int page) {
    setState(() {
      _currentPage = page;
    });
  }

  void _skipOnboarding() {
    context.go('/login');
  }

  void _nextPage() {
    context.go('/login');
  }

  void _goToLogin() {
    context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final pages = [
      OnboardingPageData(
        title: l10n.translate('onboarding_title'),
        subtitle: l10n.translate('onboarding_subtitle'),
        description: l10n.translate('onboarding_description'),
        image: 'assets/images/speedline_logo.png',
      ),
    ];

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            // Top bar with skip button
            Padding(
              padding: EdgeInsets.only(top: 8.h, right: 20.w, left: 20.w),
              child: Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: _skipOnboarding,
                  child: Text(
                    l10n.translate('skip'),
                    style: TextStyle(
                      fontSize: 16.sp,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
              ),
            ),
            
            // PageView
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: _onPageChanged,
                itemCount: pages.length,
                itemBuilder: (context, index) {
                  return OnboardingPage(data: pages[index]);
                },
              ),
            ),
            
            // Bottom section with buttons
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 32.w, vertical: 12.h),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Connexion Button
                  SizedBox(
                    width: double.infinity,
                    height: 56.h,
                    child: ElevatedButton(
                      onPressed: _nextPage,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(28.r),
                        ),
                        elevation: 2,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            l10n.translate('continue'),
                            style: TextStyle(
                              fontSize: 18.sp,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          SizedBox(width: 8.w),
                          Icon(Icons.arrow_forward, size: 20.sp),
                        ],
                      ),
                    ),
                  ),
                  
                  SizedBox(height: 16.h),
                  
                  // Log In Button
                  SizedBox(
                    width: double.infinity,
                    height: 56.h,
                    child: OutlinedButton(
                      onPressed: _goToLogin,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.black,
                        side: BorderSide(color: AppColors.divider, width: 1.5),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(28.r),
                        ),
                      ),
                      child: Text(
                        l10n.translate('log_in'),
                        style: TextStyle(
                          fontSize: 18.sp,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                  
                  SizedBox(height: 16.h),
                  
                  // Terms and Privacy
                  RichText(
                    textAlign: TextAlign.center,
                    text: TextSpan(
                      text: l10n.translate('terms_agreement_prefix'),
                      style: TextStyle(
                        fontSize: 12.sp,
                        color: AppColors.textSecondary,
                      ),
                      children: [
                        TextSpan(
                          text: l10n.translate('terms'),
                          style: TextStyle(
                            fontSize: 12.sp,
                            color: AppColors.textSecondary,
                            decoration: TextDecoration.underline,
                          ),
                        ),
                        const TextSpan(text: ' & '),
                        TextSpan(
                          text: l10n.translate('privacy_policy'),
                          style: TextStyle(
                            fontSize: 12.sp,
                            color: AppColors.textSecondary,
                            decoration: TextDecoration.underline,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class OnboardingPageData {
  final String title;
  final String subtitle;
  final String description;
  final String image;

  OnboardingPageData({
    required this.title,
    required this.subtitle,
    required this.description,
    required this.image,
  });
}
