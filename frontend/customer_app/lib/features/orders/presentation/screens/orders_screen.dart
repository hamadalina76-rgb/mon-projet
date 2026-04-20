import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/media_url.dart';
import '../../../../core/widgets/speedline_app_bar.dart';
import '../../../cart/cart_providers.dart';
import '../../../partners/data/models/partner_nearby_dto.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';
import '../../domain/entities/order.dart';
import '../providers/order_provider.dart';
import '../utils/order_status_palette.dart';

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

final _cartProductImageProvider = FutureProvider.family<String?, String>((
  ref,
  key,
) async {
  final separatorIndex = key.indexOf('::');
  if (separatorIndex <= 0 || separatorIndex >= key.length - 2) {
    return null;
  }

  final partnerId = key.substring(0, separatorIndex);
  final productId = key.substring(separatorIndex + 2);

  try {
    final product = await ref
        .read(partnerApiServiceProvider)
        .fetchMenuProductDetails(partnerId, productId);
    final image = resolveMediaUrl(product.imageUrl);
    return image.trim().isEmpty ? null : image;
  } catch (_) {
    return null;
  }
});

class OrdersScreen extends ConsumerStatefulWidget {
  const OrdersScreen({super.key});

  @override
  ConsumerState<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends ConsumerState<OrdersScreen> {
  bool _showAllOrders = false;

  String _money(double value) {
    return '${value.toStringAsFixed(2)} DT';
  }

  List<Order> _sortedOrders(List<Order> source) {
    final copy = List<Order>.from(source);
    copy.sort((a, b) {
      final ad = a.orderTime ?? a.createdAt;
      final bd = b.orderTime ?? b.createdAt;
      if (ad == null && bd == null) return 0;
      if (ad == null) return 1;
      if (bd == null) return -1;
      return bd.compareTo(ad);
    });
    return copy;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final customerOrdersAsync = ref.watch(customerOrdersProvider);
    final cartState = ref.watch(cartNotifierProvider);
    final cartItems = cartState.items;

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: SpeedlineAppBar(title: l10n.translate('my_orders')),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: () async {
          ref.invalidate(customerOrdersProvider);
          await ref.read(customerOrdersProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            // Container(
            //   padding: const EdgeInsets.all(16),

            //   child: Row(
            //     children: [
            //       const Icon(
            //         Icons.receipt_long_rounded,
            //         color: AppColors.primary,
            //         size: 30,
            //       ),
            //       const SizedBox(width: 12),
            //       Expanded(
            //         child: Text(
            //           l10n.translate('orders_intro_message'),
            //           style: const TextStyle(
            //             color: AppColors.textPrimary,
            //             fontSize: 14,
            //             fontWeight: FontWeight.w600,
            //             height: 1.35,
            //           ),
            //         ),
            //       ),
            //     ],
            //   ),
            // ),
            const SizedBox(height: 18),
            _SectionHeader(
              title: l10n.translate('validated_orders_title'),
              icon: Icons.receipt_long_rounded,
            ),
            const SizedBox(height: 10),
            customerOrdersAsync.when(
              loading: () => const _LoadingPanel(),
              error: (error, _) => _ErrorPanel(
                message: l10n.translate('orders_load_error'),
                onRetry: () => ref.invalidate(customerOrdersProvider),
              ),
              data: (orders) {
                if (orders.isEmpty) {
                  return _EmptyPanel(
                    text: l10n.translate('no_validated_orders'),
                  );
                }

                final sorted = _sortedOrders(orders);
                final remainingOrdersCount = sorted.length - 1;
                final visibleOrders = _showAllOrders
                    ? sorted
                    : sorted.take(1).toList();

                return Column(
                  children: [
                    ...visibleOrders.map((order) {
                      final partnerId = order.partnerId?.trim();
                      final partnerPreview =
                          partnerId == null || partnerId.isEmpty
                          ? const AsyncValue<PartnerNearbyDto?>.data(null)
                          : ref.watch(_orderPartnerPreviewProvider(partnerId));
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _OrderTile(
                          order: order,
                          money: _money,
                          statusStyle: OrderStatusPalette.forStatus(
                            order.normalizedStatus,
                          ),
                          partnerPreview: partnerPreview,
                          onTrackTap: () =>
                              context.push(RouteNames.orderTracking(order.id)),
                        ),
                      );
                    }),
                    if (sorted.length > 1)
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton.icon(
                          onPressed: () {
                            setState(() => _showAllOrders = !_showAllOrders);
                          },
                          icon: Icon(
                            _showAllOrders
                                ? Icons.keyboard_arrow_up_rounded
                                : Icons.keyboard_arrow_down_rounded,
                            color: AppColors.primary,
                          ),
                          label: Text(
                            _showAllOrders
                                ? l10n.translate('collapse')
                                : '${l10n.translate('show_all')} ($remainingOrdersCount)',
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
            ),
            const SizedBox(height: 14),
            _SectionHeader(
              title: l10n.translate('cart'),
              icon: Icons.shopping_bag_rounded,
            ),
            const SizedBox(height: 10),
            _CartPreviewPanel(
              cartItems: cartItems,
              subtotal: cartState.subtotal,
              money: _money,
              onOpenCart: () => context.push(RouteNames.cart),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionHeader({required this.title, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, size: 20, color: AppColors.primary),
        ),
        const SizedBox(width: 10),
        Text(
          title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}

class _OrderTile extends StatelessWidget {
  final Order order;
  final String Function(double value) money;
  final OrderStatusStyle statusStyle;
  final AsyncValue<PartnerNearbyDto?> partnerPreview;
  final VoidCallback onTrackTap;

  const _OrderTile({
    required this.order,
    required this.money,
    required this.statusStyle,
    required this.partnerPreview,
    required this.onTrackTap,
  });

  String _formatDate(DateTime? value) {
    if (value == null) return '--';
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')} ${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final localizedStatus = OrderStatusPalette.localizedLabel(
      rawStatus: order.normalizedStatus,
      l10n: l10n,
      fallbackLabel: order.statusLabel,
    );
    final partnerName = order.partnerName?.trim();
    final initials = partnerName != null && partnerName.isNotEmpty
        ? partnerName.characters.take(1).toString().toUpperCase()
        : 'S';

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
        boxShadow: const [
          BoxShadow(
            color: AppColors.shadow,
            blurRadius: 10,
            offset: Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _PartnerAvatar(
                initials: initials,
                partnerPreview: partnerPreview,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  order.partnerName?.trim().isNotEmpty == true
                      ? order.partnerName!.trim()
                      : l10n.translate('partner_unavailable'),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: statusStyle.background,
                  border: Border.all(color: statusStyle.border),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: statusStyle.dot,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 5),
                    Text(
                      localizedStatus,
                      style: TextStyle(
                        color: statusStyle.foreground,
                        fontWeight: FontWeight.w700,
                        fontSize: 11.5,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            order.orderNumber?.trim().isNotEmpty == true
                ? '#${order.orderNumber}'
                : '#${order.id}',
            style: const TextStyle(
              color: AppColors.textHint,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Text(
                money(order.total),
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const Spacer(),
              Text(
                _formatDate(order.orderTime ?? order.createdAt),
                style: const TextStyle(
                  color: AppColors.textSecondary,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            height: 44,
            child: OutlinedButton.icon(
              onPressed: onTrackTap,
              style: OutlinedButton.styleFrom(
                backgroundColor: AppColors.surface,
                foregroundColor: AppColors.black,
                side: const BorderSide(color: AppColors.black, width: 1.2),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                textStyle: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 14,
                ),
              ),
              icon: const Icon(Icons.route_rounded, size: 18),
              label: Text(l10n.translate('track_order')),
            ),
          ),
        ],
      ),
    );
  }
}

class _CartPreviewPanel extends StatelessWidget {
  final List<CartItemModel> cartItems;
  final double subtotal;
  final String Function(double value) money;
  final VoidCallback onOpenCart;

  const _CartPreviewPanel({
    required this.cartItems,
    required this.subtotal,
    required this.money,
    required this.onOpenCart,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    if (cartItems.isEmpty) {
      return _EmptyPanel(text: l10n.translate('cart_empty_title'));
    }

    final preview = cartItems.take(5).toList();
    final partnerName = cartItems.first.partnerName.trim().isNotEmpty
        ? cartItems.first.partnerName.trim()
        : l10n.translate('partner');
    final names = cartItems.map((item) => item.productName).toList().join(', ');
    final totalItems = cartItems.fold<int>(
      0,
      (sum, item) => sum + item.quantity,
    );

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(22),
        boxShadow: const [
          BoxShadow(
            color: AppColors.shadow,
            blurRadius: 14,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            partnerName,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w800,
              fontSize: 15,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _OverlappingProductAvatars(items: preview),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  names,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: AppColors.primary.withOpacity(0.08),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Text(
                  '$totalItems ${l10n.translate('products_count_suffix')}',
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const Spacer(),
                Text(
                  money(subtotal),
                  style: const TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                    fontSize: 17,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: onOpenCart,
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.black,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              icon: const Icon(Icons.shopping_cart_checkout_rounded, size: 18),
              label: Text(l10n.translate('open_cart')),
            ),
          ),
        ],
      ),
    );
  }
}

class _OverlappingProductAvatars extends ConsumerWidget {
  final List<CartItemModel> items;

  const _OverlappingProductAvatars({required this.items});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final visible = items.take(3).toList();
    const size = 38.0;
    const overlap = 20.0;
    final totalWidth = visible.isEmpty
        ? 0.0
        : size + (visible.length - 1) * (size - overlap);

    return SizedBox(
      width: totalWidth,
      height: size,
      child: Stack(
        children: visible.asMap().entries.toList().reversed.map((entry) {
          final index = entry.key;
          final item = entry.value;
          final fallbackImage = resolveMediaUrl(item.productImageUrl);
          final imageKey = '${item.partnerId}::${item.productId}';
          final image = ref
              .watch(_cartProductImageProvider(imageKey))
              .maybeWhen(
                data: (value) => (value != null && value.trim().isNotEmpty)
                    ? value
                    : fallbackImage,
                orElse: () => fallbackImage,
              );

          return Positioned(
            left: index * (size - overlap),
            child: Container(
              width: size,
              height: size,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.surface, width: 2),
              ),
              child: ClipOval(
                child: image.isEmpty
                    ? Container(
                        color: AppColors.primary.withOpacity(0.14),
                        child: const Icon(
                          Icons.fastfood_rounded,
                          color: AppColors.primary,
                          size: 18,
                        ),
                      )
                    : CachedNetworkImage(
                        imageUrl: image,
                        fit: BoxFit.cover,
                        errorWidget: (_, __, ___) => Container(
                          color: AppColors.primary.withOpacity(0.14),
                          child: const Icon(
                            Icons.fastfood_rounded,
                            color: AppColors.primary,
                            size: 18,
                          ),
                        ),
                      ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _LoadingPanel extends StatelessWidget {
  const _LoadingPanel();

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _ErrorPanel extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorPanel({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 18),
              label: Text(l10n.translate('retry')),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyPanel extends StatelessWidget {
  final String text;

  const _EmptyPanel({required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(text, style: const TextStyle(color: AppColors.textSecondary)),
    );
  }
}

class _PartnerAvatar extends StatelessWidget {
  final String initials;
  final AsyncValue<PartnerNearbyDto?> partnerPreview;

  const _PartnerAvatar({required this.initials, required this.partnerPreview});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 42,
      height: 42,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: partnerPreview.when(
          data: (partner) {
            final logoUrl = partner?.logo?.trim() ?? '';
            if (logoUrl.isNotEmpty) {
              return CachedNetworkImage(
                imageUrl: logoUrl,
                fit: BoxFit.cover,
                errorWidget: (_, __, ___) => _avatarFallback(),
              );
            }
            return _avatarFallback();
          },
          loading: _avatarFallback,
          error: (_, __) => _avatarFallback(),
        ),
      ),
    );
  }

  Widget _avatarFallback() {
    return Container(
      color: AppColors.primary.withOpacity(0.14),
      child: Center(
        child: Text(
          initials,
          style: const TextStyle(
            color: AppColors.primary,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
    );
  }
}
