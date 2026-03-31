import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../partners/data/models/partner_nearby_dto.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';
import '../../../partners/presentation/screens/partner_details_screen.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';

class SearchScreen extends ConsumerStatefulWidget {
  final String heroTag;
  final double? initialLat;
  final double? initialLng;
  final String? initialQuery;

  const SearchScreen({
    super.key,
    this.heroTag = 'nearby-search-hero',
    this.initialLat,
    this.initialLng,
    this.initialQuery,
  });

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  static const _historyKey = 'search_history_v1';
  static const _debounceDuration = Duration(milliseconds: 400);
  static const _minQueryLength = 2;
  static const _maxHistory = 5;

  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  Timer? _debounce;
  bool _isLoading = false;
  bool _didSearch = false;
  List<PartnerNearbyDto> _results = const [];
  List<String> _history = const [];
  List<String> _trending = const [];

  ({double lat, double lng}) _resolveCoordinates() {
    final lat = widget.initialLat;
    final lng = widget.initialLng;
    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
      return (lat: lat, lng: lng);
    }

    final selectedLoc = ref.read(locationNotifierProvider).location;
    if (selectedLoc != null) {
      return (lat: selectedLoc.latitude, lng: selectedLoc.longitude);
    }

    final addresses =
        ref.read(addressNotifierProvider).valueOrNull ?? <AddressModel>[];
    for (final a in addresses) {
      if (a.isDefault && a.latitude != null && a.longitude != null) {
        return (lat: a.latitude!, lng: a.longitude!);
      }
    }
    for (final a in addresses) {
      if (a.latitude != null && a.longitude != null) {
        return (lat: a.latitude!, lng: a.longitude!);
      }
    }

