import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';

/// Explore Screen
/// Main screen for browsing restaurants and food options
class ExploreScreen extends ConsumerStatefulWidget {
  const ExploreScreen({super.key});

  @override
  ConsumerState<ExploreScreen> createState() => _ExploreScreenState();
}

class _ExploreScreenState extends ConsumerState<ExploreScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final authState = ref.read(authNotifierProvider);
      authState.whenOrNull(
        authenticated: (user) =>
            ref.read(addressNotifierProvider.notifier).fetchAddresses(user.id),
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    
    // Set status bar color
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        statusBarColor: AppColors.primary,
        statusBarIconBrightness: Brightness.light,
      ),
    );
    
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Red gradient background section with location and categories
              Stack(
                clipBehavior: Clip.none,
                children: [
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          AppColors.primary,
                          AppColors.secondaryDark,
                          AppColors.secondary,
                        ],
                      ),
                    ),
                    child: Column(
                      children: [
                        Padding(
                          padding: EdgeInsets.all(
                            ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
                          ),
                          child: Column(
                            children: [
                              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),
                              Center(child: _LocationHeader(l10n: l10n)),
                              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 14)),
                              _CategoriesSection(l10n: l10n),
                              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 5)),
                            ],
                          ),
                        ),
                        // Wavy bottom edge
                        CustomPaint(
                          painter: _WavePainter(),
                          size: Size(
                            MediaQuery.of(context).size.width,
                            ResponsiveUtils.getResponsiveSize(context, 30),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              
              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),
              
              Padding(
                padding: EdgeInsets.symmetric(
                  horizontal: ResponsiveUtils.getResponsiveSpacing(context, AppConstants.horizontalPadding),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Special for you Section
                    _SpecialForYouSection(l10n: l10n),
                    
                    SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),
                    
                    // Free Delivery Banner
                    _FreeDeliveryBanner(l10n: l10n),
                    
                    SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Location Header
/// Location Header — tappable pill that shows the active address label
/// and opens a bottom-sheet address picker on tap.
class _LocationHeader extends ConsumerWidget {
  final AppLocalizations l10n;

  const _LocationHeader({required this.l10n});

  // ─── helpers ────────────────────────────────────────────────────────────────

  IconData _iconFor(AddressType type) {
    switch (type) {
      case AddressType.home:      return Icons.home_rounded;
      case AddressType.work:      return Icons.business_rounded;
      case AddressType.apartment: return Icons.apartment_rounded;
      case AddressType.other:     return Icons.location_on_rounded;
    }
  }

  void _showPicker(BuildContext context, WidgetRef ref) {
    final userId = ref.read(authNotifierProvider)
        .whenOrNull(authenticated: (u) => u.id);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Consumer(
        builder: (ctx, ref, _) {
          final l10n = AppLocalizations.of(ctx)!;
          final addressesAsync = ref.watch(addressNotifierProvider);
          final addresses = addressesAsync.valueOrNull ?? [];
          final activeLabel = ref.watch(
            locationNotifierProvider.select((s) {
              final loc = s.location;
              if (loc == null) return null;
              return loc.customLabel?.isNotEmpty == true
                  ? loc.customLabel
                  : loc.shortAddress;
            }),
          );

          return SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // ── Sheet handle ──────────────────────────────────────────
                  Center(
                    child: Container(
                      width: 40, height: 4,
                      margin: const EdgeInsets.only(bottom: 16),
                      decoration: BoxDecoration(
                        color: Colors.grey[300],
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),

                  // ── Title ────────────────────────────────────────────────
                  Text(
                    l10n.translate('delivery_addresses'),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      color: AppColors.darkGrey,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // ── Saved addresses ───────────────────────────────────────
                  if (addressesAsync.isLoading)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                    )
                  else if (addresses.isEmpty)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: Text(
                        l10n.translate('no_address_saved'),
                        style: TextStyle(color: Colors.grey[500], fontSize: 14),
                      ),
                    )
                  else
                    ...addresses.map((address) {
                      final label  = address.displayLabel;
                      final isActive = label == activeLabel;
                      final subtitle = address.formattedAddress ??
                          [address.street, address.city]
                              .where((s) => s != null && s!.isNotEmpty)
                              .join(', ');
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: Container(
                          width: 44, height: 44,
                          decoration: BoxDecoration(
                            color: AppColors.primary.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                          ),
                          child: Icon(
                            _iconFor(address.type),
                            color: AppColors.primary,
                            size: AppConstants.iconSizeMedium,
                          ),
                        ),
                        title: Text(
                          label,
                          style: TextStyle(
                            fontWeight: FontWeight.w600,
                            color: isActive ? AppColors.primary : const Color(0xFF1A1A1A),
                          ),
                        ),
                        subtitle: subtitle.isNotEmpty
                            ? Text(
                                subtitle!,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                              )
                            : null,
                        trailing: isActive
                            ? Icon(Icons.check_circle, color: AppColors.primary)
                            : null,
                        onTap: () {
                          ref.read(locationNotifierProvider.notifier)
                              .tagLocation(address.type, label);
                          Navigator.of(ctx).pop();
                        },
                      );
                    }),

                  const Divider(height: 8),

                  // ── Choose on map ─────────────────────────────────────────
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Container(
                      width: 44, height: 44,
                      decoration: BoxDecoration(
                        color: Colors.grey[100],
                        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                      ),
                      child: Icon(Icons.my_location_rounded, color: Colors.grey[700], size: AppConstants.iconSizeMedium),
                    ),
                    title: Text(
                      l10n.translate('choose_on_map'),
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      context.push(RouteNames.confirmLocation);
                    },
                  ),

                  // ── Add new address ───────────────────────────────────────
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Container(
                      width: 44, height: 44,
                      decoration: BoxDecoration(
                        border: Border.all(color: AppColors.primary.withOpacity(0.5)),
                        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
                      ),
                      child: Icon(Icons.add_location_alt_outlined, color: AppColors.primary, size: AppConstants.iconSizeMedium),
                    ),
                    title: Text(
                      l10n.translate('add_new_address'),
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      context.push(
                        RouteNames.addressTypeSelector,
                        extra: {
                          'customerId': userId ?? '',
                          'redirectOnSuccess': RouteNames.explore,
                        },
                      );
                    },
                  ),

                  const SizedBox(height: 4),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  // ─── build ──────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locState = ref.watch(locationNotifierProvider);
    final loc = locState.location;
    final address = (loc != null
            ? (loc.customLabel != null && loc.customLabel!.isNotEmpty
                ? loc.customLabel!
                : loc.shortAddress)
            : null) ??
        l10n.translate('default_location');
    return GestureDetector(
      onTap: () => context.push(
        RouteNames.confirmLocation,
        extra: {
          'latitude': loc?.latitude ?? 36.8065,
          'longitude': loc?.longitude ?? 10.1815,
          'initialAddress': loc?.formattedAddress ?? '',
        },
      ),
      child: Container(
        padding: EdgeInsets.symmetric(
          horizontal: ResponsiveUtils.getResponsiveSpacing(context, 16),
          vertical: ResponsiveUtils.getResponsiveSpacing(context, 10),
        ),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(25),
          border: Border.all(
            color: Colors.white.withValues(alpha: 0.3),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.location_on,
              color: Colors.white,
              size: ResponsiveUtils.getResponsiveFontSize(context, 18),
            ),
            SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 6)),
            Flexible(
              child: Text(
                address,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                  fontWeight: FontWeight.w600,
                ),
                overflow: TextOverflow.ellipsis,
                maxLines: 1,
              ),
            ),
            SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 6)),
            Icon(
              Icons.keyboard_arrow_down_rounded,
              color: Colors.white,
              size: ResponsiveUtils.getResponsiveFontSize(context, 18),
            ),
          ],
        ),
      ),
    );
  }
}

