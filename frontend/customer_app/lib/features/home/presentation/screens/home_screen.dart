import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/locale_provider.dart';
import '../../../../core/localization/localization_extension.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../config/dependency_injection/injection.dart';

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
    
    // Check if user is already logged in
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      // Check authentication status
      await ref.read(authNotifierProvider.notifier).checkAuthStatus();
      
      // Navigate to main screen if authenticated
      final authState = ref.read(authNotifierProvider);
      authState.maybeWhen(
        authenticated: (_) {
          if (mounted) {
            context.go(RouteNames.enableLocation);
          }
        },
        orElse: () {
          // User not authenticated, continue with onboarding
          // Show language selection banner on first launch
          _checkLanguageSelection();
        },
      );
    });
  }

  Future<void> _checkLanguageSelection() async {
    final hasSelected = await ref.read(localeProvider.notifier).hasSelectedLanguage();
    if (!hasSelected && mounted) {
      _showLanguageSelectionBanner();
    }
    // Load saved language
    final currentLocale = ref.read(localeProvider);
    setState(() => _selectedLanguage = currentLocale.languageCode);
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
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  // Language selector

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



  Widget _buildPageContent(OnboardingPage page) {
    final horizontalPadding =
        ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding);

    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompactHeight = constraints.maxHeight < 460;
        final logoSize = ResponsiveUtils.getResponsiveWidth(context, 0.32)
            .clamp(isCompactHeight ? 90.0 : 100.0, isCompactHeight ? 120.0 : 150.0)
            .toDouble();
        final titleFontSize =
            ResponsiveUtils.getResponsiveFontSize(context, isCompactHeight ? 28 : 32);

        return SingleChildScrollView(
          physics: const BouncingScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: horizontalPadding),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Keep content centered on tall screens, but prevent overflow on short ones.
                  SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, isCompactHeight ? 12 : 24)),

                  // Logo SpeedLine
                  Container(
                    width: logoSize,
                    height: logoSize,
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
                      padding: EdgeInsets.all(
                        ResponsiveUtils.getResponsiveSpacing(context, isCompactHeight ? 14 : 20),
                      ),
                      child: Image.asset(
                        'assets/images/speedline_logo.png',
                        fit: BoxFit.contain,
                        errorBuilder: (context, error, stackTrace) {
                          return Icon(
                            Icons.delivery_dining,
                            size: ResponsiveUtils.getResponsiveSize(context, isCompactHeight ? 48 : 60),
                            color: AppColors.primary,
                          );
                        },
                      ),
                    ),
                  ),

                  SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, isCompactHeight ? 24 : 60)),

                  // Title
                  Column(
                    children: [
                      Text(
                        context.tr(page.title),
                        style: TextStyle(
                          fontSize: titleFontSize,
                          fontWeight: FontWeight.bold,
                          color: AppColors.textPrimary,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 4)),
                      Text(
                        context.tr(page.highlightedTitle),
                        style: TextStyle(
                          fontSize: titleFontSize,
                          fontWeight: FontWeight.bold,
                          fontStyle: FontStyle.italic,
                          color: AppColors.primary,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),

                  SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, isCompactHeight ? 16 : 24)),

                  // Description
                  Text(
                    context.tr(page.description),
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, isCompactHeight ? 15 : 16),
                      color: AppColors.textSecondary,
                      height: 1.5,
                    ),
                    textAlign: TextAlign.center,
                  ),

                  SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, isCompactHeight ? 12 : 24)),
                ],
              ),
            ),
          ),
        );
      },
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

  /// Show modern language selection banner at bottom
  void _showLanguageSelectionBanner() {
    showModalBottomSheet(
      context: context,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) {
        return WillPopScope(
          onWillPop: () async => false,
          child: Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.only(
                topLeft: Radius.circular(AppConstants.borderRadiusLarge),
                topRight: Radius.circular(AppConstants.borderRadiusLarge),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.2),
                  blurRadius: 20,
                  offset: const Offset(0, -5),
                ),
              ],
            ),
            padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 24)),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Welcome icon
                Container(
                  width: ResponsiveUtils.getResponsiveSize(context, 60),
                  height: ResponsiveUtils.getResponsiveSize(context, 60),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [AppColors.primary, AppColors.primaryDark],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.waving_hand,
                    size: ResponsiveUtils.getResponsiveSize(context, 30),
                    color: Colors.white,
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),

                // Welcome title
                Text(
                  'Welcome!',
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 26),
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                  textAlign: TextAlign.center,
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),

                // Subtitle
                Text(
                  'Select your preferred language to continue',
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 15),
                    color: AppColors.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),

                // Language buttons
                Row(
                  children: [
                    Expanded(
                      child: _buildLanguageCard(
                        context: context,
                        flag: '🇫🇷',
                        language: 'Français',
                        languageCode: 'fr',
                      ),
                    ),
                    SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 12)),
                    Expanded(
                      child: _buildLanguageCard(
                        context: context,
                        flag: '🇬🇧',
                        language: 'English',
                        languageCode: 'en',
                      ),
                    ),
                    SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 12)),
                    Expanded(
                      child: _buildLanguageCard(
                        context: context,
                        flag: '🇸🇦',
                        language: 'العربية',
                        languageCode: 'ar',
                      ),
                    ),
                  ],
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),
              ],
            ),
          ),
        );
      },
    );
  }

  /// Build language selection card
  Widget _buildLanguageCard({
    required BuildContext context,
    required String flag,
    required String language,
    required String languageCode,
  }) {
    return InkWell(
      onTap: () => _selectLanguageFromBanner(languageCode),
      borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
      child: Container(
        padding: EdgeInsets.symmetric(
          vertical: ResponsiveUtils.getResponsiveSpacing(context, 16),
          horizontal: ResponsiveUtils.getResponsiveSpacing(context, 8),
        ),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
          border: Border.all(color: AppColors.border, width: 1.5),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Flag
            Text(
              flag,
              style: TextStyle(
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 36),
              ),
            ),

            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),

            // Language name
            Text(
              language,
              style: TextStyle(
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimary,
              ),
              textAlign: TextAlign.center,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  /// Select language from banner and close
  void _selectLanguageFromBanner(String languageCode) {
    final locale = Locale(
      languageCode,
      languageCode == 'en' ? 'US' : languageCode == 'fr' ? 'FR' : 'SA',
    );
    ref.read(localeProvider.notifier).setLocale(locale);
    setState(() => _selectedLanguage = languageCode);
    Navigator.of(context).pop();
  }

  /// Show modern language selection dialog
  Future<void> _showLanguageSelectionDialog() async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
          ),
          elevation: 10,
          child: Container(
            padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 24)),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Logo
                Container(
                  width: ResponsiveUtils.getResponsiveSize(context, 80),
                  height: ResponsiveUtils.getResponsiveSize(context, 80),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.language,
                    size: ResponsiveUtils.getResponsiveSize(context, 40),
                    color: AppColors.primary,
                  ),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 20)),

                // Title
                Text(
                  'Choose Your Language',
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 24),
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                  textAlign: TextAlign.center,
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),

                // Subtitle
                Text(
                  'Choisissez votre langue • اختر لغتك',
                  style: TextStyle(
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                    color: AppColors.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),

                // Language options
                _buildLanguageOption(
                  context: context,
                  flag: '🇫🇷',
                  language: 'Français',
                  subtitle: 'French',
                  onTap: () => _selectLanguage('fr'),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

                _buildLanguageOption(
                  context: context,
                  flag: '🇬🇧',
                  language: 'English',
                  subtitle: 'Anglais',
                  onTap: () => _selectLanguage('en'),
                ),

                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),

                _buildLanguageOption(
                  context: context,
                  flag: '🇸🇦',
                  language: 'العربية',
                  subtitle: 'Arabic',
                  onTap: () => _selectLanguage('ar'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// Build language option tile
  Widget _buildLanguageOption({
    required BuildContext context,
    required String flag,
    required String language,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
      child: Container(
        padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 16)),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
          border: Border.all(color: AppColors.border, width: 1),
        ),
        child: Row(
          children: [
            // Flag
            Text(
              flag,
              style: TextStyle(
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 32),
              ),
            ),

            SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 16)),

            // Language info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    language,
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 18),
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 2)),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),

            // Arrow icon
            Icon(
              Icons.arrow_forward_ios,
              size: ResponsiveUtils.getResponsiveSize(context, 16),
              color: AppColors.textSecondary,
            ),
          ],
        ),
      ),
    );
  }

  /// Select language and close dialog
  void _selectLanguage(String languageCode) {
    final locale = Locale(
      languageCode,
      languageCode == 'en' ? 'US' : languageCode == 'fr' ? 'FR' : 'SA',
    );
    ref.read(localeProvider.notifier).setLocale(locale);
    setState(() => _selectedLanguage = languageCode);
    Navigator.of(context).pop();
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
