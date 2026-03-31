import 'dart:math' as math;

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shimmer/shimmer.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/media_url.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../home/presentation/screens/filter_screen.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../main/presentation/screens/main_scaffold.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../../../search/presentation/screens/search_screen.dart';
import 'partner_details_screen.dart';
import 'partner_favorites_screen.dart';
import '../providers/favorite_partners_provider.dart';
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
  static const _searchHeroTag = 'nearby-search-hero';

  final ScrollController _scrollController = ScrollController();
  String _searchQuery = '';
  DateTime? _lastLoadMoreAt;
  ProviderSubscription<NearbyPartnersState>? _nearbyErrorSubscription;
  bool _showScrollToTop = false;
  bool _isOpeningSearch = false;
  String? _lastAllClosedSnackKey;

  ({double lat, double lng, String? label}) _resolveCoordinates() {
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
      final label = target.label?.isNotEmpty == true
          ? target.label
          : target.formattedAddress;
      return (lat: target.latitude!, lng: target.longitude!, label: label);
    }

    return (lat: 0.0, lng: 0.0, label: null);
  }

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);

    _nearbyErrorSubscription = ref.listenManual<NearbyPartnersState>(
      nearbyPartnersNotifierProvider,
      (prev, next) {
        if (!mounted) return;
        if (next.errorMessage != null &&
            next.errorMessage != prev?.errorMessage) {
          final l10n = AppLocalizations.of(context)!;
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
          Future<void>(() {
            if (!mounted) return;
            ref.read(nearbyPartnersNotifierProvider.notifier).clearError();
          });
        }
      },
    );

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialLoad();
      ref.read(favoritePartnersNotifierProvider.notifier).loadFavorites();
    });
  }

  @override
  void dispose() {
    _nearbyErrorSubscription?.close();
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
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
    if (!mounted || !_scrollController.hasClients) return;

    final pos = _scrollController.position;
    if (pos.maxScrollExtent <= 0) return;

    final shouldShowTopButton = pos.pixels > 420;
    if (shouldShowTopButton != _showScrollToTop) {
      setState(() => _showScrollToTop = shouldShowTopButton);
    }

    final isNearBottom = pos.pixels >= pos.maxScrollExtent - 220;
    final isScrollingDown = pos.userScrollDirection == ScrollDirection.reverse;

    if (isNearBottom && isScrollingDown) {
      final now = DateTime.now();
      if (_lastLoadMoreAt != null &&
          now.difference(_lastLoadMoreAt!) <
              const Duration(milliseconds: 700)) {
        return;
      }
      _lastLoadMoreAt = now;

      final coords = _resolveCoordinates();
      if (coords.lat == 0.0 && coords.lng == 0.0) return;
      ref
          .read(nearbyPartnersNotifierProvider.notifier)
          .loadMore(lat: coords.lat, lng: coords.lng);
    }
  }

  Future<void> _scrollToTop() async {
    if (!_scrollController.hasClients) return;
    await _scrollController.animateTo(
      0,
      duration: const Duration(milliseconds: 360),
      curve: Curves.easeOutCubic,
    );
  }

  DateTime? _nextOpeningDateTime(PartnerNearbyDto partner) {
    if (partner.openingHours.isEmpty) return null;

    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    final now = DateTime.now();
    DateTime? minDate;

    for (final h in partner.openingHours) {
      if (h.isClosed || h.openTime == null) continue;

      final dayIndex = dayNames.indexOf(h.dayOfWeek.toUpperCase());
      if (dayIndex < 0) continue;

      final parts = h.openTime!.split(':');
      if (parts.length < 2) continue;

      final hour = int.tryParse(parts[0]);
      final minute = int.tryParse(parts[1]);
      if (hour == null || minute == null) continue;

      var daysUntil = (dayIndex + 1) - now.weekday;
      if (daysUntil < 0) daysUntil += 7;

      var candidate = DateTime(
        now.year,
        now.month,
        now.day + daysUntil,
        hour,
        minute,
      );

      if (daysUntil == 0 && candidate.isBefore(now)) {
        candidate = candidate.add(const Duration(days: 7));
      }

      if (minDate == null || candidate.isBefore(minDate)) {
        minDate = candidate;
      }
    }

    return minDate;
  }

  String _formatTime(DateTime dt) {
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  String _nextOpeningLabel(DateTime nearest, AppLocalizations l10n) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final target = DateTime(nearest.year, nearest.month, nearest.day);
    final dayDiff = target.difference(today).inDays;

    if (dayDiff <= 0) {
      return '${l10n.translate('opens_at')} ${_formatTime(nearest)}';
    }
    if (dayDiff == 1) {
      return '${l10n.translate('opens_tomorrow_at')} ${_formatTime(nearest)}';
    }
    return '${l10n.translate('opens_at')} ${_formatTime(nearest)}';
  }

  void _maybeShowAllClosedNotice(
    List<PartnerNearbyDto> visiblePartners,
    AppLocalizations l10n,
  ) {
    if (!mounted || visiblePartners.isEmpty) {
      _lastAllClosedSnackKey = null;
      return;
    }

    final hasOpen = visiblePartners.any((p) => p.isOpen);
    if (hasOpen) {
      _lastAllClosedSnackKey = null;
      return;
    }

    final key = visiblePartners.map((p) => p.id).join('|');
    if (_lastAllClosedSnackKey == key) return;
    _lastAllClosedSnackKey = key;

    DateTime? nearest;
    for (final p in visiblePartners) {
      final nextOpen = _nextOpeningDateTime(p);
      if (nextOpen == null) continue;
      if (nearest == null || nextOpen.isBefore(nearest)) {
        nearest = nextOpen;
      }
    }

    final message = nearest == null
        ? l10n.translate('all_closed_now_notice')
        : '${l10n.translate('all_closed_now_notice')} ${_nextOpeningLabel(nearest, l10n)}';

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            message,
            style: const TextStyle(
              color: Colors.black,
              fontWeight: FontWeight.w600,
            ),
          ),
          backgroundColor: Colors.white,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: AppColors.border),
          ),
        ),
      );
    });
  }

  void _openFiltersBottomSheet(AppLocalizations l10n) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return PartnerFiltersBottomSheet(l10n: l10n, searchQuery: _searchQuery);
      },
    );
  }

  Future<void> _openDedicatedSearch() async {
    if (_isOpeningSearch || !mounted) return;
    _isOpeningSearch = true;

    final coords = _resolveCoordinates();
    try {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => SearchScreen(
            heroTag: _searchHeroTag,
            initialLat: coords.lat,
            initialLng: coords.lng,
          ),
        ),
      );
    } finally {
      _isOpeningSearch = false;
    }
  }

  Future<void> _openFavoritesScreen() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const PartnerFavoritesScreen()),
    );

    if (!mounted) return;
    await ref.read(favoritePartnersNotifierProvider.notifier).loadFavorites();
  }

  Future<void> _handleFavoriteToggle(PartnerNearbyDto partner) async {
    final result = await ref
        .read(favoritePartnersNotifierProvider.notifier)
        .toggleFavorite(partnerId: partner.id, partnerSnapshot: partner);

    if (!mounted) return;

    if (!result.success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Erreur, veuillez reessayer'),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    final message = result.action == FavoriteToggleAction.added
        ? 'Ajoute aux favoris'
        : 'Retire des favoris';

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  Future<void> _onRefresh() async {
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) return;
    await Future.wait([
      ref
          .read(nearbyPartnersNotifierProvider.notifier)
          .refresh(lat: coords.lat, lng: coords.lng),
      ref.read(favoritePartnersNotifierProvider.notifier).refresh(),
    ]);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final state = ref.watch(nearbyPartnersNotifierProvider);
    final favoritesState = ref.watch(favoritePartnersNotifierProvider);
    final categories = ref.watch(categoriesProvider).valueOrNull ?? [];
    final locale = Localizations.localeOf(context).languageCode;
    ref.watch(addressNotifierProvider);
    ref.watch(locationNotifierProvider);

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
    final favoriteCount = favoritesState.favoritePartners.length;

    _maybeShowAllClosedNotice(visiblePartners, l10n);

    // Status bar: dark icons on white app bar
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
      ),
    );

    return MainScaffold(
      currentPath: RouteNames.explore,
      child: Stack(
        children: [
          ColoredBox(
            color: AppColors.background,
            child: RefreshIndicator(
              onRefresh: _onRefresh,
              color: AppColors.primary,
              child: CustomScrollView(
                controller: _scrollController,
                physics: const AlwaysScrollableScrollPhysics(),
                slivers: [
                  // ── App Bar (white, collapsible) ──────────────────────────
                  SliverPersistentHeader(
                    pinned: true,
                    delegate: _NearbyAppBar(
                      topPadding: MediaQuery.paddingOf(context).top,
                      addressLabel: addressLabel,
                      categoryLabel: selectedCategoryLabel,
                      activeFilterCount: state.activeFilterCount,
                      l10n: l10n,
                      onSearchTap: _openDedicatedSearch,
                      onFilterTap: () => _openFiltersBottomSheet(l10n),
                      onFavoritesTap: _openFavoritesScreen,
                      favoritesCount: favoriteCount,
                      onBackTap: () => Navigator.of(context).pop(),
                    ),
                  ),

                  // ── Sub-categories ────────────────────────────────────────
                  SliverToBoxAdapter(
                    child: _SubCategoriesRow(l10n: l10n, state: state),
                  ),

                  const SliverToBoxAdapter(child: SizedBox(height: 4)),

                  // ── All partners header ───────────────────────────────────
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: ResponsiveUtils.getResponsiveSpacing(
                          context,
                          AppConstants.horizontalPadding,
                        ),
                        vertical: ResponsiveUtils.getResponsiveSpacing(
                          context,
                          12,
                        ),
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

                  // ── Partner list ──────────────────────────────────────────
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
                        if (i < visiblePartners.length) {
                          final partner = visiblePartners[i];
                          return _PartnerCard(
                            partner: partner,
                            isFavorite: favoritesState.favoriteIds.contains(
                              partner.id,
                            ),
                            onToggleFavorite: () =>
                                _handleFavoriteToggle(partner),
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => PartnerDetailsScreen(
                                    partnerId: partner.id,
                                    initialPartner: partner,
                                  ),
                                ),
                              );
                            },
                          );
                        }
                        return null;
                      }, childCount: visiblePartners.length),
                    ),

                  // ── Load more indicator ───────────────────────────────────
                  SliverToBoxAdapter(child: _LoadMoreIndicator(state: state)),

                  const SliverToBoxAdapter(child: SizedBox(height: 90)),
                ],
              ),
            ),
          ),

          // ── Scroll to top FAB ─────────────────────────────────────────────
          Positioned(
            right: 16,
            bottom: 22,
            child: AnimatedSlide(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOut,
              offset: _showScrollToTop ? Offset.zero : const Offset(0, 1.5),
              child: AnimatedOpacity(
                duration: const Duration(milliseconds: 220),
                opacity: _showScrollToTop ? 1 : 0,
                child: ElevatedButton.icon(
                  onPressed: _scrollToTop,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.black,
                    foregroundColor: Colors.white,
                    elevation: 5,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24),
                    ),
                  ),
                  icon: const Icon(Icons.keyboard_arrow_up_rounded, size: 18),
                  label: Text(l10n.translate('back_to_top')),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── App Bar (SliverPersistentHeaderDelegate) ─────────────────────────────────

class _NearbyAppBar extends SliverPersistentHeaderDelegate {
  final double topPadding;
  final String addressLabel;
  final String categoryLabel;
  final int activeFilterCount;
  final int favoritesCount;
  final AppLocalizations l10n;
  final VoidCallback onSearchTap;
  final VoidCallback onFilterTap;
  final VoidCallback onFavoritesTap;
  final VoidCallback onBackTap;

  const _NearbyAppBar({
    required this.topPadding,
    required this.addressLabel,
    required this.categoryLabel,
    required this.activeFilterCount,
    required this.favoritesCount,
    required this.l10n,
    required this.onSearchTap,
    required this.onFilterTap,
    required this.onFavoritesTap,
    required this.onBackTap,
  });

  // Expanded: top padding + address row (56) + category label (28) + search row (54) + gaps
  @override
  double get maxExtent => topPadding + 152.0;

  // Collapsed: top padding + standard toolbar height
  @override
  double get minExtent => topPadding + kToolbarHeight + 8;

  @override
  bool shouldRebuild(_NearbyAppBar old) =>
      old.topPadding != topPadding ||
      old.addressLabel != addressLabel ||
      old.categoryLabel != categoryLabel ||
      old.activeFilterCount != activeFilterCount ||
      old.favoritesCount != favoritesCount;

  @override
  Widget build(
    BuildContext context,
    double shrinkOffset,
    bool overlapsContent,
  ) {
    final scrollRange = maxExtent - minExtent;
    final progress = (shrinkOffset / scrollRange).clamp(0.0, 1.0);

    // Expanded elements fade out in the first 50% of scroll
    final expandedOpacity = (1.0 - progress * 2.2).clamp(0.0, 1.0);
    // Collapsed search bar fades in after 55% scroll
    final collapsedOpacity = ((progress - 0.55) / 0.45).clamp(0.0, 1.0);

    return Container(
      color: Colors.white,
      // Subtle bottom border appears as we scroll
      foregroundDecoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: AppColors.border.withValues(alpha: progress.clamp(0.0, 1.0)),
            width: 0.5,
          ),
        ),
      ),
      padding: EdgeInsets.fromLTRB(16, topPadding + 8, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Row 1: Back | Address (expanded) ↔ Search (collapsed) | Filter ─
          SizedBox(
            height: kToolbarHeight - 8,
            child: Row(
              children: [
                // Back button — always visible
                GestureDetector(
                  onTap: onBackTap,
                  child: Container( 
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      shape: BoxShape.circle,
                      border: Border.all(color: AppColors.border),
                    ),
                    child: const Icon(
                      Icons.arrow_back_ios_new,
                      color: AppColors.textPrimary,
                      size: 16,
                    ),
                  ),
                ),
                const SizedBox(width: 10),

                // Center area: address pill ↔ inline search bar
                Expanded(
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // Address label — visible when expanded
                      Opacity(
                        opacity: expandedOpacity,
                        child: IgnorePointer(
                          ignoring: progress > 0.25,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.location_on,
                                color: AppColors.primary,
                                size: 15,
                              ),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  addressLabel,
                                  style: const TextStyle(
                                    color: AppColors.textPrimary,
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),

                      // Inline search bar — visible when collapsed
                      Opacity(
                        opacity: collapsedOpacity,
                        child: IgnorePointer(
                          ignoring: progress < 0.7,
                          child: GestureDetector(
                            onTap: onSearchTap,
                            child: Container(
                              height: 40,
                              decoration: BoxDecoration(
                                color: AppColors.background,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                              ),
                              alignment: Alignment.centerLeft,
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.search_rounded,
                                    color: AppColors.textSecondary,
                                    size: 18,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      l10n.translate('search_partners_hint'),
                                      style: const TextStyle(
                                        color: AppColors.textSecondary,
                                        fontSize: 13,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(width: 10),

                // Filter icon button — visible only when collapsed search is visible.
                Opacity(
                  opacity: collapsedOpacity,
                  child: IgnorePointer(
                    ignoring: progress < 0.7,
                    child: Stack(
                      clipBehavior: Clip.none,
                      children: [
                        GestureDetector(
                          onTap: onFilterTap,
                          child: Container(
                            width: 36,
                            height: 36,
                            decoration: BoxDecoration(
                              color: AppColors.background,
                              shape: BoxShape.circle,
                              border: Border.all(color: AppColors.border),
                            ),
                            child: const Icon(
                              Icons.tune_rounded,
                              color: AppColors.textPrimary,
                              size: 18,
                            ),
                          ),
                        ),
                        if (activeFilterCount > 0)
                          Positioned(
                            top: -4,
                            right: -4,
                            child: Container(
                              width: 18,
                              height: 18,
                              decoration: const BoxDecoration(
                                color: Color(0xFFE1062C),
                                shape: BoxShape.circle,
                              ),
                              alignment: Alignment.center,
                              child: Text(
                                '$activeFilterCount',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 8),

                // Favorites icon button — always visible.
                Stack(
                  clipBehavior: Clip.none,
                  children: [
                    GestureDetector(
                      onTap: onFavoritesTap,
                      child: Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: AppColors.background,
                          shape: BoxShape.circle,
                          border: Border.all(color: AppColors.border),
                        ),
                        child: const Icon(
                          Icons.favorite_rounded,
                          color: AppColors.primary,
                          size: 18,
                        ),
                      ),
                    ),
                    if (favoritesCount > 0)
                      Positioned(
                        top: -5,
                        right: -5,
                        child: Container(
                          constraints: const BoxConstraints(minWidth: 18),
                          height: 18,
                          padding: const EdgeInsets.symmetric(horizontal: 4),
                          decoration: const BoxDecoration(
                            color: Color(0xFFE1062C),
                            shape: BoxShape.circle,
                          ),
                          alignment: Alignment.center,
                          child: Text(
                            favoritesCount > 99 ? '99+' : '$favoritesCount',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 9,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),

          // ── Row 2: Category label — shrinks away on collapse ──────────────
          ClipRect(
            child: SizedBox(
              height: 30.0 * (1.0 - progress),
              child: Opacity(
                opacity: expandedOpacity,
                child: Padding(
                  padding: const EdgeInsets.only(top: 4, left: 2),
                  child: Text(
                    categoryLabel,
                    style: const TextStyle(
                      color: AppColors.textPrimary,
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
            ),
          ),

          // ── Row 3: Search bar + Filter button — shrinks away on collapse ───
          ClipRect(
            child: SizedBox(
              height: 62.0 * (1.0 - progress),
              child: Opacity(
                opacity: expandedOpacity,
                child: IgnorePointer(
                  ignoring: progress > 0.2,
                  child: Padding(
                    padding: const EdgeInsets.only(top: 10, bottom: 8),
                    child: Row(
                      children: [
                        // Search bar
                        Expanded(
                          child: GestureDetector(
                            onTap: onSearchTap,
                            child: Container(
                              height: 44,
                              decoration: BoxDecoration(
                                color: AppColors.background,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                              ),
                              alignment: Alignment.centerLeft,
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.search_rounded,
                                    color: AppColors.textSecondary,
                                    size: 20,
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      l10n.translate('search_partners_hint'),
                                      style: const TextStyle(
                                        color: AppColors.textSecondary,
                                        fontSize: 14,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),

                        // Filter button next to expanded search bar (before scrolling)
                        Stack(
                          clipBehavior: Clip.none,
                          children: [
                            GestureDetector(
                              onTap: onFilterTap,
                              child: Container(
                                height: 44,
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 14,
                                ),
                                decoration: BoxDecoration(
                                  color: AppColors.softGrey,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const Icon(
                                      Icons.tune_rounded,
                                      color: AppColors.black,
                                      size: 18,
                                    ),
                                    const SizedBox(width: 6),
                                    Text(
                                      l10n.translate('filters'),
                                      style: const TextStyle(
                                        color: AppColors.black,
                                        fontSize: 14,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            if (activeFilterCount > 0)
                              Positioned(
                                top: -6,
                                right: -6,
                                child: Container(
                                  width: 20,
                                  height: 20,
                                  decoration: const BoxDecoration(
                                    color: Color(0xFFE1062C),
                                    shape: BoxShape.circle,
                                  ),
                                  alignment: Alignment.center,
                                  child: Text(
                                    '$activeFilterCount',
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 11,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Sub-categories Row ──────────────────────────────────────────────────────

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

  static bool _looksLikeImageRef(String? value) {
    if (value == null || value.trim().isEmpty) return false;
    final v = value.trim().toLowerCase();
    return v.startsWith('http://') ||
        v.startsWith('https://') ||
        v.startsWith('/uploads/') ||
        v.startsWith('uploads/') ||
        v.contains('/uploads/');
  }

  Widget _buildCategoryVisual(CategoryDto cat, bool isSelected, Color accent) {
    final candidate = cat.image?.trim().isNotEmpty == true
        ? cat.image
        : cat.icon;
    final imageUrl = _looksLikeImageRef(candidate)
        ? resolveMediaUrl(candidate)
        : '';

    if (imageUrl.isNotEmpty) {
      return ClipOval(
        child: CachedNetworkImage(
          imageUrl: imageUrl,
          width: 56,
          height: 56,
          fit: BoxFit.cover,
          placeholder: (_, __) => Icon(
            _iconFor(cat),
            size: 26,
            color: isSelected ? accent : AppColors.textSecondary,
          ),
          errorWidget: (_, __, ___) => Icon(
            _iconFor(cat),
            size: 26,
            color: isSelected ? accent : AppColors.textSecondary,
          ),
        ),
      );
    }

    return Icon(
      _iconFor(cat),
      size: 26,
      color: isSelected ? accent : AppColors.textSecondary,
    );
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
                        child: _buildCategoryVisual(cat, isSelected, accent),
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

class _FavoritePulseButton extends StatefulWidget {
  final bool isFavorite;
  final VoidCallback? onTap;

  const _FavoritePulseButton({required this.isFavorite, required this.onTap});

  @override
  State<_FavoritePulseButton> createState() => _FavoritePulseButtonState();
}

class _FavoritePulseButtonState extends State<_FavoritePulseButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _handleTap() {
    _controller.forward(from: 0);
    widget.onTap?.call();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final t = Curves.easeOut.transform(_controller.value);
        final scale = 1 + math.sin(t * math.pi) * 0.18;
        return Transform.scale(scale: scale, child: child);
      },
      child: Material(
        color: Colors.white.withValues(alpha: 0.93),
        shape: const CircleBorder(),
        elevation: 2,
        child: InkWell(
          onTap: widget.onTap == null ? null : _handleTap,
          customBorder: const CircleBorder(),
          child: SizedBox(
            width: 34,
            height: 34,
            child: Icon(
              widget.isFavorite
                  ? Icons.favorite_rounded
                  : Icons.favorite_border_rounded,
              size: 18,
              color: widget.isFavorite
                  ? AppColors.primary
                  : AppColors.textSecondary,
            ),
          ),
        ),
      ),
    );
  }
}

// ─── Partner Card ─────────────────────────────────────────────────────────────

class _PartnerCard extends StatelessWidget {
  final PartnerNearbyDto partner;
  final bool isFavorite;
  final VoidCallback? onToggleFavorite;
  final VoidCallback? onTap;

  const _PartnerCard({
    required this.partner,
    this.isFavorite = false,
    this.onToggleFavorite,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final closed = !partner.isOpen;

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
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
          onTap: onTap,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(AppConstants.borderRadiusLarge),
            child: Ink(
              decoration: BoxDecoration(
                color: closed ? const Color(0xFFF7F7F7) : Colors.white,
                borderRadius: BorderRadius.circular(
                  AppConstants.borderRadiusLarge,
                ),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.shadow.withValues(
                      alpha: closed ? 0.04 : 0.07,
                    ),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Stack(
                    children: [
                      _PartnerCoverImage(
                        coverUrl: partner.coverImage,
                        height: 140,
                      ),
                      if (closed)
                        Positioned.fill(
                          child: Container(
                            color: Colors.black.withValues(alpha: 0.42),
                          ),
                        ),
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
                      Positioned(
                        top: 10,
                        right: 10,
                        child: _FavoritePulseButton(
                          isFavorite: isFavorite,
                          onTap: onToggleFavorite,
                        ),
                      ),
                      if (closed)
                        Positioned.fill(
                          child: Center(
                            child: _Badge(
                              label: l10n.translate('closed_badge'),
                              color: AppColors.error,
                            ),
                          ),
                        ),
                    ],
                  ),
                  Opacity(
                    opacity: closed ? 0.65 : 1.0,
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _PartnerLogo(logoUrl: partner.logo, size: 50),
                          const SizedBox(width: 12),
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
                                    _InfoChip(
                                      icon: Icons.star_rounded,
                                      label: partner.rating.toStringAsFixed(1),
                                      iconColor: AppColors.starYellow,
                                    ),
                                    if (partner.preparationTime != null)
                                      _InfoChip(
                                        icon: Icons.access_time,
                                        label: '${partner.preparationTime} min',
                                        iconColor: AppColors.primary,
                                      ),
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
          ),
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

// ─── Shimmer ─────────────────────────────────────────────────────────────────

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
    return const SizedBox.shrink();
  }
}

// ─── Shared helper widgets ───────────────────────────────────────────────────

class _Badge extends StatelessWidget {
  final String label;
  final Color color;

  const _Badge({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
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
    final resolved = resolveMediaUrl(coverUrl);
    if (resolved.isNotEmpty) {
      return CachedNetworkImage(
        imageUrl: resolved,
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
    final resolved = resolveMediaUrl(logoUrl);
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
        child: resolved.isNotEmpty
            ? CachedNetworkImage(
                imageUrl: resolved,
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