/// Categories Section
class _CategoriesSection extends StatelessWidget {
  final AppLocalizations l10n;

  const _CategoriesSection({required this.l10n});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Top category (Restaurants)
        _CircularCategoryCard(
          imagePath: 'assets/icons/restaurant.png',
          label: l10n.translate('restaurants'),
          borderColor: AppColors.primaryDark,
        ),
        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 24)),
        // Bottom two categories
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _CircularCategoryCard(
              imagePath: 'assets/icons/panier-de-courses.png',
              label: l10n.translate('groceries'),
              borderColor: AppColors.primaryDark,
            ),
            SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 20)),
            Padding(
              padding: EdgeInsets.only(
                top: ResponsiveUtils.getResponsiveSpacing(context, 30),
              ),
              child: _CircularCategoryCard(
                imagePath: 'assets/icons/expedition-rapide.png',
                label: l10n.translate('service_courier'),
                borderColor: AppColors.primaryDark,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Circular Category Card with colored border
class _CircularCategoryCard extends StatelessWidget {
  final String imagePath;
  final String label;
  final Color borderColor;

  const _CircularCategoryCard({
    required this.imagePath,
    required this.label,
    required this.borderColor,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      alignment: Alignment.center,
      children: [
        Column(
          children: [
            Container(
              width: ResponsiveUtils.getResponsiveSize(context, 110),
              height: ResponsiveUtils.getResponsiveSize(context, 110),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: const Color(0xFFF5E6D3),
                  width: 5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.shadow.withValues(alpha: 0.2),
                    blurRadius: 25,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: borderColor,
                    width: 4,
                  ),
                ),
                child: Padding(
                  padding: EdgeInsets.all(
                    ResponsiveUtils.getResponsiveSpacing(context, 24),
                  ),
                  child: Image.asset(
                    imagePath,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
            ),
            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 15)),
          ],
        ),
        Positioned(
          bottom: 0,
          child: Container(
            padding: EdgeInsets.symmetric(
              horizontal: ResponsiveUtils.getResponsiveSpacing(context, 16),
              vertical: ResponsiveUtils.getResponsiveSpacing(context, 6),
            ),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: borderColor,
                width: 2,
              ),
              boxShadow: [
                BoxShadow(
                  color: AppColors.shadow.withValues(alpha: 0.15),
                  blurRadius: 10,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Text(
              label,
              style: TextStyle(
                color: AppColors.textPrimary,
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 12),
                fontWeight: FontWeight.w700,
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ],
    );
  }
}

/// Special for you Section
class _SpecialForYouSection extends StatelessWidget {
  final AppLocalizations l10n;

  const _SpecialForYouSection({required this.l10n});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.translate('special_for_you'),
          style: TextStyle(
            color: AppColors.textPrimary,
            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 18),
            fontWeight: FontWeight.w700,
          ),
        ),
        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),
        Row(
          children: [
            Expanded(
              child: _SpecialCard(
                imagePath: 'assets/icons/promotion.png',
                label: l10n.translate('promotions'),
                color: const Color(0xFFFF6B6B),
              ),
            ),
            SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 16)),
            Expanded(
              child: _SpecialCard(
                icon: Icons.store_rounded,
                label: l10n.translate('stores'),
                color: const Color(0xFFFFC107),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Special Card
class _SpecialCard extends StatelessWidget {
  final IconData? icon;
  final String? imagePath;
  final String label;
  final Color color;

  const _SpecialCard({
    this.icon,
    this.imagePath,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(
        ResponsiveUtils.getResponsiveSpacing(context, 14),
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
        boxShadow: [
          BoxShadow(
            color: AppColors.shadow.withValues(alpha: 0.08),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            padding: EdgeInsets.all(
              ResponsiveUtils.getResponsiveSpacing(context, 8),
            ),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: imagePath != null
                ? Image.asset(
                    imagePath!,
                    width: ResponsiveUtils.getResponsiveFontSize(context, 28),
                    height: ResponsiveUtils.getResponsiveFontSize(context, 28),
                  )
                : Icon(
                    icon!,
                    color: color,
                    size: ResponsiveUtils.getResponsiveFontSize(context, 28),
                  ),
          ),
          SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 8)),
          Text(
            label,
            style: TextStyle(
              color: AppColors.textPrimary,
              fontSize: ResponsiveUtils.getResponsiveFontSize(context, 12),
              fontWeight: FontWeight.w600,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

/// Free Delivery Banner
class _FreeDeliveryBanner extends StatelessWidget {
  final AppLocalizations l10n;

  const _FreeDeliveryBanner({required this.l10n});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: ResponsiveUtils.getResponsiveSpacing(context, 12),
        vertical: ResponsiveUtils.getResponsiveSpacing(context, 8),
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
        boxShadow: [
          BoxShadow(
            color: AppColors.shadow.withValues(alpha: 0.08),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.translate('free_delivery_first_order'),
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 14),
                    fontWeight: FontWeight.w700,
                    height: 1.2,
                  ),
                ),
                SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 2)),
                Text(
                  l10n.translate('order_and_enjoy'),
                  style: TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: ResponsiveUtils.getResponsiveFontSize(context, 11),
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(width: ResponsiveUtils.getResponsiveSpacing(context, 12)),
          Image.asset(
            'assets/images/free-delivery.png',
            width: ResponsiveUtils.getResponsiveFontSize(context, 150),
            height: ResponsiveUtils.getResponsiveFontSize(context, 100),
            fit: BoxFit.contain,
          ),
        ],
      ),
    );
  }
}

/// Wave Painter for bottom edge of gradient
class _WavePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.background
      ..style = PaintingStyle.fill;

    final path = Path();
    
    // Start from top left
    path.lineTo(0, 0);
    
    // Create a single modern curved shape
    path.cubicTo(
      size.width * 0.25, 0,           // First control point
      size.width * 0.35, size.height, // Second control point (curve down)
      size.width * 0.5, size.height,  // End point (center bottom)
    );
    
    path.cubicTo(
      size.width * 0.70, size.height, // Continue curve
      size.width * 0.80, 0,           // Control point (curve up)
      size.width, 0,                  // End at top right
    );
    
    // Complete the path
    path.lineTo(size.width, size.height);
    path.lineTo(0, size.height);
    path.close();
    
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
