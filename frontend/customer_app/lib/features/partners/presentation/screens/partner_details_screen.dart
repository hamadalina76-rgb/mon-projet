import 'dart:math' as math;

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../config/routes/route_names.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/delivery_zone_utils.dart';
import '../../../../core/utils/media_url.dart';
import '../../../cart/cart_providers.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../menu/presentation/screens/product_detail_screen.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../../data/datasources/partner_api_service.dart';
import '../../data/models/partner_nearby_dto.dart';
import 'partner_establishment_info_screen.dart';
import '../providers/favorite_partners_provider.dart';
import '../providers/nearby_partners_provider.dart';

// ─────────────────────────────────────────────────────────────────────────────
// DESIGN CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────
const double _kLogoSize = 84.0;
const double _kCoverHeight = 160.0;
const double _kLogoBorderRadius = 40.0;
const double _kHeaderBaseHeight = 158.0;
const double _kHeaderPromoExtraHeight = 36.0;
const double _kHeaderOutOfZoneExtraHeight = 78.0;

final Set<String> _failedMediaUrls = <String>{};

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────
class PartnerDetailsScreen extends ConsumerStatefulWidget {
  final String partnerId;
  final PartnerNearbyDto? initialPartner;

  const PartnerDetailsScreen({
    super.key,
    required this.partnerId,
    this.initialPartner,
  });

  @override
  ConsumerState<PartnerDetailsScreen> createState() =>
      _PartnerDetailsScreenState();
}

