import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/localization/locale_provider.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _isDarkMode = false;

  void _handleLogout() {
    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.translate('logout')),
        content: Text(l10n.translate('are_you_sure_logout')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(l10n.translate('cancel')),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              ref.read(authNotifierProvider.notifier).logout();
              context.go(RouteNames.login);
            },
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: Text(l10n.translate('logout')),
          ),
        ],
      ),
    );
  }

  void _showLanguageDialog() {
    final l10n = AppLocalizations.of(context)!;
    final currentLocale = ref.read(localeProvider);
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.translate('select_language')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RadioListTile<String>(
              title: Text(l10n.translate('english')),
              value: 'en',
              groupValue: currentLocale.languageCode,
              activeColor: AppColors.primary,
              onChanged: (value) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('en', 'US'));
                if (mounted) {
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<String>(
              title: Text(l10n.translate('french')),
              value: 'fr',
              groupValue: currentLocale.languageCode,
              activeColor: AppColors.primary,
              onChanged: (value) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('fr', 'FR'));
                if (mounted) {
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<String>(
              title: Text(l10n.translate('arabic')),
              value: 'ar',
              groupValue: currentLocale.languageCode,
              activeColor: AppColors.primary,
              onChanged: (value) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('ar', 'SA'));
                if (mounted) {
                  Navigator.pop(context);
                }
              },
            ),
          ],
        ),
      ),
    );
  }

 String _getLanguageName(String code) {
    switch (code) {
      case 'en':
        return 'English';
      case 'fr':
        return 'Français';
      case 'ar':
        return 'العربية';
      default:
        return 'English';
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final currentLocale = ref.watch(localeProvider);
    
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(l10n.translate('settings')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: ListView(
        children: [
          const SizedBox(height: 8),
          
          // Account Section
          _buildSectionHeader(l10n.translate('account')),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.person_outline,
            titleKey: 'edit_profile',
            subtitleKey: 'manage_preferences',
            onTap: () => context.push('/edit-profile'),
          ),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.lock_outline,
            titleKey: 'change_password',
            subtitleKey: 'update_security_credentials',
            onTap: () => context.push(RouteNames.changePassword),
          ),
          
          const SizedBox(height: 24),
          
          // App Preferences Section
          _buildSectionHeader(l10n.translate('app_preferences')),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.language_outlined,
            titleKey: 'language',
            subtitle: _getLanguageName(currentLocale.languageCode),
            onTap: _showLanguageDialog,
            trailing: const Icon(Icons.chevron_right, color: Colors.grey),
          ),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.brightness_6_outlined,
            titleKey: 'dark_mode',
            subtitleKey: 'enable_dark_theme',
            trailing: Switch(
              value: _isDarkMode,
              onChanged: (value) {
                setState(() => _isDarkMode = value);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(l10n.translate(value ? 'dark_mode_enabled' : 'light_mode_enabled')),
                  ),
                );
              },
              activeColor: AppColors.primary,
            ),
          ),
          
          const SizedBox(height: 24),
          
          // Notifications Section
          _buildSectionHeader(l10n.translate('notifications')),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.notifications_outlined,
            titleKey: 'push_notifications',
            subtitleKey: 'receive_order_updates',
            trailing: Switch(
              value: true,
              onChanged: (value) {
                // TODO: Handle notification toggle
              },
              activeColor: AppColors.primary,
            ),
          ),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.email_outlined,
            titleKey: 'email_notifications',
            subtitleKey: 'receive_news_offers',
            trailing: Switch(
              value: false,
              onChanged: (value) {
                // TODO: Handle email notification toggle
              },
              activeColor: AppColors.primary,
            ),
          ),
          
          const SizedBox(height: 24),
          
          // Support Section
          _buildSectionHeader(l10n.translate('support')),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.help_outline,
            titleKey: 'help_center',
            subtitleKey: 'faq_support_articles',
            onTap: () {
              // TODO: Navigate to help
            },
          ),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.contact_support_outlined,
            titleKey: 'contact_us',
            subtitleKey: 'get_in_touch',
            onTap: () {
              // TODO: Navigate to contact
            },
          ),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.description_outlined,
            titleKey: 'terms_conditions',
            subtitleKey: 'legal_information',
            onTap: () {
              // TODO: Navigate to terms
            },
          ),
          
          const SizedBox(height: 24),
          
          // About Section
          _buildSectionHeader(l10n.translate('about')),
          _buildSettingsTile(
            l10n: l10n,
            icon: Icons.info_outline,
            titleKey: 'app_version',
            subtitle: '1.0.0',
            onTap: null,
          ),
          
          const SizedBox(height: 32),
          
          // Logout Button
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppConstants.verticalPadding),
            child: SizedBox(
              width: double.infinity,
              height: AppConstants.buttonHeightSmall,
              child: OutlinedButton.icon(
                onPressed: _handleLogout,
                icon: const Icon(Icons.logout),
                label: Text(
                  l10n.translate('logout'),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.error,
                  side: const BorderSide(color: AppColors.error, width: 2),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                  ),
                ),
              ),
            ),
          ),
          
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(AppConstants.verticalPadding, AppConstants.verticalPadding, AppConstants.verticalPadding, 8),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.bold,
          color: Colors.grey[600],
          letterSpacing: 1.2,
        ),
      ),
    );
  }

  Widget _buildSettingsTile({
    required AppLocalizations l10n,
    required IconData icon,
    String? titleKey,
    String? subtitleKey,
    String? subtitle,
    VoidCallback? onTap,
    Widget? trailing,
  }) {
    return Container(
      color: Colors.white,
      child: ListTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(
            icon,
            color: AppColors.primary,
            size: AppConstants.iconSizeMedium,
          ),
        ),
        title: Text(
          titleKey != null ? l10n.translate(titleKey) : '',
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        subtitle: (subtitleKey != null || subtitle != null)
            ? Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  subtitleKey != null ? l10n.translate(subtitleKey) : subtitle!,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                  ),
                ),
              )
            : null,
        trailing: trailing ?? (onTap != null ? const Icon(Icons.chevron_right, color: Colors.grey) : null),
        onTap: onTap,
      ),
    );
  }
}
