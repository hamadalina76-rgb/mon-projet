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
      'verify_identity': 'Verify Your Identity',
      'otp_sent_to': 'We sent a verification code to',
      'code_expired': 'Code Expired',
      'code_expired_message': 'Your OTP code has expired. Please request a new code.',
      'back': 'Back',
      'resend_code': 'Resend',
      'expires_in': 'Expires in',
      'verify_and_login': 'Verify and Login',
      'enter_6_digit_code': 'Please enter a 6-digit code',
      'invalid_or_expired_code': 'Invalid or expired code',
      'login_success': '✓ Login successful!',
      'resend_login_otp_message': 'To resend the code, please log in again',
      'wait': 'Wait',
      'seconds': 'seconds',
      'sec': 's',
      
      // Snackbar messages
      'login_failed': 'Login failed. Please check your credentials.',
      'otp_verification_failed': 'OTP verification failed. Please try again.',
      'otp_verification_success': 'OTP verified successfully!',
      'password_reset_failed': 'Password reset failed. Please try again.',
      'password_reset_complete': 'Your password has been reset successfully!',
      'email_not_found': 'Email not found. Please check and try again.',
      'invalid_credentials': 'Invalid email or password.',
      'generic_error': 'An error occurred. Please try again.',
      'please_wait': 'Please wait...',
      'otp_sent': 'OTP sent successfully to your email.',
      'stay_connected': 'Stay connected',
      
      // Accueil Screen
      'welcome_speedline': 'Welcome to SpeedLine!',
      'welcome_message': 'You are now logged in to your account.\nThe full home screen is coming soon!',
      'welcome_user': 'Welcome',
      
      // Settings Screen
      'settings': 'Settings',
      'account': 'ACCOUNT',
      'edit_profile': 'Edit Profile',
      'manage_preferences': 'Manage your profile information',
      'change_password': 'Change Password',
      'update_security_credentials': 'Update your security credentials',
      'app_preferences': 'APP PREFERENCES',
      'language': 'Language',
      'choose_app_language': 'Choose your app language',
      'dark_mode': 'Dark Mode',
      'enable_dark_theme': 'Enable dark theme',
      'dark_mode_enabled': 'Dark mode enabled',
      'light_mode_enabled': 'Light mode enabled',
      'notifications': 'NOTIFICATIONS',
      'push_notifications': 'Push Notifications',
      'receive_order_updates': 'Receive order updates and offers',
      'email_notifications': 'Email Notifications',
      'receive_news_offers': 'Receive news and special offers',
      'support': 'SUPPORT',
      'help_center': 'Help Center',
      'faq_support_articles': 'FAQ and support articles',
      'contact_us': 'Contact Us',
      'get_in_touch': 'Get in touch with our team',
      'terms_conditions': 'Terms & Conditions',
      'legal_information': 'Legal information and policies',
      'about': 'ABOUT',
      'app_version': 'App Version',
      'logout': 'Logout',
      'are_you_sure_logout': 'Are you sure you want to logout?',
      'cancel': 'Cancel',
      'select_language': 'Select Language',
      'english': 'English',
      'french': 'French',
      'arabic': 'Arabic',
      
      // Enable Location Screen
      'enable_location': 'Enable Location Services',
      'location_permission_title': 'What is your location?',
      'location_permission_desc': 'We need to know your location to show you nearby restaurants and ',
      'accurate_delivery': 'provide accurate delivery estimates',
      'allow_location_access': 'Allow Location Access',
      'enter_address_manually': 'Enter Address Manually',
      'skip_for_now': 'Skip for now',
      'location_services_disabled': 'Location services are disabled. Please enable them in settings.',
      'location_permission_denied': 'Location permission denied. Please allow access to continue.',
      'location_permanently_denied': 'Location permissions are permanently denied. Please enable them in app settings.',
      'location_timeout': 'Location request timed out. Please try again.',
      'location_error': 'Failed to get location',
      'manual_address_entry': 'Manual Address Entry',
      'manual_address_coming_soon': 'Manual address entry screen coming soon!',
      'ok': 'OK',
      
      // Edit Profile Screen
      'save_changes': 'Save Changes',
      'profile_updated_success': 'Profile updated successfully!',
      'profile_update_error': 'Failed to update profile. Please try again.',
      'choose_photo_source': 'Choose Photo Source',
      'take_photo': 'Take Photo',
      'choose_from_gallery': 'Choose from Gallery',
      'remove_photo': 'Remove Photo',
      'uploading': 'Uploading...',
      
      // Profile Screen
      'my_profile': 'My Profile',
      'delivery_addresses': 'Delivery Addresses',
      'manage_addresses': 'Manage your addresses',
      'add_new_address': 'Add New Address',
      'home_address': 'Home',
      'work_address': 'Work',
      'use_current_location': 'Use My Current Location',
      'payment_methods': 'Payment Methods',
      'manage_payment': 'Manage your payment methods',
      'add_payment_method': 'Add Payment Method',
      'order_history': 'Order History',
      'view_all_orders': 'View all your past orders',
      'delivered': 'DELIVERED',
      'reorder': 'Reorder',
      'view_details': 'View Details',
      'manage_all': 'Manage All',
      'manage': 'Manage',
      'view_all': 'View All',
      
      // Search Screen
      'search': 'Search',
      'coming_soon': 'Coming soon...',
      
      // Orders Screen
      'my_orders': 'My Orders',
      
      // Main Navigation
      'explore': 'Explore',
      
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
      
      // Change Password
      'change_password_title': 'Change Your Password',
      'change_password_desc': 'To change your password, we\'ll send a verification code to your email address.',
      'continue_to_verify': 'Continue',
      'secure_account': 'Keep your account secure by changing your password regularly.',
      
      // Social login in development
      'google_signin_dev': 'Google Sign In - Under development',
      'facebook_signin_dev': 'Facebook Sign In - Under development',
      'register_success': 'Registration successful! Please log in with your credentials.',
      
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
      'verify_identity': 'Vérifier votre identité',
      'otp_sent_to': 'Nous avons envoyé un code de vérification à',
      'code_expired': 'Code expiré',
      'code_expired_message': 'Votre code OTP a expiré. Veuillez demander un nouveau code.',
      'back': 'Retour',
      'resend_code': 'Renvoyer',
      'expires_in': 'Expire dans',
      'verify_and_login': 'Vérifier et se connecter',
      'enter_6_digit_code': 'Veuillez entrer un code de 6 chiffres',
      'invalid_or_expired_code': 'Code invalide ou expiré',
      'login_success': '✓ Connexion réussie!',
      'resend_login_otp_message': 'Pour renvoyer le code, veuillez vous reconnecter',
      'wait': 'Attendez',
      'seconds': 'secondes',
      'sec': 's',
      
      // Snackbar messages
      'login_failed': 'Échec de la connexion. Veuillez vérifier vos identifiants.',
      'otp_verification_failed': 'Échec de la vérification OTP. Veuillez réessayer.',
      'otp_verification_success': 'OTP vérifié avec succès!',
      'password_reset_failed': 'Échec de la réinitialisation. Veuillez réessayer.',
      'password_reset_complete': 'Votre mot de passe a été réinitialisé avec succès!',
      'email_not_found': 'Email introuvable. Veuillez vérifier et réessayer.',
      'invalid_credentials': 'Email ou mot de passe invalide.',
      'generic_error': 'Une erreur s\'est produite. Veuillez réessayer.',
      'please_wait': 'Veuillez patienter...',
      'otp_sent': 'OTP envoyé avec succès à votre email.',
      'stay_connected': 'Rester connecté',
      
      // Accueil Screen
      'welcome_speedline': 'Bienvenue sur SpeedLine!',
      'welcome_message': 'Vous êtes maintenant connecté à votre compte.\nL\'écran d\'accueil complet arrive bientôt!',
      'welcome_user': 'Bienvenue',
      
      // Settings Screen
      'settings': 'Paramètres',
      'account': 'COMPTE',
      'edit_profile': 'Modifier le Profil',
      'manage_preferences': 'Gérer vos informations de profil',
      'change_password': 'Changer le Mot de Passe',
      'update_security_credentials': 'Mettre à jour vos identifiants de sécurité',
      'app_preferences': 'PRÉFÉRENCES',
      'language': 'Langue',
      'choose_app_language': 'Choisissez la langue de l\'application',
      'dark_mode': 'Mode Sombre',
      'enable_dark_theme': 'Activer le thème sombre',
      'dark_mode_enabled': 'Mode sombre activé',
      'light_mode_enabled': 'Mode clair activé',
      'notifications': 'NOTIFICATIONS',
      'push_notifications': 'Notifications Push',
      'receive_order_updates': 'Recevoir des mises à jour de commande et des offres',
      'email_notifications': 'Notifications Email',
      'receive_news_offers': 'Recevoir des actualités et des offres spéciales',
      'support': 'SUPPORT',
      'help_center': 'Centre d\'Aide',
      'faq_support_articles': 'FAQ et articles de support',
      'contact_us': 'Contactez-Nous',
      'get_in_touch': 'Entrez en contact avec notre équipe',
      'terms_conditions': 'Conditions Générales',
      'legal_information': 'Informations légales et politiques',
      'about': 'À PROPOS',
      'app_version': 'Version de l\'Application',
      'logout': 'Déconnexion',
      'are_you_sure_logout': 'Êtes-vous sûr de vouloir vous déconnecter?',
      'cancel': 'Annuler',
      'select_language': 'Sélectionner la Langue',
      'english': 'Anglais',
      'french': 'Français',
      'arabic': 'Arabe',
      
      // Enable Location Screen
      'enable_location': 'Activer les Services de Localisation',
      'location_permission_title': 'Quelle est votre position?',
      'location_permission_desc': 'Nous devons connaître votre position pour vous montrer les restaurants à proximité et ',
      'accurate_delivery': 'fournir des estimations de livraison précises',
      'allow_location_access': 'Autoriser l\'Accès à la Localisation',
      'enter_address_manually': 'Entrer l\'Adresse Manuellement',
      'skip_for_now': 'Passer pour l\'instant',
      'location_services_disabled': 'Les services de localisation sont désactivés. Veuillez les activer dans les paramètres.',
      'location_permission_denied': 'Permission de localisation refusée. Veuillez autoriser l\'accès pour continuer.',
      'location_permanently_denied': 'Les autorisations de localisation sont refusées de façon permanente. Veuillez les activer dans les paramètres de l\'application.',
      'location_timeout': 'La demande de localisation a expiré. Veuillez réessayer.',
      'location_error': 'Échec de l\'obtention de la localisation',
      'manual_address_entry': 'Saisie Manuelle de l\'Adresse',
      'manual_address_coming_soon': 'L\'écran de saisie manuelle de l\'adresse arrive bientôt!',
      'ok': 'OK',
      
      // Edit Profile Screen
      'save_changes': 'Enregistrer les Modifications',
      'profile_updated_success': 'Profil mis à jour avec succès!',
      'profile_update_error': 'Échec de la mise à jour du profil. Veuillez réessayer.',
      'choose_photo_source': 'Choisir la Source de la Photo',
      'take_photo': 'Prendre une Photo',
      'choose_from_gallery': 'Choisir dans la Galerie',
      'remove_photo': 'Supprimer la Photo',
      'uploading': 'Téléchargement...',
      
      // Profile Screen
      'my_profile': 'Mon Profil',
      'delivery_addresses': 'Adresses de Livraison',
      'manage_addresses': 'Gérer vos adresses',
      'add_new_address': 'Ajouter une Nouvelle Adresse',
      'home_address': 'Domicile',
      'work_address': 'Travail',
      'use_current_location': 'Utiliser Ma Position Actuelle',
      'payment_methods': 'Moyens de Paiement',
      'manage_payment': 'Gérer vos moyens de paiement',
      'add_payment_method': 'Ajouter un Moyen de Paiement',
      'order_history': 'Historique des Commandes',
      'view_all_orders': 'Voir toutes vos commandes passées',
      'delivered': 'LIVRÉ',
      'reorder': 'Recommander',
      'view_details': 'Voir les Détails',
      'manage_all': 'Tout Gérer',
      'manage': 'Gérer',
      'view_all': 'Tout Voir',
      
      // Search Screen
      'search': 'Rechercher',
      'coming_soon': 'Bientôt disponible...',
      
      // Orders Screen
      'my_orders': 'Mes Commandes',
      
      // Main Navigation
      'explore': 'Explorer',
      
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
      
      // Change Password
      'change_password_title': 'Changer Votre Mot de Passe',
      'change_password_desc': 'Pour changer votre mot de passe, nous enverrons un code de vérification à votre adresse e-mail.',
      'continue_to_verify': 'Continuer',
      'secure_account': 'Gardez votre compte sécurisé en changeant régulièrement votre mot de passe.',
      
      // Social login in development
      'google_signin_dev': 'Google Sign In - En cours de développement',
      'facebook_signin_dev': 'Facebook Sign In - En cours de développement',
      'register_success': 'Inscription réussie! Veuillez vous connecter avec vos identifiants.',
      
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
      'verify_identity': 'التحقق من هويتك',
      'otp_sent_to': 'لقد أرسلنا رمز التحقق إلى',
      'code_expired': 'انتهت صلاحية الرمز',
      'code_expired_message': 'انتهت صلاحية رمز OTP الخاص بك. يرجى طلب رمز جديد.',
      'back': 'رجوع',
      'resend_code': 'إعادة إرسال',
      'expires_in': 'تنتهي الصلاحية في',
      'verify_and_login': 'التحقق وتسجيل الدخول',
      'enter_6_digit_code': 'يرجى إدخال رمز مكون من 6 أرقام',
      'invalid_or_expired_code': 'رمز غير صالح أو منتهي الصلاحية',
      'login_success': '✓ تم تسجيل الدخول بنجاح!',
      'resend_login_otp_message': 'لإعادة إرسال الرمز، يرجى تسجيل الدخول مرة أخرى',
      'wait': 'انتظر',
      'seconds': 'ثانية',
      'sec': 'ث',
      
      // Snackbar messages
      'login_failed': 'فشل تسجيل الدخول. يرجى التحقق من بيانات الاعتماد الخاصة بك.',
      'otp_verification_failed': 'فشل التحقق من OTP. يرجى المحاولة مرة أخرى.',
      'otp_verification_success': 'تم التحقق من OTP بنجاح!',
      'password_reset_failed': 'فشلت إعادة تعيين كلمة المرور. يرجى المحاولة مرة أخرى.',
      'password_reset_complete': 'تمت إعادة تعيين كلمة المرور الخاصة بك بنجاح!',
      'email_not_found': 'البريد الإلكتروني غير موجود. يرجى التحقق والمحاولة مرة أخرى.',
      'invalid_credentials': 'بريد إلكتروني أو كلمة مرور غير صالحة.',
      'generic_error': 'حدث خطأ. يرجى المحاولة مرة أخرى.',
      'please_wait': 'يرجى الانتظار...',
      'otp_sent': 'تم إرسال OTP بنجاح إلى بريدك الإلكتروني.',
      'stay_connected': 'ابق متصلاً',
      
      // Accueil Screen
      'welcome_speedline': 'مرحبا بك في سبيد لاين!',
      'welcome_message': 'أنت الآن متصل بحسابك.\nشاشة الرئيسية الكاملة قادمة قريباً!',
      'welcome_user': 'مرحبا',
      
      // Settings Screen
      'settings': 'الإعدادات',
      'account': 'الحساب',
      'edit_profile': 'تعديل الملف الشخصي',
      'manage_preferences': 'إدارة معلومات ملفك الشخصي',
      'change_password': 'تغيير كلمة المرور',
      'update_security_credentials': 'تحديث بيانات الأمان الخاصة بك',
      'app_preferences': 'التفضيلات',
      'language': 'اللغة',
      'choose_app_language': 'اختر لغة التطبيق',
      'dark_mode': 'الوضع الداكن',
      'enable_dark_theme': 'تفعيل المظهر الداكن',
      'dark_mode_enabled': 'تم تفعيل الوضع الداكن',
      'light_mode_enabled': 'تم تفعيل الوضع المضيء',
      'notifications': 'الإشعارات',
      'push_notifications': 'إشعارات الدفع',
      'receive_order_updates': 'تلقي تحديثات الطلبات والعروض',
      'email_notifications': 'إشعارات البريد الإلكتروني',
      'receive_news_offers': 'تلقي الأخبار والعروض الخاصة',
      'support': 'الدعم',
      'help_center': 'مركز المساعدة',
      'faq_support_articles': 'الأسئلة الشائعة ومقالات الدعم',
      'contact_us': 'اتصل بنا',
      'get_in_touch': 'تواصل مع فريقنا',
      'terms_conditions': 'الشروط والأحكام',
      'legal_information': 'المعلومات القانونية والسياسات',
      'about': 'حول',
      'app_version': 'إصدار التطبيق',
      'logout': 'تسجيل الخروج',
      'are_you_sure_logout': 'هل أنت متأكد من تسجيل الخروج؟',
      'cancel': 'إلغاء',
      'select_language': 'اختر اللغة',
      'english': 'الإنجليزية',
      'french': 'الفرنسية',
      'arabic': 'العربية',
      
      // Enable Location Screen
      'enable_location': 'تفعيل خدمات الموقع',
      'location_permission_title': 'ما هو موقعك؟',
      'location_permission_desc': 'نحتاج إلى معرفة موقعك لإظهار المطاعم القريبة منك و ',
      'accurate_delivery': 'توفير تقديرات توصيل دقيقة',
      'allow_location_access': 'السماح بالوصول إلى الموقع',
      'enter_address_manually': 'إدخال العنوان يدوياً',
      'skip_for_now': 'تخطي في الوقت الحالي',
      'location_services_disabled': 'خدمات الموقع معطلة. يرجى تفعيلها في الإعدادات.',
      'location_permission_denied': 'تم رفض إذن الموقع. يرجى السماح بالوصول للمتابعة.',
      'location_permanently_denied': 'تم رفض أذونات الموقع بشكل دائم. يرجى تفعيلها في إعدادات التطبيق.',
      'location_timeout': 'انتهت مهلة طلب الموقع. يرجى المحاولة مرة أخرى.',
      'location_error': 'فشل في الحصول على الموقع',
      'manual_address_entry': 'إدخال العنوان يدوياً',
      'manual_address_coming_soon': 'شاشة إدخال العنوان يدوياً قادمة قريباً!',
      'ok': 'حسناً',
      
      // Edit Profile Screen
      'save_changes': 'حفظ التغييرات',
      'profile_updated_success': 'تم تحديث الملف الشخصي بنجاح!',
      'profile_update_error': 'فشل تحديث الملف الشخصي. يرجى المحاولة مرة أخرى.',
      'choose_photo_source': 'اختر مصدر الصورة',
      'take_photo': 'التقاط صورة',
      'choose_from_gallery': 'اختر من المعرض',
      'remove_photo': 'حذف الصورة',
      'uploading': 'جاري الرفع...',
      
      // Profile Screen
      'my_profile': 'ملفي الشخصي',
      'delivery_addresses': 'عناوين التوصيل',
      'manage_addresses': 'إدارة عناوينك',
      'add_new_address': 'إضافة عنوان جديد',
      'home_address': 'المنزل',
      'work_address': 'العمل',
      'use_current_location': 'استخدام موقعي الحالي',
      'payment_methods': 'طرق الدفع',
      'manage_payment': 'إدارة طرق الدفع الخاصة بك',
      'add_payment_method': 'إضافة طريقة دفع',
      'order_history': 'سجل الطلبات',
      'view_all_orders': 'عرض جميع طلباتك السابقة',
      'delivered': 'تم التوصيل',
      'reorder': 'إعادة الطلب',
      'view_details': 'عرض التفاصيل',
      'manage_all': 'إدارة الكل',
      'manage': 'إدارة',
      'view_all': 'عرض الكل',
      
      // Search Screen
      'search': 'بحث',
      'coming_soon': 'قريباً...',
      
      // Orders Screen
      'my_orders': 'طلباتي',
      
      // Main Navigation
      'explore': 'استكشف',
      
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
      
      // Change Password
      'change_password_title': 'تغيير كلمة المرور',
      'change_password_desc': 'لتغيير كلمة المرور، سنرسل رمز التحقق إلى عنوان بريدك الإلكتروني.',
      'continue_to_verify': 'متابعة',
      'secure_account': 'حافظ على أمان حسابك عن طريق تغيير كلمة المرور بانتظام.',
      
      // Social login in development
      'google_signin_dev': 'تسجيل الدخول عبر Google - قيد التطوير',
      'facebook_signin_dev': 'تسجيل الدخول عبر Facebook - قيد التطوير',
      'register_success': 'تم التسجيل بنجاح! الرجاء تسجيل الدخول باستخدام بيانات الاعتماد الخاصة بك.',
      
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
