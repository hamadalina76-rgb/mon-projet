# SpeedLine Partner Dashboard

🏪 Dashboard web pour les partenaires (restaurants) de SpeedLine.

## 📋 Prérequis

- Node.js >= 18.x
- npm >= 9.x
- Angular CLI 17.x

## 🚀 Installation

```bash
# Installer les dépendances
npm install

# Démarrer le serveur de développement
npm start
```

L'application sera accessible sur `http://localhost:4200`

## 📁 Structure du Projet

```
partner-dashboard/
├── src/
│   ├── app/
│   │   ├── core/                 # Services singleton, guards, interceptors
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── shared/               # Composants réutilisables
│   │   │   ├── components/
│   │   │   ├── pipes/
│   │   │   └── directives/
│   │   ├── features/             # Modules de fonctionnalités
│   │   │   ├── auth/
│   │   │   ├── dashboard/
│   │   │   ├── orders/
│   │   │   ├── menu/
│   │   │   ├── analytics/
│   │   │   ├── reviews/
│   │   │   ├── promotions/
│   │   │   ├── profile/
│   │   │   └── finance/
│   │   └── layout/               # Layouts de l'application
│   ├── assets/
│   │   ├── images/
│   │   ├── icons/
│   │   ├── styles/
│   │   └── i18n/
│   └── environments/
└── ...
```

## 🛠️ Scripts Disponibles

```bash
# Développement
npm start                 # Démarrer le serveur de dev
npm run start:dev         # Démarrer en mode development
npm run start:prod        # Démarrer en mode production

# Build
npm run build             # Build de production
npm run build:prod        # Build optimisé pour production

# Tests
npm test                  # Lancer les tests unitaires
npm run test:ci           # Tests en mode CI

# Qualité de code
npm run lint              # Lancer ESLint
```

## 🔧 Configuration

### Environnements

- `environment.development.ts` - Développement local
- `environment.production.ts` - Production

### Variables d'environnement

| Variable | Description |
|----------|-------------|
| `apiUrl` | URL de l'API backend |
| `wsUrl` | URL WebSocket |
| `mapboxToken` | Token Mapbox pour les cartes |

## 📦 Dépendances Principales

| Package | Version | Description |
|---------|---------|-------------|
| @angular/core | ^17.0.0 | Framework Angular |
| @angular/material | ^17.0.0 | Composants Material Design |
| rxjs | ^7.8.0 | Programmation réactive |
| ngx-socket-io | ^4.6.0 | WebSocket client |
| chart.js | ^4.4.0 | Graphiques |
| ngx-translate | ^15.0.0 | Internationalisation |
| ngx-toastr | ^17.0.2 | Notifications toast |

## 🌍 Internationalisation

L'application supporte 3 langues:
- 🇫🇷 Français (fr) - par défaut
- 🇬🇧 Anglais (en)
- 🇹🇳 Arabe (ar)

Les fichiers de traduction sont dans `src/assets/i18n/`

## 📱 Fonctionnalités

### Dashboard
- Statistiques en temps réel
- Graphiques de revenus
- Commandes récentes

### Gestion des Commandes
- Liste des commandes en temps réel
- Notifications sonores pour nouvelles commandes
- Actions: accepter, préparer, marquer prêt

### Gestion du Menu
- CRUD produits
- Gestion des catégories
- Options et suppléments

### Analytiques
- Revenus
- Tendances
- Rapports exportables

### Avis
- Liste des avis clients
- Réponses aux avis

### Promotions
- Création de codes promo
- Suivi des performances

### Profil
- Informations du restaurant
- Horaires d'ouverture
- Zones de livraison

### Finance
- Suivi des gains
- Historique des versements

## 🔐 Authentification

L'application utilise JWT pour l'authentification:
- Token stocké en localStorage
- Interceptor pour ajouter le header Authorization
- Guard pour protéger les routes

## 🔌 WebSocket

Connexion temps réel pour:
- Notifications de nouvelles commandes
- Mises à jour de statut
- Alertes système
