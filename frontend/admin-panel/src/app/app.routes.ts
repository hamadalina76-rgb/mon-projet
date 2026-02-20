// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { adminGuard } from '@core/guards/admin.guard';
import { permissionGuard } from '@core/guards/permission.guard';
import { PERMISSIONS } from '@core/models/role.model';

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
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.DASHBOARD_VIEW] },
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },
      {
        path: 'users',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.USERS_VIEW] },
        loadChildren: () =>
          import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'partners',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.PARTNERS_VIEW] },
        loadChildren: () =>
          import('./features/partners/partners.routes').then(
            (m) => m.PARTNERS_ROUTES
          ),
      },
      {
        path: 'orders',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.ORDERS_VIEW] },
        loadChildren: () =>
          import('./features/orders/orders.routes').then((m) => m.ORDERS_ROUTES),
      },
      {
        path: 'payments',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.PAYMENTS_VIEW] },
        loadChildren: () =>
          import('./features/payments/payments.routes').then(
            (m) => m.PAYMENTS_ROUTES
          ),
      },
      {
        path: 'promotions',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.PROMOTIONS_VIEW] },
        loadChildren: () =>
          import('./features/promotions/promotions.routes').then(
            (m) => m.PROMOTIONS_ROUTES
          ),
      },
      {
        path: 'reviews',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.REVIEWS_VIEW] },
        loadChildren: () =>
          import('./features/reviews/reviews.routes').then(
            (m) => m.REVIEWS_ROUTES
          ),
      },
      {
        path: 'support',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.SUPPORT_VIEW] },
        loadChildren: () =>
          import('./features/support/support.routes').then(
            (m) => m.SUPPORT_ROUTES
          ),
      },
      {
        path: 'zones',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.ZONES_VIEW] },
        loadChildren: () =>
          import('./features/zones/zones.routes').then((m) => m.ZONES_ROUTES),
      },
      {
        path: 'analytics',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.ANALYTICS_VIEW] },
        loadChildren: () =>
          import('./features/analytics/analytics.routes').then(
            (m) => m.ANALYTICS_ROUTES
          ),
      },
      {
        path: 'notifications',
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.NOTIFICATIONS_VIEW] },
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
        canActivate: [permissionGuard],
        data: { permissions: [PERMISSIONS.MONITORING_VIEW] },
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
