import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/locale_provider.dart';
import '../../../../core/localization/localization_extension.dart';
import '../../../../config/routes/route_names.dart';

/// Écran d'accueil / Onboarding
/// Premier écran affiché au lancement de l'app
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;
  String _selectedLanguage = 'fr';

  final List<OnboardingPage> _pages = [
    OnboardingPage(
      title: 'onboarding_page1_title',
      highlightedTitle: 'onboarding_page1_highlight',
      description: 'onboarding_page1_desc',
    ),
    OnboardingPage(
      title: 'onboarding_page2_title',
      highlightedTitle: 'onboarding_page2_highlight',
      description: 'onboarding_page2_desc',
    ),
    OnboardingPage(
      title: 'onboarding_page3_title',
      highlightedTitle: 'onboarding_page3_highlight',
      description: 'onboarding_page3_desc',
    ),
  ];

  @override
  void initState() {
    super.initState();
    // Load saved language
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final currentLocale = ref.read(localeProvider);
      setState(() => _selectedLanguage = currentLocale.languageCode);
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }



  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            // Language selector and Skip button
            Padding(
              padding: EdgeInsets.symmetric(
                horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
                vertical: ResponsiveUtils.getResponsiveSpacing(context, 16),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Language selector
                  _buildLanguageSelector(),
                  // Skip button
                  TextButton(
                    onPressed: () => context.go(RouteNames.login),
                    child: Text(
                      context.tr('skip'),
                      style: TextStyle(
                        fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // Content
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (index) {
                  setState(() => _currentPage = index);
                },
                itemCount: _pages.length,
                itemBuilder: (context, index) {
                  return _buildPageContent(_pages[index]);
                },
              ),
            ),

            // Page indicators
            _buildPageIndicators(),

            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 40)),

            // Get Started button
            Padding(
              padding: EdgeInsets.symmetric(
                horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
              ),
              child: SizedBox(
                width: double.infinity,
                height: ResponsiveUtils.getResponsiveButtonHeight(context),
                child: ElevatedButton(
                  onPressed: () {
                    if (_currentPage < _pages.length - 1) {
                      _pageController.nextPage(
                        duration: AppConstants.animationDuration,
                        curve: Curves.easeInOut,
                      );
                    } else {
                      context.go(RouteNames.login);
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    elevation: 0,
                    shadowColor: AppColors.primary.withOpacity(0.3),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppConstants.borderRadiusMedium),
                    ),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        _currentPage < _pages.length - 1
                            ? context.tr('next')
                            : context.tr('get_started'),
                        style: TextStyle(
                          fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 8)),
                      Icon(Icons.arrow_forward, size: ResponsiveUtils.getResponsiveFontSize(context, 20)),
                    ],
                  ),
                ),
              ),
            ),

            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

            // Log In button
            Padding(
              padding: EdgeInsets.symmetric(
                horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
              ),
              child: SizedBox(
                width: double.infinity,
                height: ResponsiveUtils.getResponsiveButtonHeight(context),
                child: OutlinedButton(
                  onPressed: () => context.go(RouteNames.login),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.textPrimary,
                    side: BorderSide(color: AppColors.border, width: 1.5),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppConstants.borderRadiusMedium),
                    ),
                  ),
                  child: Text(
                    context.tr('log_in'),
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),

            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

            // Terms & Privacy
            Padding(
              padding: EdgeInsets.symmetric(
                horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
              ),
              child: Text.rich(
                TextSpan(
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 12),
                    color: AppColors.textSecondary,
                  ),
                  children: [
                    TextSpan(
                      text: context.tr('terms_prefix'),
                    ),
                    TextSpan(
                      text: context.tr('terms'),
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                        decoration: TextDecoration.underline,
                      ),
                    ),
                    TextSpan(text: ' & '),
                    TextSpan(
                      text: context.tr('privacy_policy'),
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                        decoration: TextDecoration.underline,
                      ),
                    ),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ),

            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageSelector() {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: ResponsiveUtils.getResponsiveSpacing(context, 12),
        vertical: ResponsiveUtils.getResponsiveSpacing(context, 6),
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
        border: Border.all(color: AppColors.border, width: 1),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: _selectedLanguage,
          isDense: true,
          icon: Icon(
            Icons.language,
            size: ResponsiveUtils.getResponsiveFontSize(context, 18),
            color: AppColors.textSecondary,
          ),
          items: [
            DropdownMenuItem(
              value: 'fr',
              child: Row(
                children: [
                  Text(
                    '🇫🇷',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 18),
                    ),
                  ),
                  SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 8)),
                  Text(
                    'Français',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      fontWeight: FontWeight.w500,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
            DropdownMenuItem(
              value: 'en',
              child: Row(
                children: [
                  Text(
                    '🇬🇧',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 18),
                    ),
                  ),
                  SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 8)),
                  Text(
                    'English',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      fontWeight: FontWeight.w500,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
            DropdownMenuItem(
              value: 'ar',
              child: Row(
                children: [
                  Text(
                    '🇸🇦',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 18),
                    ),
                  ),
                  SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 8)),
                  Text(
                    'العربية',
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      fontWeight: FontWeight.w500,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ],
          onChanged: (String? newValue) {
            if (newValue != null) {
              setState(() => _selectedLanguage = newValue);
              // Update app locale
              final locale = Locale(newValue, newValue == 'en' ? 'US' : newValue == 'fr' ? 'FR' : 'SA');
              ref.read(localeProvider.notifier).setLocale(locale);
            }
          },
        ),
      ),
    );
  }

  Widget _buildPageContent(OnboardingPage page) {
    return Padding(
      padding: EdgeInsets.symmetric(
        horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Logo SpeedLine
          Container(
            width: ResponsiveUtils.getResponsiveWidth(context, 0.32).clamp(100.0, 150.0),
            height: ResponsiveUtils.getResponsiveWidth(context, 0.32).clamp(100.0, 150.0),
            decoration: BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: AppColors.primary.withOpacity(0.2),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Padding(
              padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 20)),
              child: Image.asset(
                'assets/images/speedline_logo.png',
                fit: BoxFit.contain,
                errorBuilder: (context, error, stackTrace) {
                  return Icon(
                    Icons.delivery_dining,
                    size: ResponsiveUtils.getResponsiveSize(context, 60),
                    color: AppColors.primary,
                  );
                },
              ),
            ),
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 60)),

          // Title
          Column(
            children: [
              Text(
                context.tr(page.title),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 32),
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 4)),
              Text(
                context.tr(page.highlightedTitle),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 32),
                  fontWeight: FontWeight.bold,
                  fontStyle: FontStyle.italic,
                  color: AppColors.primary,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),

          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

          // Description
          Text(
            context.tr(page.description),
            style: TextStyle(
              fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
              color: AppColors.textSecondary,
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildPageIndicators() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(
        _pages.length,
        (index) => AnimatedContainer(
          duration: AppConstants.animationDuration,
          margin: EdgeInsets.symmetric(
            horizontal: ResponsiveUtils.getResponsiveSpacing(context, 4),
          ),
          width: _currentPage == index
              ? ResponsiveUtils.getResponsiveSize(context, 24)
              : ResponsiveUtils.getResponsiveSize(context, 8),
          height: ResponsiveUtils.getResponsiveSize(context, 8),
          decoration: BoxDecoration(
            color: _currentPage == index
                ? AppColors.primary
                : AppColors.border,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
      ),
    );
  }
}

/// Modèle pour les pages d'onboarding
class OnboardingPage {
  final String title;
  final String highlightedTitle;
  final String description;

  OnboardingPage({
    required this.title,
    required this.highlightedTitle,
    required this.description,
  });
}
