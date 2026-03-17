import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shimmer/shimmer.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../main/presentation/screens/main_scaffold.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../providers/nearby_partners_provider.dart';
import '../../data/models/category_dto.dart';
import '../../data/models/partner_nearby_dto.dart';

class NearbyPartnersScreen extends ConsumerStatefulWidget {
  const NearbyPartnersScreen({super.key});

  @override
  ConsumerState<NearbyPartnersScreen> createState() =>
      _NearbyPartnersScreenState();
}

class _NearbyPartnersScreenState extends ConsumerState<NearbyPartnersScreen> {
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';

  /// Resolve coordinates: prefer selected/default address, fall back to GPS.
  ({double lat, double lng, String? label}) _resolveCoordinates() {
    // 1. User-selected address from the explore screen picker (stored in
    //    locationNotifierProvider by selectAddress()). This always reflects the
    //    most-recent explicit selection.
    final selectedLoc = ref.read(locationNotifierProvider).location;
    if (selectedLoc != null) {
      return (
        lat: selectedLoc.latitude,
        lng: selectedLoc.longitude,
        label: selectedLoc.customLabel?.isNotEmpty == true
            ? selectedLoc.customLabel
            : selectedLoc.shortAddress,
      );
    }

    // 2. Default delivery address from the backend profile
    final addresses = ref.read(addressNotifierProvider).valueOrNull ?? [];
    AddressModel? target;
    for (final a in addresses) {
      if (a.isDefault && a.latitude != null && a.longitude != null) {
        target = a;
        break;
      }
    }
    // 3. Any profile address with coordinates as fallback
    if (target == null) {
      for (final a in addresses) {
        if (a.latitude != null && a.longitude != null) {
          target = a;
          break;
        }
      }
    }
    if (target != null) {
      final label = target.label?.isNotEmpty == true
          ? target.label
          : target.formattedAddress;
      return (lat: target.latitude!, lng: target.longitude!, label: label);
    }

    // 4. No coordinates available
    return (lat: 0.0, lng: 0.0, label: null);
  }

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _initialLoad());
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _initialLoad() {
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) return;
    ref
        .read(nearbyPartnersNotifierProvider.notifier)
        .load(lat: coords.lat, lng: coords.lng);
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 300) {
      final coords = _resolveCoordinates();
      if (coords.lat == 0.0 && coords.lng == 0.0) return;
      ref
          .read(nearbyPartnersNotifierProvider.notifier)
          .loadMore(lat: coords.lat, lng: coords.lng);
    }
  }

  Future<void> _onRefresh() async {
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) return;
    await ref
        .read(nearbyPartnersNotifierProvider.notifier)
        .refresh(lat: coords.lat, lng: coords.lng);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final state = ref.watch(nearbyPartnersNotifierProvider);
    final categories = ref.watch(categoriesProvider).valueOrNull ?? [];
    final locale = Localizations.localeOf(context).languageCode;
    // Watch both providers so the UI rebuilds when either changes.
    ref.watch(addressNotifierProvider);
    ref.watch(locationNotifierProvider);

    // addressLabel and coordinates are both derived from _resolveCoordinates()
    // so they stay in sync: user-selected address first, then backend default.
    final coords = _resolveCoordinates();
    final addressLabel = coords.label?.isNotEmpty == true
        ? coords.label!
        : l10n.translate('default_location');

    CategoryDto? selectedCategory;
    for (final cat in categories) {
      if (cat.id == state.selectedCategoryId) {
        selectedCategory = cat;
        break;
      }
    }

    CategoryDto? selectedRootCategory;
    if (selectedCategory != null && selectedCategory.parentId == null) {
      selectedRootCategory = selectedCategory;
    } else if (selectedCategory?.parentId != null) {
      for (final cat in categories) {
        if (cat.id == selectedCategory!.parentId) {
          selectedRootCategory = cat;
          break;
        }
      }
    }

    final selectedCategoryLabel =
        selectedRootCategory?.localizedName(locale) ??
        l10n.translate('categories');

    final query = _searchQuery.trim().toLowerCase();
    final visiblePartners = state.filteredPartners.where((p) {
      if (query.isEmpty) return true;
      final name = p.displayName.toLowerCase();
      final type = (p.type ?? '').toLowerCase();
      return name.contains(query) || type.contains(query);
    }).toList();

    // Show snackbar on error (new error only)
    ref.listen<NearbyPartnersState>(nearbyPartnersNotifierProvider, (
      prev,
      next,
    ) {
      if (next.errorMessage != null &&
          next.errorMessage != prev?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.translate('partners_loading_error')),
            action: SnackBarAction(
              label: l10n.translate('retry'),
              onPressed: _initialLoad,
            ),
            backgroundColor: AppColors.error,
          ),
        );
        ref.read(nearbyPartnersNotifierProvider.notifier).clearError();
      }
    });

    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.light,
      ),
    );

    return MainScaffold(
      currentPath: RouteNames.explore,
      child: ColoredBox(
        color: AppColors.background,
        child: RefreshIndicator(
          onRefresh: _onRefresh,
          color: AppColors.primary,
          child: CustomScrollView(
            controller: _scrollController,
            physics: const AlwaysScrollableScrollPhysics(),
            slivers: [
              // ── App Bar ───────────────────────────────────────────────────
              SliverPersistentHeader(
                pinned: true,
                delegate: _NearbyAppBar(
                  addressLabel: addressLabel,
                  categoryLabel: selectedCategoryLabel,
                  l10n: l10n,
                ),
              ),

              // ── Category label + search (under app bar) ───────────────────
              SliverToBoxAdapter(
                child: _CategoryHeaderAndSearch(
                  categoryLabel: selectedCategoryLabel,
                  l10n: l10n,
                  controller: _searchController,
                  onChanged: (value) => setState(() => _searchQuery = value),
                ),
              ),

              // ── Sub-categories ────────────────────────────────────────────
              SliverToBoxAdapter(
                child: _SubCategoriesRow(l10n: l10n, state: state),
              ),

              // ── Filter chips ──────────────────────────────────────────────
              SliverToBoxAdapter(child: _FilterRow(l10n: l10n)),

              const SliverToBoxAdapter(child: SizedBox(height: 4)),

              // ── All partners header ───────────────────────────────────────
              SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: ResponsiveUtils.getResponsiveSpacing(
                      context,
                      AppConstants.horizontalPadding,
                    ),
                    vertical: ResponsiveUtils.getResponsiveSpacing(context, 12),
                  ),
                  child: Text(
                    l10n.translate('all_partners'),
                    style: TextStyle(
                      color: AppColors.textPrimary,
                      fontSize: ResponsiveUtils.getResponsiveFontSize(
                        context,
                        18,
                      ),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),

              // ── Partner list ──────────────────────────────────────────────
              if (state.isLoading)
                SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (_, i) => const _PartnerCardShimmer(),
                    childCount: 5,
                  ),
                )
              else if (visiblePartners.isEmpty)
                SliverToBoxAdapter(child: _EmptyState(l10n: l10n))
              else
                SliverList(
                  delegate: SliverChildBuilderDelegate((_, i) {
                    final partners = visiblePartners;
                    if (i < partners.length) {
                      return _PartnerCard(partner: partners[i]);
                    }
                    return null;
                  }, childCount: visiblePartners.length),
                ),

              // ── Load more indicator ───────────────────────────────────────
              SliverToBoxAdapter(child: _LoadMoreIndicator(state: state)),

              const SliverToBoxAdapter(child: SizedBox(height: 24)),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── App Bar ────────────────────────────────────────────────────────────────

class _NearbyAppBar extends SliverPersistentHeaderDelegate {
  final String addressLabel;
  final String categoryLabel;
  final AppLocalizations l10n;

  _NearbyAppBar({
    required this.addressLabel,
    required this.categoryLabel,
    required this.l10n,
  });

  @override
  double get minExtent => kToolbarHeight + 16;

  @override
  double get maxExtent => 120.0;

  @override
  bool shouldRebuild(_NearbyAppBar oldDelegate) =>
      oldDelegate.addressLabel != addressLabel ||
      oldDelegate.categoryLabel != categoryLabel;

  @override
  Widget build(
    BuildContext context,
    double shrinkOffset,
    bool overlapsContent,
  ) {
    final progress = (shrinkOffset / (maxExtent - minExtent)).clamp(0.0, 1.0);
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            AppColors.black,
            Color.lerp(AppColors.black, AppColors.darkGrey, 1 - progress)!,
          ],
        ),
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(18 * (1 - progress)),
          bottomRight: Radius.circular(18 * (1 - progress)),
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              // Back button
              GestureDetector(
                onTap: () => Navigator.of(context).pop(),
                child: Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.arrow_back_ios_new,
                    color: Colors.white,
                    size: 18,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      l10n.translate('nearby_partners'),
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w700,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                    ),
                    if (progress < 0.7)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(
                            Icons.location_on,
                            color: Colors.white70,
                            size: 14,
                          ),
                          const SizedBox(width: 3),
                          Flexible(
                            child: Text(
                              addressLabel,
                              style: const TextStyle(
                                color: Colors.white70,
                                fontSize: 14,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
              ),
              const SizedBox(width: 50),
            ],
          ),
        ),
      ),
    );
  }
}

