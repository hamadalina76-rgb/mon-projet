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
      'log_in': 'Log In',
      'sign_up': 'Sign Up',
      'email': 'Email',
      'email_address': 'Email Address',
      'email_hint': 'you@example.com',
      'password': 'Password',
      'forgot_password': 'Forgot Password?',
      'dont_have_account': "Don't have an account?",
      'home': 'Home',
      'orders': 'Orders',
      'profile': 'Profile',
      'search_restaurants': 'Search restaurants...',
      'cart': 'Cart',
      'checkout': 'Checkout',
      'order_placed': 'Order Placed Successfully',
      'track_order': 'Track Order',
      
      // Auth screens
      'sign_in': 'Sign In',
      'first_name': 'First Name',
      'first_name_hint': 'first name',
      'last_name': 'Last Name',
      'last_name_hint': 'last name',
      'phone_number': 'Phone Number',
      'phone_hint': '+216 22 345 678',
      'or_continue_with': 'Or continue with',
      'having_trouble': 'Having trouble? ',
      'contact_support': 'Contact Support',
      
      // Validation messages
      'please_enter_email': 'Please enter your email',
      'invalid_email': 'Invalid email',
      'please_enter_password': 'Please enter your password',
      'password_min_length': 'Password must have at least 8 characters',
      'please_enter_first_name': 'Please enter your first name',
      'please_enter_last_name': 'Please enter your last name',
      'please_enter_valid_email': 'Please enter your valid email',
      'please_enter_phone': 'Please enter your phone number',
      'phone_min_length': 'Phone number must have 8 numbers',
      
      // Forgot Password
      'forgot_password_title': 'Forgot Password?',
      'forgot_password_desc': "Don't worry! It happens. Please enter the email address associated with your account.",
      'send_code': 'Send Code',
      'remember_password': 'Remember your password? ',
      
      // Verify OTP
      'verify_code': 'Verify Code',
      'verify_code_desc': 'Please enter the code we just sent to',
      'didnt_receive_code': "Didn't receive the code? ",
      'resend': 'Resend',
      'please_enter_6_digit_code': 'Please enter the 6-digit code',
      'code_resent_success': 'Code resent successfully',
      
      // Reset Password
      'reset_password': 'Reset Password',
      'reset_password_desc': "Please enter your new password. Make sure it's at least 6 characters long.",
      'new_password': 'New Password',
      'confirm_password': 'Confirm Password',
      'please_enter_password_field': 'Please enter a password',
      'minimum_6_chars': 'Minimum 6 characters',
      'please_confirm_password': 'Please confirm the password',
      'passwords_dont_match': 'Passwords do not match',
      'password_reset_success': 'Password reset successfully!',
      
      // Social login in development
      'google_signin_dev': 'Google Sign In - Under development',
      'facebook_signin_dev': 'Facebook Sign In - Under development',
      
      // Home/Onboarding screen
      'skip': 'Skip',
      'next': 'Next',
      'get_started': 'Get Started',
      'terms_prefix': 'By continuing, you agree to our ',
      'terms': 'Terms',
      'privacy_policy': 'Privacy Policy',
      
      // Onboarding pages
      'onboarding_page1_title': 'Deliver with',
      'onboarding_page1_highlight': 'Speed & Precision',
      'onboarding_page1_desc': 'Manage your fleet in real-time. The fastest way to get from A to B starts here.',
      'onboarding_page2_title': 'Track Your',
      'onboarding_page2_highlight': 'Order Live',
      'onboarding_page2_desc': 'Real-time tracking keeps you informed every step of the way. Never miss a delivery.',
      'onboarding_page3_title': 'Fast & Reliable',
      'onboarding_page3_highlight': 'Service',
      'onboarding_page3_desc': 'Experience lightning-fast deliveries with our trusted network of professional couriers.',
    },
    'fr': {
      'app_name': 'SpeedLine',
      'welcome': 'Bienvenue',
      'login': 'Connexion',
      'log_in': 'Connexion',
      'sign_up': "S'inscrire",
      'email': 'Email',
      'email_address': 'Adresse Email',
      'email_hint': 'votre@email.com',
      'password': 'Mot de passe',
      'forgot_password': 'Mot de passe oublié?',
      'dont_have_account': "Vous n'avez pas de compte?",
      'home': 'Accueil',
      'orders': 'Commandes',
      'profile': 'Profil',
      'search_restaurants': 'Rechercher des restaurants...',
      'cart': 'Panier',
      'checkout': 'Commander',
      'order_placed': 'Commande passée avec succès',
      'track_order': 'Suivre la commande',
      
      // Auth screens
      'sign_in': 'Se Connecter',
      'first_name': 'Prénom',
      'first_name_hint': 'prénom',
      'last_name': 'Nom',
      'last_name_hint': 'nom',
      'phone_number': 'Numéro de Téléphone',
      'phone_hint': '+216 22 345 678',
      'or_continue_with': 'Ou continuer avec',
      'having_trouble': 'Besoin d\'aide? ',
      'contact_support': 'Contacter le Support',
      
      // Validation messages
      'please_enter_email': 'Veuillez entrer votre email',
      'invalid_email': 'Email invalide',
      'please_enter_password': 'Veuillez entrer votre mot de passe',
      'password_min_length': 'Le mot de passe doit contenir au moins 8 caractères',
      'please_enter_first_name': 'Veuillez entrer votre prénom',
      'please_enter_last_name': 'Veuillez entrer votre nom',
      'please_enter_valid_email': 'Veuillez entrer votre email valide',
      'please_enter_phone': 'Veuillez entrer votre numéro de téléphone',
      'phone_min_length': 'Le numéro doit contenir 8 chiffres',
      
      // Forgot Password
      'forgot_password_title': 'Mot de passe oublié?',
      'forgot_password_desc': "Pas d'inquiétude! Cela arrive. Veuillez entrer l'adresse email associée à votre compte.",
      'send_code': 'Envoyer le Code',
      'remember_password': 'Vous vous souvenez de votre mot de passe? ',
      
      // Verify OTP
      'verify_code': 'Vérifier le Code',
      'verify_code_desc': 'Veuillez entrer le code que nous venons d\'envoyer à',
      'didnt_receive_code': "Vous n'avez pas reçu le code? ",
      'resend': 'Renvoyer',
      'please_enter_6_digit_code': 'Veuillez entrer le code à 6 chiffres',
      'code_resent_success': 'Code renvoyé avec succès',
      
      // Reset Password
      'reset_password': 'Réinitialiser le Mot de Passe',
      'reset_password_desc': 'Veuillez entrer votre nouveau mot de passe. Assurez-vous qu\'il contient au moins 6 caractères.',
      'new_password': 'Nouveau Mot de Passe',
      'confirm_password': 'Confirmer le Mot de Passe',
      'please_enter_password_field': 'Veuillez entrer un mot de passe',
      'minimum_6_chars': 'Minimum 6 caractères',
      'please_confirm_password': 'Veuillez confirmer le mot de passe',
      'passwords_dont_match': 'Les mots de passe ne correspondent pas',
      'password_reset_success': 'Mot de passe réinitialisé avec succès!',
      
      // Social login in development
      'google_signin_dev': 'Google Sign In - En cours de développement',
      'facebook_signin_dev': 'Facebook Sign In - En cours de développement',
      
      // Home/Onboarding screen
      'skip': 'Passer',
      'next': 'Suivant',
      'get_started': 'Commencer',
      'terms_prefix': 'En continuant, vous acceptez nos ',
      'terms': 'Conditions',
      'privacy_policy': 'Politique de confidentialité',
      
      // Onboarding pages
      'onboarding_page1_title': 'Livraison avec',
      'onboarding_page1_highlight': 'Rapidité & Précision',
      'onboarding_page1_desc': 'Gérez votre flotte en temps réel. Le moyen le plus rapide d\'aller d\'un point A à B commence ici.',
      'onboarding_page2_title': 'Suivez votre',
      'onboarding_page2_highlight': 'Commande en Direct',
      'onboarding_page2_desc': 'Le suivi en temps réel vous tient informé à chaque étape. Ne manquez jamais une livraison.',
      'onboarding_page3_title': 'Service Rapide',
      'onboarding_page3_highlight': 'et Fiable',
      'onboarding_page3_desc': 'Profitez de livraisons ultra-rapides avec notre réseau de coursiers professionnels de confiance.',
    },
    'ar': {
      'app_name': 'سبيد لاين',
      'welcome': 'مرحبا',
      'login': 'تسجيل الدخول',
      'log_in': 'تسجيل الدخول',
      'sign_up': 'إنشاء حساب',
      'email': 'البريد الإلكتروني',
      'email_address': 'عنوان البريد الإلكتروني',
      'email_hint': 'بريدك@example.com',
      'password': 'كلمة المرور',
      'forgot_password': 'نسيت كلمة المرور؟',
      'dont_have_account': 'ليس لديك حساب؟',
      'home': 'الرئيسية',
      'orders': 'الطلبات',
      'profile': 'الملف الشخصي',
      'search_restaurants': 'ابحث عن المطاعم...',
      'cart': 'السلة',
      'checkout': 'الدفع',
      'order_placed': 'تم تقديم الطلب بنجاح',
      'track_order': 'تتبع الطلب',
      
      // Auth screens
      'sign_in': 'تسجيل الدخول',
      'first_name': 'الاسم الأول',
      'first_name_hint': 'الاسم الأول',
      'last_name': 'اسم العائلة',
      'last_name_hint': 'اسم العائلة',
      'phone_number': 'رقم الهاتف',
      'phone_hint': '+216 22 345 678',
      'or_continue_with': 'أو المتابعة مع',
      'having_trouble': 'تواجه مشكلة؟ ',
      'contact_support': 'اتصل بالدعم',
      
      // Validation messages
      'please_enter_email': 'يرجى إدخال بريدك الإلكتروني',
      'invalid_email': 'بريد إلكتروني غير صالح',
      'please_enter_password': 'يرجى إدخال كلمة المرور',
      'password_min_length': 'يجب أن تحتوي كلمة المرور على 8 أحرف على الأقل',
      'please_enter_first_name': 'يرجى إدخال اسمك الأول',
      'please_enter_last_name': 'يرجى إدخال اسم العائلة',
      'please_enter_valid_email': 'يرجى إدخال بريد إلكتروني صالح',
      'please_enter_phone': 'يرجى إدخال رقم هاتفك',
      'phone_min_length': 'يجب أن يحتوي رقم الهاتف على 8 أرقام',
      
      // Forgot Password
      'forgot_password_title': 'نسيت كلمة المرور؟',
      'forgot_password_desc': 'لا تقلق! هذا يحدث. يرجى إدخال عنوان البريد الإلكتروني المرتبط بحسابك.',
      'send_code': 'إرسال الرمز',
      'remember_password': 'تذكرت كلمة المرور؟ ',
      
      // Verify OTP
      'verify_code': 'تحقق من الرمز',
      'verify_code_desc': 'يرجى إدخال الرمز الذي أرسلناه للتو إلى',
      'didnt_receive_code': 'لم تستلم الرمز؟ ',
      'resend': 'إعادة إرسال',
      'please_enter_6_digit_code': 'يرجى إدخال الرمز المكون من 6 أرقام',
      'code_resent_success': 'تم إعادة إرسال الرمز بنجاح',
      
      // Reset Password
      'reset_password': 'إعادة تعيين كلمة المرور',
      'reset_password_desc': 'يرجى إدخال كلمة المرور الجديدة. تأكد من أنها تحتوي على 6 أحرف على الأقل.',
      'new_password': 'كلمة المرور الجديدة',
      'confirm_password': 'تأكيد كلمة المرور',
      'please_enter_password_field': 'يرجى إدخال كلمة مرور',
      'minimum_6_chars': 'الحد الأدنى 6 أحرف',
      'please_confirm_password': 'يرجى تأكيد كلمة المرور',
      'passwords_dont_match': 'كلمات المرور غير متطابقة',
      'password_reset_success': 'تم إعادة تعيين كلمة المرور بنجاح!',
      
      // Social login in development
      'google_signin_dev': 'تسجيل الدخول عبر Google - قيد التطوير',
      'facebook_signin_dev': 'تسجيل الدخول عبر Facebook - قيد التطوير',
      
      // Home/Onboarding screen
      'skip': 'تخطي',
      'next': 'التالي',
      'get_started': 'ابدأ',
      'terms_prefix': 'بالمتابعة، فإنك توافق على ',
      'terms': 'الشروط',
      'privacy_policy': 'سياسة الخصوصية',
      
      // Onboarding pages
      'onboarding_page1_title': 'التوصيل مع',
      'onboarding_page1_highlight': 'السرعة والدقة',
      'onboarding_page1_desc': 'إدارة أسطولك في الوقت الفعلي. أسرع طريقة للانتقال من أ إلى ب تبدأ هنا.',
      'onboarding_page2_title': 'تتبع',
      'onboarding_page2_highlight': 'طلبك مباشرة',
      'onboarding_page2_desc': 'التتبع في الوقت الفعلي يبقيك على اطلاع في كل خطوة. لن تفوت أي توصيل.',
      'onboarding_page3_title': 'خدمة سريعة',
      'onboarding_page3_highlight': 'وموثوقة',
      'onboarding_page3_desc': 'استمتع بتوصيل سريع للغاية مع شبكتنا الموثوقة من السعاة المحترفين.',
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
