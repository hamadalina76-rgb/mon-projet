import 'package:flutter/material.dart';

class AppLocalizations {
  final Locale locale;

  AppLocalizations(this.locale);

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate = _AppLocalizationsDelegate();

  static final Map<String, Map<String, String>> _localizedValues = {
    'en': {
      'app_name': 'SpeedLine',
      'welcome': 'Welcome',
      'login': 'Login',
      'email': 'Email',
      'password': 'Password',
      'forgot_password': 'Forgot Password?',
      'dont_have_account': "Don't have an account?",
      'sign_up': 'Sign Up',
      'home': 'Home',
      'orders': 'Orders',
      'profile': 'Profile',
      'search_restaurants': 'Search restaurants...',
      'cart': 'Cart',
      'checkout': 'Checkout',
      'order_placed': 'Order Placed Successfully',
      'track_order': 'Track Order',
    },
    'fr': {
      'app_name': 'SpeedLine',
      'welcome': 'Bienvenue',
      'login': 'Connexion',
      'email': 'Email',
      'password': 'Mot de passe',
      'forgot_password': 'Mot de passe oublié?',
      'dont_have_account': "Vous n'avez pas de compte?",
      'sign_up': "S'inscrire",
      'home': 'Accueil',
      'orders': 'Commandes',
      'profile': 'Profil',
      'search_restaurants': 'Rechercher des restaurants...',
      'cart': 'Panier',
      'checkout': 'Commander',
      'order_placed': 'Commande passée avec succès',
      'track_order': 'Suivre la commande',
    },
    'ar': {
      'app_name': 'سبيد لاين',
      'welcome': 'مرحبا',
      'login': 'تسجيل الدخول',
      'email': 'البريد الإلكتروني',
      'password': 'كلمة المرور',
      'forgot_password': 'نسيت كلمة المرور؟',
      'dont_have_account': 'ليس لديك حساب؟',
      'sign_up': 'إنشاء حساب',
      'home': 'الرئيسية',
      'orders': 'الطلبات',
      'profile': 'الملف الشخصي',
      'search_restaurants': 'ابحث عن المطاعم...',
      'cart': 'السلة',
      'checkout': 'الدفع',
      'order_placed': 'تم تقديم الطلب بنجاح',
      'track_order': 'تتبع الطلب',
    },
  };

  String translate(String key) {
    return _localizedValues[locale.languageCode]?[key] ?? key;
  }
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => ['en', 'fr', 'ar'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async {
    return AppLocalizations(locale);
  }

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
