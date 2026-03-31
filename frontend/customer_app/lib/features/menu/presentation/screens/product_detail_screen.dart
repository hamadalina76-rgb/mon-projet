import 'dart:math' as math;

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/media_url.dart';
import '../../../partners/data/datasources/partner_api_service.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────
const double _kImageHeight = 300.0;
const double _kWaveHeight = 28.0;
const double _kAppBarHeight = 56.0;

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────
class ProductDetailScreen extends ConsumerStatefulWidget {
  final String partnerId;
  final MenuProductDto initialProduct;
  final String? categoryName;

  const ProductDetailScreen({
    super.key,
    required this.partnerId,
    required this.initialProduct,
    this.categoryName,
  });

  @override
  ConsumerState<ProductDetailScreen> createState() =>
      _ProductDetailScreenState();
}

class _ProductDetailScreenState extends ConsumerState<ProductDetailScreen>
    with SingleTickerProviderStateMixin {
  late MenuProductDto _product;
  List<MenuProductOptionGroupDto> _optionGroups = const [];
  final Map<String, Set<String>> _selectedOptionIds = {};

  bool _isLoading = true;
  String? _loadError;
  int _quantity = 1;
  bool _isFavorite = false;

  late final ScrollController _scrollController;
  late final AnimationController _titleFadeController;
  late final Animation<double> _titleFadeAnim;

  @override
  void initState() {
    super.initState();
    _product = widget.initialProduct;

    _scrollController = ScrollController()..addListener(_onScroll);
    _titleFadeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 200),
    );
    _titleFadeAnim = CurvedAnimation(
      parent: _titleFadeController,
      curve: Curves.easeOut,
    );

    _load();
  }

  void _onScroll() {
    // Title fades in once image has scrolled fully past the app bar
    final threshold = _kImageHeight - _kAppBarHeight - _kWaveHeight;
    if (_scrollController.offset >= threshold &&
        !_titleFadeController.isCompleted) {
      _titleFadeController.forward();
    } else if (_scrollController.offset < threshold &&
        !_titleFadeController.isDismissed) {
      _titleFadeController.reverse();
    }
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _titleFadeController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _loadError = null;
    });

    final api = ref.read(partnerApiServiceProvider);
    String? error;

    try {
      _product = await api.fetchMenuProductDetails(
        widget.partnerId,
        widget.initialProduct.id,
      );
    } catch (_) {
      error = _tr('product_detail_refresh_error');
      _product = widget.initialProduct;
    }

    try {
      _optionGroups = await api.fetchMenuProductOptionGroups(
        widget.partnerId,
        widget.initialProduct.id,
      );
      if (_optionGroups.isEmpty && _product.optionGroups.isNotEmpty) {
        _optionGroups = _product.optionGroups;
      }
    } catch (_) {
      _optionGroups = _product.optionGroups;
      error = error ?? _tr('product_detail_options_load_error');
    }

    _seedDefaultSelections();

    if (!mounted) return;
    setState(() {
      _isLoading = false;
      _loadError = error;
    });
  }

  String _tr(String key, [Map<String, String> params = const {}]) {
    var text = AppLocalizations.of(context).translate(key);
    params.forEach((name, value) {
      text = text.replaceAll('{$name}', value);
    });
    return text;
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

  String _translateDynamicLabel(
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

  String _selectionRuleLabel(MenuProductOptionGroupDto group) {
    if (group.isSingle) return _tr('product_detail_pick_one');
    if (group.minSelection > 0) {
      return _tr('product_detail_pick_range', {
        'min': group.minSelection.toString(),
        'max': group.maxSelection.toString(),
      });
    }
    return _tr('product_detail_up_to', {'max': group.maxSelection.toString()});
  }

  void _seedDefaultSelections() {
    _selectedOptionIds.clear();
    for (final group in _optionGroups) {
      final requiredMin = group.isRequired && group.minSelection == 0
          ? 1
          : group.minSelection;
      final available = group.options.where((o) => o.isAvailable).toList();
      final defaults = group.options
          .where((o) => o.isAvailable && o.isDefault)
          .map((o) => o.id)
          .toList();

      if (group.isSingle) {
        if (defaults.isNotEmpty) {
          _selectedOptionIds[group.id] = {defaults.first};
        } else if (requiredMin > 0 && available.isNotEmpty) {
          _selectedOptionIds[group.id] = {available.first.id};
        }
        continue;
      }

      final selected = defaults.take(group.maxSelection).toSet();
      if (selected.length < requiredMin) {
        for (final o in available) {
          if (selected.length >= requiredMin) break;
          selected.add(o.id);
        }
      }
      if (selected.isNotEmpty) _selectedOptionIds[group.id] = selected;
    }
  }

  void _changeQuantity(int delta) {
    final next = _quantity + delta;
    if (next < 1) return;
    setState(() => _quantity = next);
  }

  bool _isSelected(
    MenuProductOptionGroupDto group,
    MenuProductOptionDto option,
  ) => _selectedOptionIds[group.id]?.contains(option.id) ?? false;

  void _toggleOption(
    MenuProductOptionGroupDto group,
    MenuProductOptionDto option,
  ) {
    if (!option.isAvailable) return;
    final selected = <String>{...?_selectedOptionIds[group.id]};

    if (group.isSingle) {
      final canClear = !group.isRequired && group.minSelection == 0;
      if (selected.contains(option.id) && canClear) {
        selected.clear();
      } else {
        selected
          ..clear()
          ..add(option.id);
      }
      setState(() {
        if (selected.isEmpty)
          _selectedOptionIds.remove(group.id);
        else
          _selectedOptionIds[group.id] = selected;
      });
      return;
    }

    if (selected.contains(option.id)) {
      final requiredMin = group.isRequired && group.minSelection == 0
          ? 1
          : group.minSelection;
      if (selected.length <= requiredMin) {
        final groupName = _translateDynamicLabel(group.name, const [
          'menu_option_group',
          'menu_label',
        ]);
        _showMessage(
          _tr('product_detail_keep_min_options', {
            'min': requiredMin.toString(),
            'group': groupName,
          }),
        );
        return;
      }
      selected.remove(option.id);
      setState(() {
        if (selected.isEmpty)
          _selectedOptionIds.remove(group.id);
        else
          _selectedOptionIds[group.id] = selected;
      });
      return;
    }

    final availableMax = group.options.where((e) => e.isAvailable).length;
    final effectiveMax = availableMax < group.maxSelection
        ? availableMax
        : group.maxSelection;
    if (selected.length >= effectiveMax) {
      _showMessage(
        _tr('product_detail_select_up_to', {'max': effectiveMax.toString()}),
      );
      return;
    }
    selected.add(option.id);
    setState(() => _selectedOptionIds[group.id] = selected);
  }

  String? _validateSelections() {
    for (final group in _optionGroups) {
      final selectedCount = _selectedOptionIds[group.id]?.length ?? 0;
      final requiredMin = group.isRequired && group.minSelection == 0
          ? 1
          : group.minSelection;
      final groupName = _translateDynamicLabel(group.name, const [
        'menu_option_group',
        'menu_label',
      ]);
      if (selectedCount < requiredMin)
        return _tr('product_detail_select_at_least', {
          'min': requiredMin.toString(),
          'group': groupName,
        });
      if (selectedCount > group.maxSelection)
        return _tr('product_detail_select_at_most', {
          'max': group.maxSelection.toString(),
          'group': groupName,
        });
    }
    return null;
  }

  double get _selectedOptionsTotal {
    double total = 0;
    for (final group in _optionGroups) {
      final ids = _selectedOptionIds[group.id];
      if (ids == null || ids.isEmpty) continue;
      for (final option in group.options) {
        if (ids.contains(option.id)) total += option.priceModifier;
      }
    }
    return total;
  }

  double get _unitPrice => _product.price + _selectedOptionsTotal;
  double get _totalPrice => _unitPrice * _quantity;

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(3)} DT';
  }

  String get _resolvedCategoryName {
    final fromRoute = widget.categoryName?.trim() ?? '';
    if (fromRoute.isNotEmpty) return fromRoute;

    final fromProduct = _product.categoryName?.trim() ?? '';
    if (fromProduct.isNotEmpty) return fromProduct;

    return _tr('product_detail_fallback_category');
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.all(16),
      ),
    );
  }

  void _submit() {
    if (!_product.available) {
      _showMessage(_tr('product_detail_unavailable'));
      return;
    }
    final err = _validateSelections();
    if (err != null) {
      _showMessage(err);
      return;
    }

    final selectedGroups = _optionGroups
        .map((group) {
          final ids = _selectedOptionIds[group.id] ?? const <String>{};
          return ProductSelectionGroup(
            groupId: group.id,
            groupName: _translateDynamicLabel(group.name, const [
              'menu_option_group',
              'menu_label',
            ]),
            options: group.options.where((o) => ids.contains(o.id)).toList(),
          );
        })
        .where((g) => g.options.isNotEmpty)
        .toList();

    Navigator.of(context).pop(
      ProductSelectionResult(
        productId: _product.id,
        productName: _product.name,
        quantity: _quantity,
        unitPrice: _unitPrice,
        totalPrice: _totalPrice,
        selections: selectedGroups,
      ),
    );
  }

  // ─────────────────────────────────────────────────────────────────────────
  // BUILD
  // ─────────────────────────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final imageUrl = resolveMediaUrl(_product.imageUrl);
    final safeTop = MediaQuery.of(context).padding.top;
    final localizedProductName = _translateDynamicLabel(_product.name, const [
      'menu_product',
      'menu_label',
    ]);

    return Theme(
      data: ThemeData.light(useMaterial3: true).copyWith(
        scaffoldBackgroundColor: AppColors.surface,
        colorScheme: const ColorScheme.light(
          primary: AppColors.primary,
          onPrimary: Colors.white,
          surface: Colors.white,
          onSurface: AppColors.black,
        ),
      ),
      child: Scaffold(
        backgroundColor: AppColors.surface,
        // ── Floating top bar ────────────────────────────────────────────────
        extendBodyBehindAppBar: true,
        appBar: _FloatingAppBar(
          safeTop: safeTop,
          productName: localizedProductName,
          titleAnim: _titleFadeAnim,
          isFavorite: _isFavorite,
          onBack: () => Navigator.of(context).pop(),
          onFavorite: () => setState(() => _isFavorite = !_isFavorite),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _buildScrollableBody(l10n, imageUrl),
        bottomNavigationBar: _isLoading
            ? null
            : _BottomBar(
                quantity: _quantity,
                totalPrice: _totalPrice,
                money: _money,
                available: _product.available,
                l10n: l10n,
                onMinus: () => _changeQuantity(-1),
                onPlus: () => _changeQuantity(1),
                onSubmit: _submit,
              ),
      ),
    );
  }

  Widget _buildScrollableBody(AppLocalizations l10n, String imageUrl) {
    final translatedCategoryName = _translateDynamicLabel(
      _resolvedCategoryName,
      const ['menu_category', 'menu_label'],
    );

    return SingleChildScrollView(
      controller: _scrollController,
      physics: const BouncingScrollPhysics(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Hero image with wave clip ──────────────────────────────────────
          _WaveHeroImage(imageUrl: imageUrl, height: _kImageHeight),

          // ── Content ────────────────────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 140),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Error banner
                if (_loadError != null) _ErrorBanner(message: _loadError!),

                // Product name + popular badge
                _ProductTitle(
                  product: _product,
                  productName: _translateDynamicLabel(_product.name, const [
                    'menu_product',
                    'menu_label',
                  ]),
                  l10n: l10n,
                ),
                const SizedBox(height: 10),

                // Price + prep time
                _PriceRow(product: _product, money: _money, l10n: l10n),
                const SizedBox(height: 20),

                // Description — plain text, no box
                if (_product.description.trim().isNotEmpty) ...[
                  Text(
                    _product.description,
                    style: const TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 14.5,
                      height: 1.6,
                    ),
                  ),
                  const SizedBox(height: 24),
                ],

                // Option groups — no boxes, just dividers
                if (_optionGroups.isNotEmpty)
                  ..._optionGroups.map((group) {
                    final translatedGroupName = _translateDynamicLabel(
                      group.name,
                      const ['menu_option_group', 'menu_label'],
                    );

                    return _OptionGroupSection(
                      group: group,
                      sectionTitle: _tr('product_detail_choose_for', {
                        'group': translatedGroupName,
                        'category': translatedCategoryName,
                      }),
                      groupName: translatedGroupName,
                      selectionRuleLabel: _selectionRuleLabel(group),
                      requiredLabel: _tr('product_detail_required'),
                      optionalLabel: _tr('product_detail_optional'),
                      translateOptionName: (raw) => _translateDynamicLabel(
                        raw,
                        const ['menu_option', 'menu_label'],
                      ),
                      isSelected: (opt) => _isSelected(group, opt),
                      onTapOption: (opt) => _toggleOption(group, opt),
                      money: _money,
                    );
                  }),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// FLOATING APP BAR
// ─────────────────────────────────────────────────────────────────────────────
class _FloatingAppBar extends StatelessWidget implements PreferredSizeWidget {
  final double safeTop;
  final String productName;
  final Animation<double> titleAnim;
  final bool isFavorite;
  final VoidCallback onBack;
  final VoidCallback onFavorite;

  const _FloatingAppBar({
    required this.safeTop,
    required this.productName,
    required this.titleAnim,
    required this.isFavorite,
    required this.onBack,
    required this.onFavorite,
  });

  @override
  Size get preferredSize => Size.fromHeight(safeTop + _kAppBarHeight);

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: titleAnim,
      builder: (context, _) {
        final collapsed = titleAnim.value;
        return Container(
          color: Colors.white.withValues(alpha: collapsed * 0.96),
          padding: EdgeInsets.only(top: safeTop),
          child: SizedBox(
            height: _kAppBarHeight,
            child: Row(
              children: [
                const SizedBox(width: 12),
                _AppBarIcon(
                  icon: Icons.arrow_back_ios_new_rounded,
                  onTap: onBack,
                  frosted: collapsed < 0.5,
                ),
                Expanded(
                  child: Opacity(
                    opacity: collapsed,
                    child: Text(
                      productName,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ),
                ),
                _AppBarIcon(
                  icon: isFavorite
                      ? Icons.favorite_rounded
                      : Icons.favorite_border_rounded,
                  iconColor: isFavorite ? AppColors.primary : Colors.black,
                  onTap: onFavorite,
                  frosted: collapsed < 0.5,
                ),
                const SizedBox(width: 12),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _AppBarIcon extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final VoidCallback onTap;
  final bool frosted;

  const _AppBarIcon({
    required this.icon,
    this.iconColor = Colors.black,
    required this.onTap,
    required this.frosted,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: frosted
              ? Colors.white.withValues(alpha: 0.88)
              : Colors.transparent,
          shape: BoxShape.circle,
          boxShadow: frosted
              ? [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.12),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Icon(icon, color: iconColor, size: 20),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// WAVE HERO IMAGE
// ─────────────────────────────────────────────────────────────────────────────
class _WaveHeroImage extends StatelessWidget {
  final String imageUrl;
  final double height;

  const _WaveHeroImage({required this.imageUrl, required this.height});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height + _kWaveHeight + 18,
      width: double.infinity,
      child: Stack(
        children: [
          const Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: SizedBox(
              height: 26,
              child: CustomPaint(painter: _HeroWaveShadowPainter()),
            ),
          ),
          ClipPath(
            clipper: _WaveClipper(),
            child: SizedBox(
              height: height + _kWaveHeight,
              width: double.infinity,
              child: imageUrl.isEmpty
                  ? Container(
                      color: const Color(0xFFF7F7F7),
                      child: const Icon(
                        Icons.fastfood_rounded,
                        size: 80,
                        color: AppColors.textHint,
                      ),
                    )
                  : CachedNetworkImage(
                      imageUrl: imageUrl,
                      fit: BoxFit.cover,
                      errorWidget: (_, __, ___) => Container(
                        color: const Color(0xFFF7F7F7),
                        child: const Icon(
                          Icons.fastfood_rounded,
                          size: 80,
                          color: AppColors.textHint,
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

class _HeroWaveShadowPainter extends CustomPainter {
  const _HeroWaveShadowPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final outerPaint = Paint()
      ..color = const Color(0xFFEDEDED)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 10
      ..strokeCap = StrokeCap.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 10);

    final innerPaint = Paint()
      ..color = const Color(0xFFF5F5F5)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6
      ..strokeCap = StrokeCap.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6);

    final baseY = size.height * 0.34;
    final path = Path()..moveTo(0, baseY);
    path.quadraticBezierTo(
      size.width * 0.25,
      size.height,
      size.width * 0.5,
      baseY + (_kWaveHeight * 0.32),
    );
    path.quadraticBezierTo(
      size.width * 0.75,
      baseY - (_kWaveHeight * 0.45),
      size.width,
      baseY + (_kWaveHeight * 0.05),
    );

    canvas.drawPath(path, outerPaint);
    canvas.drawPath(path, innerPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Clips the bottom of the hero image into a smooth wave shape.
class _WaveClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final path = Path();
    path.lineTo(0, size.height - _kWaveHeight);

    final controlPoint1 = Offset(size.width * 0.25, size.height);
    final endPoint1 = Offset(
      size.width * 0.5,
      size.height - _kWaveHeight * 0.6,
    );
    path.quadraticBezierTo(
      controlPoint1.dx,
      controlPoint1.dy,
      endPoint1.dx,
      endPoint1.dy,
    );

    final controlPoint2 = Offset(
      size.width * 0.75,
      size.height - _kWaveHeight * 1.4,
    );
    final endPoint2 = Offset(size.width, size.height - _kWaveHeight * 0.3);
    path.quadraticBezierTo(
      controlPoint2.dx,
      controlPoint2.dy,
      endPoint2.dx,
      endPoint2.dy,
    );

    path.lineTo(size.width, 0);
    path.close();
    return path;
  }

  @override
  bool shouldReclip(_WaveClipper oldClipper) => false;
}

// ─────────────────────────────────────────────────────────────────────────────
// PRODUCT TITLE
// ─────────────────────────────────────────────────────────────────────────────
class _ProductTitle extends StatelessWidget {
  final MenuProductDto product;
  final String productName;
  final AppLocalizations l10n;

  const _ProductTitle({
    required this.product,
    required this.productName,
    required this.l10n,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            productName,
            style: const TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.w900,
              color: AppColors.black,
              height: 1.1,
              letterSpacing: -0.5,
            ),
          ),
        ),
        if (product.popular) ...[
          const SizedBox(width: 10),
          Container(
            margin: const EdgeInsets.only(top: 6),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: const Color(0xFFFFF1E7),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              l10n.translate('product_detail_badge_popular'),
              style: const TextStyle(
                color: Color(0xFFB45309),
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),
        ],
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRICE ROW
// ─────────────────────────────────────────────────────────────────────────────
class _PriceRow extends StatelessWidget {
  final MenuProductDto product;
  final String Function(double) money;
  final AppLocalizations l10n;

  const _PriceRow({
    required this.product,
    required this.money,
    required this.l10n,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(
          money(product.price),
          style: const TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.w900,
            color: AppColors.primary,
          ),
        ),
        if (product.originalPrice != null &&
            product.originalPrice! > product.price) ...[
          const SizedBox(width: 10),
          Text(
            money(product.originalPrice!),
            style: const TextStyle(
              fontSize: 15,
              color: AppColors.textSecondary,
              decoration: TextDecoration.lineThrough,
            ),
          ),
        ],
        const Spacer(),
        if (product.preparationTime != null)
          Row(
            children: [
              const Icon(
                Icons.schedule_rounded,
                size: 15,
                color: AppColors.textSecondary,
              ),
              const SizedBox(width: 4),
              Text(
                '${product.preparationTime} ${l10n.translate('product_detail_minutes_abbr')}',
                style: const TextStyle(
                  color: AppColors.textSecondary,
                  fontWeight: FontWeight.w700,
                  fontSize: 14,
                ),
              ),
            ],
          ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// OPTION GROUP SECTION — flat, no card box
// ─────────────────────────────────────────────────────────────────────────────
class _OptionGroupSection extends StatelessWidget {
  final MenuProductOptionGroupDto group;
  final String sectionTitle;
  final String groupName;
  final String selectionRuleLabel;
  final String requiredLabel;
  final String optionalLabel;
  final String Function(String) translateOptionName;
  final bool Function(MenuProductOptionDto) isSelected;
  final ValueChanged<MenuProductOptionDto> onTapOption;
  final String Function(double) money;

  const _OptionGroupSection({
    required this.group,
    required this.sectionTitle,
    required this.groupName,
    required this.selectionRuleLabel,
    required this.requiredLabel,
    required this.optionalLabel,
    required this.translateOptionName,
    required this.isSelected,
    required this.onTapOption,
    required this.money,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _WavySectionDivider(),
        const SizedBox(height: 12),

        Text(
          sectionTitle,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.black,
            letterSpacing: -0.2,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 10),

        // Group header
        Row(
          children: [
            Expanded(
              child: Text(
                groupName,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.textPrimary,
                  letterSpacing: -0.2,
                ),
              ),
            ),
            _RequiredBadge(
              isRequired: group.isRequired,
              requiredLabel: requiredLabel,
              optionalLabel: optionalLabel,
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          selectionRuleLabel,
          style: const TextStyle(
            color: AppColors.textSecondary,
            fontSize: 13,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 14),

        // Options
        if (group.isSingle)
          _SingleOptionsGrid(
            group: group,
            translateOptionName: translateOptionName,
            isSelected: isSelected,
            onTapOption: onTapOption,
            money: money,
          )
        else
          _MultiOptionsList(
            group: group,
            translateOptionName: translateOptionName,
            isSelected: isSelected,
            onTapOption: onTapOption,
            money: money,
          ),

        const SizedBox(height: 8),
      ],
    );
  }
}

class _WavySectionDivider extends StatelessWidget {
  const _WavySectionDivider();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 12,
      child: CustomPaint(painter: _WavySectionDividerPainter()),
    );
  }
}

class _WavySectionDividerPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFEFEFEF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4
      ..strokeCap = StrokeCap.round;

    const amplitude = 2.6;
    const step = 32.0;
    final midY = size.height / 2;

    final path = Path()..moveTo(0, midY);

    double x = 0;
    while (x < size.width) {
      final next = math.min(x + step, size.width);
      final half = x + (next - x) / 2;
      path.quadraticBezierTo(
        x + (next - x) * 0.25,
        midY - amplitude,
        half,
        midY,
      );
      path.quadraticBezierTo(
        x + (next - x) * 0.75,
        midY + amplitude,
        next,
        midY,
      );
      x = next;
    }

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _RequiredBadge extends StatelessWidget {
  final bool isRequired;
  final String requiredLabel;
  final String optionalLabel;

  const _RequiredBadge({
    required this.isRequired,
    required this.requiredLabel,
    required this.optionalLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: isRequired ? const Color(0xFFFFECEC) : const Color(0xFFF1F2F4),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        isRequired ? requiredLabel : optionalLabel,
        style: TextStyle(
          color: isRequired ? AppColors.primaryDark : AppColors.textSecondary,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// SINGLE OPTION GRID (pill chips)
// ─────────────────────────────────────────────────────────────────────────────
class _SingleOptionsGrid extends StatelessWidget {
  final MenuProductOptionGroupDto group;
  final String Function(String) translateOptionName;
  final bool Function(MenuProductOptionDto) isSelected;
  final ValueChanged<MenuProductOptionDto> onTapOption;
  final String Function(double) money;

  const _SingleOptionsGrid({
    required this.group,
    required this.translateOptionName,
    required this.isSelected,
    required this.onTapOption,
    required this.money,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: group.options.map((option) {
        final selected = isSelected(option);
        final enabled = option.isAvailable;

        return Opacity(
          opacity: enabled ? 1 : 0.45,
          child: GestureDetector(
            onTap: enabled ? () => onTapOption(option) : null,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              curve: Curves.easeOut,
              constraints: const BoxConstraints(minWidth: 100),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFFE0E0E0), width: 1.2),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Radio dot
                  Container(
                    width: 20,
                    height: 20,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: selected
                            ? AppColors.primary
                            : const Color(0xFFCCCCCC),
                        width: selected ? 5 : 1.5,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    translateOptionName(option.name),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w700,
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    option.priceModifier > 0
                        ? '+ ${money(option.priceModifier)}'
                        : money(0),
                    style: TextStyle(
                      color: option.priceModifier > 0
                          ? AppColors.primaryDark
                          : AppColors.textSecondary,
                      fontWeight: FontWeight.w600,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// MULTI OPTION LIST (flat rows)
// ─────────────────────────────────────────────────────────────────────────────
class _MultiOptionsList extends StatelessWidget {
  final MenuProductOptionGroupDto group;
  final String Function(String) translateOptionName;
  final bool Function(MenuProductOptionDto) isSelected;
  final ValueChanged<MenuProductOptionDto> onTapOption;
  final String Function(double) money;

  const _MultiOptionsList({
    required this.group,
    required this.translateOptionName,
    required this.isSelected,
    required this.onTapOption,
    required this.money,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: group.options.map((option) {
        final selected = isSelected(option);
        final enabled = option.isAvailable;

        return Opacity(
          opacity: enabled ? 1.0 : 0.45,
          child: GestureDetector(
            onTap: enabled ? () => onTapOption(option) : null,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 160),
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFFE4E4E4), width: 1.2),
              ),
              child: Row(
                children: [
                  // Checkbox
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 160),
                    width: 22,
                    height: 22,
                    decoration: BoxDecoration(
                      color: selected ? AppColors.primary : Colors.transparent,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(
                        color: selected
                            ? AppColors.primary
                            : const Color(0xFFCCCCCC),
                        width: 1.8,
                      ),
                    ),
                    child: selected
                        ? const Icon(
                            Icons.check_rounded,
                            size: 14,
                            color: Colors.white,
                          )
                        : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      translateOptionName(option.name),
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w700,
                        fontSize: 14,
                      ),
                    ),
                  ),
                  Text(
                    option.priceModifier > 0
                        ? '+ ${money(option.priceModifier)}'
                        : money(0),
                    style: TextStyle(
                      color: option.priceModifier > 0
                          ? AppColors.primaryDark
                          : AppColors.textSecondary,
                      fontWeight: FontWeight.w700,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR
// ─────────────────────────────────────────────────────────────────────────────
class _BottomBar extends StatelessWidget {
  final int quantity;
  final double totalPrice;
  final String Function(double) money;
  final bool available;
  final AppLocalizations l10n;
  final VoidCallback onMinus;
  final VoidCallback onPlus;
  final VoidCallback onSubmit;

  const _BottomBar({
    required this.quantity,
    required this.totalPrice,
    required this.money,
    required this.available,
    required this.l10n,
    required this.onMinus,
    required this.onPlus,
    required this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(
        16,
        12,
        16,
        MediaQuery.of(context).padding.bottom + 12,
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.07),
            blurRadius: 16,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: Row(
        children: [
          // Quantity stepper
          Container(
            decoration: BoxDecoration(
              color: const Color(0xFFF5F5F5),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                _StepperButton(icon: Icons.remove_rounded, onTap: onMinus),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: Text(
                    '$quantity',
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 17,
                    ),
                  ),
                ),
                _StepperButton(icon: Icons.add_rounded, onTap: onPlus),
              ],
            ),
          ),
          const SizedBox(width: 12),
          // Cart button
          Expanded(
            child: SizedBox(
              height: 54,
              child: ElevatedButton(
                onPressed: available ? onSubmit : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  disabledBackgroundColor: AppColors.primary.withValues(
                    alpha: 0.4,
                  ),
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: Text(
                  '${l10n.translate('cart')}  •  ${money(totalPrice)}',
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                    letterSpacing: 0.2,
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

class _StepperButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _StepperButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: SizedBox(
          width: 44,
          height: 44,
          child: Icon(icon, size: 20, color: AppColors.textPrimary),
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR BANNER
// ─────────────────────────────────────────────────────────────────────────────
class _ErrorBanner extends StatelessWidget {
  final String message;
  const _ErrorBanner({required this.message});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF4F4),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFFDADA)),
      ),
      child: Text(
        message,
        style: const TextStyle(
          color: AppColors.primaryDark,
          fontWeight: FontWeight.w600,
          fontSize: 13,
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS (unchanged)
// ─────────────────────────────────────────────────────────────────────────────
class ProductSelectionResult {
  final String productId;
  final String productName;
  final int quantity;
  final double unitPrice;
  final double totalPrice;
  final List<ProductSelectionGroup> selections;

  const ProductSelectionResult({
    required this.productId,
    required this.productName,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
    required this.selections,
  });
}

class ProductSelectionGroup {
  final String groupId;
  final String groupName;
  final List<MenuProductOptionDto> options;

  const ProductSelectionGroup({
    required this.groupId,
    required this.groupName,
    required this.options,
  });
}
