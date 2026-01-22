// src/app/features/analytics/analytics.routes.ts
import { Routes } from '@angular/router';

export const ANALYTICS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./analytics-overview/analytics-overview.component').then(
        (m) => m.AnalyticsOverviewComponent
      ),
  },
  {
    path: 'sales',
    loadComponent: () =>
      import('./sales-report/sales-report.component').then(
        (m) => m.SalesReportComponent
      ),
  },
];
