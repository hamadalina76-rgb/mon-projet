import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/routes/route_names.dart';
import '../../../core/localization/app_localizations.dart';
import '../../partners/presentation/screens/partner_details_screen.dart';
import '../cart_providers.dart';
import '../data/models/cart_item_model.dart';
import '../data/repositories/cart_repository.dart';

class CartScreen extends ConsumerStatefulWidget {
  const CartScreen({super.key});

  @override
  ConsumerState<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends ConsumerState<CartScreen> {
  String? _loadedPartnerId;
  PartnerCartInfo? _partnerInfo;
  bool _isLoadingPartner = false;

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(2)} DT';
  }

  void _ensurePartnerLoaded(String? partnerId) {
    if (_loadedPartnerId == partnerId) return;

    _loadedPartnerId = partnerId;
    _partnerInfo = null;
    _isLoadingPartner = false;

    if (partnerId == null || partnerId.isEmpty) {
      if (mounted) setState(() {});
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      setState(() => _isLoadingPartner = true);
      final info = await ref.read(cartRepositoryProvider).fetchPartnerInfo(partnerId);
      if (!mounted) return;
      setState(() {
        _partnerInfo = info;
        _isLoadingPartner = false;
      });
    });
  }

  Future<void> _confirmClearCart() async {
    final l10n = AppLocalizations.of(context);

    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: Text(l10n.translate('clear_cart_question')),
          content: Text(l10n.translate('clear_cart_message')),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: Text(l10n.translate('cancel')),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              child: Text(l10n.translate('clear_cart_action')),
            ),
          ],
        );
      },
    );

    if (accepted == true) {
      await ref.read(cartNotifierProvider.notifier).clearCart();
    }
  }

  Future<void> _openPartnerDetails(String partnerId) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => PartnerDetailsScreen(partnerId: partnerId),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    final screenWidth = media.size.width;
    final horizontalPadding = screenWidth < 360 ? 12.0 : 16.0;
    final isCompact = screenWidth < 360;
    final l10n = AppLocalizations.of(context);

    final lightTheme = Theme.of(context).copyWith(
      brightness: Brightness.light,
      scaffoldBackgroundColor: Colors.white,
      colorScheme: Theme.of(context).colorScheme.copyWith(
            brightness: Brightness.light,
            surface: Colors.white,
          ),
      appBarTheme: Theme.of(context).appBarTheme.copyWith(
            backgroundColor: Colors.white,
            foregroundColor: Colors.black87,
            surfaceTintColor: Colors.white,
          ),
    );

    final cartState = ref.watch(cartNotifierProvider);
    final items = cartState.items;
    final subtotal = cartState.subtotal;

    final partnerId = items.isNotEmpty ? items.first.partnerId : null;
    _ensurePartnerLoaded(partnerId);

    final partnerName =
        _partnerInfo?.partnerName ?? (items.isNotEmpty ? items.first.partnerName : l10n.translate('partner'));
    final partnerLogoUrl = _partnerInfo?.partnerLogoUrl ??
        (items.isNotEmpty ? items.first.partnerLogoUrl : '');

    return Theme(
      data: lightTheme,
      child: Scaffold(
      appBar: AppBar(
        title: Text(l10n.translate('my_cart')),
        actions: [
          if (items.isNotEmpty)
            TextButton(
              onPressed: _confirmClearCart,
              child: Text(
                l10n.translate('clear_cart_action'),
                style: TextStyle(
                  color: Colors.red,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
        ],
      ),
      body: items.isEmpty
          ? const _EmptyCartView()
          : ListView(
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                14,
                horizontalPadding,
                120 + media.padding.bottom,
              ),
              children: [
                _PartnerBlock(
                  partnerName: partnerName,
                  partnerLogoUrl: partnerLogoUrl,
                  isLoading: _isLoadingPartner,
                  onDetailsPressed: partnerId == null ? null : () => _openPartnerDetails(partnerId),
                ),
                const SizedBox(height: 10),
                ...items.map(
                  (item) => _CartLineItem(
                    item: item,
                    onQuantityChanged: (newQty) {
                      ref.read(cartNotifierProvider.notifier).updateQuantity(
                            itemKey: item.uniqueKey,
                            quantity: newQty,
                          );
                    },
                    onRemove: () {
                      ref.read(cartNotifierProvider.notifier).removeItem(item.uniqueKey);
                    },
                  ),
                ),
                const SizedBox(height: 6),
                OutlinedButton.icon(
                  onPressed: partnerId == null ? null : () => _openPartnerDetails(partnerId),
                  icon: const Icon(Icons.add_circle_outline),
                  label: Text(l10n.translate('add_more_from_partner')),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Text(
                      l10n.translate('products_total'),
                      style: TextStyle(
                        fontSize: 15,
                        color: Colors.black54,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      _money(subtotal),
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 18,
                      ),
                    ),
                  ],
                ),
              ],
            ),
      bottomNavigationBar: items.isEmpty
          ? null
          : SafeArea(
              top: false,
              minimum: EdgeInsets.fromLTRB(
                horizontalPadding,
                8,
                horizontalPadding,
                10,
              ),
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: Colors.black12),
                ),
                  child: isCompact
                      ? Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    l10n.translate('total'),
                                    style: const TextStyle(
                                      fontSize: 13,
                                      color: Colors.black54,
                                    ),
                                  ),
                                ),
                                Text(
                                  _money(subtotal),
                                  style: const TextStyle(
                                    fontSize: 19,
                                    fontWeight: FontWeight.w900,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            SizedBox(
                              height: 46,
                              child: ElevatedButton(
                                onPressed: () => context.push(RouteNames.checkout),
                                child: Text(
                                  l10n.translate('proceed_to_checkout'),
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(fontWeight: FontWeight.w700),
                                ),
                              ),
                            ),
                          ],
                        )
                      : Row(
                          children: [
                            Expanded(
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    l10n.translate('total'),
                                    style: const TextStyle(
                                      fontSize: 13,
                                      color: Colors.black54,
                                    ),
                                  ),
                                  Text(
                                    _money(subtotal),
                                    style: const TextStyle(
                                      fontSize: 19,
                                      fontWeight: FontWeight.w900,
                                    ),
                                  ),
                                ],
                            ),
                          ),
                            const SizedBox(width: 10),
                            Expanded(
                              flex: 2,
                              child: SizedBox(
                                height: 48,
                                child: ElevatedButton(
                                  onPressed: () => context.push(RouteNames.checkout),
                                  child: Text(
                                    l10n.translate('proceed_to_checkout'),
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(fontWeight: FontWeight.w700),
                                  ),
                                ),
                            ),
                          ),
                        ],
                      ),
              ),
            ),
      ),
    );
  }
}