    return (lat: 0.0, lng: 0.0);
  }

  @override
  void initState() {
    super.initState();
    _loadHistory();
    _loadTrending();

    final initialQuery = widget.initialQuery?.trim();
    if (initialQuery != null && initialQuery.isNotEmpty) {
      _controller.text = initialQuery;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _onQueryChanged(initialQuery);
      });
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    final prefs = ref.read(sharedPreferencesProvider);
    final loaded = prefs.getStringList(_historyKey) ?? <String>[];
    if (!mounted) return;
    setState(() => _history = loaded);
  }

  Future<void> _saveHistory() async {
    final prefs = ref.read(sharedPreferencesProvider);
    await prefs.setStringList(_historyKey, _history.take(_maxHistory).toList());
  }

  Future<void> _loadTrending() async {
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) return;

    try {
      final items = await ref
          .read(partnerApiServiceProvider)
          .fetchTrendingSearches(lat: coords.lat, lng: coords.lng, limit: 5);
      if (!mounted) return;
      setState(() => _trending = items);
    } catch (_) {
      if (!mounted) return;
      setState(() => _trending = const []);
    }
  }

  Future<void> _addToHistory(String value) async {
    final item = value.trim();
    if (item.isEmpty) return;

    final normalized = item.toLowerCase();
    final next = <String>[item];
    for (final h in _history) {
      if (h.toLowerCase() != normalized) next.add(h);
      if (next.length >= _maxHistory) break;
    }

    setState(() => _history = next);
    await _saveHistory();
  }

  Future<void> _removeHistoryItem(String item) async {
    setState(() => _history = _history.where((e) => e != item).toList());
    await _saveHistory();
  }

  void _onQueryChanged(String value) {
    _debounce?.cancel();

    final query = value.trim();
    if (query.length < _minQueryLength) {
      setState(() {
        _isLoading = false;
        _didSearch = false;
        _results = const [];
      });
      return;
    }

    _debounce = Timer(_debounceDuration, () {
      _search(query);
    });
  }

  Future<void> _search(String query) async {
    final coords = _resolveCoordinates();
    if (coords.lat == 0.0 && coords.lng == 0.0) return;

    setState(() {
      _isLoading = true;
      _didSearch = true;
    });

    try {
      final page = await ref
          .read(partnerApiServiceProvider)
          .searchPartners(
            query: query,
            lat: coords.lat,
            lng: coords.lng,
            page: 0,
            size: 20,
          );

      if (!mounted) return;
      setState(() {
        _results = page.content;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _results = const [];
        _isLoading = false;
      });
    }
  }

  void _submit(String value) {
    final query = value.trim();
    if (query.length < _minQueryLength) return;
    _addToHistory(query);
    _search(query);
  }

  List<InlineSpan> _highlightSpans(String text, String needle) {
    if (needle.isEmpty) {
      return [TextSpan(text: text)];
    }

    final lowerText = text.toLowerCase();
    final lowerNeedle = needle.toLowerCase();
    final spans = <InlineSpan>[];

    var start = 0;
    while (true) {
      final index = lowerText.indexOf(lowerNeedle, start);
      if (index < 0) {
        if (start < text.length) {
          spans.add(TextSpan(text: text.substring(start)));
        }
        break;
      }

      if (index > start) {
        spans.add(TextSpan(text: text.substring(start, index)));
      }

      spans.add(
        TextSpan(
          text: text.substring(index, index + needle.length),
          style: const TextStyle(
            color: AppColors.primary,
            fontWeight: FontWeight.w800,
          ),
        ),
      );

      start = index + needle.length;
    }

    return spans;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final query = _controller.text.trim();
    final showSuggestions = query.length < _minQueryLength;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: GestureDetector(
          onTap: () => FocusScope.of(context).unfocus(),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
                child: Row(
                  children: [
                    InkWell(
                      onTap: () => Navigator.of(context).maybePop(),
                      borderRadius: BorderRadius.circular(24),
                      child: Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: const Color(0xFFF2F2F5),
                          borderRadius: BorderRadius.circular(24),
                        ),
                        child: const Icon(Icons.arrow_back_ios_new_rounded),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Container(
                        height: 48,
                        decoration: BoxDecoration(
                          color: const Color(0xFFF2F2F5),
                          borderRadius: BorderRadius.circular(26),
                        ),
                        child: TextField(
                          controller: _controller,
                          focusNode: _focusNode,
                          textInputAction: TextInputAction.search,
                          onChanged: _onQueryChanged,
                          onSubmitted: _submit,
                          style: const TextStyle(
                            color: Colors.black,
                            fontSize: 15,
                            fontWeight: FontWeight.w500,
                          ),
                          cursorColor: AppColors.primary,
                          decoration: InputDecoration(
                            hintText: l10n.translate('search'),
                            hintStyle: const TextStyle(
                              color: AppColors.darkGrey,
                              fontSize: 15,
                              fontWeight: FontWeight.w500,
                            ),
                            prefixIcon: const Icon(
                              Icons.search_rounded,
                              color: AppColors.darkGrey,
                            ),
                            suffixIcon: query.isNotEmpty
                                ? IconButton(
                                    onPressed: () {
                                      _controller.clear();
                                      _onQueryChanged('');
                                    },
                                    icon: const Icon(
                                      Icons.close_rounded,
                                      color: AppColors.grey,
                                    ),
                                  )
                                : null,
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(26),
                              borderSide: const BorderSide(
                                color: AppColors.primary,
                                width: 1,
                              ),
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(26),
                              borderSide: const BorderSide(
                                color: AppColors.primary,
                                width: 1.5,
                              ),
                            ),
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(26),
                              borderSide: const BorderSide(
                                color: Color(0xFFE2E2E8),
                                width: 1,
                              ),
                            ),
                            contentPadding: const EdgeInsets.symmetric(
                              vertical: 12,
                              horizontal: 4,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: _isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : showSuggestions
                    ? _SearchSuggestions(
                        history: _history,
                        trending: _trending,
                        l10n: l10n,
                        onTap: (value) {
                          _controller.text = value;
                          _controller.selection = TextSelection.collapsed(
                            offset: value.length,
                          );
                          _submit(value);
                        },
                        onRemoveHistory: _removeHistoryItem,
                      )
                    : _didSearch && _results.isEmpty
                    ? Center(
                        child: Text(
                          '${l10n.translate('no_search_results_for')} "${_controller.text.trim()}"',
                          style: const TextStyle(
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                        itemBuilder: (_, index) {
                          final partner = _results[index];
                          final name = partner.displayName;

                          return InkWell(
                            onTap: () {
                              _addToHistory(name);
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => PartnerDetailsScreen(
                                    partnerId: partner.id,
                                    initialPartner: partner,
                                  ),
                                ),
                              );
                            },
                            borderRadius: BorderRadius.circular(16),
                            child: Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(color: AppColors.border),
                                boxShadow: const [
                                  BoxShadow(
                                    color: Color(0x12000000),
                                    blurRadius: 10,
                                    offset: Offset(0, 3),
                                  ),
                                ],
                              ),
                              child: Row(
                                children: [
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(12),
                                    child: SizedBox(
                                      width: 58,
                                      height: 58,
                                      child: partner.logo?.isNotEmpty == true
                                          ? CachedNetworkImage(
                                              imageUrl: partner.logo!,
                                              fit: BoxFit.cover,
                                              errorWidget: (_, __, ___) =>
                                                  _resultAvatarFallback(name),
                                            )
                                          : _resultAvatarFallback(name),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        RichText(
                                          text: TextSpan(
                                            style: const TextStyle(
                                              color: AppColors.textPrimary,
                                              fontSize: 16,
                                              fontWeight: FontWeight.w700,
                                            ),
                                            children: _highlightSpans(
                                              name,
                                              query,
                                            ),
                                          ),
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          partner.type ??
                                              l10n.translate('partner'),
                                          style: const TextStyle(
                                            color: AppColors.textSecondary,
                                            fontSize: 13,
                                          ),
                                        ),
                                        const SizedBox(height: 6),
                                        Row(
                                          children: [
                                            const Icon(
                                              Icons.star_rounded,
                                              color: AppColors.starYellow,
                                              size: 16,
                                            ),
                                            const SizedBox(width: 4),
                                            Text(
                                              partner.rating.toStringAsFixed(1),
                                              style: const TextStyle(
                                                color: AppColors.textSecondary,
                                                fontWeight: FontWeight.w600,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                        separatorBuilder: (_, __) => const SizedBox(height: 10),
                        itemCount: _results.length,
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _resultAvatarFallback(String name) {
    final text = name.trim();
    final letter = text.isEmpty ? '?' : text.substring(0, 1).toUpperCase();
    return Container(
      color: AppColors.softGrey,
      alignment: Alignment.center,
      child: Text(
        letter,
        style: const TextStyle(
          color: AppColors.textPrimary,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _SearchSuggestions extends StatelessWidget {
  final List<String> history;
  final List<String> trending;
  final AppLocalizations l10n;
  final ValueChanged<String> onTap;
  final ValueChanged<String> onRemoveHistory;

  const _SearchSuggestions({
    required this.history,
    required this.trending,
    required this.l10n,
    required this.onTap,
    required this.onRemoveHistory,
  });

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      children: [
        if (history.isNotEmpty) ...[
          Text(
            l10n.translate('search_recent'),
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: history
                .map(
                  (item) => Material(
                    color: const Color(0xFFF2F2F5),
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: () => onTap(item),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 8,
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              item,
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                                color: Colors.black,
                              ),
                            ),
                            const SizedBox(width: 6),
                            GestureDetector(
                              onTap: () => onRemoveHistory(item),
                              child: const Icon(
                                Icons.close_rounded,
                                size: 14,
                                color: Color(0xFF5B5B60),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
          const SizedBox(height: 26),
        ],
        if (trending.isNotEmpty) ...[
          Text(
            l10n.translate('search_trending'),
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 10,
            children: trending
                .map(
                  (item) => Material(
                    color: const Color(0xFFF2F2F5),
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: () => onTap(item),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 9,
                        ),
                        child: Text(
                          item,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
          const SizedBox(height: 16),
          Text(
            l10n.translate('search'),
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 10,
            children: trending
                .map(
                  (item) => Material(
                    color: const Color(0xFFF2F2F5),
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: () => onTap(item),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 9,
                        ),
                        child: Text(
                          item,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
        ],
      ],
    );
  }
}
