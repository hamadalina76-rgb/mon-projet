import { Routes } from '@angular/router';
import { permissionGuard } from '@core/guards/permission.guard';

export const DISPATCH_ROUTES: Routes = [
  {
    path: '',
    canActivate: [permissionGuard],
    data: { permissions: ['delivery:view'] },
    loadComponent: () =>
      import('./dispatch-dashboard/dispatch-dashboard.component').then(
        (m) => m.DispatchDashboardComponent
      ),
  },
];
