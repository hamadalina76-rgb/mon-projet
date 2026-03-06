import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/data/models/saved_location.dart';

/// Écran de sélection du type d'adresse
/// Affiché depuis AddressesScreen via le FAB "Ajouter une adresse"
class AddressTypeSelectorScreen extends StatelessWidget {
  /// Si l'utilisateur arrive depuis la confirmation GPS, on propage la location
  final SavedLocation? fromLocation;

  /// ID du client connecté (String UUID auth-service)
  final String customerId;

  /// Si fourni, navigue vers cette route après la sauvegarde de l'adresse
  /// (ex: RouteNames.explore quand on vient du flux de confirmation GPS)
  final String? redirectOnSuccess;

  const AddressTypeSelectorScreen({
    super.key,
    required this.customerId,
    this.fromLocation,
    this.redirectOnSuccess,
  });

  static const List<_TypeOption> _options = [
    _TypeOption(
      type: AddressType.home,
      icon: Icons.home_rounded,
      titleKey: 'address_type_home',
      subtitleKey: 'home_type_subtitle',
      color: AppColors.primary,
    ),
    _TypeOption(
      type: AddressType.work,
      icon: Icons.business_rounded,
      titleKey: 'address_type_work',
      subtitleKey: 'work_type_subtitle',
      color: AppColors.primaryDark,
    ),
    _TypeOption(
      type: AddressType.apartment,
      icon: Icons.apartment_rounded,
      titleKey: 'address_type_apartment',
      subtitleKey: 'apartment_type_subtitle',
      color: AppColors.secondary,
    ),
    _TypeOption(
      type: AddressType.other,
      icon: Icons.place_rounded,
      titleKey: 'address_type_other',
      subtitleKey: 'other_type_subtitle',
      color: AppColors.secondaryDark,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded,
              color: AppColors.textPrimary),
          onPressed: () => context.pop(),
        ),
        title: Text(
          l10n.translate('address_type_label'),
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 24),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                l10n.translate('choose_address_type'),
                style: const TextStyle(
                  fontSize: 15,
                  color: AppColors.textSecondary,
                  height: 1.5,
                ),
              ),
            ),
            const SizedBox(height: 24),
            ..._options.map((option) => _buildCard(context, option, l10n)),
          ],
        ),
      ),
    );
  }

  Widget _buildCard(BuildContext context, _TypeOption option, AppLocalizations l10n) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () {
            context.push(
              RouteNames.addressDetails,
              extra: {
                'type': option.type,
                'customerId': customerId,
                'fromLocation': fromLocation,
                'redirectOnSuccess': redirectOnSuccess,
              },
            );
          },
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                // Icon container
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: option.color.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(option.icon, color: option.color, size: 26),
                ),
                const SizedBox(width: 16),
                // Text
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.translate(option.titleKey),
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        l10n.translate(option.subtitleKey),
                        style: const TextStyle(
                          fontSize: 13,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(Icons.arrow_forward_ios_rounded,
                    size: 16, color: option.color),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _TypeOption {
  final AddressType type;
  final IconData icon;
  final String titleKey;
  final String subtitleKey;
  final Color color;

  const _TypeOption({
    required this.type,
    required this.icon,
    required this.titleKey,
    required this.subtitleKey,
    required this.color,
  });
}
