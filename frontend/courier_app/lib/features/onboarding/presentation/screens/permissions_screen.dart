import 'package:flutter/material.dart';

import '../../../../core/localization/app_localizations.dart';

class PermissionsScreen extends StatelessWidget {
  const PermissionsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      body: Center(child: Text(l10n.translate('permissions_title'))),
    );
  }
}
