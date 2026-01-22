// src/app/features/payments/payments.routes.ts
import { Routes } from '@angular/router';

export const PAYMENTS_ROUTES: Routes = [
  {
    path: 'transactions',
    loadComponent: () =>
      import('./transactions/transactions.component').then(
        (m) => m.TransactionsComponent
      ),
  },
  {
    path: 'payouts',
    loadComponent: () =>
      import('./payouts/payouts.component').then((m) => m.PayoutsComponent),
  },
  {
    path: 'refunds',
    loadComponent: () =>
      import('./refunds/refunds.component').then((m) => m.RefundsComponent),
  },
  {
    path: 'commission',
    loadComponent: () =>
      import('./commission-settings/commission-settings.component').then(
        (m) => m.CommissionSettingsComponent
      ),
  },
  {
    path: '',
    redirectTo: 'transactions',
    pathMatch: 'full',
  },
];
