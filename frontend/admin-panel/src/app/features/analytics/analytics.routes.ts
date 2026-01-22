// src/app/features/analytics/analytics.routes.ts
import { Routes } from '@angular/router';

export const ANALYTICS_ROUTES: Routes = [
  {
    path: 'overview',
    loadComponent: () =>
      import('./overview/overview.component').then((m) => m.OverviewComponent),
  },
  {
    path: 'revenue',
    loadComponent: () =>
      import('./revenue-analytics/revenue-analytics.component').then(
        (m) => m.RevenueAnalyticsComponent
      ),
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./user-analytics/user-analytics.component').then(
        (m) => m.UserAnalyticsComponent
      ),
  },
  {
    path: 'partners',
    loadComponent: () =>
      import('./partner-performance/partner-performance.component').then(
        (m) => m.PartnerPerformanceComponent
      ),
  },
  {
    path: 'couriers',
    loadComponent: () =>
      import('./courier-performance/courier-performance.component').then(
        (m) => m.CourierPerformanceComponent
      ),
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./custom-reports/custom-reports.component').then(
        (m) => m.CustomReportsComponent
      ),
  },
  {
    path: '',
    redirectTo: 'overview',
    pathMatch: 'full',
  },
];
