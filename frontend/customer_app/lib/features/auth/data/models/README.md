# 📁 Auth Models - Documentation

## 📋 Vue d'ensemble

Ce dossier contient tous les **modèles de données** (Data Transfer Objects) pour le module d'authentification. Ces modèles gèrent la communication avec le backend et la sérialisation JSON.

---

## 🗂️ Structure des fichiers

### Fichiers principaux (à modifier)
- `*.dart` - Définition des modèles avec annotations Freezed

### Fichiers générés (NE PAS MODIFIER)
- `*.freezed.dart` - Code généré par Freezed (immutabilité, copyWith, equality)
- `*.g.dart` - Code généré par json_serializable (fromJson, toJson)

---

## 📦 Modèles disponibles

### 1️⃣ Requêtes d'authentification

| Fichier | Endpoint | Description |
|---------|----------|-------------|
| `login_request.dart` | `POST /api/auth/login` | Connexion avec email/password |
| `register_request.dart` | `POST /api/auth/register` | Inscription nouveau compte |
| `forgot_password_request.dart` | `POST /api/auth/forgot-password` | Demande OTP pour reset password |
| `verify_otp_request.dart` | `POST /api/auth/verify-otp` | Vérification code OTP |
| `reset_password_request.dart` | `POST /api/auth/reset-password` | Définir nouveau password |

### 2️⃣ Réponses du backend

| Fichier | Description | Utilisation |
|---------|-------------|-------------|
| `auth_response.dart` | Réponse après login/register | Contient tokens JWT + user |
| `user_model.dart` | Données utilisateur | Converti vers User entity |

---

## 🔄 Flux de données typique

```
┌─────────────────────────────────────────────────────────┐
│ 1. UI LAYER (LoginScreen)                              │
├─────────────────────────────────────────────────────────┤
│   final request = LoginRequest(                         │
│     email: emailController.text,                        │
│     password: passwordController.text                   │
│   );                                                    │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 2. DATA LAYER (AuthRemoteDataSource)                   │
├─────────────────────────────────────────────────────────┤
│   final response = await dio.post(                      │
│     '/api/auth/login',                                  │
│     data: request.toJson(), // ← Sérialisation          │
│   );                                                    │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 3. BACKEND                                              │
├─────────────────────────────────────────────────────────┤
│   Validation, génération tokens, retour JSON           │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 4. DATA LAYER (Désérialisation)                        │
├─────────────────────────────────────────────────────────┤
│   final authResponse = AuthResponse.fromJson(          │
│     response.data  // ← Désérialisation                │
│   );                                                    │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 5. DOMAIN LAYER (Conversion vers Entity)               │
├─────────────────────────────────────────────────────────┤
│   final user = authResponse.user.toEntity();           │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 6. PRESENTATION LAYER (State Management)               │
├─────────────────────────────────────────────────────────┤
│   state = AuthState.authenticated(user);               │
└─────────────────────────────────────────────────────────┘
```

---

## 🛠️ Génération du code

Après modification des fichiers `*.dart`, exécuter :

```bash
# Générer les fichiers .freezed.dart et .g.dart
flutter pub run build_runner build --delete-conflicting-outputs

# Ou en mode watch (régénération automatique)
flutter pub run build_runner watch --delete-conflicting-outputs
```

---

## 🎯 Bonnes pratiques

### ✅ À FAIRE
- Utiliser `@freezed` pour tous les modèles
- Ajouter validation côté backend (pas dans les modèles)
- Documenter chaque modèle avec des commentaires
- Tester la sérialisation/désérialisation

### ❌ À ÉVITER
- Ne jamais modifier les fichiers `.freezed.dart` ou `.g.dart`
- Ne pas ajouter de logique métier dans les modèles
- Ne pas stocker de mot de passe en clair
- Ne pas utiliser les modèles directement dans l'UI (utiliser entities)

---

## 🔐 Sécurité

### Données sensibles
- **Passwords** : Jamais stockés localement
- **Tokens** : Stockés dans `FlutterSecureStorage`
- **User data** : Stocké dans `SharedPreferences` (données non sensibles)

### Mapping vers JSON
```dart
// Sérialisation (Dart → JSON)
final json = loginRequest.toJson();
// {"email": "user@example.com", "password": "secret"}

// Désérialisation (JSON → Dart)
final response = AuthResponse.fromJson(jsonData);
```

---

## 📚 Ressources

- [Freezed Documentation](https://pub.dev/packages/freezed)
- [json_serializable](https://pub.dev/packages/json_serializable)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

## 🔗 Relations entre modèles

```
AuthResponse
├── token: String
├── refreshToken: String
└── user: UserModel
    ├── id: String
    ├── email: String
    ├── firstName: String
    ├── lastName: String
    ├── phone: String
    └── role: String
        └── toEntity() → User (domain/entities)
```

