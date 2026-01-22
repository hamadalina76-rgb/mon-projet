# 🚗 SpeedLine Courier App

Application mobile Flutter pour les livreurs de la plateforme SpeedLine.

## 📱 Description

Application de livraison dédiée aux coursiers permettant de:
- ✅ Accepter/Refuser des commandes en temps réel
- 📍 Tracking GPS en arrière-plan
- 🗺️ Navigation GPS intégrée
- 💰 Suivi des gains et statistiques
- 💬 Chat avec clients et support
- 📸 Preuves de livraison (photo + signature)
- 🔔 Notifications push temps réel
- 🎯 Système de bonus et incentives

---

## 🏗️ Architecture

### Clean Architecture (Feature-First)

```
lib/
├── core/               # Infrastructure & utilities
│   ├── api/           # HTTP client (Dio + Retrofit)
│   ├── database/      # Local DB (Drift)
│   ├── storage/       # Secure storage & cache
│   ├── network/       # Connectivity & interceptors
│   ├── error/         # Error handling
│   ├── constants/     # App constants
│   ├── theme/         # Themes & styles
│   ├── localization/  # i18n (AR/FR/EN)
│   └── utils/         # Utilities
│
├── config/            # Configuration
│   ├── env/          # Environment configs
│   ├── router/       # Navigation (go_router)
│   └── di/           # Dependency Injection (GetIt)
│
├── features/          # Features (Clean Architecture)
│   ├── splash/
│   ├── onboarding/
│   ├── auth/         # Login, Register, Phone Verification
│   ├── home/         # Dashboard, Stats, Earnings
│   ├── orders/       # Available Orders, Accept/Reject
│   ├── deliveries/   # Active Delivery, Navigation, POD
│   ├── location/     # GPS Tracking
│   ├── navigation/   # Google Maps/Mapbox Navigation
│   ├── chat/         # Messaging with customers
│   ├── camera/       # Photo & Signature capture
│   ├── earnings/     # Earnings, Payouts, Reports
│   ├── profile/      # Profile, Vehicle, Documents
│   ├── notifications/# Notifications
│   ├── support/      # Help, FAQ, Emergency
│   └── incentives/   # Bonuses, Quests
│
├── shared/            # Shared widgets & models
│   ├── widgets/      # Reusable UI components
│   └── models/       # Common models
│
├── services/          # Global services
│   ├── background_location_service.dart
│   ├── websocket_service.dart
│   ├── notification_service.dart
│   ├── firebase_service.dart
│   ├── analytics_service.dart
│   ├── crash_reporting_service.dart
│   ├── sync_service.dart
│   ├── connectivity_service.dart
│   ├── audio_service.dart
│   ├── permission_service.dart
│   ├── local_notification_service.dart
│   └── deep_link_service.dart
│
└── providers/         # Global providers (Riverpod)
    ├── app_state_provider.dart
    ├── courier_state_provider.dart
    ├── connectivity_provider.dart
    └── app_lifecycle_provider.dart
```

### Feature Structure (Clean Architecture)

Chaque feature suit la structure suivante:

```
feature_name/
├── domain/
│   ├── entities/          # Business objects
│   ├── repositories/      # Repository interfaces
│   └── usecases/          # Business logic
│
├── data/
│   ├── models/            # Data models (JSON)
│   ├── datasources/       # API & Local datasources
│   └── repositories/      # Repository implementations
│
└── presentation/
    ├── providers/         # Riverpod providers
    ├── screens/           # UI screens
    └── widgets/           # Feature widgets
```

---

## 🚀 Guide de Démarrage

### Prérequis

- Flutter SDK >=3.2.0
- Dart SDK >=3.2.0
- Android Studio / Xcode
- Compte Firebase
- Compte Google Maps API
- Compte Stripe (pour paiements)

### Installation

1. **Cloner le projet**
```bash
cd frontend/courier_app
```

2. **Installer les dépendances**
```bash
flutter pub get
```

3. **Configuration des clés API**

Créer les fichiers `.env`:

**.env.development**
```bash
APP_ENV=development
APP_NAME=SpeedLine Courier Dev

# API
API_BASE_URL=http://localhost:8080/api/v1
API_TIMEOUT=30000

# WebSocket
WS_URL=ws://localhost:8080/ws

# Google Maps
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_DEV

# Firebase
FIREBASE_ENABLED=true

# Feature Flags
ENABLE_ANALYTICS=false
ENABLE_CRASHLYTICS=false
ENABLE_DEBUG_LOGGING=true
```

**.env.production**
```bash
APP_ENV=production
APP_NAME=SpeedLine Courier

# API
API_BASE_URL=https://api.speedline.com/api/v1
API_TIMEOUT=30000

# WebSocket
WS_URL=wss://api.speedline.com/ws

# Google Maps
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_PROD

# Firebase
FIREBASE_ENABLED=true

# Feature Flags
ENABLE_ANALYTICS=true
ENABLE_CRASHLYTICS=true
ENABLE_DEBUG_LOGGING=false
```