class _PartnerDetailsScreenState extends ConsumerState<PartnerDetailsScreen>
    with TickerProviderStateMixin {
  late final AnimationController _favPulseController;
  late final ScrollController _contentScrollController;
  TabController? _tabController;
  final List<GlobalKey> _sectionKeys = [];

  late Future<_PartnerDetailsData> _future;
  bool _isFavorite = false;
  bool _favoriteReady = false;
  bool _isProgrammaticScroll = false;
  final Set<String> _favoriteProductIds = <String>{};

  ThemeData _screenLightTheme(BuildContext context) {
    final base = ThemeData.light(useMaterial3: true);
    return base.copyWith(
      brightness: Brightness.light,
      scaffoldBackgroundColor: AppColors.surface,
      canvasColor: AppColors.surface,
      colorScheme: const ColorScheme.light(
        primary: AppColors.primary,
        onPrimary: Colors.white,
        surface: Colors.white,
        onSurface: Colors.black,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: Colors.black,
        displayColor: Colors.black,
      ),
      iconTheme: const IconThemeData(color: Colors.black),
    );
  }

  @override
  void initState() {
    super.initState();
    _favPulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 260),
    );
    _contentScrollController = ScrollController()
      ..addListener(_handleContentScroll);
    _future = _load();
  }

  @override
  void dispose() {
    _contentScrollController.removeListener(_handleContentScroll);
    _contentScrollController.dispose();
    _tabController?.dispose();
    _favPulseController.dispose();
    super.dispose();
  }

  void _syncTabControllerLength(int length) {
    if (_tabController?.length == length) return;
    final previousIndex = _tabController?.index ?? 0;
    _tabController?.dispose();
    _tabController = TabController(
      length: length,
      vsync: this,
      initialIndex: previousIndex.clamp(0, length - 1),
    );
  }

  void _syncSectionKeys(int length) {
    if (_sectionKeys.length == length) return;
    if (_sectionKeys.length < length) {
      for (int i = _sectionKeys.length; i < length; i++) {
        _sectionKeys.add(GlobalKey());
      }
      return;
    }
    _sectionKeys.removeRange(length, _sectionKeys.length);
  }

  Future<_PartnerDetailsData> _load() async {
    final api = ref.read(partnerApiServiceProvider);
    final userId = _currentUserId();

    final partnerFuture = api.fetchPartnerById(widget.partnerId);
    final menuFuture = api.fetchPartnerMenu(widget.partnerId);

    final partner = await partnerFuture;
    final menu = await menuFuture;

    bool favorite = false;
    if (userId != null) {
      try {
        final favIds = await api.fetchFavoritePartnerIds(userId);
        favorite =
            favIds.contains(widget.partnerId) || favIds.contains(partner.id);
      } catch (_) {
        favorite = false;
      }
    }

    if (mounted) {
      setState(() {
        _favoriteReady = true;
        _isFavorite = favorite;
      });
    }

    return _PartnerDetailsData(
      partner: partner,
      menuSections: menu,
      isFavorite: favorite,
    );
  }

  String? _currentUserId() {
    final auth = ref.read(authNotifierProvider);
    return auth.whenOrNull(authenticated: (u) => u.id);
  }

  Future<void> _toggleFavorite(PartnerNearbyDto partner) async {
    await _favPulseController.forward(from: 0);
    if (!mounted) return;

    final l10n = AppLocalizations.of(context);

    final customerId = _currentUserId();
    if (customerId == null) {
      _toast(l10n.translate('partner_details_login_to_favorite'));
      return;
    }

    final next = !_isFavorite;
    setState(() => _isFavorite = next);

    final api = ref.read(partnerApiServiceProvider);
    try {
      if (next) {
        await api.addFavorite(customerId, partner.id);
        _toast(l10n.translate('partner_details_favorite_added'));
      } else {
        await api.removeFavorite(customerId, partner.id);
        _toast(l10n.translate('partner_details_favorite_removed'));
      }
      await ref.read(favoritePartnersNotifierProvider.notifier).loadFavorites();
    } catch (_) {
      if (!mounted) return;
      setState(() => _isFavorite = !next);
      _toast(l10n.translate('partner_details_favorite_update_failed'));
    }
  }

  void _toggleProductFavorite(String productId) {
    setState(() {
      if (!_favoriteProductIds.add(productId)) {
        _favoriteProductIds.remove(productId);
      }
    });
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

  Future<void> _openProductDetails(
    MenuProductDto product, {
    String? categoryName,
    required PartnerNearbyDto partner,
  }) async {
    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;
    final outOfZone = _isOutOfZone(
      partner,
      userLat: zoneUserLat,
      userLng: zoneUserLng,
    );
    if (outOfZone) {
      final l10n = AppLocalizations.of(context);
      _toast(l10n.translate('partner_details_out_of_zone'));
      return;
    }

    final result = await Navigator.of(context).push<ProductSelectionResult>(
      MaterialPageRoute<ProductSelectionResult>(
        builder: (_) => ProductDetailScreen(
          partnerId: widget.partnerId,
          initialProduct: product,
          categoryName: categoryName,
        ),
      ),
    );

    if (!mounted || result == null) return;

    final selectedOptions = result.selections
        .expand((group) {
          final translatedGroup = _translateMenuLabel(
            group.groupName,
            const ['menu_option_group', 'menu_label'],
          );

          return group.options.map(
            (option) => CartItemSelectedOption(
              optionId: group.groupId,
              optionName: translatedGroup,
              valueId: option.id,
              valueName: _translateMenuLabel(
                option.name,
                const ['menu_option', 'menu_label'],
              ),
              priceModifier: option.priceModifier,
            ),
          );
        })
        .toList();

    final cartItem = CartItemModel(
      productId: result.productId,
      productImageUrl: resolveMediaUrl(product.imageUrl),
      partnerId: partner.id,
      partnerName: partner.displayName,
      partnerLogoUrl: partner.logo ?? '',
      productName: result.productName,
      unitPrice: result.unitPrice,
      quantity: result.quantity,
      selectedOptions: selectedOptions,
      kitchenNote: result.kitchenNote,
    );

    final added = await ref
        .read(cartNotifierProvider.notifier)
        .addItem(cartItem, context: context);

    if (!mounted || !added) return;

    final l10n = AppLocalizations.of(context);
    _toast(
      '${result.quantity}x ${result.productName} ${l10n.translate('partner_details_added_to_cart')} (${_money(result.totalPrice)})',
    );
  }

  void _toast(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(text),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
      ),
    );
  }

  bool _isOutOfZone(PartnerNearbyDto p, {double? userLat, double? userLng}) {
    return isOutsideDeliveryZone(
      deliveryRadius: p.deliveryRadius,
      distanceKm: p.distanceKm,
      userLat: userLat,
      userLng: userLng,
      partnerLat: p.latitude,
      partnerLng: p.longitude,
    );
  }

  String _money(double? value) {
    if (value == null) return '--';
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(3)} DT';
  }

  String? _lookupDynamicLabel(
    String normalized,
    List<String> prefixes,
    AppLocalizations l10n,
  ) {
    if (normalized.isEmpty) return null;
    for (final prefix in prefixes) {
      final key = '${prefix}_$normalized';
      final translated = l10n.translate(key);
      if (translated != key) return translated;
    }
    return null;
  }

  String _normalizeLabelToKey(String value) {
    var normalized = value.trim().toLowerCase();
    if (normalized.isEmpty) return '';

    const accentMap = <String, String>{
      'à': 'a',
      'á': 'a',
      'â': 'a',
      'ä': 'a',
      'ã': 'a',
      'å': 'a',
      'ç': 'c',
      'è': 'e',
      'é': 'e',
      'ê': 'e',
      'ë': 'e',
      'ì': 'i',
      'í': 'i',
      'î': 'i',
      'ï': 'i',
      'ñ': 'n',
      'ò': 'o',
      'ó': 'o',
      'ô': 'o',
      'ö': 'o',
      'õ': 'o',
      'ù': 'u',
      'ú': 'u',
      'û': 'u',
      'ü': 'u',
      'ý': 'y',
      'ÿ': 'y',
    };

    accentMap.forEach((from, to) {
      normalized = normalized.replaceAll(from, to);
    });

    normalized = normalized.replaceAll('&', ' and ');
    normalized = normalized.replaceAll("'", '');
    normalized = normalized.replaceAll(RegExp(r'[^a-z0-9]+'), '_');
    normalized = normalized.replaceAll(RegExp(r'_+'), '_');
    normalized = normalized.replaceAll(RegExp(r'^_+|_+$'), '');
    return normalized;
  }

  String _translateMenuLabel(
    String value, [
    List<String> prefixes = const ['menu_label'],
  ]) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return value;

    final l10n = AppLocalizations.of(context);
    final direct = _lookupDynamicLabel(
      _normalizeLabelToKey(trimmed),
      prefixes,
      l10n,
    );
    if (direct != null) return direct;

    final words = trimmed.split(RegExp(r'\s+'));
    if (words.length <= 1) return trimmed;

    bool changed = false;
    final translatedWords = words.map((word) {
      final translated = _lookupDynamicLabel(
        _normalizeLabelToKey(word),
        prefixes,
        l10n,
      );
      if (translated != null) {
        changed = true;
        return translated;
      }
      return word;
    }).toList();

    return changed ? translatedWords.join(' ') : trimmed;
  }

  String _formatOpenLabel(PartnerNearbyDto partner) {
    final l10n = AppLocalizations.of(context);
    if (partner.isOpen) return l10n.translate('partner_status_open');
    final info = partner.nextOpenInfo;
    if (info == null) return l10n.translate('partner_status_closed');
    final formattedTime = info.time.replaceFirst(':', 'h');
    if (info.opensToday) {
      return '${l10n.translate('partner_details_opens_today_at')} $formattedTime';
    }
    final nextDay = _nextOpeningDayLabel(partner);
    if (nextDay == null) {
      return '${l10n.translate('partner_details_opens_at')} $formattedTime';
    }
    return '${l10n.translate('partner_details_opens')} $nextDay ${l10n.translate('partner_details_at')} $formattedTime';
  }

  String? _nextOpeningDayLabel(PartnerNearbyDto partner) {
    if (partner.openingHours.isEmpty) return null;
    final l10n = AppLocalizations.of(context);
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];
    final dayLabels = {
      'MONDAY': l10n.translate('day_monday'),
      'TUESDAY': l10n.translate('day_tuesday'),
      'WEDNESDAY': l10n.translate('day_wednesday'),
      'THURSDAY': l10n.translate('day_thursday'),
      'FRIDAY': l10n.translate('day_friday'),
      'SATURDAY': l10n.translate('day_saturday'),
      'SUNDAY': l10n.translate('day_sunday'),
    };
    final now = DateTime.now();
    for (int d = 1; d <= 7; d++) {
      final nextDay = DateTime(now.year, now.month, now.day + d);
      final key = dayNames[nextDay.weekday - 1];
      final hasOpening = partner.openingHours.any(
        (h) =>
            h.dayOfWeek.toUpperCase() == key &&
            !h.isClosed &&
            h.openTime != null,
      );
      if (hasOpening) return dayLabels[key] ?? key;
    }
    return null;
  }

  double _sectionOffset(int index) {
    if (index < 0 || index >= _sectionKeys.length) return 0;
    final context = _sectionKeys[index].currentContext;
    if (context == null) return 0;
    final renderObject = context.findRenderObject();
    if (renderObject == null) return 0;
    final viewport = RenderAbstractViewport.of(renderObject);
    return viewport.getOffsetToReveal(renderObject, 0).offset;
  }

  Future<void> _scrollToSection(int index) async {
    if (index < 0 ||
        index >= _sectionKeys.length ||
        !_contentScrollController.hasClients)
      return;
    final target = _sectionOffset(index);
    _isProgrammaticScroll = true;
    try {
      await _contentScrollController.animateTo(
        target,
        duration: const Duration(milliseconds: 320),
        curve: Curves.easeOutCubic,
      );
    } finally {
      _isProgrammaticScroll = false;
    }
  }

  void _handleContentScroll() {
    final controller = _tabController;
    if (controller == null ||
        _isProgrammaticScroll ||
        !_contentScrollController.hasClients)
      return;
    const activationInset = 20.0;
    final currentOffset =
        _contentScrollController.position.pixels + activationInset;
    int activeIndex = 0;
    for (int i = 0; i < _sectionKeys.length; i++) {
      final sectionTop = _sectionOffset(i);
      if (currentOffset >= sectionTop) {
        activeIndex = i;
      } else {
        break;
      }
    }
    if (activeIndex != controller.index) {
      controller.animateTo(
        activeIndex,
        duration: const Duration(milliseconds: 120),
        curve: Curves.easeOut,
      );
    }
  }

  void _openOptionsSheet(PartnerNearbyDto partner) {
    final l10n = AppLocalizations.of(context);
    final lightTheme = _screenLightTheme(context);
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (ctx) {
        return Theme(
          data: lightTheme,
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 42,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 18),
                      decoration: BoxDecoration(
                        color: AppColors.border,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  Text(
                    l10n.translate('partner_details_options_title'),
                    style: const TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                      color: Colors.black,
                    ),
                  ),
                  const SizedBox(height: 20),
                  _optionTile(
                    icon: Icons.info_outline,
                    label: l10n.translate(
                      'partner_details_option_establishment_info',
                    ),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      Navigator.of(context).push(
                        MaterialPageRoute<void>(
                          builder: (_) =>
                              PartnerEstablishmentInfoScreen(partner: partner),
                        ),
                      );
                    },
                  ),
                  _optionTile(
                    icon: Icons.toll_outlined,
                    label: l10n.translate('partner_details_option_fees_info'),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      _toast(l10n.translate('partner_details_fees_info_toast'));
                    },
                  ),
                  _optionTile(
                    icon: Icons.share_outlined,
                    label: l10n.translate(
                      'partner_details_option_share_establishment',
                    ),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      _toast(l10n.translate('partner_details_share_toast'));
                    },
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _optionTile({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(icon, color: Colors.black),
      title: Text(
        label,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w700,
          color: Colors.black,
        ),
      ),
      onTap: onTap,
    );
  }

  // ─────────────────────────────────────────────────────────────────────────
  // BUILD
  // ─────────────────────────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final lightTheme = _screenLightTheme(context);
    ref.watch(addressNotifierProvider);
    ref.watch(locationNotifierProvider);
    final cartState = ref.watch(cartNotifierProvider);

    final partnerCartItems = cartState.items
        .where((item) => item.partnerId == widget.partnerId)
        .toList();
    final partnerItemCount = partnerCartItems.fold<int>(
      0,
      (sum, item) => sum + item.quantity,
    );
    final partnerSubtotal = partnerCartItems.fold<double>(
      0,
      (sum, item) => sum + item.lineTotal,
    );
    final showPartnerCheckoutBar = partnerCartItems.isNotEmpty;

    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;

    return Theme(
      data: lightTheme,
      child: Scaffold(
        backgroundColor: AppColors.surface,
        bottomNavigationBar: showPartnerCheckoutBar
            ? SafeArea(
                minimum: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.black,
                    borderRadius: BorderRadius.circular(18),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.20),
                        blurRadius: 16,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '$partnerItemCount ${partnerItemCount > 1 ? l10n.translate('items_plural') : l10n.translate('item_singular')}',
                              style: const TextStyle(
                                color: Colors.white70,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              _money(partnerSubtotal),
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                fontSize: 20,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 10),
                      SizedBox(
                        height: 44,
                        child: ElevatedButton.icon(
                          onPressed: () => context.push(RouteNames.cart),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.primary,
                            foregroundColor: AppColors.surface,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                            elevation: 0,
                          ),
                          icon: const Icon(Icons.payment_rounded, size: 18),
                          label: Text(
                            l10n.translate('pay_action'),
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              )
            : null,
        body: FutureBuilder<_PartnerDetailsData>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError || !snapshot.hasData) {
              return Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.error_outline,
                      size: 48,
                      color: AppColors.textSecondary,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      l10n.translate('partner_details_load_error'),
                      style: const TextStyle(color: AppColors.textSecondary),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: () => setState(() => _future = _load()),
                      child: Text(l10n.translate('retry')),
                    ),
                  ],
                ),
              );
            }

            final data = snapshot.data!;
            final partner = data.partner;
            final outOfZone = _isOutOfZone(
              partner,
              userLat: zoneUserLat,
              userLng: zoneUserLng,
            );
            final hasPromo = data.menuSections
                .expand((s) => s.products)
                .any((p) => p.hasDiscount);
            final categorySections = data.menuSections
                .where((s) => s.products.isNotEmpty)
                .toList();
            final tabCount = categorySections.isEmpty
                ? 1
                : categorySections.length;
            _syncTabControllerLength(tabCount);
            _syncSectionKeys(categorySections.length);
            final tabController = _tabController;
            if (tabController == null)
              return const Center(child: CircularProgressIndicator());
            final headerInfoHeight =
                _kHeaderBaseHeight +
                (hasPromo ? _kHeaderPromoExtraHeight : 0) +
                (outOfZone ? _kHeaderOutOfZoneExtraHeight : 0);
            final expandedSliverHeight = _kCoverHeight + headerInfoHeight;
            final headerTop = _kCoverHeight - 14;
            final logoTop = _kCoverHeight - (_kLogoSize * 0.42);
            if (!_favoriteReady) {
              _isFavorite = data.isFavorite;
              _favoriteReady = true;
            }

            return CustomScrollView(
              controller: _contentScrollController,
              slivers: [
                // ── APP BAR WITH COVER ──────────────────────────────────────
                SliverLayoutBuilder(
                  builder: (context, constraints) {
                    final showCollapsedTitle =
                        constraints.scrollOffset >
                        (expandedSliverHeight - kToolbarHeight - 12);
                    final showClosedLabelInAppBar =
                        showCollapsedTitle && !partner.isOpen;
                    return SliverAppBar(
                      pinned: true,
                      stretch: true,
                      expandedHeight: expandedSliverHeight,
                      toolbarHeight: showClosedLabelInAppBar
                          ? 72
                          : kToolbarHeight,
                      clipBehavior: Clip.none,
                      backgroundColor: AppColors.surface,
                      foregroundColor: AppColors.black,
                      surfaceTintColor: AppColors.surface,
                      shadowColor: AppColors.black.withValues(alpha: 0.08),
                      automaticallyImplyLeading: false,
                      centerTitle: true,
                      title: AnimatedOpacity(
                        opacity: showCollapsedTitle ? 1 : 0,
                        duration: const Duration(milliseconds: 180),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              partner.displayName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w700,
                                color: AppColors.black,
                              ),
                            ),
                            if (!partner.isOpen)
                              Text(
                                _formatOpenLabel(partner),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.error,
                                ),
                              ),
                          ],
                        ),
                      ),
                      leadingWidth: 62,
                      leading: Padding(
                        padding: const EdgeInsets.only(left: 14),
                        child: _circleIcon(
                          icon: Icons.arrow_back_ios_new_rounded,
                          size: 36,
                          iconSize: 16,
                          onTap: () => Navigator.of(context).pop(),
                        ),
                      ),
                      actions: [
                        _circleIcon(icon: Icons.search, onTap: () {}),
                        const SizedBox(width: 6),
                        AnimatedBuilder(
                          animation: _favPulseController,
                          builder: (context, child) {
                            final t = Curves.easeOut.transform(
                              _favPulseController.value,
                            );
                            final scale = 1 + math.sin(t * math.pi) * 0.18;
                            return Transform.scale(scale: scale, child: child);
                          },
                          child: _circleIcon(
                            icon: _isFavorite
                                ? Icons.favorite
                                : Icons.favorite_border,
                            iconColor: _isFavorite
                                ? AppColors.primary
                                : AppColors.black,
                            onTap: () => _toggleFavorite(partner),
                          ),
                        ),
                        if (!showCollapsedTitle) ...[
                          const SizedBox(width: 6),
                          _circleIcon(
                            icon: Icons.more_horiz,
                            onTap: () => _openOptionsSheet(partner),
                          ),
                        ],
                        const SizedBox(width: 12),
                      ],
                      flexibleSpace: FlexibleSpaceBar(
                        collapseMode: CollapseMode.none,
                        background: Stack(
                          clipBehavior: Clip.none,
                          children: [
                            Positioned(
                              top: 0,
                              left: 0,
                              right: 0,
                              height: _kCoverHeight,
                              child: _cover(partner.coverImage),
                            ),
                            // Gradient overlay — stronger at top for icon legibility
                            Positioned(
                              top: 0,
                              left: 0,
                              right: 0,
                              height: _kCoverHeight,
                              child: Container(
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    begin: Alignment.topCenter,
                                    end: Alignment.bottomCenter,
                                    stops: const [0.0, 0.45, 1.0],
                                    colors: [
                                      Colors.black.withValues(alpha: 0.36),
                                      Colors.transparent,
                                      Colors.black.withValues(alpha: 0.52),
                                    ],
                                  ),
                                ),
                              ),
                            ),
                            Positioned(
                              top: headerTop,
                              left: 0,
                              right: 0,
                              bottom: 0,
                              child: _PartnerHeaderSection(
                                partner: partner,
                                outOfZone: outOfZone,
                                hasPromo: hasPromo,
                                money: _money,
                                formatOpenLabel: _formatOpenLabel,
                              ),
                            ),
                            Positioned(
                              top: logoTop,
                              left: 20,
                              child: _LogoBox(url: partner.logo),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),

                // ── STICKY TABS ─────────────────────────────────────────────
                SliverPersistentHeader(
                  pinned: true,
                  delegate: _TabsHeaderDelegate(
                    TabBar(
                      controller: tabController,
                      labelColor: AppColors.black,
                      unselectedLabelColor: AppColors.textSecondary,
                      labelStyle: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 15,
                      ),
                      unselectedLabelStyle: const TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 15,
                      ),
                      indicatorColor: AppColors.primary,
                      indicatorWeight: 2.5,
                      dividerColor: AppColors.secondaryGrey,
                      isScrollable: true,
                      tabAlignment: TabAlignment.start,
                      onTap: categorySections.isEmpty
                          ? (_) {}
                          : _scrollToSection,
                      tabs: categorySections.isEmpty
                          ? [
                              Tab(
                                text: l10n.translate(
                                  'partner_details_menu_unavailable',
                                ),
                              ),
                            ]
                          : categorySections
                                .map(
                                  (s) => Tab(
                                    text: _translateMenuLabel(s.name, const [
                                      'menu_category',
                                      'menu_label',
                                    ]),
                                  ),
                                )
                                .toList(),
                    ),
                  ),
                ),

                // ── CONTENT SECTIONS ────────────────────────────────────────
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 40),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (categorySections.isEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 8, bottom: 20),
                            child: Text(
                              l10n.translate(
                                'partner_details_menu_unavailable',
                              ),
                              style: const TextStyle(
                                color: AppColors.textSecondary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        for (int i = 0; i < categorySections.length; i++)
                          _CategorySection(
                            key: _sectionKeys[i],
                            section: categorySections[i],
                            displaySectionName: _translateMenuLabel(
                              categorySections[i].name,
                              const ['menu_category', 'menu_label'],
                            ),
                            translateProductName: (raw) => _translateMenuLabel(
                              raw,
                              const ['menu_product', 'menu_label'],
                            ),
                            partnerRating: partner.rating,
                            favoriteProductIds: _favoriteProductIds,
                            onToggleFavorite: _toggleProductFavorite,
                            onSelectProduct: (product, categoryName) =>
                                _openProductDetails(
                                  product,
                                  categoryName: categoryName,
                                  partner: partner,
                                ),
                            onAddToCart: (product, categoryName) =>
                                _openProductDetails(
                                  product,
                                  categoryName: categoryName,
                                  partner: partner,
                                ),
                          ),
                      ],
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _cover(String? url) {
    final resolved = resolveMediaUrl(url);
    if (resolved.isEmpty || _failedMediaUrls.contains(resolved)) {
      return Container(
        color: const Color(0xFFE8E8E8),
        child: const Icon(
          Icons.storefront,
          size: 62,
          color: AppColors.textHint,
        ),
      );
    }
    return CachedNetworkImage(
      imageUrl: resolved,
      fit: BoxFit.cover,
      errorWidget: (_, __, ___) {
        _failedMediaUrls.add(resolved);
        return Container(
          color: const Color(0xFFE8E8E8),
          child: const Icon(
            Icons.storefront,
            size: 62,
            color: AppColors.textHint,
          ),
        );
      },
    );
  }

  Widget _circleIcon({
    required IconData icon,
    required VoidCallback onTap,
    Color iconColor = Colors.black,
    double size = 40,
    double iconSize = 20,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.90),
          shape: BoxShape.circle,
          border: Border.all(color: Colors.black.withValues(alpha: 0.06)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.10),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Icon(icon, color: iconColor, size: iconSize),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGO WIDGETS
// ─────────────────────────────────────────────────────────────────────────────
class _LogoBox extends StatelessWidget {
  final String? url;
  const _LogoBox({required this.url});

  @override
  Widget build(BuildContext context) {
    final resolved = resolveMediaUrl(url);
    if (resolved.isEmpty || _failedMediaUrls.contains(resolved)) {
      return Container(
        width: _kLogoSize,
        height: _kLogoSize,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(_kLogoBorderRadius),
          color: Colors.white,
          border: Border.all(color: Colors.white, width: 3),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.18),
              blurRadius: 16,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: const Icon(Icons.store, color: AppColors.textHint, size: 36),
      );
    }

    return Container(
      width: _kLogoSize,
      height: _kLogoSize,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(_kLogoBorderRadius),
        color: Colors.white,
        border: Border.all(color: Colors.white, width: 3),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.18),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: CachedNetworkImage(
        imageUrl: resolved,
        fit: BoxFit.cover,
        errorWidget: (_, __, ___) {
          _failedMediaUrls.add(resolved);
          return const Icon(Icons.store, color: AppColors.textHint, size: 36);
        },
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// PARTNER HEADER SECTION
// ─────────────────────────────────────────────────────────────────────────────
class _PartnerHeaderSection extends StatelessWidget {
  final PartnerNearbyDto partner;
  final bool outOfZone;
  final bool hasPromo;
  final String Function(double?) money;
  final String Function(PartnerNearbyDto) formatOpenLabel;

  const _PartnerHeaderSection({
    required this.partner,
    required this.outOfZone,
    required this.hasPromo,
    required this.money,
    required this.formatOpenLabel,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final openLabel = formatOpenLabel(partner);

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 2),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Keep a clear gap so title never collides with the overlapping logo.
              const SizedBox(width: _kLogoSize + 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      partner.displayName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: AppColors.black,
                        height: 1.25,
                      ),
                    ),
                    const SizedBox(height: 8),
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 240),
                      child: _OpenStateBadge(
                        label: openLabel,
                        isOpen: partner.isOpen,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // ── STATS ROW (like the reference image) ────────────────────────
          _StatsRow(partner: partner, money: money),

          if (hasPromo) ...[
            const SizedBox(height: 12),
            _StatusBadge(
              label: l10n.translate('partner_details_promo_available'),
              color: Color(0xFFD97706),
              backgroundColor: Color(0xFFFFFBEB),
              icon: Icons.local_offer_outlined,
            ),
          ],

          // ── OUT OF ZONE WARNING ──────────────────────────────────────────
          if (outOfZone)
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF2F2),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFFFD7D7)),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.location_off_outlined,
                    color: AppColors.error,
                    size: 18,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    l10n.translate('partner_details_out_of_zone'),
                    style: const TextStyle(
                      color: AppColors.error,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATS ROW  (thumb / clock / delivery)
// ─────────────────────────────────────────────────────────────────────────────
class _StatsRow extends StatelessWidget {
  final PartnerNearbyDto partner;
  final String Function(double?) money;

  const _StatsRow({required this.partner, required this.money});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isFreeDelivery =
        partner.deliveryFee == null || partner.deliveryFee == 0;
    final freeLabel = l10n.translate('free');

    return Row(
      children: [
        // Rating
        Expanded(
          child: _StatCell(
            icon: Icons.star_rounded,
            iconColor: AppColors.primary2,
            topText:
                '${(partner.rating * 100 / 5).toStringAsFixed(0)}% (${partner.totalRatings} ${l10n.translate('partner_details_reviews_count_suffix')})',
            bottomText: '',
            strikeBottom: false,
          ),
        ),
        _statDivider(),
        // Prep time
        if (partner.preparationTime != null) ...[
          Expanded(
            child: _StatCell(
              icon: Icons.access_time_rounded,
              iconColor: AppColors.primary2,
              topText:
                  '${partner.preparationTime} ${l10n.translate('product_detail_minutes_abbr')}',
              bottomText: '',
              strikeBottom: false,
            ),
          ),
          _statDivider(),
        ],
        // Delivery fee
        Expanded(
          child: _StatCell(
            icon: Icons.delivery_dining_outlined,
            iconColor: AppColors.primary2,
            topText: isFreeDelivery ? '' : money(partner.deliveryFee),
            strikeTop: !isFreeDelivery,
            bottomText: freeLabel,
            bottomColor: isFreeDelivery
                ? AppColors.black
                : AppColors.primary2.withValues(alpha: 0.80),
            bottomBold: true,
            strikeBottom: false,
          ),
        ),
      ],
    );
  }

  Widget _statDivider() => Container(
    width: 1,
    height: 44,
    color: AppColors.border,
    margin: const EdgeInsets.symmetric(horizontal: 4),
  );
}

class _StatCell extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String topText;
  final bool strikeTop;
  final String bottomText;
  final Color? bottomColor;
  final bool bottomBold;
  final bool strikeBottom;

  const _StatCell({
    required this.icon,
    required this.iconColor,
    required this.topText,
    this.strikeTop = false,
    required this.bottomText,
    this.bottomColor,
    this.bottomBold = false,
    required this.strikeBottom,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: iconColor.withValues(alpha: 0.10),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: iconColor, size: 20),
        ),
        const SizedBox(height: 3),
        if (topText.isNotEmpty)
          Text(
            topText,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
              decoration: strikeTop ? TextDecoration.lineThrough : null,
              decorationColor: AppColors.textSecondary,
            ),
          ),
        if (bottomText.isNotEmpty)
          Text(
            bottomText,
            style: TextStyle(
              fontSize: 12,
              fontWeight: bottomBold ? FontWeight.w800 : FontWeight.w500,
              color: bottomColor ?? AppColors.textSecondary,
              decoration: strikeBottom ? TextDecoration.lineThrough : null,
            ),
          ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATUS BADGE
// ─────────────────────────────────────────────────────────────────────────────
class _StatusBadge extends StatelessWidget {
  final String label;
  final Color color;
  final Color backgroundColor;
  final IconData icon;

  const _StatusBadge({
    required this.label,
    required this.color,
    required this.backgroundColor,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: color),
          const SizedBox(width: 5),
          Text(
            label,
            style: TextStyle(
              color: color,
              fontWeight: FontWeight.w700,
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }
}

class _OpenStateBadge extends StatelessWidget {
  final String label;
  final bool isOpen;

  const _OpenStateBadge({required this.label, required this.isOpen});

  @override
  Widget build(BuildContext context) {
    final color = isOpen ? const Color(0xFF15CA61) : AppColors.primary;
    final background = isOpen
        ? const Color(0xFFE8F8F0)
        : const Color(0xFFFFF0F0);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isOpen ? const Color(0xFFBFEBD4) : const Color(0xFFF6C9C9),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 20,
            height: 14,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.14),
              shape: BoxShape.circle,
            ),
            child: Icon(
              isOpen ? Icons.circle : Icons.access_time,
              size: 12,
              color: color,
            ),
          ),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              label,
              maxLines: 3,
              softWrap: true,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.w700,
                fontSize: 12,
                height: 1.15,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────
class _PartnerDetailsData {
  final PartnerNearbyDto partner;
  final List<PartnerMenuSectionDto> menuSections;
  final bool isFavorite;

  const _PartnerDetailsData({
    required this.partner,
    required this.menuSections,
    required this.isFavorite,
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// TABS HEADER DELEGATE
// ─────────────────────────────────────────────────────────────────────────────
class _TabsHeaderDelegate extends SliverPersistentHeaderDelegate {
  final TabBar tabBar;
  const _TabsHeaderDelegate(this.tabBar);

  @override
  double get minExtent => tabBar.preferredSize.height;
  @override
  double get maxExtent => tabBar.preferredSize.height;

  @override
  Widget build(
    BuildContext context,
    double shrinkOffset,
    bool overlapsContent,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: const Border(
          bottom: BorderSide(color: AppColors.secondaryGrey, width: 1),
        ),
        boxShadow: overlapsContent
            ? [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.04),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ]
            : null,
      ),
      child: tabBar,
    );
  }

  @override
  bool shouldRebuild(covariant _TabsHeaderDelegate oldDelegate) =>
      oldDelegate.tabBar != tabBar;
}

// ─────────────────────────────────────────────────────────────────────────────
// CATEGORY SECTION
// ─────────────────────────────────────────────────────────────────────────────
class _CategorySection extends StatelessWidget {
  final PartnerMenuSectionDto section;
  final String displaySectionName;
  final String Function(String) translateProductName;
  final double partnerRating;
  final Set<String> favoriteProductIds;
  final ValueChanged<String> onToggleFavorite;
  final void Function(MenuProductDto product, String categoryName)
  onSelectProduct;
  final void Function(MenuProductDto product, String categoryName) onAddToCart;

  const _CategorySection({
    super.key,
    required this.section,
    required this.displaySectionName,
    required this.translateProductName,
    required this.partnerRating,
    required this.favoriteProductIds,
    required this.onToggleFavorite,
    required this.onSelectProduct,
    required this.onAddToCart,
  });

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(3)} DT';
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final products = section.products;
    if (products.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 14),
        child: Text(
          l10n.translate('partner_details_menu_unavailable'),
          style: const TextStyle(color: AppColors.textSecondary),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: _CategoryWaveTitle(title: displaySectionName),
        ),
        const SizedBox(height: 8),
        LayoutBuilder(
          builder: (context, constraints) {
            final crossAxisCount = constraints.maxWidth >= 960 ? 3 : 2;
            final childAspectRatio = constraints.maxWidth >= 960
                ? 0.82
                : constraints.maxWidth >= 700
                ? 0.74
                : 0.68;

            return GridView.builder(
              itemCount: products.length,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: crossAxisCount,
                crossAxisSpacing: 14,
                mainAxisSpacing: 20,
                childAspectRatio: childAspectRatio,
              ),
              itemBuilder: (context, index) {
                final product = products[index];
                return _ProductCard(
                  product: product,
                  money: _money,
                  fallbackRating: partnerRating,
                  isFavorite: favoriteProductIds.contains(product.id),
                  onToggleFavorite: () => onToggleFavorite(product.id),
                  productName: translateProductName(product.name),
                  onOpenDetails: () =>
                      onSelectProduct(product, displaySectionName),
                  onAddToCart: () => onAddToCart(product, displaySectionName),
                );
              },
            );
          },
        ),
        const SizedBox(height: 20),
      ],
    );
  }
}

