import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'dart:math' as math;
import 'package:cached_network_image/cached_network_image.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/media_url.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../../../partners/data/models/category_dto.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';

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
     const SystemUiOverlayStyle(
        statusBarColor: AppColors.secondary3,
        statusBarIconBrightness: Brightness.light,
      ),
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      body: Column(
        children: [
          SizedBox(
            height: MediaQuery.of(context).padding.top,
            width: double.infinity,
            child: const ColoredBox(color: AppColors.secondary3),
          ),
          Expanded(
            child: SafeArea(
              top: false,
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
              // Red gradient background section with location and categories
              Stack(
                clipBehavior: Clip.none,
                children: [
                  Container(
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          AppColors.secondary3,
                          AppColors.primary,
                          AppColors.primary2,
                        ],
                      ),
                    ),
                    child: Column(
                      children: [
                        Padding(
                          padding: EdgeInsets.all(
                            ResponsiveUtils.getResponsiveSpacing(
                              context,
                              AppConstants.horizontalPadding,
                            ),
                          ),
                          child: Column(
                            children: [
                              SizedBox(
                                height: ResponsiveUtils.getResponsiveSpacing(
                                  context,
                                  8,
                                ),
                              ),
                              Center(child: _LocationHeader(l10n: l10n)),
                              SizedBox(
                                height: ResponsiveUtils.getResponsiveSpacing(
                                  context,
                                  24,
                                ),
                              ),
                              _CategoriesSection(l10n: l10n),
                              SizedBox(
                                height: ResponsiveUtils.getResponsiveSpacing(
                                  context,
                                  5,
                                ),
                              ),
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

              SizedBox(
                height: ResponsiveUtils.getResponsiveSpacing(context, 12),
              ),

              Padding(
                padding: EdgeInsets.symmetric(
                  horizontal: ResponsiveUtils.getResponsiveSpacing(
                    context,
                    AppConstants.horizontalPadding,
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Special for you Section
                    _SpecialForYouSection(l10n: l10n),

                    SizedBox(
                      height: ResponsiveUtils.getResponsiveSpacing(context, 12),
                    ),

                    // Free Delivery Banner
                    _FreeDeliveryBanner(l10n: l10n),

                    SizedBox(
                      height: ResponsiveUtils.getResponsiveSpacing(context, 12),
                    ),
                  ],
                ),
              ),
                  ],
                ),
              ),
            ),
          ),
        ],
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
      case AddressType.home:
        return Icons.home_rounded;
      case AddressType.work:
        return Icons.business_rounded;
      case AddressType.apartment:
        return Icons.apartment_rounded;
      case AddressType.other:
        return Icons.location_on_rounded;
    }
  }

  void _showPicker(BuildContext context, WidgetRef ref) {
    final userId = ref
        .read(authNotifierProvider)
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
                      width: 40,
                      height: 4,
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
                      child: Center(
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
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
                      final label = address.displayLabel;
                      final isActive = label == activeLabel;
                      final String subtitle =
                          address.formattedAddress ??
                          [
                            address.street,
                            address.city,
                          ].whereType<String>().where((s) => s.isNotEmpty).join(', ');
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: Container(
                          width: 44,
                          height: 44,
                          decoration: BoxDecoration(
                            color: AppColors.primary.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(
                              AppConstants.borderRadiusMedium,
                            ),
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
                            color: isActive
                                ? AppColors.primary
                                : const Color(0xFF1A1A1A),
                          ),
                        ),
                        subtitle: subtitle.isNotEmpty
                            ? Text(
                                subtitle,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: 13,
                                  color: Colors.grey[600],
                                ),
                              )
                            : null,
                        trailing: isActive
                            ? const Icon(Icons.check_circle, color: AppColors.primary)
                            : null,
                        onTap: () {
                          if (address.latitude != null &&
                              address.longitude != null) {
                            ref
                                .read(locationNotifierProvider.notifier)
                                .selectAddress(
                                  latitude: address.latitude!,
                                  longitude: address.longitude!,
                                  label: label,
                                  type: address.type,
                                  formattedAddress:
                                      address.formattedAddress ?? label,
                                  street: address.street ?? '',
                                  city: address.city ?? '',
                                );
                          } else {
                            ref
                                .read(locationNotifierProvider.notifier)
                                .tagLocation(address.type, label);
                          }
                          Navigator.of(ctx).pop();
                        },
                      );
                    }),

                  const Divider(height: 8),

                  // ── Choose on map ─────────────────────────────────────────
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: Colors.grey[100],
                        borderRadius: BorderRadius.circular(
                          AppConstants.borderRadiusMedium,
                        ),
                      ),
                      child: Icon(
                        Icons.my_location_rounded,
                        color: Colors.grey[700],
                        size: AppConstants.iconSizeMedium,
                      ),
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
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        border: Border.all(
                          color: AppColors.primary.withOpacity(0.5),
                        ),
                        borderRadius: BorderRadius.circular(
                          AppConstants.borderRadiusMedium,
                        ),
                      ),
                      child: const Icon(
                        Icons.add_location_alt_outlined,
                        color: AppColors.primary,
                        size: AppConstants.iconSizeMedium,
                      ),
                    ),
                    title: Text(
                      l10n.translate('add_new_address'),
                      style:const TextStyle(
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
    final address =
        (loc != null
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
              color: Colors.black,
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

/// Categories Section — dynamically built from /api/v1/categories.
/// Root categories (parentId == null) are shown as circular cards.
/// Tapping navigates to NearbyPartnersScreen pre-filtered by that category.
class _CategoriesSection extends ConsumerStatefulWidget {
  final AppLocalizations l10n;

  const _CategoriesSection({required this.l10n});

  @override
  ConsumerState<_CategoriesSection> createState() => _CategoriesSectionState();
}

class _CategoriesSectionState extends ConsumerState<_CategoriesSection> {
  bool _isNavigatingToNearby = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _ensureNearbyLoaded());
  }

  ({double lat, double lng}) _resolveCoordinates() {
    final selectedLoc = ref.read(locationNotifierProvider).location;
    if (selectedLoc != null) {
      return (lat: selectedLoc.latitude, lng: selectedLoc.longitude);
    }

    final addresses = ref.read(addressNotifierProvider).valueOrNull ?? [];
    AddressModel? target;

    for (final a in addresses) {
      if (a.isDefault && a.latitude != null && a.longitude != null) {
        target = a;
        break;
      }
    }

    if (target == null) {
      for (final a in addresses) {
        if (a.latitude != null && a.longitude != null) {
          target = a;
          break;
        }
      }
    }

    if (target != null) {
      return (lat: target.latitude!, lng: target.longitude!);
    }

    return (lat: 0.0, lng: 0.0);
  }

  void _ensureNearbyLoaded() {
    final nearbyState = ref.read(nearbyPartnersNotifierProvider);
    if (nearbyState.isLoading || nearbyState.allPartners.isNotEmpty) {
      return;
    }
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) {
      return;
    }
    ref
        .read(nearbyPartnersNotifierProvider.notifier)
        .load(lat: coords.lat, lng: coords.lng);
  }

  Future<void> _onCategoryTap(int categoryId) async {
    if (_isNavigatingToNearby) return;

    setState(() => _isNavigatingToNearby = true);
    try {
      ref
          .read(nearbyPartnersNotifierProvider.notifier)
          .setCategoryById(categoryId);

      if (!mounted) return;
      await context.push(RouteNames.nearbyPartners);
    } catch (e, st) {
      debugPrint('[CategoriesSection] category tap navigation error: $e');
      debugPrint('[CategoriesSection] stack: $st');
    } finally {
      if (mounted) {
        setState(() => _isNavigatingToNearby = false);
      }
    }
  }

  List<int> _buildRowPattern(int count) {
    final rows = <int>[];
    var remaining = count;
    var useTwo = true;

    while (remaining > 0) {
      if (remaining <= 3) {
        rows.add(remaining);
        break;
      }

      if (useTwo) {
        if (remaining == 4) {
          rows.addAll([2, 2]);
          break;
        }
        rows.add(2);
        remaining -= 2;
      } else {
        if (remaining == 4) {
          rows.addAll([2, 2]);
          break;
        }
        rows.add(3);
        remaining -= 3;
      }

      useTwo = !useTwo;
    }

    return rows;
  }

  Set<int> _descendantIds(int rootId, List<CategoryDto> all) {
    final ids = <int>{rootId};
    var changed = true;
    while (changed) {
      changed = false;
      for (final c in all) {
        if (c.parentId != null &&
            ids.contains(c.parentId) &&
            !ids.contains(c.id)) {
          ids.add(c.id);
          changed = true;
        }
      }
    }
    return ids;
  }

  // Map a backend icon name string to a Flutter IconData
  static IconData _iconForName(String? name) {
    switch (name) {
      case 'restaurant':
        return Icons.restaurant;
      case 'local_grocery_store':
        return Icons.local_grocery_store;
      case 'local_pharmacy':
        return Icons.local_pharmacy;
      case 'local_shipping':
        return Icons.local_shipping;
      case 'coffee':
        return Icons.coffee;
      case 'fastfood':
        return Icons.fastfood;
      case 'store':
        return Icons.store;
      case 'bakery_dining':
        return Icons.bakery_dining;
      case 'health_and_safety':
        return Icons.health_and_safety;
      case 'more_horiz':
        return Icons.more_horiz;
      default:
        return Icons.category_outlined;
    }
  }

  // Parse '#RRGGBB' hex string to Color
  static Color _parseHex(String? hex) {
    if (hex == null || hex.length < 7) return AppColors.primaryDark;
    final value = int.tryParse('FF${hex.substring(1)}', radix: 16);
    return value != null ? Color(value) : AppColors.primaryDark;
  }

  static bool _looksLikeImageRef(String? value) {
    if (value == null || value.trim().isEmpty) return false;
    final v = value.trim().toLowerCase();
    return v.startsWith('http://') ||
        v.startsWith('https://') ||
        v.startsWith('/uploads/') ||
        v.startsWith('uploads/') ||
        v.contains('/uploads/');
  }

  String _extractImageUrl(CategoryDto category) {
    if (_looksLikeImageRef(category.image)) {
      return resolveMediaUrl(category.image);
    }
    if (_looksLikeImageRef(category.icon)) {
      return resolveMediaUrl(category.icon);
    }
    return '';
  }

  @override
  Widget build(BuildContext context) {
    final categoriesAsync = ref.watch(categoriesProvider);
    final nearbyState = ref.watch(nearbyPartnersNotifierProvider);

    // Show shimmer placeholders while the API call is in flight
    if (categoriesAsync.isLoading) {
      debugPrint('[CategoriesSection] ⏳ loading…');
      return Wrap(
        alignment: WrapAlignment.center,
        spacing: ResponsiveUtils.getResponsiveSpacing(context, 10),
        runSpacing: ResponsiveUtils.getResponsiveSpacing(context, 16),
        children: List.generate(6, (_) => _CircularCategoryCardShimmer()),
      );
    }

    // Log and handle error state
    if (categoriesAsync.hasError) {
      debugPrint(
        '[CategoriesSection] ❌ provider error: ${categoriesAsync.error}',
      );
      debugPrint('[CategoriesSection]    stack: ${categoriesAsync.stackTrace}');
      return const SizedBox.shrink();
    }

    final all = categoriesAsync.value ?? [];
    debugPrint('[CategoriesSection] total categories from API: ${all.length}');

    final roots = all.where((c) => c.parentId == null && c.isActive).toList()
      ..sort((a, b) => a.displayOrder.compareTo(b.displayOrder));

    final nearCategoryIds = nearbyState.allPartners
        .expand((p) => p.categoryIds)
        .toSet();

    final nearRoots = roots.where((root) {
      final cluster = _descendantIds(root.id, all);
      return cluster.any(nearCategoryIds.contains);
    }).toList();

    debugPrint('[CategoriesSection] root active categories: ${roots.length}');
    debugPrint(
      '[CategoriesSection] root categories near client: ${nearRoots.length}',
    );

    if (nearbyState.isLoading && nearbyState.allPartners.isEmpty) {
      return Wrap(
        alignment: WrapAlignment.center,
        spacing: ResponsiveUtils.getResponsiveSpacing(context, 10),
        runSpacing: ResponsiveUtils.getResponsiveSpacing(context, 16),
        children: List.generate(4, (_) => _CircularCategoryCardShimmer()),
      );
    }

    if (nearRoots.isEmpty) {
      debugPrint(
        '[CategoriesSection] ⚠ no nearby categories to display for this client location',
      );
      return const SizedBox.shrink();
    }

    final rows = _buildRowPattern(nearRoots.length);
    final locale = Localizations.localeOf(context).languageCode;
    final cardSize = ResponsiveUtils.getResponsiveSize(context, 76);
    final rowSpacing = ResponsiveUtils.getResponsiveSpacing(context, 20);
    final itemSpacing = ResponsiveUtils.getResponsiveSpacing(context, 22);
    var cursor = 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: rows.asMap().entries.map((entry) {
        final rowIndex = entry.key;
        final rowCount = entry.value;
        final rowItems = nearRoots.sublist(cursor, cursor + rowCount);
        cursor += rowCount;

        final prevCount = rowIndex > 0 ? rows[rowIndex - 1] : null;
        final shouldOffset = prevCount == 2 && rowCount == 2;
        final offset = shouldOffset ? (cardSize + itemSpacing) / 2 : 0.0;

        return Padding(
          padding: EdgeInsets.only(
            bottom: rowIndex == rows.length - 1 ? 0 : rowSpacing,
          ),
          child: Transform.translate(
            offset: Offset(offset, 0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: rowItems.asMap().entries.map((item) {
                final cat = item.value;
                final icon = _iconForName(cat.icon);
                final color = _parseHex(cat.backgroundColor);
                final label = cat.localizedName(locale);
                final imageUrl = _extractImageUrl(cat);
                return Padding(
                  padding: EdgeInsets.only(
                    right: item.key == rowItems.length - 1 ? 0 : itemSpacing,
                  ),

                  child: GestureDetector(
                    onTap: () => _onCategoryTap(cat.id),
                    child: _FloatingCategoryBubble(
                      seed: cat.id,
                      preset: (rowIndex + item.key).isEven
                          ? _BubbleMotionPreset.calmPremium
                          : _BubbleMotionPreset.lively,
                      child: _CircularCategoryCard(
                        iconData: icon,
                        imageUrl: imageUrl,
                        label: label,
                        borderColor: color,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        );
      }).toList(),
    );
  }
}

/// Circular Category Card with colored border.
/// Created once per category returned by /api/v1/categories.
class _CircularCategoryCard extends StatelessWidget {
  final IconData iconData;
  final String imageUrl;
  final String label;
  final Color borderColor;

  const _CircularCategoryCard({
    required this.iconData,
    required this.imageUrl,
    required this.label,
    required this.borderColor,
  });

  @override
  Widget build(BuildContext context) {
    final cardSize = ResponsiveUtils.getResponsiveSize(context, 76);
    return Stack(
      clipBehavior: Clip.none,
      alignment: Alignment.center,
      children: [
        Column(
          children: [
            Container(
              width: cardSize,
              height: cardSize,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: const Color(0xFFF5E6D3), width: 2.6),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.shadow.withValues(alpha: 0.2),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  border: Border.all(color: borderColor, width: 2.2),
                ),
                child: Padding(
                  padding: EdgeInsets.all(
                    ResponsiveUtils.getResponsiveSpacing(context, 12),
                  ),
                  child: imageUrl.isNotEmpty
                      ? ClipOval(
                          child: CachedNetworkImage(
                            imageUrl: imageUrl,
                            fit: BoxFit.cover,
                            placeholder: (_, __) => Icon(
                              iconData,
                              size: ResponsiveUtils.getResponsiveSize(context, 24),
                              color: borderColor,
                            ),
                            errorWidget: (_, __, ___) => Icon(
                              iconData,
                              size: ResponsiveUtils.getResponsiveSize(context, 24),
                              color: borderColor,
                            ),
                          ),
                        )
                      : Icon(
                          iconData,
                          size: ResponsiveUtils.getResponsiveSize(context, 24),
                          color: borderColor,
                        ),
                ),
              ),
            ),
            SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 12)),
          ],
        ),
        Positioned(
          bottom: 0,
          child: Container(
            padding: EdgeInsets.symmetric(
              horizontal: ResponsiveUtils.getResponsiveSpacing(context, 8),
              vertical: ResponsiveUtils.getResponsiveSpacing(context, 3),
            ),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: borderColor, width: 1.5),
              boxShadow: [
                BoxShadow(
                  color: AppColors.shadow.withValues(alpha: 0.15),
                  blurRadius: 6,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Text(
              label,
              style: TextStyle(
                color: AppColors.textPrimary,
                fontSize: ResponsiveUtils.getResponsiveFontSize(context, 10),
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

/// Shimmer placeholder shown while categories are loading.
class _CircularCategoryCardShimmer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final size = ResponsiveUtils.getResponsiveSize(context, 72);
    return Column(
      children: [
        Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.white.withValues(alpha: 0.25),
          ),
        ),
        SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 14)),
        Container(
          width: size * 0.75,
          height: 10,
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.25),
            borderRadius: BorderRadius.circular(6),
          ),
        ),
      ],
    );
  }
}

class _FloatingCategoryBubble extends StatefulWidget {
  final Widget child;
  final int seed;
  final _BubbleMotionPreset preset;

  const _FloatingCategoryBubble({
    required this.child,
    required this.seed,
    required this.preset,
  });

  @override
  State<_FloatingCategoryBubble> createState() =>
      _FloatingCategoryBubbleState();
}

class _FloatingCategoryBubbleState extends State<_FloatingCategoryBubble>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final double _phase;
  late final double _amplitude;

  @override
  void initState() {
    super.initState();
    _phase = (widget.seed % 360) * (math.pi / 180);
    _amplitude = widget.preset == _BubbleMotionPreset.calmPremium
        ? 3.6 + ((widget.seed % 4) * 0.45)
        : 6.8 + ((widget.seed % 5) * 0.95);

    final durationMs = widget.preset == _BubbleMotionPreset.calmPremium
        ? 5600 + (widget.seed % 2200)
        : 3400 + (widget.seed % 1500);

    _controller = AnimationController(
      vsync: this,
      duration: Duration(milliseconds: durationMs),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final t = _controller.value * 2 * math.pi;
        final isCalm = widget.preset == _BubbleMotionPreset.calmPremium;
        final dx =
            math.sin((t * (isCalm ? 0.62 : 0.82)) + _phase) *
                (_amplitude * (isCalm ? 0.22 : 0.36)) +
            math.sin((t * (isCalm ? 1.1 : 1.6)) + (_phase * 0.7)) *
                (_amplitude * (isCalm ? 0.06 : 0.12));
        final dy =
            math.cos((t * (isCalm ? 0.7 : 0.95)) + _phase) * _amplitude +
            math.sin((t * (isCalm ? 1.0 : 1.3)) + (_phase * 1.1)) *
                (_amplitude * (isCalm ? 0.12 : 0.24));
        final scale =
            1.0 +
            (math.sin((t * (isCalm ? 0.58 : 0.86)) + _phase) *
                (isCalm ? 0.010 : 0.022));

        return Transform.translate(
          offset: Offset(dx, dy),
          child: Transform.scale(scale: scale, child: child),
        );
      },
      child: widget.child,
    );
  }
}

enum _BubbleMotionPreset { calmPremium, lively }

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
    return GestureDetector(
      child: Container(
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
                      fontSize: ResponsiveUtils.getResponsiveFontSize(
                        context,
                        14,
                      ),
                      fontWeight: FontWeight.w700,
                      height: 1.2,
                    ),
                  ),
                  SizedBox(
                    height: ResponsiveUtils.getResponsiveSpacing(context, 2),
                  ),
                  Text(
                    l10n.translate('order_and_enjoy'),
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: ResponsiveUtils.getResponsiveFontSize(
                        context,
                        11,
                      ),
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
      size.width * 0.25,
      0, // First control point
      size.width * 0.35,
      size.height, // Second control point (curve down)
      size.width * 0.5,
      size.height, // End point (center bottom)
    );

    path.cubicTo(
      size.width * 0.70,
      size.height, // Continue curve
      size.width * 0.80,
      0, // Control point (curve up)
      size.width,
      0, // End at top right
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
