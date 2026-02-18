# Configuration de l'authentification sociale

Ce document décrit comment configurer Google Sign-In et Facebook Login pour l'application SpeedLine Customer.

## 1. Google Sign-In

### Backend (auth-service)

Configurer dans `application.yml` :

```yaml
oauth2:
  google:
    client-id: YOUR_GOOGLE_CLIENT_ID
```

### Android

1. **Créer un projet dans Google Cloud Console** :
   - Aller sur https://console.cloud.google.com/
   - Créer un nouveau projet ou utiliser un existant
   - Activer "Google Sign-In API"

2. **Configurer OAuth 2.0** :
   - Aller dans "APIs & Services" > "Credentials"
   - Créer un "OAuth 2.0 Client ID" de type "Android"
   - Package name: `com.speedline.customer_app`
   - SHA-1 fingerprint: Obtenir avec `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`

3. **Télécharger google-services.json** (optionnel, si Firebase est utilisé)

### iOS

1. **Configurer dans Google Cloud Console** :
   - Créer un "OAuth 2.0 Client ID" de type "iOS"
   - Bundle ID: `com.speedline.customerApp`

2. **Modifier `ios/Runner/Info.plist`** :

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>com.googleusercontent.apps.YOUR_CLIENT_ID</string>
    </array>
  </dict>
</array>
<key>GIDClientID</key>
<string>YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com</string>
```

---

## 2. Facebook Login

### Backend (auth-service)

Configurer dans `application.yml` :

```yaml
oauth2:
  facebook:
    app-id: YOUR_FACEBOOK_APP_ID
    app-secret: YOUR_FACEBOOK_APP_SECRET
```

### Créer une application Facebook

1. **Aller sur Facebook Developers** :
   - https://developers.facebook.com/
   - Créer une nouvelle application
   - Type: "Consumer"

2. **Ajouter Facebook Login** :
   - Dans "Add Products", ajouter "Facebook Login"
   - Configurer les URLs de callback

3. **Récupérer les credentials** :
   - App ID: Dans "Settings" > "Basic"
   - Client Token: Dans "Settings" > "Advanced" > "Client Token"

### Android

1. **Modifier `android/app/src/main/res/values/strings.xml`** :

```xml
<resources>
    <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
    <string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
    <string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
</resources>
```

2. **Ajouter le Key Hash dans Facebook** :
   - Générer: `keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64`
   - Ajouter dans Facebook App > Settings > Basic > Key Hashes

### iOS

1. **Modifier `ios/Runner/Info.plist`** :

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>fbYOUR_FACEBOOK_APP_ID</string>
    </array>
  </dict>
</array>
<key>FacebookAppID</key>
<string>YOUR_FACEBOOK_APP_ID</string>
<key>FacebookClientToken</key>
<string>YOUR_FACEBOOK_CLIENT_TOKEN</string>
<key>FacebookDisplayName</key>
<string>SpeedLine</string>
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>fbapi</string>
  <string>fb-messenger-share-api</string>
</array>
```

---

## 3. Variables d'environnement (.env)

Ajouter dans `.env.development`, `.env.staging`, `.env.production` :

```env
# Google OAuth
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID

# Facebook OAuth (optionnel côté Flutter, requis côté backend)
FACEBOOK_APP_ID=YOUR_FACEBOOK_APP_ID
```

---

## 4. Test

1. Lancer l'application sur un émulateur ou appareil physique
2. Tester le bouton "Continuer avec Google"
3. Tester le bouton "Continuer avec Facebook"
4. Vérifier que les tokens sont envoyés au backend
5. Vérifier que l'authentification JWT est retournée

---

## Troubleshooting

### Google Sign-In

- **Error 10**: SHA-1 fingerprint incorrect ou package name incorrect
- **Error 12500**: Play Services non installé sur l'émulateur
- **PlatformException**: Client ID non configuré

### Facebook Login

- **Error 304**: Package name ou Key Hash incorrect
- **Login cancelled**: Vérifier l'URL scheme dans Info.plist (iOS)
