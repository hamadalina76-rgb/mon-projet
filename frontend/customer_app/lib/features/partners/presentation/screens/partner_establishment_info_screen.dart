import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../data/models/partner_nearby_dto.dart';

class PartnerEstablishmentInfoScreen extends StatelessWidget {
  final PartnerNearbyDto partner;

  const PartnerEstablishmentInfoScreen({super.key, required this.partner});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final topPadding = MediaQuery.of(context).padding.top;
    final lightTheme = Theme.of(context).copyWith(
      brightness: Brightness.light,
      colorScheme: Theme.of(context).colorScheme.copyWith(
        brightness: Brightness.light,
        surface: Colors.white,
        onSurface: AppColors.textPrimary,
      ),
      scaffoldBackgroundColor: AppColors.background,
      canvasColor: AppColors.background,
    );

    return Theme(
      data: lightTheme,
      child: Scaffold(
        backgroundColor: AppColors.background,
        body: LayoutBuilder(
          builder: (context, constraints) {
            final width = constraints.maxWidth;
            final horizontalPadding = _responsive(16, 24, width);
            final titleSize = _responsive(28, 40, width);
            final sectionSize = _responsive(24, 34, width);
            final bodySize = _responsive(15, 18, width);

            return Stack(
              children: [
                SingleChildScrollView(
                  padding: EdgeInsets.fromLTRB(
                    horizontalPadding,
                    topPadding + 72,
                    horizontalPadding,
                    30,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.translate('establishment_info_title'),
                        style: TextStyle(
                          fontSize: titleSize,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 18),
                      Text(
                        l10n.translate('establishment_address_title'),
                        style: TextStyle(
                          fontSize: sectionSize,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        [
                          partner.address,
                          partner.city,
                        ].where((e) => (e ?? '').trim().isNotEmpty).join(', '),
                        style: TextStyle(
                          fontSize: bodySize,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text(
                        l10n.translate('establishment_hours_title'),
                        style: TextStyle(
                          fontSize: sectionSize,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      ..._buildHours(context, partner, bodySize),
                      const SizedBox(height: 24),
                      if ((partner.description ?? '').trim().isNotEmpty) ...[
                        Text(
                          l10n.translate('establishment_description_title'),
                          style: TextStyle(
                            fontSize: sectionSize,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          partner.description!,
                          style: TextStyle(
                            fontSize: bodySize,
                            height: 1.4,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 24),
                      ],
                      Text(
                        l10n.translate('establishment_contact_title'),
                        style: TextStyle(
                          fontSize: sectionSize,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 10),
                      if ((partner.phoneNumber ?? '').trim().isNotEmpty)
                        Text(
                          partner.phoneNumber!,
                          style: TextStyle(
                            fontSize: bodySize,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      if ((partner.email ?? '').trim().isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(
                            partner.email!,
                            style: TextStyle(
                              fontSize: bodySize,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                Positioned(
                  top: topPadding + 10,
                  left: 14,
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
              ],
            );
          },
        ),
      ),
    );
  }

  double _responsive(double min, double max, double width) {
    final t = ((width - 320) / 480).clamp(0.0, 1.0);
    return min + (max - min) * t;
  }

  List<Widget> _buildHours(
    BuildContext context,
    PartnerNearbyDto partner,
    double bodySize,
  ) {
    final l10n = AppLocalizations.of(context);
    final map = <String, OpeningHourDto>{};
    for (final h in partner.openingHours) {
      final key = h.dayOfWeek.toUpperCase();
      if (key.isNotEmpty && !map.containsKey(key)) {
        map[key] = h;
      }
    }

    final days = [
      ('MONDAY', l10n.translate('day_monday')),
      ('TUESDAY', l10n.translate('day_tuesday')),
      ('WEDNESDAY', l10n.translate('day_wednesday')),
      ('THURSDAY', l10n.translate('day_thursday')),
      ('FRIDAY', l10n.translate('day_friday')),
      ('SATURDAY', l10n.translate('day_saturday')),
      ('SUNDAY', l10n.translate('day_sunday')),
    ];

    return days.map((entry) {
      final h = map[entry.$1];
      String value = l10n.translate('establishment_closed');
      if (h != null) {
        if (h.is24Hours) {
          value = l10n.translate('establishment_24h');
        } else if (!h.isClosed && h.openTime != null && h.closeTime != null) {
          value = '${h.openTime} - ${h.closeTime}';
        }
      }

      return Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Text(
          '${entry.$2} : $value',
          style: TextStyle(fontSize: bodySize, color: AppColors.textSecondary),
        ),
      );
    }).toList();
  }
}
