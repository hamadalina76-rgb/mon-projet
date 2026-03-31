import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/media_url.dart';
import '../../data/models/partner_nearby_dto.dart';
import '../providers/favorite_partners_provider.dart';
import 'partner_details_screen.dart';

class PartnerFavoritesScreen extends ConsumerStatefulWidget {
  const PartnerFavoritesScreen({super.key});

  @override
  ConsumerState<PartnerFavoritesScreen> createState() =>
      _PartnerFavoritesScreenState();
}

class _PartnerFavoritesScreenState
    extends ConsumerState<PartnerFavoritesScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(favoritePartnersNotifierProvider.notifier).loadFavorites();
    });
  }

  Future<void> _onRefresh() async {
    await ref.read(favoritePartnersNotifierProvider.notifier).refresh();
  }

  Future<bool> _confirmRemove(BuildContext context, String partnerName) async {
    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.translate('favorites_remove_dialog_title')),
        content: Text(
          '${l10n.translate('favorites_remove_dialog_message')} "$partnerName"',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.translate('cancel')),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.error,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(l10n.translate('favorites_remove_action')),
          ),
        ],
      ),
    );

    return confirmed ?? false;
  }

  void _showErrorSnack() {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(l10n.translate('favorites_error_retry')),
        backgroundColor: AppColors.error,
      ),
    );
  }

  void _showSuccessSnack() {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(l10n.translate('favorites_removed_success')),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final favoritesState = ref.watch(favoritePartnersNotifierProvider);
    final favorites = favoritesState.favoritePartners;
    final width = MediaQuery.of(context).size.width;
    final horizontalPadding = width >= 900
        ? width * 0.16
        : width >= 600
        ? 22.0
        : 14.0;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        leadingWidth: 56,
        leading: Padding(
          padding: const EdgeInsets.only(left: 12, top: 8, bottom: 8),
          child: InkWell(
            onTap: () => Navigator.of(context).pop(),
            borderRadius: BorderRadius.circular(24),
            child: Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: AppColors.background,
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.border),
              ),
              child: const Icon(
                Icons.arrow_back_ios_new_rounded,
                color: AppColors.textPrimary,
                size: 16,
              ),
            ),
          ),
        ),
        titleSpacing: 4,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.translate('favorites_title'),
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
            ),
            Text(
              '${favorites.length} ${l10n.translate('favorites_count_label')}',
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.textSecondary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.white, AppColors.background],
          ),
        ),
        child: RefreshIndicator(
          onRefresh: _onRefresh,
          color: AppColors.primary,
          child: favoritesState.isLoading && favorites.isEmpty
              ? ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: const [
                    SizedBox(height: 170),
                    Center(child: CircularProgressIndicator()),
                  ],
                )
              : favorites.isEmpty
              ? ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: EdgeInsets.symmetric(horizontal: horizontalPadding),
                  children: [
                    const SizedBox(height: 70),
                    _EmptyFavoritesState(
                      l10n: l10n,
                      onExplore: () => Navigator.of(context).pop(),
                    ),
                  ],
                )
              : ListView.separated(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: EdgeInsets.fromLTRB(
                    horizontalPadding,
                    14,
                    horizontalPadding,
                    24,
                  ),
                  itemCount: favorites.length + 1,
                  separatorBuilder: (_, __) => const SizedBox(height: 12),
                  itemBuilder: (context, index) {
                    if (index == 0) {
                      return _FavoritesSummaryCard(
                        l10n: l10n,
                        count: favorites.length,
                        syncing: favoritesState.isSyncing,
                        hasError: favoritesState.errorMessage != null,
                      );
                    }

                    final partner = favorites[index - 1];
                    return Dismissible(
                      key: ValueKey('favorite-${partner.id}'),
                      direction: DismissDirection.endToStart,
                      background: Container(
                        alignment: Alignment.centerRight,
                        padding: const EdgeInsets.symmetric(horizontal: 18),
                        decoration: BoxDecoration(
                          color: AppColors.error,
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: const Icon(
                          Icons.delete_outline_rounded,
                          color: Colors.white,
                          size: 28,
                        ),
                      ),
                      confirmDismiss: (_) =>
                          _confirmRemove(context, partner.displayName),
                      onDismissed: (_) async {
                        final result = await ref
                            .read(favoritePartnersNotifierProvider.notifier)
                            .toggleFavorite(
                              partnerId: partner.id,
                              partnerSnapshot: partner,
                            );

                        if (!result.success) {
                          _showErrorSnack();
                          return;
                        }
                        _showSuccessSnack();
                      },
                      child: _FavoriteListCard(
                        partner: partner,
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
                      ),
                    );
                  },
                ),
        ),
      ),
    );
  }
}

