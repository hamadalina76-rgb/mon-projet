# 📱 SpeedLine - Customer Mobile App

Application mobile Flutter pour la plateforme de livraison rapide SpeedLine.

## 📋 Table des matières

- [Architecture du Projet](#-architecture-du-projet)
- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Démarrage](#-démarrage)
- [Structure des Dossiers](#-structure-des-dossiers)
- [Patterns et Principes](#-patterns-et-principes)

---

## 🏗️ Architecture du Projet

Ce projet suit les principes de **Clean Architecture** avec une approche **Feature-First**.

```
customer_app/
│
├── lib/
│   ├── main.dart                      # Point d'entrée de l'application
│   │
│   ├── core/                          # 🔧 Utilitaires core
│   │   ├── constants/                 # Constantes de l'app
│   │   │   ├── app_constants.dart
│   │   │   ├── api_endpoints.dart
│   │   │   └── app_colors.dart
│   │   ├── themes/                    # Thèmes de l'application
│   │   │   ├── app_theme.dart
│   │   │   └── dark_theme.dart
│   │   ├── utils/                     # Utilitaires généraux
│   │   │   ├── validators.dart
│   │   │   ├── date_formatter.dart
│   │   │   └── price_formatter.dart
│   │   ├── errors/                    # Gestion des erreurs
│   │   │   ├── failures.dart
│   │   │   └── exceptions.dart
│   │   ├── network/                   # Configuration réseau
│   │   │   ├── dio_client.dart
│   │   │   ├── api_interceptor.dart
│   │   │   └── network_info.dart
│   │   └── storage/                   # Stockage local
│   │       ├── secure_storage.dart
│   │       └── shared_prefs.dart
│   │
│   ├── config/                        # ⚙️ Configuration
│   │   ├── routes/                    # Navigation (GoRouter)
│   │   │   ├── app_router.dart
│   │   │   └── route_names.dart
│   │   ├── env/                       # Variables d'environnement
│   │   │   ├── env.dart
│   │   │   └── env.g.dart
│   │   └── dependency_injection/      # Injection de dépendances
│   │       └── injection.dart
│   │
│   ├── features/                      # 🎯 Fonctionnalités (Clean Architecture)
│   │   │
│   │   ├── auth/                      # 🔐 Authentification
│   │   │   ├── presentation/
│   │   │   │   ├── screens/           # Écrans UI
│   │   │   │   ├── widgets/           # Widgets réutilisables
│   │   │   │   └── providers/         # État (Riverpod)
│   │   │   ├── domain/
│   │   │   │   ├── entities/          # Entités métier
│   │   │   │   ├── repositories/      # Contrats repository
│   │   │   │   └── usecases/          # Cas d'utilisation
│   │   │   └── data/
│   │   │       ├── models/            # Modèles de données
│   │   │       ├── datasources/       # Sources de données (API, Local)
│   │   │       └── repositories/      # Implémentations repository
│   │   │
│   │   ├── home/                      # 🏠 Accueil & Découverte
│   │   ├── menu/                      # 🍕 Menu & Produits
│   │   ├── cart/                      # 🛒 Panier
│   │   ├── orders/                    # 📦 Commandes
│   │   ├── payment/                   # 💳 Paiement
│   │   ├── profile/                   # 👤 Profil
│   │   ├── notifications/             # 🔔 Notifications
│   │   └── reviews/                   # ⭐ Avis
│   │
│   ├── shared/                        # 🔗 Composants partagés
│   │   ├── widgets/                   # Widgets communs
│   │   ├── models/                    # Modèles partagés
│   │   └── extensions/                # Extensions Dart
│   │
│   └── services/                      # 🛠️ Services globaux
│       ├── location_service.dart
│       ├── notification_service.dart
│       ├── websocket_service.dart
│       └── map_service.dart
│
├── assets/                            # 🎨 Ressources
│   ├── images/
│   ├── icons/
│   ├── fonts/
│   └── animations/
│
├── test/                              # 🧪 Tests
│   ├── unit/
│   ├── widget/
│   └── integration/
│
├── pubspec.yaml                       # 📦 Dépendances
├── analysis_options.yaml              # 🔍 Analyse de code
└── README.md
```

---

## 🛠️ Technologies

### Framework & Language
- **Flutter** SDK 3.x
- **Dart** 3.x

### State Management
- **Riverpod** 2.4.9 - Gestion d'état réactive
- **Riverpod Generator** - Génération de code

### Navigation
- **GoRouter** 13.0.0 - Navigation déclarative

### Réseau
- **Dio** 5.4.0 - Client HTTP
- **Retrofit** 4.0.3 - REST API client
- **Pretty Dio Logger** - Logs réseau

### Stockage Local
- **Shared Preferences** - Préférences simples
- **Flutter Secure Storage** - Stockage sécurisé
- **Hive** - Base de données NoSQL locale

### Maps & Localisation
- **Google Maps Flutter** 2.5.0
- **Geolocator** 10.1.0
- **Geocoding** 2.1.1

### Firebase
- **Firebase Core** - Services Firebase
- **Firebase Messaging** - Notifications push
- **Firebase Analytics** - Analytique

### UI/UX
- **Cached Network Image** - Cache d'images
- **Shimmer** - Effets de chargement
- **Flutter Rating Bar** - Notation par étoiles
- **Lottie** - Animations

### Paiement
- **Flutter Stripe** 10.1.0 - Intégration Stripe

### Autres
- **WebSocket Channel** - Communication temps réel
- **Image Picker** - Sélection d'images
- **QR Flutter** - Génération QR codes
- **Mobile Scanner** - Scan QR codes

---

## 📋 Prérequis

Avant de commencer, assurez-vous d'avoir installé :

- **Flutter SDK** >= 3.0.0
- **Dart SDK** >= 3.0.0
- **Android Studio** (pour Android)
- **Xcode** (pour iOS, macOS uniquement)
- **VS Code** ou **Android Studio** avec plugins Flutter
- **Git**

### Vérifier l'installation Flutter

```bash
flutter doctor
```

---

## 🚀 Installation

### 1. Cloner le projet

```bash
git clone <repository-url>
cd speedline/frontend/customer_app
```

### 2. Installer les dépendances

```bash
flutter pub get
```

### 3. Générer le code

```bash
# Générer les fichiers .g.dart et .freezed.dart
flutter pub run build_runner build --delete-conflicting-outputs

# En mode watch (régénération automatique)
flutter pub run build_runner watch --delete-conflicting-outputs
```

---

## ⚙️ Configuration

### 1. Configuration Firebase

1. Créer un projet Firebase sur [Firebase Console](https://console.firebase.google.com/)
2. Ajouter les applications Android et iOS
3. Télécharger les fichiers de configuration :
   - **Android** : `google-services.json` → `android/app/`
   - **iOS** : `GoogleService-Info.plist` → `ios/Runner/`

### 2. Configuration des Variables d'Environnement

Créer un fichier `lib/config/env/env.dart` :

```dart
import 'package:envied/envied.dart';

part 'env.g.dart';

@Envied(path: '.env')
abstract class Env {
  @EnviedField(varName: 'API_BASE_URL')
  static const String apiBaseUrl = _Env.apiBaseUrl;
  
  @EnviedField(varName: 'GOOGLE_MAPS_API_KEY')
  static const String googleMapsApiKey = _Env.googleMapsApiKey;
  
  @EnviedField(varName: 'STRIPE_PUBLISHABLE_KEY')
  static const String stripePublishableKey = _Env.stripePublishableKey;
}
```

Créer un fichier `.env` à la racine :

```env
API_BASE_URL=http://localhost:8080/api
GOOGLE_MAPS_API_KEY=your_google_maps_key
STRIPE_PUBLISHABLE_KEY=your_stripe_key
```

### 3. Configuration Google Maps

**Android** (`android/app/src/main/AndroidManifest.xml`) :

```xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_GOOGLE_MAPS_API_KEY"/>
    </application>
</manifest>
```

**iOS** (`ios/Runner/AppDelegate.swift`) :

```swift
import GoogleMaps

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GMSServices.provideAPIKey("YOUR_GOOGLE_MAPS_API_KEY")
    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
}
```

---

## 🎯 Démarrage

### Mode Debug

```bash
# Android
flutter run

# iOS
flutter run

# Spécifier un device
flutter run -d <device_id>

# Lister les devices disponibles
flutter devices
```

### Mode Release

```bash
# Android
flutter run --release

# iOS
flutter run --release
```

### Build

```bash
# Android APK
flutter build apk --release

# Android App Bundle (pour Play Store)
flutter build appbundle --release

# iOS
flutter build ios --release

# iOS Archive (pour App Store)
flutter build ipa
```

---

## 📁 Structure des Dossiers

### Feature Module Structure (Clean Architecture)

Chaque feature suit cette structure :

```
feature_name/
├── presentation/          # Couche UI
│   ├── screens/          # Pages de l'application
│   ├── widgets/          # Composants UI spécifiques
│   └── providers/        # État (Riverpod)
│
├── domain/               # Logique métier
│   ├── entities/        # Entités pures (business objects)
│   ├── repositories/    # Interfaces repository
│   └── usecases/        # Cas d'utilisation
│
└── data/                # Gestion des données
    ├── models/          # Modèles de données (JSON)
    ├── datasources/     # Sources de données (API, DB)
    └── repositories/    # Implémentations repository
```

---

## 🎨 Patterns et Principes

### Clean Architecture

- **Séparation des responsabilités** : UI, Business Logic, Data
- **Indépendance des frameworks** : Le domaine ne dépend d'aucun package
- **Testabilité** : Chaque couche peut être testée indépendamment

### State Management (Riverpod)

```dart
// Provider
@riverpod
class AuthNotifier extends _$AuthNotifier {
  @override
  Future<User?> build() async {
    return await _checkAuthStatus();
  }
  
  Future<void> login(String email, String password) async {
    // Logic...
  }
}

// Consumer
class LoginScreen extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authNotifierProvider);
    // UI...
  }
}
```

### Navigation (GoRouter)

```dart
final appRouter = GoRouter(
  routes: [
    GoRoute(
      path: '/home',
      builder: (context, state) => HomeScreen(),
    ),
  ],
);

// Navigation
context.go('/home');
context.push('/details/123');
```

---

## 🧪 Tests

```bash
# Tests unitaires
flutter test

# Tests avec coverage
flutter test --coverage

# Tests d'intégration
flutter drive --target=test_driver/app.dart
```

---

## 📦 Scripts Utiles

```bash
# Nettoyer le projet
flutter clean

# Réparer les dépendances
flutter pub get

# Analyser le code
flutter analyze

# Formater le code
flutter format .

# Générer les icônes
flutter pub run flutter_launcher_icons:main

# Générer le splash screen
flutter pub run flutter_native_splash:create
```

---

## 🔐 Sécurité

- Utiliser **Flutter Secure Storage** pour les tokens
- Ne jamais commit les fichiers `.env`
- Activer ProGuard/R8 pour Android en production
- Utiliser HTTPS pour toutes les communications

---

## 📱 Plateformes Supportées

- ✅ Android (API 21+)
- ✅ iOS (13.0+)
- ⚠️ Web (en développement)
- ⚠️ Windows (en développement)
- ⚠️ macOS (en développement)
- ⚠️ Linux (en développement)

---

## 📄 License

Ce projet fait partie de la plateforme SpeedLine.

---

## 👥 Contact & Support

Pour toute question ou problème, veuillez contacter l'équipe de développement.
