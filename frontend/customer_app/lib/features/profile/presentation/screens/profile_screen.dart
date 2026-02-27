import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import 'package:customer_app/features/auth/domain/entities/user.dart';
import 'package:flutter/material.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';

/// Profile Screen
/// Displays user information, delivery locations, payment methods, order history
class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  // Mock data for demonstration
  final List<Map<String, dynamic>> _mockOrders = [
    {
      'restaurant': 'Bella Pizza & Pasta',
      'date': 'Oct 24, 2023',
      'amount': '\$32.50',
      'status': 'DELIVERED',
      'icon': '🍕',
    },
    {
      'restaurant': 'Burger Empire',
      'date': 'Oct 21, 2023',
      'amount': '\$18.90',
      'status': 'DELIVERED',
      'icon': '🍔',
    },
  ];

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

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authNotifierProvider);
    
    return authState.maybeWhen(
      authenticated: (user) => _buildProfileContent(user),
      orElse: () => const Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
        ),
      ),
    );
  }

  Widget _buildProfileContent(User user) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(l10n.translate('profile')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () {
              context.push('/settings');
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // User Profile Header
            Container(
              color: Colors.white,
              padding: const EdgeInsets.all(AppConstants.horizontalPadding),
              child: Column(
                children: [
                  // Profile Picture
                  Stack(
                    children: [
                      CircleAvatar(
                        radius: 50,
                        backgroundColor: AppColors.primary.withOpacity(0.1),
                        backgroundImage: user.profilePicture != null && user.profilePicture!.isNotEmpty
                            ? NetworkImage(user.profilePicture!)
                            : null,
                        child: user.profilePicture == null || user.profilePicture!.isEmpty
                            ? Text(
                                user.firstName.substring(0, 1).toUpperCase(),
                                style: const TextStyle(
                                  fontSize: 36,
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.primary,
                                ),
                              )
                            : null,
                      ),
                      Positioned(
                        bottom: 0,
                        right: 0,
                        child: GestureDetector(
                          onTap: () {
                            context.push('/edit-profile');
                          },
                          child: Container(
                            padding: const EdgeInsets.all(AppConstants.borderRadiusSmall),
                            decoration: BoxDecoration(
                              color: AppColors.primary,
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.white, width: 3),
                            ),
                            child: const Icon(
                              Icons.edit,
                              size: AppConstants.verticalPadding,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  
                  // User Name
                  Text(
                    '${user.firstName} ${user.lastName}',
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 4),
                  
                  // Email
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.email_outlined, size: 14, color: Colors.grey[600]),
                      const SizedBox(width: 4),
                      Text(
                        user.email,
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  
                  // Phone Number
                  if (user.phone.isNotEmpty)
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.phone_outlined, size: 14, color: Colors.grey[600]),
                        const SizedBox(width: 4),
                        Text(
                          user.phone,
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  const SizedBox(height: 12),
                  
                  // Premium Member Badge
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppConstants.verticalPadding,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(AppConstants.cardPadding),
                    ),
                    child: const Text(
                      'PREMIUM MEMBER',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primary,
                        letterSpacing: 1,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Delivery Locations Section
            _buildSection(
              l10n: l10n,
              title: 'delivery_addresses',
              action: 'manage_all',
              onAction: () {
                // TODO: Navigate to manage locations
              },
              child: _buildAddressSection(l10n),
            ),
            
            const SizedBox(height: 16),
            
            // Payment Methods Section
            _buildSection(
              l10n: l10n,
              title: 'payment_methods',
              action: 'manage',
              onAction: () {
                // TODO: Navigate to manage payments
              },
              child: _buildPaymentCard(),
            ),
            
            const SizedBox(height: 16),
            
            // Order History Section
            _buildSection(
              l10n: l10n,
              title: 'order_history',
              action: 'view_all',
              onAction: () {
                // TODO: Navigate to full order history
              },
              child: Column(
                children: _mockOrders.map((order) => _buildOrderItem(order, l10n)).toList(),
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Logout Button
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppConstants.horizontalPadding),
              child: SizedBox(
                width: double.infinity,
                height: AppConstants.buttonHeightSmall,
                child: OutlinedButton(
                  onPressed: _handleLogout,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.error,
                    side: const BorderSide(color: AppColors.error),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(25),
                    ),
                  ),
                  child: Text(
                    l10n.translate('logout'),
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),
            
            const SizedBox(height: 100), // Space for bottom nav
          ],
        ),
      ),
    );
  }

  Widget _buildAddressSection(AppLocalizations l10n) {
    final loc = ref.watch(
      locationNotifierProvider.select((s) => s.location),
    );
    return Column(
      children: [
        _buildSavedAddressTile(l10n, loc),
        const Divider(height: 1),
        _buildAddNewItem(
          l10n: l10n,
          icon: Icons.add_location_outlined,
          title: 'add_new_address',
        ),
      ],
    );
  }

  Widget _buildSavedAddressTile(AppLocalizations l10n, SavedLocation? loc) {
    final IconData icon;
    final String title;
    if (loc == null) {
      icon = Icons.location_off_outlined;
      title = l10n.translate('no_address_saved');
    } else {
      switch (loc.addressType) {
        case AddressType.work:
          icon = Icons.work_outline;
          title = l10n.translate('work_address');
          break;
        case AddressType.other:
          icon = Icons.place_outlined;
          title = loc.customLabel?.isNotEmpty == true
              ? loc.customLabel!
              : l10n.translate('other_address');
          break;
        default:
          icon = Icons.home_outlined;
          title = l10n.translate('home_address');
      }
    }

    final subtitle = loc != null
        ? (loc.shortAddress.isNotEmpty ? loc.shortAddress : loc.formattedAddress)
        : l10n.translate('use_current_location');

    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: AppColors.primary.withOpacity(0.1),
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        ),
        child: Icon(icon, color: AppColors.primary, size: AppConstants.iconSizeMedium),
      ),
      title: Row(
        children: [
          Flexible(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          if (loc != null) ...[
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(4),
              ),
              child: const Text(
                'LIVE',
                style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white),
              ),
            ),
          ],
        ],
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          subtitle,
          style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
      onTap: () {
        // Navigate back to location screen to update
        if (loc != null) {
          context.push(
            RouteNames.confirmLocation,
            extra: {
              'latitude': loc.latitude,
              'longitude': loc.longitude,
              'initialAddress': loc.formattedAddress,
            },
          );
        }
      },
    );
  }

  Widget _buildSection({
    required AppLocalizations l10n,
    required String title,
    required String action,
    required VoidCallback onAction,
    required Widget child,
  }) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.all(AppConstants.horizontalPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                l10n.translate(title).toUpperCase(),
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: Colors.grey[600],
                  letterSpacing: 1.2,
                ),
              ),
              TextButton(
                onPressed: onAction,
                style: TextButton.styleFrom(
                  padding: EdgeInsets.zero,
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                child: Text(
                  l10n.translate(action),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.primary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }

  Widget _buildLocationItem({
    required AppLocalizations l10n,
    required IconData icon,
    required String title,
    required String subtitle,
    bool isLive = false,
  }) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: AppColors.primary.withOpacity(0.1),
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        ),
        child: Icon(
          icon,
          color: AppColors.primary,
          size: AppConstants.iconSizeMedium,
        ),
      ),
      title: Row(
        children: [
          Text(
            l10n.translate(title),
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
          if (isLive) ...[
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(AppConstants.borderRadiusSmall / 2),
              ),
              child: const Text(
                'LIVE',
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ],
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          subtitle,
          style: TextStyle(
            fontSize: 14,
            color: Colors.grey[600],
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      trailing: const Icon(
        Icons.chevron_right,
        color: Colors.grey,
      ),
    );
  }

  Widget _buildAddNewItem({
    required AppLocalizations l10n,
    required IconData icon,
    required String title,
  }) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey[300]!, width: 2, style: BorderStyle.solid),
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        ),
        child: Icon(
          icon,
          color: AppColors.primary,
          size: AppConstants.iconSizeMedium,
        ),
      ),
      title: Text(
        l10n.translate(title),
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: AppColors.primary,
        ),
      ),
    );
  }

  Widget _buildPaymentCard() {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF2C3E50), Color(0xFF34495E)],
        ),
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      padding: const EdgeInsets.all(AppConstants.cardPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                width: 48,
                height: 32,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(6),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: AppConstants.borderRadiusMedium, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                ),
                child: const Text(
                  'Primary',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const Spacer(),
          Row(
            children: [
              ...List.generate(
                4,
                (index) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              ...List.generate(
                4,
                (index) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              ...List.generate(
                4,
                (index) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              const Text(
                '4242',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 2,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'ALEX JOHNSON',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1.2,
                ),
              ),
              Text(
                '12/26',
                style: TextStyle(
                  color: Colors.white.withOpacity(0.8),
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildOrderItem(Map<String, dynamic> order, AppLocalizations l10n) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppConstants.verticalPadding),
      padding: const EdgeInsets.all(AppConstants.verticalPadding),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: Column(
        children: [
          Row(
            children: [
              // Restaurant Icon
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                ),
                child: Center(
                  child: Text(
                    order['icon'],
                    style: const TextStyle(fontSize: 32),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              
              // Order Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      order['restaurant'],
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Text(
                          order['date'],
                          style: TextStyle(
                            fontSize: 13,
                            color: Colors.grey[600],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '•',
                          style: TextStyle(
                            color: Colors.grey[400],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          order['amount'],
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: Colors.grey[700],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              
              // Status Badge
              Container(
                padding: const EdgeInsets.symmetric(horizontal: AppConstants.borderRadiusMedium, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.green[50],
                  borderRadius: BorderRadius.circular(AppConstants.cardPadding),
                ),
                child: Text(
                  l10n.translate('delivered'),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: Colors.green[700],
                  ),
                ),
              ),
            ],
          ),
          
          const SizedBox(height: 12),
          
          // Action Buttons
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 40,
                  child: ElevatedButton(
                    onPressed: () {
                      // TODO: Reorder
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(AppConstants.borderRadiusSmall),
                      ),
                      elevation: 0,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.refresh, size: 18),
                        const SizedBox(width: 6),
                        Text(
                          l10n.translate('reorder'),
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                height: 40,
                child: OutlinedButton(
                  onPressed: () {
                    // TODO: View details
                  },
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.grey[700],
                    side: BorderSide(color: Colors.grey[300]!),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(AppConstants.borderRadiusSmall),
                    ),
                  ),
                  child: Text(
                    l10n.translate('view_details'),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
