import 'package:flutter/material.dart';
import 'app_localizations.dart';

/// Extension to easily access AppLocalizations from BuildContext
extension LocalizationExtension on BuildContext {
  AppLocalizations? get loc => AppLocalizations.of(this);
  
  String tr(String key) {
    return AppLocalizations.of(this)?.translate(key) ?? key;
  }
}
