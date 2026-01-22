# SpeedLine Frontend

Applications frontend Angular 17 pour la plateforme de livraison SpeedLine.

## 📱 Applications

Ce dossier contient deux applications Angular distinctes :

### 1. Partner Dashboard (`partner-dashboard/`)
Dashboard web pour les restaurants et partenaires.
- **Port** : 4200
- **Fonctionnalités** : Gestion menu, commandes, statistiques, avis

### 2. Admin Panel (`admin-panel/`)
Panneau d'administration pour la gestion de la plateforme.
- **Port** : 4300
- **Fonctionnalités** : Gestion utilisateurs, partenaires, commandes, analytics, monitoring

## 🚀 Stack technique commune

| Technologie | Version | Usage |
|-------------|---------|-------|
| Angular | 17.0.0 | Framework frontend |
| Angular Material | 17.0.0 | Composants UI |
| RxJS | 7.8.0 | Programmation réactive |
| ngx-translate | 15.0.0 | Internationalisation |
| ngx-socket-io | 4.6.0 | WebSocket temps réel |
| ng2-charts | 5.0.0 | Graphiques |
| Chart.js | 4.4.0 | Bibliothèque graphiques |
| date-fns | 2.30.0 | Manipulation dates |
| ngx-toastr | 17.0.2 | Notifications toast |

## 📁 Structure commune

Chaque application suit la même architecture :

```
src/
├── app/
│   ├── core/           # Services singleton, guards, interceptors
│   ├── shared/         # Composants, pipes, directives partagés
│   ├── layout/         # Layouts (main, auth)
│   ├── features/       # Modules métier (lazy-loaded)
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
├── assets/
│   ├── i18n/           # Traductions (fr, en, ar)
│   ├── images/
│   └── styles/         # SCSS (variables, mixins, utilities)
├── environments/
└── styles.scss
```

## 🛠️ Installation

```bash
# Depuis le dossier racine frontend/

# Partner Dashboard
cd partner-dashboard
npm install
npm start   # http://localhost:4200

# Admin Panel
cd admin-panel
npm install
npm start   # http://localhost:4300
```

## 🔧 Scripts NPM communs

| Script | Description |
|--------|-------------|
| `npm start` | Serveur de développement |
| `npm run build` | Build production |
| `npm run build:dev` | Build développement |
| `npm test` | Tests unitaires |
| `npm run lint` | Linting ESLint |

## 🌍 Internationalisation

Les deux applications supportent 3 langues :
- 🇫🇷 **Français** (par défaut)
- 🇬🇧 **Anglais**
- 🇹🇳 **Arabe** (RTL supporté)

## 🎨 Thème et styles

### Variables SCSS communes

```scss
// Couleurs Partner Dashboard
$primary: #FF6B35;   // Orange

// Couleurs Admin Panel
$primary: #4F46E5;   // Indigo
```

### Breakpoints

```scss
$breakpoints: (
  'sm': 576px,
  'md': 768px,
  'lg': 1024px,
  'xl': 1280px,
);
```

## 🔐 Authentification

Les deux applications utilisent JWT pour l'authentification :

1. **Login** → Token JWT stocké en localStorage
2. **Auth Interceptor** → Ajoute le token aux requêtes
3. **Auth Guard** → Protège les routes authentifiées
4. **Error Interceptor** → Gère les erreurs 401/403

## 📡 Communication avec le Backend

```
Frontend Apps ──────► API Gateway (8080) ──────► Microservices
                            │
                            ├── auth-service
                            ├── partner-service
                            ├── order-service
                            ├── user-service
                            └── ...
```

## 🔄 WebSocket (temps réel)

Les applications utilisent Socket.IO pour :
- **Partner Dashboard** : Nouvelles commandes, mises à jour statut
- **Admin Panel** : Alertes système, notifications, monitoring

## 📝 Conventions de développement

### Composants
- Utiliser les standalone components Angular 17
- Préférer les signals pour l'état local
- Lazy loading pour les feature modules

### Services
- Pattern inject() pour l'injection de dépendances
- Functional guards et interceptors
- Utiliser les signaux Angular pour l'état

### Styles
- SCSS avec BEM naming convention
- Utiliser les variables et mixins partagés
- Mobile-first pour le responsive (Partner) / Desktop-first pour (Admin)

## 🚀 Déploiement

### Build pour production

```bash
# Partner Dashboard
cd partner-dashboard
npm run build
# Output: dist/partner-dashboard/

# Admin Panel
cd admin-panel
npm run build
# Output: dist/admin-panel/
```

### Configuration Nginx suggérée

```nginx
# Partner Dashboard
server {
    listen 80;
    server_name partners.speedline.tn;
    root /var/www/partner-dashboard;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}

# Admin Panel
server {
    listen 80;
    server_name admin.speedline.tn;
    root /var/www/admin-panel;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 📄 Licence

Propriétaire - SpeedLine © 2024
