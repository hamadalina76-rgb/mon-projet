import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../config/dependency_injection/injection.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  String _selectedLanguage = 'English';
  bool _isDarkMode = false;

  void _handleLogout() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Logout'),
        content: const Text('Are you sure you want to logout?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              ref.read(authNotifierProvider.notifier).logout();
              context.go('/login');
            },
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: const Text('Logout'),
          ),
        ],
      ),
    );
  }

  void _showLanguageDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Select Language'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RadioListTile<String>(
              title: const Text('English'),
              value: 'English',
              groupValue: _selectedLanguage,
              activeColor: AppColors.primary,
              onChanged: (value) {
                setState(() => _selectedLanguage = value!);
                Navigator.pop(context);
              },
            ),
            RadioListTile<String>(
              title: const Text('Français'),
              value: 'Français',
              groupValue: _selectedLanguage,
              activeColor: AppColors.primary,
              onChanged: (value) {
                setState(() => _selectedLanguage = value!);
                Navigator.pop(context);
              },
            ),
            RadioListTile<String>(
              title: const Text('العربية'),
              value: 'العربية',
              groupValue: _selectedLanguage,
              activeColor: AppColors.primary,
              onChanged: (value) {
                setState(() => _selectedLanguage = value!);
                Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: ListView(
        children: [
          const SizedBox(height: 8),
          
          // Account Section
          _buildSectionHeader('ACCOUNT'),
          _buildSettingsTile(
            icon: Icons.person_outline,
            title: 'Edit Profile',
            subtitle: 'Update your personal information',
            onTap: () => context.push('/edit-profile'),
          ),
          _buildSettingsTile(
            icon: Icons.lock_outline,
            title: 'Change Password',
            subtitle: 'Update your account password',
            onTap: () {
              // TODO: Navigate to change password
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Change password coming soon')),
              );
            },
          ),
          
          const SizedBox(height: 24),
          
          // App Preferences Section
          _buildSectionHeader('APP PREFERENCES'),
          _buildSettingsTile(
            icon: Icons.language_outlined,
            title: 'Language',
            subtitle: _selectedLanguage,
            onTap: _showLanguageDialog,
            trailing: const Icon(Icons.chevron_right, color: Colors.grey),
          ),
          _buildSettingsTile(
            icon: Icons.brightness_6_outlined,
            title: 'Dark Mode',
            subtitle: 'Switch between light and dark theme',
            trailing: Switch(
              value: _isDarkMode,
              onChanged: (value) {
                setState(() => _isDarkMode = value);
                // TODO: Implement theme switching
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(value ? 'Dark mode enabled' : 'Light mode enabled'),
                  ),
                );
              },
              activeColor: AppColors.primary,
            ),
          ),
          
          const SizedBox(height: 24),
          
          // Notifications Section
          _buildSectionHeader('NOTIFICATIONS'),
          _buildSettingsTile(
            icon: Icons.notifications_outlined,
            title: 'Push Notifications',
            subtitle: 'Receive order updates',
            trailing: Switch(
              value: true,
              onChanged: (value) {
                // TODO: Handle notification toggle
              },
              activeColor: AppColors.primary,
            ),
          ),
          _buildSettingsTile(
            icon: Icons.email_outlined,
            title: 'Email Notifications',
            subtitle: 'Receive promotional emails',
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
          _buildSectionHeader('SUPPORT'),
          _buildSettingsTile(
            icon: Icons.help_outline,
            title: 'Help & Support',
            subtitle: 'Get help with your orders',
            onTap: () {
              // TODO: Navigate to help
            },
          ),
          _buildSettingsTile(
            icon: Icons.privacy_tip_outlined,
            title: 'Privacy Policy',
            subtitle: 'Read our privacy policy',
            onTap: () {
              // TODO: Navigate to privacy policy
            },
          ),
          _buildSettingsTile(
            icon: Icons.description_outlined,
            title: 'Terms of Service',
            subtitle: 'Read our terms and conditions',
            onTap: () {
              // TODO: Navigate to terms
            },
          ),
          
          const SizedBox(height: 24),
          
          // About Section
          _buildSectionHeader('ABOUT'),
          _buildSettingsTile(
            icon: Icons.info_outline,
            title: 'App Version',
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
                label: const Text(
                  'Logout',
                  style: TextStyle(
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
    required IconData icon,
    required String title,
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
          title,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        subtitle: subtitle != null
            ? Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  subtitle,
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
