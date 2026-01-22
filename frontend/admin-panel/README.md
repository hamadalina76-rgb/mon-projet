# SpeedLine Admin Panel

Panneau d'administration Angular 17 pour la plateforme de livraison SpeedLine.

## 🚀 Technologies

- **Angular 17.0.0** - Framework frontend avec standalone components
- **Angular Material 17** - Composants UI Material Design
- **RxJS 7.8.0** - Programmation réactive
- **ngx-translate** - Internationalisation (FR, EN, AR)
- **ngx-socket-io** - WebSocket pour le temps réel
- **Chart.js + ng2-charts** - Graphiques et visualisations
- **Leaflet** - Cartes interactives pour les zones
- **date-fns** - Manipulation des dates

## 📁 Structure du projet

```
admin-panel/
├── src/
│   ├── app/
│   │   ├── core/                    # Module cœur (singleton)
│   │   │   ├── guards/              # Route guards
│   │   │   │   ├── auth.guard.ts    # Protection authentification
│   │   │   │   ├── admin.guard.ts   # Vérification rôle admin
│   │   │   │   └── permission.guard.ts # Contrôle permissions
│   │   │   ├── interceptors/        # HTTP interceptors
│   │   │   │   ├── auth.interceptor.ts    # Injection JWT
│   │   │   │   ├── error.interceptor.ts   # Gestion erreurs
│   │   │   │   └── loading.interceptor.ts # État chargement
│   │   │   ├── services/            # Services globaux
│   │   │   │   ├── auth.service.ts  # Authentification
│   │   │   │   ├── api.service.ts   # Client HTTP
│   │   │   │   └── loading.service.ts # État chargement
│   │   │   └── models/              # Interfaces TypeScript
│   │   │       ├── user.model.ts    # User avec permissions
│   │   │       └── api-response.model.ts
│   │   │
│   │   ├── shared/                  # Composants partagés
│   │   │   ├── components/
│   │   │   │   ├── sidebar/         # Navigation latérale
│   │   │   │   ├── header/          # En-tête avec alertes
│   │   │   │   ├── data-table/      # Table de données générique
│   │   │   │   ├── stats-card/      # Carte statistiques
│   │   │   │   ├── loading-spinner/ # Indicateur chargement
│   │   │   │   ├── empty-state/     # État vide
│   │   │   │   └── confirmation-dialog/
│   │   │   ├── pipes/               # Pipes personnalisés
│   │   │   │   ├── currency.pipe.ts # Format TND
│   │   │   │   ├── date-format.pipe.ts
│   │   │   │   └── time-ago.pipe.ts
│   │   │   └── directives/
│   │   │       └── permission.directive.ts # Contrôle UI par permission
│   │   │
│   │   ├── layout/                  # Layouts
│   │   │   ├── admin-layout/        # Layout principal admin
│   │   │   └── auth-layout/         # Layout authentification
│   │   │
│   │   ├── features/                # Modules fonctionnels (lazy-loaded)
│   │   │   ├── auth/                # Authentification
│   │   │   ├── dashboard/           # Tableau de bord
│   │   │   ├── users/               # Gestion utilisateurs
│   │   │   │   ├── customers/       # Clients
│   │   │   │   ├── couriers/        # Livreurs
│   │   │   │   └── admins/          # Administrateurs
│   │   │   ├── partners/            # Gestion partenaires
│   │   │   ├── orders/              # Gestion commandes
│   │   │   ├── payments/            # Gestion paiements
│   │   │   ├── promotions/          # Gestion promotions
│   │   │   ├── reviews/             # Modération avis
│   │   │   ├── support/             # Support client
│   │   │   ├── zones/               # Gestion zones (carte)
│   │   │   ├── analytics/           # Analytiques
│   │   │   ├── notifications/       # Notifications push
│   │   │   ├── settings/            # Paramètres plateforme
│   │   │   └── monitoring/          # Monitoring système
│   │   │
│   │   ├── app.component.ts
│   │   ├── app.config.ts
│   │   └── app.routes.ts
│   │
│   ├── assets/
│   │   ├── i18n/                    # Fichiers de traduction
│   │   │   ├── fr.json
│   │   │   ├── en.json
│   │   │   └── ar.json
│   │   ├── images/
│   │   └── styles/
│   │       ├── _variables.scss
│   │       ├── _mixins.scss
│   │       └── _utilities.scss
│   │
│   ├── environments/
│   │   ├── environment.ts
│   │   ├── environment.development.ts
│   │   └── environment.production.ts
│   │
│   ├── index.html
│   ├── main.ts
│   └── styles.scss
│
├── angular.json
├── package.json
├── tsconfig.json
└── README.md
```