class _CategoryWaveTitle extends StatelessWidget {
  final String title;

  const _CategoryWaveTitle({required this.title});

  @override
  Widget build(BuildContext context) {
    return ClipPath(
      clipper: _WaveClipper(),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        color: AppColors.primary,
        alignment: Alignment.center,
        child: Text(
          title,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: AppColors.surface,
          ),
        ),
      ),
    );
  }
}

class _WaveClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    const wave = 6.0;
    final path = Path()..moveTo(0, wave);

    path.quadraticBezierTo(size.width * 0.12, 0, size.width * 0.24, wave);
    path.quadraticBezierTo(
      size.width * 0.36,
      wave * 2,
      size.width * 0.48,
      wave,
    );
    path.quadraticBezierTo(size.width * 0.60, 0, size.width * 0.72, wave);
    path.quadraticBezierTo(size.width * 0.84, wave * 2, size.width, wave);

    path.lineTo(size.width, size.height - wave);
    path.quadraticBezierTo(
      size.width * 0.88,
      size.height,
      size.width * 0.76,
      size.height - wave,
    );
    path.quadraticBezierTo(
      size.width * 0.64,
      size.height - (wave * 2),
      size.width * 0.52,
      size.height - wave,
    );
    path.quadraticBezierTo(
      size.width * 0.40,
      size.height,
      size.width * 0.28,
      size.height - wave,
    );
    path.quadraticBezierTo(
      size.width * 0.16,
      size.height - (wave * 2),
      0,
      size.height - wave,
    );
    path.close();

    return path;
  }

  @override
  bool shouldReclip(covariant CustomClipper<Path> oldClipper) => false;
}

