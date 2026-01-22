// src/app/features/finance/finance.routes.ts
import { Routes } from '@angular/router';

export const FINANCE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./earnings/earnings.component').then((m) => m.EarningsComponent),
  },
  {
    path: 'payouts',
    loadComponent: () =>
      import('./payouts/payouts.component').then((m) => m.PayoutsComponent),
  },
];
