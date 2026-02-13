// src/app/features/users/users.routes.ts
import { Routes } from '@angular/router';
import { permissionGuard } from '@core/guards/permission.guard';

export const USERS_ROUTES: Routes = [
  {
    path: 'customers',
    loadComponent: () =>
      import('./customers/customers-list/customers-list.component').then(
        (m) => m.CustomersListComponent
      ),
  },
  {
    path: 'customers/:id',
    loadComponent: () =>
      import('./customers/customer-detail/customer-detail.component').then(
        (m) => m.CustomerDetailComponent
      ),
  },
  {
    path: 'couriers',
    loadComponent: () =>
      import('./couriers/couriers-list/couriers-list.component').then(
        (m) => m.CouriersListComponent
      ),
  },
  {
    path: 'couriers/:id',
    loadComponent: () =>
      import('./couriers/courier-detail/courier-detail.component').then(
        (m) => m.CourierDetailComponent
      ),
  },
  {
    path: 'couriers/:id/approval',
    loadComponent: () =>
      import('./couriers/courier-approval/courier-approval.component').then(
        (m) => m.CourierApprovalComponent
      ),
  },
  {
    path: 'admins',
    canActivate: [permissionGuard],
    data: { permissions: ['admins:view'] },
    loadComponent: () =>
      import('./admins/admins-list/admins-list.component').then(
        (m) => m.AdminsListComponent
      ),
  },
  {
    path: 'admins/new',
    canActivate: [permissionGuard],
    data: { permissions: ['admins:create'] },
    loadComponent: () =>
      import('./admins/admin-form/admin-form.component').then(
        (m) => m.AdminFormComponent
      ),
  },
  {
    path: 'admins/:id/edit',
    canActivate: [permissionGuard],
    data: { permissions: ['admins:edit'] },
    loadComponent: () =>
      import('./admins/admin-form/admin-form.component').then(
        (m) => m.AdminFormComponent
      ),
  },
  {
    path: '',
    redirectTo: 'customers',
    pathMatch: 'full',
  },
];