class _FavoritesSummaryCard extends StatelessWidget {
  final AppLocalizations l10n;
  final int count;
  final bool syncing;
  final bool hasError;

  const _FavoritesSummaryCard({
    required this.l10n,
    required this.count,
    required this.syncing,
    required this.hasError,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFF4F4), Color(0xFFFFFFFF)],
        ),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons.favorite_rounded,
                color: AppColors.primary,
                size: 18,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  l10n.translate('favorites_subtitle'),
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  '$count',
                  style: const TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          if (syncing || hasError) ...[
            const SizedBox(height: 10),
            Row(
              children: [
                if (syncing)
                  const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else
                  const Icon(
                    Icons.error_outline,
                    color: AppColors.error,
                    size: 14,
                  ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    syncing
                        ? l10n.translate('favorites_syncing_label')
                        : l10n.translate('favorites_error_retry'),
                    style: TextStyle(
                      fontSize: 12,
                      color: syncing ? AppColors.textSecondary : AppColors.error,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _EmptyFavoritesState extends StatelessWidget {
  final AppLocalizations l10n;
  final VoidCallback onExplore;

  const _EmptyFavoritesState({required this.l10n, required this.onExplore});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 28, 20, 24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppColors.border),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.primary.withValues(alpha: 0.10),
            ),
            child: const Icon(
              Icons.favorite_border_rounded,
              size: 34,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            l10n.translate('favorites_empty_title'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            l10n.translate('favorites_empty_description'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              color: AppColors.textSecondary,
              height: 1.35,
            ),
          ),
          const SizedBox(height: 22),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: onExplore,
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: Text(l10n.translate('favorites_explore_button')),
            ),
          ),
        ],
      ),
    );
  }
}

class _FavoriteListCard extends StatelessWidget {
  final PartnerNearbyDto partner;
  final VoidCallback onTap;

  const _FavoriteListCard({required this.partner, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final logoUrl = resolveMediaUrl(partner.logo);
    final closed = !partner.isOpen;
    final width = MediaQuery.of(context).size.width;
    final compact = width < 380;

    final prep = partner.preparationTime != null
        ? '${partner.preparationTime} min'
        : '-- min';
    final delivery = partner.deliveryFee == null || partner.deliveryFee == 0
        ? l10n.translate('free')
        : _formatMoney(partner.deliveryFee!);

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: Container(
          padding: EdgeInsets.all(compact ? 12 : 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.border),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.05),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: compact ? 58 : 64,
                height: compact ? 58 : 64,
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(16),
                ),
                clipBehavior: Clip.antiAlias,
                child: logoUrl.isEmpty
                    ? const Icon(Icons.storefront, color: AppColors.textHint)
                    : CachedNetworkImage(
                        imageUrl: logoUrl,
                        fit: BoxFit.cover,
                        errorWidget: (_, __, ___) => const Icon(
                          Icons.storefront,
                          color: AppColors.textHint,
                        ),
                      ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      partner.displayName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      partner.type?.replaceAll('_', ' ') ?? l10n.translate('partner'),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _MetaPill(
                          icon: Icons.star_rounded,
                          iconColor: AppColors.starYellow,
                          text: partner.rating.toStringAsFixed(1),
                        ),
                        _MetaPill(
                          icon: Icons.schedule_rounded,
                          iconColor: AppColors.textSecondary,
                          text: prep,
                        ),
                        _MetaPill(
                          icon: Icons.delivery_dining_outlined,
                          iconColor: AppColors.primary2,
                          text: delivery,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: closed ? const Color(0xFFFFEFF1) : const Color(0xFFEAF8EF),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: closed ? const Color(0xFFF4C5CB) : const Color(0xFFC5ECD4),
                  ),
                ),
                child: Text(
                  closed
                      ? l10n.translate('partner_status_closed')
                      : l10n.translate('partner_status_open'),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: closed ? AppColors.error : const Color(0xFF159957),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatMoney(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(3)} DT';
  }
}

class _MetaPill extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String text;

  const _MetaPill({
    required this.icon,
    required this.iconColor,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: iconColor.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: iconColor),
          const SizedBox(width: 5),
          Text(
            text,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
