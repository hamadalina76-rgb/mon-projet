import { Routes } from '@angular/router';
import { permissionGuard } from '@core/guards/permission.guard';
import { PERMISSIONS } from '@core/models/role.model';

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
  {
    path: 'config',
    canActivate: [permissionGuard],
    data: { permissions: [PERMISSIONS.DELIVERY_MANAGE] },
    loadComponent: () =>
      import('./dispatch-config/dispatch-config-page.component').then(
        (m) => m.DispatchConfigPageComponent
      ),
  },
];
