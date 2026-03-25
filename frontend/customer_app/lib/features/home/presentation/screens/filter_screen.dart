import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../partners/presentation/providers/nearby_partners_provider.dart';

class FilterScreen extends StatelessWidget {
  const FilterScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Filter')),
      body: const Center(child: Text('Filter Screen')),
    );
  }
}

class PartnerFiltersBottomSheet extends ConsumerWidget {
  final AppLocalizations l10n;
  final String searchQuery;

  const PartnerFiltersBottomSheet({
    super.key,
    required this.l10n,
    required this.searchQuery,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(nearbyPartnersNotifierProvider);
    final notifier = ref.read(nearbyPartnersNotifierProvider.notifier);
    final q = searchQuery.trim().toLowerCase();
    final resultCount = state.filteredPartners.where((p) {
      if (q.isEmpty) return true;
      final name = p.displayName.toLowerCase();
      final type = (p.type ?? '').toLowerCase();
      return name.contains(q) || type.contains(q);
    }).length;

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom + 10,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 10),
              Container(
                width: 46,
                height: 5,
                decoration: BoxDecoration(
                  color: const Color(0xFFD2D2D8),
                  borderRadius: BorderRadius.circular(99),
                ),
              ),
              const SizedBox(height: 14),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        l10n.translate('filters_and_sort'),
                        style: const TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                    OutlinedButton(
                      onPressed: notifier.resetFilters,
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: AppColors.primary),
                        foregroundColor: AppColors.primary,
                      ),
                      child: Text(l10n.translate('reset')),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              _FilterSwitchRow(
                icon: Icons.watch_later_outlined,
                label: l10n.translate('open_now'),
                value: state.openNowOnly,
                onChanged: notifier.setOpenNowOnly,
              ),
              _FilterSwitchRow(
                icon: Icons.local_shipping_outlined,
                label: l10n.translate('free_delivery_title'),
                value: state.freeDeliveryOnly,
                onChanged: notifier.setFreeDeliveryOnly,
              ),
              _FilterSectionLabel(label: l10n.translate('minimum_rating')),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _FilterChoiceChip(
                      label: '3.0+ ★',
                      selected: state.minRating == 3.0,
                      onTap: () => notifier.setMinRating(
                        state.minRating == 3.0 ? null : 3.0,
                      ),
                    ),
                    _FilterChoiceChip(
                      label: '4.0+ ★',
                      selected: state.minRating == 4.0,
                      onTap: () => notifier.setMinRating(
                        state.minRating == 4.0 ? null : 4.0,
                      ),
                    ),
                    _FilterChoiceChip(
                      label: '4.5+ ★',
                      selected: state.minRating == 4.5,
                      onTap: () => notifier.setMinRating(
                        state.minRating == 4.5 ? null : 4.5,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 10),
              _FilterSectionLabel(label: l10n.translate('delivery_time')),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _FilterChoiceChip(
                      label: '15 min',
                      selected: state.maxDeliveryTime == 15,
                      onTap: () => notifier.setMaxDeliveryTime(
                        state.maxDeliveryTime == 15 ? null : 15,
                      ),
                    ),
                    _FilterChoiceChip(
                      label: '30 min',
                      selected: state.maxDeliveryTime == 30,
                      onTap: () => notifier.setMaxDeliveryTime(
                        state.maxDeliveryTime == 30 ? null : 30,
                      ),
                    ),
                    _FilterChoiceChip(
                      label: '45 min',
                      selected: state.maxDeliveryTime == 45,
                      onTap: () => notifier.setMaxDeliveryTime(
                        state.maxDeliveryTime == 45 ? null : 45,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 10),
              _FilterSectionLabel(label: l10n.translate('sort_by')),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _FilterChoiceChip(
                      label: l10n.translate('sort_popularity'),
                      selected: state.sortOption == PartnerSortOption.popularity,
                      onTap: () =>
                          notifier.setSortOption(PartnerSortOption.popularity),
                    ),
                    _FilterChoiceChip(
                      label: l10n.translate('sort_rating'),
                      selected: state.sortOption == PartnerSortOption.rating,
                      onTap: () =>
                          notifier.setSortOption(PartnerSortOption.rating),
                    ),
                    _FilterChoiceChip(
                      label: l10n.translate('sort_newest'),
                      selected: state.sortOption == PartnerSortOption.newest,
                      onTap: () =>
                          notifier.setSortOption(PartnerSortOption.newest),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18),
                child: SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.black,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18),
                      ),
                    ),
                    child: Text(
                      '${l10n.translate('see_partners_count')} $resultCount ${l10n.translate('partners')}',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _FilterSectionLabel extends StatelessWidget {
  final String label;

  const _FilterSectionLabel({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 10),
      child: Align(
        alignment: Alignment.centerLeft,
        child: Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            letterSpacing: 0.8,
            color: AppColors.textHint,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

class _FilterSwitchRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _FilterSwitchRow({
    required this.icon,
    required this.label,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 4),
      child: Row(
        children: [
          Icon(icon, color: AppColors.textHint),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          Switch(value: value, onChanged: onChanged),
        ],
      ),
    );
  }
}

class _FilterChoiceChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChoiceChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
        decoration: BoxDecoration(
          color: selected ? AppColors.primary : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: selected ? AppColors.primary : AppColors.border,
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: AppColors.primary.withValues(alpha: 0.2),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: selected ? Colors.white : AppColors.textPrimary,
            fontSize: 15,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}