4. **Configuration Firebase**

- Télécharger `google-services.json` (Android)
- Télécharger `GoogleService-Info.plist` (iOS)
- Placer dans les dossiers respectifs

5. **Générer le code**
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

6. **Lancer l'application**
```bash
# Android
flutter run -d <device-id>

# iOS
flutter run -d <device-id>

# Chrome (dev)
flutter run -d chrome
```

---

## 📦 Dépendances Principales

### State Management
- `flutter_riverpod: ^2.4.9` - State management
- `riverpod_annotation: ^2.3.3` - Code generation

### Networking
- `dio: ^5.4.0` - HTTP client
- `retrofit: ^4.0.3` - Type-safe API client
- `web_socket_channel: ^2.4.0` - WebSocket
- `connectivity_plus: ^5.0.2` - Network status

### Database & Storage
- `drift: ^2.14.1` - SQLite local database
- `flutter_secure_storage: ^9.0.0` - Secure storage
- `shared_preferences: ^2.2.2` - Simple storage
- `hive: ^2.2.3` - Lightweight database

### Location & Maps
- `geolocator: ^10.1.0` - GPS location
- `google_maps_flutter: ^2.5.3` - Google Maps
- `background_location: ^0.13.0` - Background GPS
- `geocoding: ^2.1.1` - Address geocoding

### Firebase
- `firebase_core: ^2.24.2`
- `firebase_messaging: ^14.7.9` - Push notifications
- `firebase_analytics: ^10.8.0`
- `firebase_crashlytics: ^3.4.9`
- `firebase_auth: ^4.16.0`

### Notifications
- `flutter_local_notifications: ^16.3.0`
- `awesome_notifications: ^0.8.2`

### Camera & Image
- `image_picker: ^1.0.5`
- `image_cropper: ^5.0.1`
- `signature: ^5.4.1` - Digital signature

### UI/UX
- `flutter_screenutil: ^5.9.0` - Responsive UI
- `shimmer: ^3.0.0` - Loading animations
- `lottie: ^2.7.0` - Lottie animations
- `fl_chart: ^0.66.0` - Charts

---

## 🎯 Features Implémentées

### ✅ Core Features (Priorité Critique)

#### 1. Authentication
- Login avec email/password
- Enregistrement nouveau livreur
- Vérification téléphone (OTP)
- Upload documents (permis, véhicule)
- Gestion session sécurisée

#### 2. Orders Management
- Liste commandes disponibles en temps réel
- Filtrage par distance/gains
- Accepter/Refuser commandes
- Timer acceptation (30s)
- Preview route sur carte
- Détails commande (restaurant, client, items)

#### 3. Deliveries
- Livraison active avec statuts:
  - En route vers restaurant
  - Arrivé au restaurant
  - Commande récupérée
  - En route vers client
  - Arrivé chez client
  - Livraison terminée
- Navigation GPS intégrée
- ETA temps réel
- Tracking position en arrière-plan
- Preuve de livraison:
  - Photo
  - Signature numérique
  - Code de vérification

#### 4. Location Tracking
- GPS haute précision
- Mise à jour position toutes les 10s
- Tracking en arrière-plan
- Sync automatique quand connexion
- Stockage local si offline

#### 5. Notifications
- Push notifications (Firebase)
- Notifications locales
- Sons personnalisés par type
- Vibrations
- Channels Android:
  - Nouvelles commandes (priorité max)
  - Updates livraison
  - Messages chat

### 🟡 Important Features

#### 6. Chat
- Messagerie avec client
- Messagerie avec support
- Messages texte
- Messages audio (optionnel)
- Indicateur "typing..."
- Stockage local conversations

#### 7. Earnings
- Dashboard gains:
  - Aujourd'hui
  - Cette semaine
  - Ce mois
- Détail par livraison
- Graphiques statistiques
- Historique paiements
- Export rapports PDF

#### 8. Camera
- Capture photo haute qualité
- Compression automatique
- Signature numérique
- Preview avant envoi

### 🟢 Features Normales

#### 9. Profile
- Informations personnelles
- Photo profil
- Informations véhicule
- Documents (permis, assurance, carte grise)
- Note moyenne
- Statistiques performances

#### 10. Incentives & Bonuses
- Quêtes journalières
- Bonus pic hours
- Challenges hebdomadaires
- Progress tracking
- Historique bonus

#### 11. Support
- FAQ
- Contact support (email/phone)
- Bouton urgence (SOS)
- Signaler problème

---

## 🔧 Services Background

