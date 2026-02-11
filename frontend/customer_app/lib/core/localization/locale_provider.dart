import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'locale_config.dart';

/// Provider pour gérer la locale de l'application
final localeProvider = StateNotifierProvider<LocaleNotifier, Locale>((ref) {
  return LocaleNotifier();
});

class LocaleNotifier extends StateNotifier<Locale> {
  static const String _localeKey = 'app_locale';
  static const String _languageSelectedKey = 'language_selected';
  
  LocaleNotifier() : super(defaultLocale) {
    _loadLocale();
  }

  /// Charger la locale sauvegardée
  Future<void> _loadLocale() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final languageCode = prefs.getString(_localeKey);
      
      if (languageCode != null) {
        final locale = supportedLocales.firstWhere(
          (locale) => locale.languageCode == languageCode,
          orElse: () => defaultLocale,
        );
        state = locale;
      }
    } catch (e) {
      // En cas d'erreur, utiliser la locale par défaut
      state = defaultLocale;
    }
  }

  /// Changer la locale et la sauvegarder
  Future<void> setLocale(Locale locale) async {
    if (supportedLocales.contains(locale)) {
      state = locale;
      
      try {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(_localeKey, locale.languageCode);
        await prefs.setBool(_languageSelectedKey, true);
      } catch (e) {
        // Erreur lors de la sauvegarde
      }
    }
  }

  /// Vérifier si l'utilisateur a déjà choisi une langue
  Future<bool> hasSelectedLanguage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getBool(_languageSelectedKey) ?? false;
    } catch (e) {
      return false;
    }
  }

  /// Obtenir la langue actuelle
  String get currentLanguage => state.languageCode;
}