class _PartnerBlock extends StatelessWidget {
  final String partnerName;
  final String partnerLogoUrl;
  final bool isLoading;
  final VoidCallback? onDetailsPressed;

  const _PartnerBlock({
    required this.partnerName,
    required this.partnerLogoUrl,
    required this.isLoading,
    required this.onDetailsPressed,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.black12),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(24),
            child: partnerLogoUrl.trim().isEmpty
                ? Container(
                    width: 48,
                    height: 48,
                    color: const Color(0xFFF3F3F3),
                    child: const Icon(Icons.storefront),
                  )
                : CachedNetworkImage(
                    imageUrl: partnerLogoUrl,
                    width: 48,
                    height: 48,
                    fit: BoxFit.cover,
                    errorWidget: (_, __, ___) => Container(
                      width: 48,
                      height: 48,
                      color: const Color(0xFFF3F3F3),
                      child: const Icon(Icons.storefront),
                    ),
                  ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  partnerName,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 2),
                TextButton(
                  onPressed: onDetailsPressed,
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(0, 30),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    alignment: Alignment.centerLeft,
                  ),
                  child: Text(l10n.translate('view_partner_details')),
                ),
              ],
            ),
          ),
          if (isLoading)
            const SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
        ],
      ),
    );
  }
}

class _CartLineItem extends StatelessWidget {
  final CartItemModel item;
  final ValueChanged<int> onQuantityChanged;
  final VoidCallback onRemove;

  const _CartLineItem({
    required this.item,
    required this.onQuantityChanged,
    required this.onRemove,
  });

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(2)} DT';
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    item.productName,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                IconButton(
                  onPressed: onRemove,
                  icon: const Icon(Icons.delete_outline, color: Colors.red),
                ),
              ],
            ),
            if (item.selectedOptions.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(
                  item.selectedOptionsDisplay.join(', '),
                  style: const TextStyle(color: Colors.black54),
                ),
              ),
            if ((item.kitchenNote ?? '').trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Text(
                  '${l10n.translate('kitchen_note')}: ${item.kitchenNote}',
                  style: const TextStyle(color: Colors.black54),
                ),
              ),
            const SizedBox(height: 4),
            Row(
              children: [
                Container(
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.black12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: () {
                          final next = item.quantity - 1;
                          if (next <= 0) {
                            onRemove();
                          } else {
                            onQuantityChanged(next);
                          }
                        },
                        icon: const Icon(Icons.remove),
                      ),
                      Text(
                        '${item.quantity}',
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: () => onQuantityChanged(item.quantity + 1),
                        icon: const Icon(Icons.add),
                      ),
                    ],
                  ),
                ),
                const Spacer(),
                Text(
                  _money(item.lineTotal),
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 15,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCartView extends StatelessWidget {
  const _EmptyCartView();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.shopping_bag_outlined, size: 56, color: Colors.black38),
            const SizedBox(height: 8),
            Text(
              l10n.translate('cart_empty_title'),
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            Text(
              l10n.translate('cart_empty_subtitle'),
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 14),
            OutlinedButton(
              onPressed: () => context.go(RouteNames.explore),
              child: Text(l10n.translate('explore_partners')),
            ),
          ],
        ),
      ),
    );
  }
}