### Background Location Service
```dart
// Démarre quand coursier passe "Online"
// Mise à jour GPS toutes les 10s
// Sync via WebSocket
// Stockage local si offline
// Battery optimized
```

### WebSocket Service
```dart
// Connexion temps réel serveur
// Auto-reconnect
// Ping/Pong heartbeat
// Queue messages offline
```

### Notification Service
```dart
// Firebase Cloud Messaging
// Local notifications
// Sons personnalisés
// Gestion channels Android
```

### Sync Service
```dart
// Synchronisation données offline
// Upload photos/signatures
// Queue requests failed
// Retry logic intelligent
```

---

## 🎨 Thème

### Couleurs Principales
```dart
Primary: #FF6B35    // Orange SpeedLine
Secondary: #2C3E50  // Dark Blue
Success: #27AE60
Error: #E74C3C
Warning: #F39C12
```

### Typographie
- Font: Poppins
- Tailles: 12, 14, 16, 18, 20, 24, 32

---

## 🌍 Localisation

Support multilingue:
- 🇫🇷 Français (défaut)
- 🇬🇧 English
- 🇸🇦 العربية (Arabic)

Fichiers: `lib/core/localization/l10n/`

---

## 🧪 Tests

```bash
# Unit tests
flutter test

# Widget tests
flutter test test/widget/

# Integration tests
flutter test integration_test/
```

---

## 📱 Build & Release

### Android

```bash
# Build APK
flutter build apk --release

# Build App Bundle
flutter build appbundle --release

# Signed APK
flutter build apk --release --split-per-abi
```

### iOS

```bash
# Build iOS
flutter build ios --release

# Archive pour App Store
flutter build ipa
```

---

## 🐛 Debugging

### Logs
```bash
# Voir logs Flutter
flutter logs

# Filtrer logs
flutter logs | grep "SpeedLine"
```

### DevTools
```bash
# Ouvrir DevTools
flutter pub global activate devtools
flutter pub global run devtools
```

---

## 📂 Structure des Données

### Drift Database Tables

1. **deliveries** - Livraisons en cours/passées
2. **locations** - Historique positions GPS
3. **earnings** - Gains par livraison
4. **messages** - Messages chat
5. **orders** - Cache commandes

---

## 🔐 Sécurité

- Tokens stockés dans `flutter_secure_storage`
- Refresh token automatique
- SSL Pinning (production)
- Obfuscation code (release)
- ProGuard rules (Android)

---

## 🚀 Performance

- Images en cache (`cached_network_image`)
- Lazy loading listes
- Pagination APIs
- Debounce recherches
- Optimistic UI updates
- Background isolates pour tâches lourdes

---

## 📝 Conventions de Code

### Naming
- Files: `snake_case.dart`
- Classes: `PascalCase`
- Variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`

### Structure
```dart
// 1. Imports
import 'package:flutter/material.dart';

// 2. Class definition
class MyWidget extends StatelessWidget {
  // 3. Fields
  final String title;
  
  // 4. Constructor
  const MyWidget({super.key, required this.title});
  
  // 5. Build method
  @override
  Widget build(BuildContext context) {
    return Container();
  }
  
  // 6. Private methods
  void _privateMethod() {}
}
```

---

## 🤝 Contribution

1. Fork le projet
2. Créer une branche (`git checkout -b feature/AmazingFeature`)
3. Commit (`git commit -m 'Add AmazingFeature'`)
4. Push (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

---

## 📄 License

Propriétaire - SpeedLine Platform © 2026

---

## 👥 Équipe

- **Backend**: Spring Boot Microservices
- **Frontend Mobile**: Flutter (Customer & Courier)
- **Frontend Web**: Angular (Admin & Partner)
- **DevOps**: Docker, Kubernetes, Azure

---

## 📞 Contact

- **Support Technique**: support@speedline.com
- **Documentation API**: https://api.speedline.com/docs
- **Slack**: #speedline-dev

---

## 🎯 Roadmap

### Version 1.1 (Q2 2026)
- [ ] Voice calls avec client
- [ ] Mode hors-ligne complet
- [ ] Optimisation batterie avancée
- [ ] Widget iOS/Android
- [ ] Apple Watch / Wear OS support

### Version 1.2 (Q3 2026)
- [ ] Multi-delivery (plusieurs commandes simultanées)
- [ ] Route optimization AI
- [ ] Prédiction gains journaliers
- [ ] Gamification avancée

---

## ⚡ Quick Start (TL;DR)

```bash
# 1. Install
flutter pub get

# 2. Generate code
flutter pub run build_runner build --delete-conflicting-outputs

# 3. Run
flutter run -d chrome  # Dev
flutter run             # Mobile

# 4. Test
flutter test

# 5. Build
flutter build apk --release
```

---

**Version:** 1.0.0  
**Last Updated:** Janvier 2026  
**Status:** 🟢 En Production
