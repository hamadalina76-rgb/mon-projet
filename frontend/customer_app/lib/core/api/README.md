# 🔧 Configuration API Client - Documentation

## ✅ Fichiers créés/modifiés

### 1. [api_client.dart](lib/core/api/api_client.dart)
Client API Singleton avec:
- ✅ Configuration depuis `.env`
- ✅ Timeout de 30 secondes
- ✅ Intercepteur d'authentification JWT
- ✅ PrettyDioLogger pour debug
- ✅ Gestion automatique refresh token
- ✅ Retry automatique après refresh

### 2. [interceptors/auth_interceptor.dart](lib/core/api/interceptors/auth_interceptor.dart)
Intercepteur pour:
- ✅ Injection automatique token Bearer
- ✅ Refresh token si 401
- ✅ Retry requête après refresh
- ✅ Logging détaillé
- ✅ Gestion endpoints publics

### 3. [api_endpoints.dart](lib/core/api/api_endpoints.dart)
Tous les endpoints organisés par module:
- ✅ Auth (login, register, OTP, forgot password, etc.)
- ✅ User & Customers
- ✅ Partners/Restaurants
- ✅ Orders & Delivery
- ✅ Cart & Payment
- ✅ Notifications
- ✅ Locations & Promotions

### 4. [.env.development](.env.development)
Configuration:
```env
API_BASE_URL=http://10.0.2.2:8080/api
API_TIMEOUT=30000
LOG_NETWORK=true
```

### 5. [errors/exceptions.dart](lib/core/errors/exceptions.dart)
Exceptions personnalisées:
- `ApiException` (base)
- `AuthException`
- `NetworkException`
- `ServerException`
- `NotFoundException`
- `ValidationException`
- `TimeoutException`
- `CacheException`

### 6. [errors/failures.dart](lib/core/errors/failures.dart)
Failures pour Clean Architecture:
- `NetworkFailure`
- `ServerFailure`
- `AuthFailure`
- `ValidationFailure`
- `NotFoundFailure`
- `CacheFailure`

### 7. [injection.dart](lib/config/dependency_injection/injection.dart)
Providers Riverpod:
- `secureStorageProvider`
- `sharedPreferencesProvider`
- `loggerProvider`
- `apiClientProvider`

---


## 🔐 Gestion de l'authentification

### Flow complet

```
1. Login/Register
   ↓
2. Backend retourne { token, refreshToken, user }
   ↓
3. apiClient.saveTokens() → FlutterSecureStorage
   ↓
4. Requêtes suivantes: AuthInterceptor injecte "Bearer <token>"
   ↓
5. Si 401: Refresh token automatique
   ↓
6. Retry requête avec nouveau token
   ↓
7. Si refresh échoue: Déconnexion
```

### Endpoints publics (sans token)

```dart
// Ces endpoints n'ont PAS besoin de token
ApiEndpoints.AUTH_LOGIN
ApiEndpoints.AUTH_REGISTER
ApiEndpoints.AUTH_FORGOT_PASSWORD
ApiEndpoints.AUTH_VERIFY_OTP
ApiEndpoints.AUTH_RESET_PASSWORD
```

### Endpoints protégés (avec token)

Tous les autres endpoints nécessitent un token JWT.

---

## ⚙️ Configuration

### .env files

Créer 3 fichiers:
- `.env.development` → Développement local
- `.env.staging` → Serveur de staging
- `.env.production` → Production

---

## 🔍 Logging

### En développement
```
📤 REQUEST: POST http://10.0.2.2:8080/api/auth/login
📋 Headers: {Authorization: Bearer xxx, ...}
📦 Body: {email: test@test.com, password: ***}
📥 RESPONSE: 200
📦 Data: {token: xxx, user: {...}}
```

### Désactiver le logging
```env
LOG_NETWORK=false
```


## 📝 Checklist avant production

- [ ] Changer `API_BASE_URL` vers le vrai serveur
- [ ] `LOG_NETWORK=false` en production
- [ ] Configurer HTTPS (certificat SSL)
- [ ] Ajouter Certificate Pinning
- [ ] Activer ProGuard (Android)
- [ ] Tester refresh token flow
- [ ] Tester timeout handling
- [ ] Tester mode offline