## 🔐 Système de permissions

L'admin panel utilise un système de permissions granulaire :

```typescript
const PERMISSIONS = {
  USERS_READ: 'users:read',
  USERS_WRITE: 'users:write',
  PARTNERS_READ: 'partners:read',
  PARTNERS_APPROVE: 'partners:approve',
  ORDERS_READ: 'orders:read',
  ORDERS_MANAGE: 'orders:manage',
  PAYMENTS_READ: 'payments:read',
  PAYMENTS_REFUND: 'payments:refund',
  ANALYTICS_READ: 'analytics:read',
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
  MONITORING_READ: 'monitoring:read',
};
```

### Utilisation dans les templates

```html
<!-- Avec directive -->
<button *appHasPermission="'partners:approve'">
  Approuver
</button>

<!-- Avec guard dans les routes -->
{
  path: 'settings',
  canActivate: [permissionGuard],
  data: { permission: 'settings:read' }
}
```

## 📊 Fonctionnalités principales

### 1. Tableau de bord
- Vue d'ensemble des KPIs (commandes, revenus, utilisateurs)
- Graphiques temps réel
- Alertes système
- État de santé des services

### 2. Gestion des utilisateurs
- **Clients** : Liste, détails, historique commandes
- **Livreurs** : Approbation, documents, performance
- **Admins** : Gestion des rôles et permissions

### 3. Gestion des partenaires
- Processus d'approbation
- Vérification des documents
- Statistiques par partenaire
- Gestion des suspensions

### 4. Gestion des commandes
- Suivi en temps réel
- Gestion des litiges
- Historique complet
- Assignation manuelle livreur

### 5. Gestion des paiements
- Transactions
- Virements partenaires
- Remboursements
- Configuration commissions

### 6. Zones de livraison
- Éditeur de carte (Leaflet)
- Définition des polygones
- Frais par zone
- Activation/désactivation

### 7. Analytics
- Revenus et commissions
- Performance partenaires/livreurs
- Analyse utilisateurs
- Rapports personnalisés

### 8. Monitoring
- État des microservices
- Logs API
- Suivi des erreurs
- Métriques temps réel

## 🛠️ Installation

```bash
# Installation des dépendances
npm install

# Démarrage en développement (port 4300)
npm start

# Build production
npm run build
```

## 🌐 Configuration des environnements

### Development (`environment.development.ts`)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080',
};
```

### Production (`environment.production.ts`)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.speedline.tn/api',
  wsUrl: 'wss://api.speedline.tn',
};
```

## 📱 Responsive Design

- Desktop first design
- Sidebar collapsible
- Tables scrollables sur mobile
- Breakpoints : 1024px, 768px, 480px

## 🔧 Scripts disponibles

```bash
npm start          # Démarre le serveur de développement
npm run build      # Build de production
npm run build:dev  # Build de développement
npm test           # Lance les tests unitaires
npm run lint       # Vérifie le code avec ESLint
```

## 📝 Conventions de code

- **Composants** : Standalone components Angular 17
- **State** : Angular Signals pour l'état local
- **Guards** : Functional guards (inject pattern)
- **Interceptors** : Functional interceptors
- **Styles** : SCSS avec variables et mixins

## 🔗 API Backend

L'admin panel communique avec l'API Gateway sur le port 8080 :

| Endpoint | Description |
|----------|-------------|
| `/api/admin/users` | Gestion utilisateurs |
| `/api/admin/partners` | Gestion partenaires |
| `/api/admin/orders` | Gestion commandes |
| `/api/admin/payments` | Gestion paiements |
| `/api/admin/analytics` | Données analytiques |
| `/api/admin/settings` | Configuration plateforme |

## 📄 Licence

Propriétaire - SpeedLine © 2024
