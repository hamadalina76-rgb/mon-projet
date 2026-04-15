import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import 'package:customer_app/features/auth/domain/entities/user.dart';
import 'package:flutter/material.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/widgets/speedline_app_bar.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../orders/domain/entities/order.dart';
import '../../../orders/presentation/providers/order_provider.dart';
import '../../../partners/data/models/partner_nearby_dto.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';
import '../../data/models/address_model.dart';
import '../providers/address_provider.dart';

final _orderPartnerPreviewProvider =
    FutureProvider.family<PartnerNearbyDto?, String>((ref, partnerId) async {
      try {
        return await ref
            .read(partnerApiServiceProvider)
            .fetchPartnerById(partnerId);
      } catch (_) {
        return null;
      }
    });

/// Profile Screen
/// Displays user information, delivery locations, payment methods, order history
class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  bool _showAllRecentOrders = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadAddresses());
  }

  void _loadAddresses() {
    final authState = ref.read(authNotifierProvider);
    authState.whenOrNull(
      authenticated: (user) =>
          ref.read(addressNotifierProvider.notifier).fetchAddresses(user.id),
    );
  }

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
            style: TextButton.styleFrom(foregroundColor: Colors.red),
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
      orElse: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
    );
  }

  Widget _buildProfileContent(User user) {
    final l10n = AppLocalizations.of(context)!;
    final recentOrdersAsync = ref.watch(customerOrdersProvider);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: SpeedlineAppBar(
        title: l10n.translate('profile'),
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
                        backgroundImage:
                            user.profilePicture != null &&
                                user.profilePicture!.isNotEmpty
                            ? NetworkImage(user.profilePicture!)
                            : null,
                        child:
                            user.profilePicture == null ||
                                user.profilePicture!.isEmpty
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
                            padding: const EdgeInsets.all(
                              AppConstants.borderRadiusSmall,
                            ),
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
                      Icon(
                        Icons.email_outlined,
                        size: 14,
                        color: Colors.grey[600],
                      ),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          user.email,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
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
                        Icon(
                          Icons.phone_outlined,
                          size: 14,
                          color: Colors.grey[600],
                        ),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            user.phone,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 14,
                              color: Colors.grey[600],
                            ),
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
                      borderRadius: BorderRadius.circular(
                        AppConstants.cardPadding,
                      ),
                    ),
                    child: Text(
                      l10n.translate('premium_member'),
                      style: const TextStyle(
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
              onAction: () => context.push(RouteNames.addresses),
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
                context.push(RouteNames.orders);
              },
              child: _buildRecentOrdersSection(l10n, recentOrdersAsync),
            ),

            const SizedBox(height: 24),

            // Logout Button
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppConstants.horizontalPadding,
              ),
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
    final addressesAsync = ref.watch(addressNotifierProvider);
    return addressesAsync.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
      ),
      error: (_, __) => _buildFallbackSection(l10n),
      data: (addresses) {
        if (addresses.isEmpty) return _buildFallbackSection(l10n);
        return Column(
          children: [
            ...addresses.map((a) => _buildAddressModelTile(l10n, a)),
            const Divider(height: 1),
            _buildAddNewItem(
              l10n: l10n,
              icon: Icons.add_location_outlined,
              title: 'add_new_address',
            ),
          ],
        );
      },
    );
  }

  Widget _buildFallbackSection(AppLocalizations l10n) {
    final loc = ref.watch(locationNotifierProvider.select((s) => s.location));
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

  Widget _buildAddressModelTile(AppLocalizations l10n, AddressModel address) {
    final IconData icon;
    switch (address.type) {
      case AddressType.work:
        icon = Icons.work_outline;
        break;
      case AddressType.apartment:
        icon = Icons.apartment_outlined;
        break;
      case AddressType.other:
        icon = Icons.place_outlined;
        break;
      default:
        icon = Icons.home_outlined;
    }
    final title = address.label?.isNotEmpty == true
        ? address.label!
        : _addressTypeLabel(l10n, address.type);
    final subtitle =
        address.formattedAddress ??
        [
          address.street,
          address.city,
        ].where((s) => s != null && s!.isNotEmpty).join(', ');
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
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: Colors.black87,
        ),
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: (subtitle?.isNotEmpty == true)
          ? Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                subtitle!,
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            )
          : null,
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
      onTap: () => context.push(RouteNames.addresses),
    );
  }

  String _addressTypeLabel(AppLocalizations l10n, AddressType type) {
    switch (type) {
      case AddressType.work:
        return l10n.translate('work_address');
      case AddressType.apartment:
        return l10n.translate('apartment_address');
      case AddressType.other:
        return l10n.translate('other_address');
      default:
        return l10n.translate('home_address');
    }
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
        ? (loc.shortAddress.isNotEmpty
              ? loc.shortAddress
              : loc.formattedAddress)
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
        child: Icon(
          icon,
          color: AppColors.primary,
          size: AppConstants.iconSizeMedium,
        ),
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
              child: Text(
                l10n.translate('live_badge'),
                style: const TextStyle(
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
          color: AppColors.secondaryDark.withOpacity(0.1),
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        ),
        child: Icon(
          icon,
          color: AppColors.secondaryDark,
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
                color: AppColors.secondaryDark,
                borderRadius: BorderRadius.circular(
                  AppConstants.borderRadiusSmall / 2,
                ),
              ),
              child: Text(
                l10n.translate('live_badge'),
                style: const TextStyle(
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
          style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
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
          border: Border.all(
            color: Colors.grey[300]!,
            width: 2,
            style: BorderStyle.solid,
          ),
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
    final l10n = AppLocalizations.of(context)!;

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
                padding: const EdgeInsets.symmetric(
                  horizontal: AppConstants.borderRadiusMedium,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(
                    AppConstants.borderRadiusMedium,
                  ),
                ),
                child: Text(
                  l10n.translate('default_label'),
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
              Text(
                l10n.translate('cardholder_placeholder'),
                style: const TextStyle(
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

  String _orderMoney(double value) {
    return '${value.toStringAsFixed(2)} TND';
  }

  LinearGradient _statusGradient(Order order) {
    if (order.isCancelled) {
      return const LinearGradient(
        colors: [AppColors.primaryDark, AppColors.primary],
      );
    }
    if (order.isDelivered) {
      return const LinearGradient(
        colors: [AppColors.primary2, AppColors.primary],
      );
    }
    if (order.waitingPartnerAcceptance) {
      return const LinearGradient(
        colors: [AppColors.secondaryDark, AppColors.primary],
      );
    }
    return const LinearGradient(
      colors: [AppColors.secondary3, AppColors.primary],
    );
  }

  String _formatOrderDate(Order order) {
    final date = (order.orderTime ?? order.createdAt)?.toLocal();
    if (date == null) return '--';
    final dd = date.day.toString().padLeft(2, '0');
    final mm = date.month.toString().padLeft(2, '0');
    final hh = date.hour.toString().padLeft(2, '0');
    final min = date.minute.toString().padLeft(2, '0');
    return '$dd/$mm • $hh:$min';
  }

  Widget _buildRecentOrdersSection(
    AppLocalizations l10n,
    AsyncValue<List<Order>> ordersAsync,
  ) {
    return ordersAsync.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
      ),
      error: (_, __) =>
          _buildOrderPlaceholder(l10n.translate('no_validated_orders')),
      data: (orders) {
        if (orders.isEmpty) {
          return _buildOrderPlaceholder(l10n.translate('no_validated_orders'));
        }

        final sorted = List<Order>.from(orders)
          ..sort((a, b) {
            final ad = a.orderTime ?? a.createdAt;
            final bd = b.orderTime ?? b.createdAt;
            if (ad == null && bd == null) return 0;
            if (ad == null) return 1;
            if (bd == null) return -1;
            return bd.compareTo(ad);
          });

        final recent = _showAllRecentOrders ? sorted : sorted.take(1).toList();
        return Column(
          children: [
            ...recent.map((order) => _buildRecentOrderCard(order, l10n)),
            if (sorted.length > 1)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: () {
                    setState(() => _showAllRecentOrders = !_showAllRecentOrders);
                  },
                  icon: Icon(
                    _showAllRecentOrders
                        ? Icons.keyboard_arrow_up_rounded
                        : Icons.keyboard_arrow_down_rounded,
                    color: AppColors.primary,
                  ),
                  label: Text(
                    _showAllRecentOrders
                        ? l10n.translate('collapse')
                        : '${l10n.translate('show_all')} (${sorted.length - 1})',
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
          ],
        );
      },
    );
  }

  Widget _buildOrderPlaceholder(String text) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(text, style: const TextStyle(color: AppColors.textSecondary)),
    );
  }

  Widget _buildRecentOrderCard(Order order, AppLocalizations l10n) {
    final badgeGradient = _statusGradient(order);
    final partnerId = order.partnerId?.trim();
    final partnerPreview = partnerId == null || partnerId.isEmpty
        ? const AsyncValue<PartnerNearbyDto?>.data(null)
        : ref.watch(_orderPartnerPreviewProvider(partnerId));

    return Container(
      margin: const EdgeInsets.only(bottom: AppConstants.verticalPadding),
      padding: const EdgeInsets.all(AppConstants.verticalPadding),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildPartnerAvatar(order, partnerPreview),
              const SizedBox(width: 16),

              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      order.partnerName?.trim().isNotEmpty == true
                          ? order.partnerName!.trim()
                          : l10n.translate('partner'),
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Text(
                          _formatOrderDate(order),
                          style: TextStyle(
                            fontSize: 13,
                            color: Colors.grey[600],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text('•', style: TextStyle(color: Colors.grey[400])),
                        const SizedBox(width: 8),
                        Text(
                          _orderMoney(order.total),
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
            ],
          ),

          const SizedBox(height: 10),
          Align(
            alignment: Alignment.centerLeft,
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppConstants.borderRadiusMedium,
                vertical: 6,
              ),
              decoration: BoxDecoration(
                gradient: badgeGradient,
                borderRadius: BorderRadius.circular(AppConstants.cardPadding),
              ),
              child: Text(
                order.displayStatus,
                style: const TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ),
          ),

          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: () => context.push(RouteNames.orderTracking(order.id)),
              icon: const Icon(Icons.route_rounded, size: 18),
              label: Text(l10n.translate('track_order')),
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.black,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPartnerAvatar(
    Order order,
    AsyncValue<PartnerNearbyDto?> partnerPreview,
  ) {
    final partnerName = order.partnerName?.trim();
    final initials = partnerName != null && partnerName.isNotEmpty
        ? partnerName.characters.take(1).toString().toUpperCase()
        : 'S';

    return SizedBox(
      width: 56,
      height: 56,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        child: partnerPreview.when(
          data: (partner) {
            final logoUrl = partner?.logo?.trim() ?? '';
            if (logoUrl.isNotEmpty) {
              return Image.network(
                logoUrl,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) => _partnerAvatarFallback(initials),
              );
            }
            return _partnerAvatarFallback(initials);
          },
          loading: () => _partnerAvatarFallback(initials),
          error: (_, __) => _partnerAvatarFallback(initials),
        ),
      ),
    );
  }

  Widget _partnerAvatarFallback(String initials) {
    return Container(
      color: AppColors.primary.withOpacity(0.14),
      child: Center(
        child: Text(
          initials,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.primary,
          ),
        ),
      ),
    );
  }
}