class _ProductCard extends StatelessWidget {
  final MenuProductDto product;
  final String productName;
  final String Function(double) money;
  final double fallbackRating;
  final bool isFavorite;
  final VoidCallback onToggleFavorite;
  final VoidCallback onOpenDetails;
  final VoidCallback onAddToCart;

  const _ProductCard({
    required this.product,
    required this.productName,
    required this.money,
    required this.fallbackRating,
    required this.isFavorite,
    required this.onToggleFavorite,
    required this.onOpenDetails,
    required this.onAddToCart,
  });

  String _prepLabel(BuildContext context, int? minutes) {
    final minuteLabel = AppLocalizations.of(
      context,
    ).translate('product_detail_minutes_abbr');
    if (minutes == null || minutes <= 0) return '-- $minuteLabel';
    return '$minutes $minuteLabel';
  }

  @override
  Widget build(BuildContext context) {
    final image = resolveMediaUrl(product.imageUrl);
    final rating = (product.rating ?? fallbackRating).clamp(0.0, 5.0);
    final prep = product.preparationTime;

    return GestureDetector(
      onTap: onOpenDetails,
      behavior: HitTestBehavior.opaque,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 170;
          final imageSize = compact ? 80.0 : 92.0;
          final cardTopInset = compact ? 26.0 : 28.0;
          final cardBottomInset = compact ? 34.0 : 36.0;
          final horizontalPadding = compact ? 10.0 : 12.0;
          final heartButtonSize = compact ? 30.0 : 32.0;
          final cartButtonSize = compact ? 34.0 : 38.0;
          final nameFontSize = compact ? 16.0 : 17.0;
          final priceFontSize = compact ? 17.0 : 18.0;
          final oldPriceFontSize = compact ? 9.0 : 10.0;
          final ratingFontSize = compact ? 11.0 : 12.0;
          final prepFontSize = compact ? 11.0 : 12.0;
          final imageTopOffset = compact ? -12.0 : -14.0;

          return Stack(
            clipBehavior: Clip.none,
            children: [
              Positioned.fill(
                top: cardTopInset,
                bottom: cardBottomInset,
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(26),
                    border: Border.all(color: AppColors.border),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.06),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(
                      horizontalPadding,
                      imageSize * 0.72,
                      horizontalPadding,
                      8,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        SizedBox(
                          height: compact ? 34 : 38,
                          child: Text(
                            productName,
                            textAlign: TextAlign.center,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: nameFontSize,
                              fontWeight: FontWeight.w800,
                              color: AppColors.black,
                              height: 1.1,
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          money(product.price),
                          style: TextStyle(
                            fontSize: priceFontSize,
                            fontWeight: FontWeight.w900,
                            color: AppColors.primaryDark,
                          ),
                        ),
                        if (product.originalPrice != null &&
                            product.originalPrice! > product.price) ...[
                          const SizedBox(height: 1),
                          Text(
                            money(product.originalPrice!),
                            style: TextStyle(
                              fontSize: oldPriceFontSize,
                              color: AppColors.textSecondary,
                              decoration: TextDecoration.lineThrough,
                            ),
                          ),
                        ],
                        const SizedBox(height: 10),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.star_rounded,
                              size: compact ? 13 : 14,
                              color: AppColors.starYellow,
                            ),
                            const SizedBox(width: 3),
                            Text(
                              rating.toStringAsFixed(1),
                              style: TextStyle(
                                fontSize: ratingFontSize,
                                color: AppColors.textSecondary,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Icon(
                              Icons.schedule_rounded,
                              size: compact ? 12 : 13,
                              color: AppColors.secondary,
                            ),
                            const SizedBox(width: 3),
                            Text(
                              _prepLabel(context, prep),
                              style: TextStyle(
                                fontSize: prepFontSize,
                                color: AppColors.textSecondary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              Positioned(
                top: imageTopOffset,
                left: 0,
                right: 0,
                child: Center(
                  child: Container(
                    width: imageSize,
                    height: imageSize,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: const Color(0xFFF1F1F1),
                      border: Border.all(color: Colors.white, width: 3),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.black.withValues(alpha: 0.08),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: image.isEmpty
                        ? const Icon(Icons.fastfood, color: AppColors.textHint)
                        : Transform.scale(
                            scale: 1.14,
                            child: CachedNetworkImage(
                              imageUrl: image,
                              fit: BoxFit.cover,
                              width: imageSize,
                              height: imageSize,
                              errorWidget: (_, __, ___) => const Icon(
                                Icons.fastfood,
                                color: AppColors.textHint,
                              ),
                            ),
                          ),
                  ),
                ),
              ),
              Positioned(
                top: cardTopInset - 20,
                right: compact ? 25 : 10,
                child: Material(
                  color: AppColors.surface,
                  shape: const CircleBorder(),
                  elevation: 3,
                  child: InkWell(
                    onTap: onToggleFavorite,
                    customBorder: const CircleBorder(),
                    child: SizedBox(
                      width: heartButtonSize,
                      height: heartButtonSize,
                      child: Icon(
                        isFavorite
                            ? Icons.favorite_rounded
                            : Icons.favorite_border_rounded,
                        size: compact ? 17 : 18,
                        color: isFavorite
                            ? AppColors.primary
                            : AppColors.textSecondary,
                      ),
                    ),
                  ),
                ),
              ),
              Positioned(
                bottom: 14,
                left: 0,
                right: 0,
                child: Center(
                  child: Material(
                    color: AppColors.black,
                    shape: const CircleBorder(),
                    child: InkWell(
                      onTap: onAddToCart,
                      customBorder: const CircleBorder(),
                      child: SizedBox(
                        width: cartButtonSize,
                        height: cartButtonSize,
                        child: Icon(
                          Icons.add_shopping_cart_rounded,
                          size: compact ? 18 : 19,
                          color: AppColors.surface,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
