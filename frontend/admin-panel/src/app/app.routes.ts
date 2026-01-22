// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { adminGuard } from '@core/guards/admin.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./layout/admin-layout/admin-layout.component').then(
        (m) => m.AdminLayoutComponent
      ),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },
      {
        path: 'users',
        loadChildren: () =>
          import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'partners',
        loadChildren: () =>
          import('./features/partners/partners.routes').then(
            (m) => m.PARTNERS_ROUTES
          ),
      },
      {
        path: 'orders',
        loadChildren: () =>
          import('./features/orders/orders.routes').then((m) => m.ORDERS_ROUTES),
      },
      {
        path: 'payments',
        loadChildren: () =>
          import('./features/payments/payments.routes').then(
            (m) => m.PAYMENTS_ROUTES
          ),
      },
      {
        path: 'promotions',
        loadChildren: () =>
          import('./features/promotions/promotions.routes').then(
            (m) => m.PROMOTIONS_ROUTES
          ),
      },
      {
        path: 'reviews',
        loadChildren: () =>
          import('./features/reviews/reviews.routes').then(
            (m) => m.REVIEWS_ROUTES
          ),
      },
      {
        path: 'support',
        loadChildren: () =>
          import('./features/support/support.routes').then(
            (m) => m.SUPPORT_ROUTES
          ),
      },
      {
        path: 'zones',
        loadChildren: () =>
          import('./features/zones/zones.routes').then((m) => m.ZONES_ROUTES),
      },
      {
        path: 'analytics',
        loadChildren: () =>
          import('./features/analytics/analytics.routes').then(
            (m) => m.ANALYTICS_ROUTES
          ),
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('./features/notifications/notifications.routes').then(
            (m) => m.NOTIFICATIONS_ROUTES
          ),
      },
      {
        path: 'settings',
        loadChildren: () =>
          import('./features/settings/settings.routes').then(
            (m) => m.SETTINGS_ROUTES
          ),
      },
      {
        path: 'monitoring',
        loadChildren: () =>
          import('./features/monitoring/monitoring.routes').then(
            (m) => m.MONITORING_ROUTES
          ),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