class _CategoryHeaderAndSearch extends StatelessWidget {
  final String categoryLabel;
  final AppLocalizations l10n;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  const _CategoryHeaderAndSearch({
    required this.categoryLabel,
    required this.l10n,
    required this.controller,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            categoryLabel,
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontSize: 18,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: controller,
            onChanged: onChanged,
            decoration: InputDecoration(
              hintText: l10n.translate('search'),
              prefixIcon: const Icon(Icons.search_rounded),
              isDense: true,
              filled: true,
              fillColor: AppColors.background,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Sub-categories Row ──────────────────────────────────────────────────────

/// Big-icon category row built from the real /v1/categories API.
/// Shows only sub-categories where parentId equals the category selected
/// from Explore (no "All" button).
class _SubCategoriesRow extends ConsumerWidget {
  final AppLocalizations l10n;
  final NearbyPartnersState state;

  const _SubCategoriesRow({required this.l10n, required this.state});

  static IconData _iconFor(CategoryDto category) {
    final raw = category.icon?.trim();
    if (raw == null || raw.isEmpty) return Icons.category_outlined;

    final hex = raw.startsWith('0x')
        ? int.tryParse(raw.substring(2), radix: 16)
        : null;
    final decimal = int.tryParse(raw);
    final codePoint = hex ?? decimal;
    if (codePoint != null) {
      return IconData(codePoint, fontFamily: 'MaterialIcons');
    }

    switch (raw.toLowerCase()) {
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

  static Color _colorFromHex(String? hex, Color fallback) {
    if (hex == null || hex.trim().isEmpty) return fallback;
    final clean = hex.replaceAll('#', '');
    if (clean.length != 6) return fallback;
    final value = int.tryParse(clean, radix: 16);
    if (value == null) return fallback;
    return Color(0xFF000000 | value);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(categoriesProvider).valueOrNull ?? [];
    int? selectedRootId = state.selectedCategoryId;
    if (selectedRootId == null && state.selectedSubCategoryIds.isNotEmpty) {
      final subId = state.selectedSubCategoryIds.first;
      for (final cat in categories) {
        if (cat.id == subId) {
          selectedRootId = cat.parentId;
          break;
        }
      }
    }

    for (final cat in categories) {
      if (cat.id == selectedRootId && cat.parentId != null) {
        selectedRootId = cat.parentId;
        break;
      }
    }

    final subCategories =
        categories
            .where(
              (c) =>
                  c.parentId != null &&
                  c.parentId == selectedRootId &&
                  c.isActive,
            )
            .toList()
          ..sort((a, b) => a.displayOrder.compareTo(b.displayOrder));

    final locale = Localizations.localeOf(context).languageCode;

    if (selectedRootId == null || subCategories.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            // ─ One item per sub-category under selected explore category ───
            ...subCategories.map((cat) {
              final isSelected = state.selectedSubCategoryIds.contains(cat.id);
              final accent = _colorFromHex(
                cat.backgroundColor,
                AppColors.primary,
              );
              final textOnAccent = _colorFromHex(cat.textColor, Colors.white);
              return GestureDetector(
                onTap: () => ref
                    .read(nearbyPartnersNotifierProvider.notifier)
                    .toggleSubCategory(cat.id),
                child: Container(
                  margin: const EdgeInsets.only(right: 16),
                  child: Column(
                    children: [
                      Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          color: isSelected
                              ? accent.withValues(alpha: 0.14)
                              : AppColors.background,
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: isSelected ? accent : AppColors.border,
                            width: isSelected ? 2 : 1,
                          ),
                        ),
                        child: Icon(
                          _iconFor(cat),
                          size: 26,
                          color: isSelected ? accent : AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        cat.localizedName(locale),
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: isSelected
                              ? FontWeight.w700
                              : FontWeight.w500,
                          color: isSelected
                              ? textOnAccent == Colors.white
                                    ? accent
                                    : textOnAccent
                              : AppColors.textSecondary,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}

// ─── Filter Row ──────────────────────────────────────────────────────────────

class _FilterRow extends ConsumerWidget {
  final AppLocalizations l10n;

  const _FilterRow({required this.l10n});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(nearbyPartnersNotifierProvider);

    final sortOptions = [
      (
        label: l10n.translate('sort_recommended'),
        option: PartnerSortOption.recommended,
      ),
      (label: l10n.translate('sort_near_me'), option: PartnerSortOption.nearMe),
      (
        label: l10n.translate('sort_best_rated'),
        option: PartnerSortOption.bestRated,
      ),
      (
        label: l10n.translate('sort_delivery_fee'),
        option: PartnerSortOption.deliveryFee,
      ),
    ];

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.only(bottom: 12, top: 4),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            // Promotions chip
            _FilterChip(
              label: l10n.translate('promotions'),
              isSelected: state.promotionsOnly,
              onTap: () => ref
                  .read(nearbyPartnersNotifierProvider.notifier)
                  .togglePromotions(),
              icon: Icons.local_offer_outlined,
            ),
            const SizedBox(width: 8),
            // Sort options
            ...sortOptions.map(
              (opt) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: _FilterChip(
                  label: opt.label,
                  isSelected: state.sortOption == opt.option,
                  onTap: () => ref
                      .read(nearbyPartnersNotifierProvider.notifier)
                      .setSortOption(opt.option),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool isSelected;
  final VoidCallback onTap;
  final IconData? icon;

  const _FilterChip({
    required this.label,
    required this.isSelected,
    required this.onTap,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: AppConstants.animationDuration,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.primary : Colors.white,
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
          border: Border.all(
            color: isSelected ? AppColors.primary : AppColors.border,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(
                icon,
                size: 14,
                color: isSelected ? Colors.white : AppColors.textSecondary,
              ),
              const SizedBox(width: 5),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: isSelected ? Colors.white : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Partner Card (vertical list) ────────────────────────────────────────────

class _PartnerCard extends StatelessWidget {
  final PartnerNearbyDto partner;

  const _PartnerCard({required this.partner});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final closed = !partner.isOpen;
    final hasFreeDelivery =
        partner.deliveryFee == 0 || partner.freeDeliveryThreshold != null;

    // Build the "opens at…" label when closed
    String? opensLabel;
    if (closed) {
      final info = partner.nextOpenInfo;
      if (info != null) {
        final key = info.opensToday ? 'opens_at' : 'opens_tomorrow_at';
        opensLabel = '${l10n.translate(key)} ${info.time}';
      }
    }

    return Padding(
      padding: EdgeInsets.symmetric(
        horizontal: ResponsiveUtils.getResponsiveSpacing(
          context,
          AppConstants.horizontalPadding,
        ),
        vertical: 6,
      ),
      child: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: closed ? const Color(0xFFF7F7F7) : Colors.white,
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
          boxShadow: [
            BoxShadow(
              color: AppColors.shadow.withValues(alpha: closed ? 0.04 : 0.07),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ── Cover image — greyscale only the image when closed ────────
            Stack(
              children: [
                ColorFiltered(
                  colorFilter: closed
                      ? const ColorFilter.matrix(<double>[
                          0.25,
                          0.65,
                          0.1,
                          0,
                          0,
                          0.25,
                          0.65,
                          0.1,
                          0,
                          0,
                          0.25,
                          0.65,
                          0.1,
                          0,
                          0,
                          0,
                          0,
                          0,
                          0.75,
                          0,
                        ])
                      : const ColorFilter.mode(
                          Colors.transparent,
                          BlendMode.multiply,
                        ),
                  child: _PartnerCoverImage(
                    coverUrl: partner.coverImage,
                    height: 140,
                  ),
                ),
                // Gradient overlay bottom
                Positioned(
                  bottom: 0,
                  left: 0,
                  right: 0,
                  child: Container(
                    height: 50,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.bottomCenter,
                        end: Alignment.topCenter,
                        colors: [
                          Colors.black.withValues(alpha: 0.5),
                          Colors.transparent,
                        ],
                      ),
                    ),
                  ),
                ),
                // Badges top-left (NEW, PREMIUM) — outside greyscale, full colour
                Positioned(
                  top: 10,
                  left: 10,
                  child: Row(
                    children: [
                      if (partner.isNew && !closed)
                        Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: _Badge(
                            label: l10n.translate('new_badge'),
                            color: AppColors.success,
                          ),
                        ),
                      if (partner.isPremium)
                        _Badge(
                          label: 'PREMIUM',
                          color: const Color(0xFFFFC107),
                        ),
                    ],
                  ),
                ),
                // CLOSED badge top-right — full colour (outside ColorFiltered)
                if (closed)
                  Positioned(
                    top: 10,
                    right: 10,
                    child: _Badge(
                      label: l10n.translate('closed_badge'),
                      color: AppColors.error,
                    ),
                  ),
                // Promo badge bottom-left
                if (hasFreeDelivery && !closed)
                  Positioned(
                    bottom: 8,
                    left: 10,
                    child: _Badge(
                      label: l10n.translate('free_delivery_promo'),
                      color: AppColors.primary,
                      small: true,
                    ),
                  ),
              ],
            ),
            // ── Details row — slightly dimmed when closed ─────────────────
            Opacity(
              opacity: closed ? 0.65 : 1.0,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Logo
                    _PartnerLogo(logoUrl: partner.logo, size: 50),
                    const SizedBox(width: 12),
                    // Info
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            partner.displayName,
                            style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: AppColors.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          if (partner.type != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 2),
                              child: Text(
                                _formatType(partner.type!),
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: AppColors.textSecondary,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          const SizedBox(height: 6),
                          Wrap(
                            spacing: 12,
                            runSpacing: 4,
                            children: [
                              // Rating
                              _InfoChip(
                                icon: Icons.star_rounded,
                                label: partner.rating.toStringAsFixed(1),
                                iconColor: AppColors.warning,
                              ),
                              // Prep time
                              if (partner.preparationTime != null)
                                _InfoChip(
                                  icon: Icons.access_time,
                                  label: '${partner.preparationTime} min',
                                  iconColor: AppColors.primary,
                                ),
                              // Delivery fee
                              if (partner.deliveryFee != null)
                                _InfoChip(
                                  icon: Icons.delivery_dining,
                                  label: partner.deliveryFee == 0
                                      ? l10n.translate('free')
                                      : '${partner.deliveryFee!.toStringAsFixed(partner.deliveryFee! % 1 == 0 ? 0 : 1)} DT',
                                  iconColor: partner.deliveryFee == 0
                                      ? AppColors.success
                                      : AppColors.textSecondary,
                                ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // ── Closed info bar — always full colour ──────────────────────
            if (closed)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: AppColors.error.withValues(alpha: 0.06),
                  border: Border(
                    top: BorderSide(
                      color: AppColors.error.withValues(alpha: 0.18),
                    ),
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.access_time_rounded,
                      size: 14,
                      color: AppColors.error,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      opensLabel ?? l10n.translate('closed_badge'),
                      style: TextStyle(
                        fontSize: 12,
                        color: AppColors.error,
                        fontWeight: FontWeight.w600,
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

  String _formatType(String type) {
    return type
        .replaceAll('_', ' ')
        .split(' ')
        .map(
          (w) => w.isEmpty
              ? ''
              : '${w[0].toUpperCase()}${w.substring(1).toLowerCase()}',
        )
        .join(' ');
  }
}

// ─── Shimmer for partner card ────────────────────────────────────────────────

class _PartnerCardShimmer extends StatelessWidget {
  const _PartnerCardShimmer();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(
        horizontal: ResponsiveUtils.getResponsiveSpacing(
          context,
          AppConstants.horizontalPadding,
        ),
        vertical: 6,
      ),
      child: Shimmer.fromColors(
        baseColor: Colors.grey[300]!,
        highlightColor: Colors.grey[100]!,
        child: Container(
          height: 240,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
          ),
        ),
      ),
    );
  }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

class _EmptyState extends StatelessWidget {
  final AppLocalizations l10n;

  const _EmptyState({required this.l10n});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 60, horizontal: 32),
      child: Column(
        children: [
          Icon(Icons.storefront_outlined, size: 64, color: AppColors.textHint),
          const SizedBox(height: 16),
          Text(
            l10n.translate('no_partners_near_you'),
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

// ─── Load More Indicator ─────────────────────────────────────────────────────

class _LoadMoreIndicator extends StatelessWidget {
  final NearbyPartnersState state;

  const _LoadMoreIndicator({required this.state});

  @override
  Widget build(BuildContext context) {
    if (state.isLoadingMore) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(
          child: SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(
              strokeWidth: 2.5,
              color: AppColors.primary,
            ),
          ),
        ),
      );
    }
    if (state.hasReachedEnd && state.allPartners.isNotEmpty) {
      return const SizedBox.shrink();
    }
    return const SizedBox.shrink();
  }
}

// ─── Shared helper widgets ───────────────────────────────────────────────────

class _Badge extends StatelessWidget {
  final String label;
  final Color color;
  final bool small;

  const _Badge({required this.label, required this.color, this.small = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: small ? 6 : 8,
        vertical: small ? 2 : 4,
      ),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: Colors.white,
          fontSize: small ? 10 : 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color iconColor;

  const _InfoChip({
    required this.icon,
    required this.label,
    required this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 13, color: iconColor),
        const SizedBox(width: 3),
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
}

class _PartnerCoverImage extends StatelessWidget {
  final String? coverUrl;
  final double height;

  const _PartnerCoverImage({this.coverUrl, required this.height});

  @override
  Widget build(BuildContext context) {
    if (coverUrl != null && coverUrl!.isNotEmpty) {
      return CachedNetworkImage(
        imageUrl: coverUrl!,
        height: height,
        width: double.infinity,
        fit: BoxFit.cover,
        placeholder: (_, __) => _placeholder(),
        errorWidget: (_, __, ___) => _placeholder(),
      );
    }
    return _placeholder();
  }

  Widget _placeholder() {
    return Container(
      height: height,
      color: const Color(0xFFF0EDED),
      child: const Center(
        child: Icon(
          Icons.storefront_outlined,
          size: 38,
          color: Color(0xFFCCCCCC),
        ),
      ),
    );
  }
}

class _PartnerLogo extends StatelessWidget {
  final String? logoUrl;
  final double size;

  const _PartnerLogo({this.logoUrl, required this.size});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
        border: Border.all(color: AppColors.border),
        boxShadow: [
          BoxShadow(
            color: AppColors.shadow.withValues(alpha: 0.1),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(
          AppConstants.borderRadiusMedium - 1,
        ),
        child: logoUrl != null && logoUrl!.isNotEmpty
            ? CachedNetworkImage(
                imageUrl: logoUrl!,
                fit: BoxFit.cover,
                placeholder: (_, __) => _logoPlaceholder(),
                errorWidget: (_, __, ___) => _logoPlaceholder(),
              )
            : _logoPlaceholder(),
      ),
    );
  }

  Widget _logoPlaceholder() {
    return Container(
      color: AppColors.background,
      child: const Icon(Icons.store, size: 20, color: AppColors.textHint),
    );
  }
}
