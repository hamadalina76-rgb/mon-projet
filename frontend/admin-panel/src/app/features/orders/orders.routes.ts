// src/app/features/orders/orders.routes.ts
import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./all-orders/all-orders.component').then(
        (m) => m.AllOrdersComponent
      ),
  },
  {
    path: 'disputed',
    loadComponent: () =>
      import('./disputed-orders/disputed-orders.component').then(
        (m) => m.DisputedOrdersComponent
      ),
  },
  {
    path: 'stats',
    loadComponent: () =>
      import('./components/orders-stats/orders-stats.component').then(
        (m) => m.OrdersStatsComponent
      ),
  },
  {
    path: 'logs',
    loadComponent: () =>
      import('./orders-logs/orders-logs.component').then(
        (m) => m.OrdersLogsComponent
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./order-detail/order-detail.component').then(
        (m) => m.OrderDetailComponent
      ),
  },
];
