import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/data/models/saved_location.dart';
import '../../data/models/address_model.dart';
import '../providers/address_provider.dart';

/// Écran listant toutes les adresses sauvegardées du client.
class AddressesScreen extends ConsumerStatefulWidget {
  const AddressesScreen({super.key});

  @override
  ConsumerState<AddressesScreen> createState() => _AddressesScreenState();
}

class _AddressesScreenState extends ConsumerState<AddressesScreen> {
  String? _customerId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadAddresses());
  }

  void _loadAddresses() {
    final authState = ref.read(authNotifierProvider);
    authState.whenOrNull(
      authenticated: (user) {
        _customerId = user.id;
        ref
            .read(addressNotifierProvider.notifier)
            .fetchAddresses(user.id);
      },
    );
  }

  // ─── Type helpers ──────────────────────────────────────────────────────────

  Color _typeColor(AddressType type) {
    switch (type) {
      case AddressType.home:      return AppColors.primary;
      case AddressType.work:      return AppColors.primaryDark;
      case AddressType.apartment: return AppColors.secondary;
      case AddressType.other:     return AppColors.secondaryDark;
    }
  }

  IconData _typeIcon(AddressType type) {
    switch (type) {
      case AddressType.home:      return Icons.home_rounded;
      case AddressType.work:      return Icons.business_rounded;
      case AddressType.apartment: return Icons.apartment_rounded;
      case AddressType.other:     return Icons.location_on_rounded;
    }
  }

  // ─── Actions ──────────────────────────────────────────────────────────────

  Future<void> _navigateToAdd() async {
    if (_customerId == null) return;
    final result = await context.push<bool>(
      RouteNames.addressTypeSelector,
      extra: {'customerId': _customerId!},
    );
    if (result == true) _loadAddresses();
  }

  Future<void> _navigateToEdit(AddressModel address) async {
    if (_customerId == null) return;
    final result = await context.push<bool>(
      RouteNames.addressDetails,
      extra: {
        'type': address.type,
        'customerId': _customerId!,
        'existing': address,
      },
    );
    if (result == true) _loadAddresses();
  }

  Future<void> _confirmDelete(AddressModel address) async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(l10n.translate('delete_address'),
            style: const TextStyle(fontWeight: FontWeight.w700)),
        content: Text(
            '"${address.displayLabel}" — ${l10n.translate('delete_address_confirm')}'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.translate('cancel')),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: Text(l10n.translate('delete')),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await ref
          .read(addressNotifierProvider.notifier)
          .deleteAddress(address.id);
    }
  }

  // ─── Build ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final addressState = ref.watch(addressNotifierProvider);

    return Scaffold(
      backgroundColor:  AppColors.surfaceLight,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded,
              color: AppColors.textPrimary),
          onPressed: () => context.pop(),
        ),
        title: Text(
          l10n.translate('my_addresses'),
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _navigateToAdd,
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.surface,
        icon: const Icon(Icons.add_rounded),
        label: Text(l10n.translate('add'),
            style: const TextStyle(fontWeight: FontWeight.w700)),
      ),
      body: addressState.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.wifi_off_rounded,
                  size: 48, color: AppColors.secondaryGrey),
              const SizedBox(height: 12),
              Text(l10n.translate('load_addresses_error'),
                  style: const TextStyle(color: AppColors.textSecondary)),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: _loadAddresses,
                child: Text(l10n.translate('retry')),
              ),
            ],
          ),
        ),
        data: (addresses) => addresses.isEmpty
            ? _buildEmptyState(l10n)
            : ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                itemCount: addresses.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) =>
                    _AddressTile(
                      address: addresses[index],
                      typeColor: _typeColor(addresses[index].type),
                      typeIcon: _typeIcon(addresses[index].type),
                      onEdit: () => _navigateToEdit(addresses[index]),
                      onDelete: () => _confirmDelete(addresses[index]),
                    ),
              ),
      ),
    );
  }

  Widget _buildEmptyState(AppLocalizations l10n) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.primary.withOpacity(0.08),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.location_off_outlined,
                size: 48, color: AppColors.primary.withOpacity(0.6)),
          ),
          const SizedBox(height: 16),
          Text(
            l10n.translate('no_address_saved'),
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: AppColors.darkGrey,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            l10n.translate('no_address_hint'),
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13, color: AppColors.grey),
          ),
        ],
      ),
    );
  }
}

// ─── Address tile ─────────────────────────────────────────────────────────────

class _AddressTile extends StatelessWidget {
  final AddressModel address;
  final Color typeColor;
  final IconData typeIcon;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const _AddressTile({
    required this.address,
    required this.typeColor,
    required this.typeIcon,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onEdit,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              // Icon
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: typeColor.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(typeIcon, color: typeColor, size: 22),
              ),
              const SizedBox(width: 14),
              // Content
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          address.displayLabel,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: typeColor,
                          ),
                        ),
                        if (address.isDefault) ...[
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: AppColors.primary.withOpacity(0.10),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              l10n.translate('default_label'),
                              style: TextStyle(
                                  fontSize: 10,
                                  color: AppColors.primary,
                                  fontWeight: FontWeight.w700),
                            ),
                          ),
                        ]
                      ],
                    ),
                    const SizedBox(height: 3),
                    Text(
                      address.formattedAddress ?? '—',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontSize: 13, color: AppColors.textSecondary),
                    ),
                  ],
                ),
              ),
              // Actions
              PopupMenuButton<_Action>(
                icon: const Icon(Icons.more_vert_rounded,
                    color: AppColors.grey),
                onSelected: (action) {
                  switch (action) {
                    case _Action.edit:   onEdit();   break;
                    case _Action.delete: onDelete(); break;
                  }
                },
                itemBuilder: (_) => [
                  PopupMenuItem(
                    value: _Action.edit,
                    child: Row(children: [
                      const Icon(Icons.edit_outlined, size: 18),
                      const SizedBox(width: 8),
                      Text(l10n.translate('edit')),
                    ]),
                  ),
                  PopupMenuItem(
                    value: _Action.delete,
                    child: Row(children: [
                      const Icon(Icons.delete_outline,
                          size: 18, color: AppColors.error),
                      const SizedBox(width: 8),
                      Text(l10n.translate('delete'),
                          style: const TextStyle(color: AppColors.error)),
                    ]),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

enum _Action { edit, delete }
