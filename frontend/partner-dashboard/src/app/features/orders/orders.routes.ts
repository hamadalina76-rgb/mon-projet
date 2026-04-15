// src/app/features/orders/orders.routes.ts
import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: 'history',
    loadComponent: () =>
      import('./order-history/order-history.component').then(
        (m) => m.OrderHistoryComponent
      ),
  },
  {
    path: 'admin-messages',
    loadComponent: () =>
      import('./admin-order-messages/admin-order-messages.component').then(
        (m) => m.AdminOrderMessagesComponent
      ),
  },
  {
    path: '',
    loadComponent: () =>
      import('./orders-list/orders-list.component').then(
        (m) => m.OrdersListComponent
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
