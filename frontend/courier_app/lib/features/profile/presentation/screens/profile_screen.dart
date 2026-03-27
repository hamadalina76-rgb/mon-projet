import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../config/di/injection_container.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/localization/locale_provider.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../auth/domain/entities/courier.dart';
import '../../../auth/domain/repositories/auth_repository.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  Future<Courier?> _loadCourier() async {
    if (!getIt.isRegistered<AuthRepository>()) return null;
    return getIt<AuthRepository>().getCurrentCourier();
  }

  static String _statusLabel(AppLocalizations l10n, String? status) {
    if (status == null || status.isEmpty) return l10n.translate('courier');
    switch (status.toUpperCase()) {
      case 'ACTIVE':
      case 'AVAILABLE':
      case 'BUSY':
      case 'OFFLINE':
        return l10n.translate('courier_active');
      case 'PENDING_APPROVAL':
        return l10n.translate('pending');
      case 'REJECTED':
        return l10n.translate('rejected');
      case 'SUSPENDED':
        return l10n.translate('suspended');
      case 'DEACTIVATED':
        return l10n.translate('deactivated');
      default:
        return status;
    }
  }

  Future<void> _onChangePhotoPressed(BuildContext context, AppLocalizations l10n) async {
    try {
      if (!getIt.isRegistered<AuthRepository>()) return;

      final picker = ImagePicker();
      final XFile? image = await picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1800,
        maxHeight: 1800,
        imageQuality: 85,
      );

      if (image == null) return;

      final authRepository = getIt<AuthRepository>();
      await authRepository.uploadProfilePhoto(filePath: image.path);

      if (!context.mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('profile_photo_updated')),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${l10n.translate('profile_photo_update_error')}: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  Future<void> _showLanguageDialog(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
  ) async {
    final currentLocale = ref.read(localeProvider);
    await showDialog<void>(
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
              onChanged: (_) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('en', 'US'));
                if (context.mounted) Navigator.pop(context);
              },
            ),
            RadioListTile<String>(
              title: Text(l10n.translate('french')),
              value: 'fr',
              groupValue: currentLocale.languageCode,
              onChanged: (_) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('fr', 'FR'));
                if (context.mounted) Navigator.pop(context);
              },
            ),
            RadioListTile<String>(
              title: Text(l10n.translate('arabic')),
              value: 'ar',
              groupValue: currentLocale.languageCode,
              onChanged: (_) async {
                await ref.read(localeProvider.notifier).setLocale(const Locale('ar', 'SA'));
                if (context.mounted) Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
    );
  }

  String _localeLabel(Locale locale) {
    switch (locale.languageCode) {
      case 'fr':
        return 'Francais';
      case 'ar':
        return 'العربية';
      default:
        return 'English';
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final currentLocale = ref.watch(localeProvider);

    return Scaffold(
      backgroundColor: Colors.grey[50],
      body: SafeArea(
        child: FutureBuilder<Courier?>(
          future: _loadCourier(),
          builder: (context, snapshot) {
            final courier = snapshot.data;
            final bool isReadOnly = courier?.canAccessApp == true;
            return SingleChildScrollView(
              child: Column(
                children: [
                  // Header
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(24.w),
                    decoration: BoxDecoration(
                      color: AppColors.primary,
                      borderRadius: BorderRadius.only(
                        bottomLeft: Radius.circular(32.r),
                        bottomRight: Radius.circular(32.r),
                      ),
                    ),
                    child: Column(
                      children: [
                        // Profile Picture + edit button
                        Stack(
                          alignment: Alignment.center,
                          children: [
                            Container(
                              width: 100.w,
                              height: 100.w,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: Colors.white,
                                border: Border.all(
                                  color: Colors.white,
                                  width: 4,
                                ),
                              ),
                              child: courier?.photoUrl != null && courier!.photoUrl!.isNotEmpty
                                  ? ClipOval(
                                      child: Image.network(
                                        courier.photoUrl!,
                                        width: 100.w,
                                        height: 100.w,
                                        fit: BoxFit.cover,
                                        errorBuilder: (_, __, ___) => Icon(
                                          Icons.person,
                                          size: 50.sp,
                                          color: AppColors.primary,
                                        ),
                                      ),
                                    )
                                  : Icon(
                                      Icons.person,
                                      size: 50.sp,
                                      color: AppColors.primary,
                                    ),
                            ),
                            if (!isReadOnly)
                              Positioned(
                                bottom: 0,
                                right: 6.w,
                                child: Material(
                                  color: Colors.transparent,
                                  child: InkWell(
                                    onTap: () => _onChangePhotoPressed(context, l10n),
                                    borderRadius: BorderRadius.circular(18.r),
                                    child: Container(
                                      padding: EdgeInsets.all(6.w),
                                      decoration: BoxDecoration(
                                        color: Colors.white,
                                        shape: BoxShape.circle,
                                        boxShadow: [
                                          BoxShadow(
                                            color: Colors.black.withValues(alpha: 0.15),
                                            blurRadius: 4,
                                            offset: const Offset(0, 2),
                                          ),
                                        ],
                                      ),
                                      child: Icon(
                                        Icons.camera_alt,
                                        size: 18.sp,
                                        color: AppColors.primary,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                        SizedBox(height: 16.h),
                        Text(
                          courier != null
                              ? '${courier.firstName} ${courier.lastName}'.trim()
                              : l10n.translate('courier'),
                          style: TextStyle(
                            fontSize: 24.sp,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        SizedBox(height: 8.h),
                        Container(
                          padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 6.h),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(20.r),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                courier?.canAccessApp == true ? Icons.verified : Icons.info_outline,
                                size: 16.sp,
                                color: Colors.white,
                              ),
                              SizedBox(width: 6.w),
                              Text(
                                courier != null
                                  ? _statusLabel(l10n, courier.status)
                                  : l10n.translate('loading'),
                                style: TextStyle(
                                  fontSize: 14.sp,
                                  color: Colors.white,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 24.h),
                  // Stats Cards
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 24.w),
                    child: Row(
                      children: [
                        Expanded(
                          child: _buildStatCard(
                            icon: Icons.delivery_dining,
                            title: l10n.translate('deliveries'),
                            value: '${courier?.totalDeliveries ?? 0}',
                            color: Colors.blue,
                          ),
                        ),
                        SizedBox(width: 12.w),
                        Expanded(
                          child: _buildStatCard(
                            icon: Icons.star,
                            title: l10n.translate('rating'),
                            value: courier?.rating != null
                                ? courier!.rating!.toStringAsFixed(1)
                                : '—',
                            color: Colors.amber,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 24.h),
                  // Données personnelles du livreur
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 24.w),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: EdgeInsets.only(bottom: 12.h),
                          child: Text(
                            l10n.translate('account_data'),
                            style: TextStyle(
                              fontSize: 18.sp,
                              fontWeight: FontWeight.bold,
                              color: Colors.black87,
                            ),
                          ),
                        ),
                        _buildInfoCard(
                          children: [
                            _buildInfoRow(Icons.person_outline, l10n.translate('first_name'), courier?.firstName ?? '—'),
                            _buildInfoRow(Icons.badge_outlined, l10n.translate('last_name'), courier?.lastName ?? '—'),
                            _buildInfoRow(Icons.phone_outlined, l10n.translate('phone'), (courier?.phone ?? '').trim().isEmpty ? '—' : (courier?.phone ?? '—')),
                            _buildInfoRow(Icons.email_outlined, l10n.translate('email'), courier?.email ?? '—'),
                          ],
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 24.h),
                  // Menu Items
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 24.w),
                child: Column(
                  children: [
                    _buildMenuItem(
                      context: context,
                      icon: Icons.description_outlined,
                      title: l10n.translate('data_documents'),
                      subtitle: l10n.translate('consult_data_documents'),
                      onTap: () => context.push('/documentation?readOnly=true'),
                    ),
                    _buildMenuItem(
                      context: context,
                      icon: Icons.language_outlined,
                      title: l10n.translate('language'),
                      subtitle: '${l10n.translate('choose_app_language')} (${_localeLabel(currentLocale)})',
                      onTap: () => _showLanguageDialog(context, ref, l10n),
                    ),
                    _buildMenuItem(
                      context: context,
                      icon: Icons.logout,
                      title: l10n.translate('logout'),
                      subtitle: l10n.translate('logout_subtitle'),
                      onTap: () => _handleLogout(context, l10n),
                      isDestructive: true,
                    ),
                  ],
                ),
              ),
              
              SizedBox(height: 24.h),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildInfoCard({required List<Widget> children}) {
    return Container(
      padding: EdgeInsets.all(16.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: children
            .asMap()
            .entries
            .map((e) => Column(
                  children: [
                    e.value,
                    if (e.key < children.length - 1)
                      Divider(height: 24.h, color: Colors.grey[200]),
                  ],
                ))
            .toList(),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: 4.h),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 22.sp, color: AppColors.primary),
          SizedBox(width: 12.w),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 12.sp,
                    color: Colors.grey[600],
                  ),
                ),
                SizedBox(height: 2.h),
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 15.sp,
                    fontWeight: FontWeight.w500,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: EdgeInsets.all(16.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(
            icon,
            size: 32.sp,
            color: color,
          ),
          SizedBox(height: 8.h),
          Text(
            value,
            style: TextStyle(
              fontSize: 24.sp,
              fontWeight: FontWeight.bold,
              color: Colors.black,
            ),
          ),
          SizedBox(height: 4.h),
          Text(
            title,
            style: TextStyle(
              fontSize: 12.sp,
              color: Colors.grey[600],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuItem({
    required BuildContext context,
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
    bool showBadge = false,
    bool isDestructive = false,
  }) {
    return Container(
      margin: EdgeInsets.only(bottom: 12.h),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: ListTile(
        onTap: onTap,
        contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        leading: Container(
          width: 48.w,
          height: 48.h,
          decoration: BoxDecoration(
            color: isDestructive
                ? Colors.red.withValues(alpha: 0.1)
                : AppColors.primary.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(12.r),
          ),
          child: Icon(
            icon,
            color: isDestructive ? Colors.red : AppColors.primary,
            size: 24.sp,
          ),
        ),
        title: Row(
          children: [
            Text(
              title,
              style: TextStyle(
                fontSize: 16.sp,
                fontWeight: FontWeight.w600,
                color: isDestructive ? Colors.red : Colors.black,
              ),
            ),
            if (showBadge) ...[
              SizedBox(width: 8.w),
              Container(
                padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 2.h),
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(12.r),
                ),
                child: Text(
                  'NEW',
                  style: TextStyle(
                    fontSize: 10.sp,
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ],
        ),
        subtitle: Padding(
          padding: EdgeInsets.only(top: 4.h),
          child: Text(
            subtitle,
            style: TextStyle(
              fontSize: 14.sp,
              color: Colors.grey[600],
            ),
          ),
        ),
        trailing: Icon(
          Icons.arrow_forward_ios,
          size: 16.sp,
          color: Colors.grey[400],
        ),
      ),
    );
  }

  void _handleLogout(BuildContext context, AppLocalizations l10n) {
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
              context.go('/login');
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
}
