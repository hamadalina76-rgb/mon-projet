import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../config/routes/route_names.dart';
import '../../../cart/cart_providers.dart';
import '../providers/order_provider.dart';
import '../widgets/order_card.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/widgets/speedline_app_bar.dart';

/// Orders Screen
/// Screen for viewing order history and active orders
class OrdersScreen extends ConsumerWidget {
  const OrdersScreen({super.key});

  String _money(double value) {
    return '${value.toStringAsFixed(2)} TND';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final customerOrdersAsync = ref.watch(customerOrdersProvider);

    final cartState = ref.watch(cartNotifierProvider);
    final cartItems = cartState.items;

    return Scaffold(
      appBar: SpeedlineAppBar(
        title: l10n.translate('my_orders'),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(customerOrdersProvider);
          await ref.read(customerOrdersProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(14, 14, 14, 24),
          children: [
            _SectionTitle(
              title: l10n.translate('validated_orders_title'),
              icon: Icons.verified_outlined,
            ),
            const SizedBox(height: 8),
            customerOrdersAsync.when(
              loading: () => const Card(
                child: Padding(
                  padding: EdgeInsets.all(14),
                  child: Center(child: CircularProgressIndicator()),
                ),
              ),
              error: (error, _) => Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Unable to load orders right now.',
                        style: TextStyle(color: AppColors.textSecondary),
                      ),
                      const SizedBox(height: 10),
                      OutlinedButton.icon(
                        onPressed: () => ref.invalidate(customerOrdersProvider),
                        icon: const Icon(Icons.refresh, size: 18),
                        label: const Text('Retry'),
                      ),
                    ],
                  ),
                ),
              ),
              data: (orders) {
                if (orders.isEmpty) {
                  return Card(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Text(
                        l10n.translate('no_validated_orders'),
                        style: const TextStyle(color: AppColors.textSecondary),
                      ),
                    ),
                  );
                }

                return Column(
                  children: orders
                      .map(
                        (order) => Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: OrderCard(
                            order: order,
                            onTrackTap: () => context.push(
                              RouteNames.orderTracking(order.id),
                            ),
                          ),
                        ),
                      )
                      .toList(),
                );
              },
            ),
            const SizedBox(height: 16),
            _SectionTitle(
              title: l10n.translate('cart'),
              icon: Icons.shopping_bag_outlined,
            ),
            const SizedBox(height: 8),
            if (cartItems.isEmpty)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Text(
                    l10n.translate('cart_empty_title'),
                    style: const TextStyle(color: AppColors.textSecondary),
                  ),
                ),
              )
            else
              ...cartItems.map((item) {
                return Card(
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    title: Text(
                      item.productName,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        Text(
                          '${item.quantity} x ${_money(item.unitPrice)}',
                          style: const TextStyle(color: AppColors.textSecondary),
                        ),
                        if (item.selectedOptions.isNotEmpty) ...[
                          const SizedBox(height: 2),
                          Text(
                            item.selectedOptionsDisplay.join(', '),
                            style: const TextStyle(
                              color: AppColors.textSecondary,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ],
                    ),
                    trailing: Text(
                      _money(item.lineTotal),
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                );
              }),
            if (cartItems.isNotEmpty) ...[
              const SizedBox(height: 8),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              l10n.translate('cart_subtotal'),
                              style: const TextStyle(fontWeight: FontWeight.w700),
                            ),
                          ),
                          Text(
                            _money(cartState.subtotal),
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton.icon(
                          onPressed: () => context.push(RouteNames.cart),
                          icon: const Icon(Icons.open_in_new_rounded, size: 18),
                          label: Text(l10n.translate('open_cart')),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionTitle({
    required this.title,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18, color: AppColors.primary),
        const SizedBox(width: 8),
        Text(
          title,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}
