import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'locale_config.dart';

final localeProvider = NotifierProvider<LocaleNotifier, Locale>(() {
  return LocaleNotifier();
});

class LocaleNotifier extends Notifier<Locale> {
  static const String _localeKey = 'app_locale';

  @override
  Locale build() {
    _loadLocale();
    return defaultLocale;
  }

  Future<void> _loadLocale() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final languageCode = prefs.getString(_localeKey);
      if (languageCode == null) {
        state = defaultLocale;
        return;
      }

      state = supportedLocales.firstWhere(
        (locale) => locale.languageCode == languageCode,
        orElse: () => defaultLocale,
      );
    } catch (_) {
      state = defaultLocale;
    }
  }

  Future<void> setLocale(Locale locale) async {
    final isSupported = supportedLocales.any(
      (l) => l.languageCode == locale.languageCode,
    );
    if (!isSupported) return;

    state = locale;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_localeKey, locale.languageCode);
    } catch (_) {}
  }
}
