// src/app/features/monitoring/monitoring.routes.ts
import { Routes } from '@angular/router';

export const MONITORING_ROUTES: Routes = [
  {
    path: 'health',
    loadComponent: () =>
      import('./system-health/system-health.component').then(
        (m) => m.SystemHealthComponent
      ),
  },
  {
    path: 'logs',
    loadComponent: () =>
      import('./audit-logs/audit-logs.component').then((m) => m.AuditLogsComponent),
  },
  {
    path: 'errors',
    loadComponent: () =>
      import('./error-logs/error-logs.component').then(
        (m) => m.ErrorLogsComponent
      ),
  },
  {
    path: '',
    redirectTo: 'health',
    pathMatch: 'full',
  },
];
