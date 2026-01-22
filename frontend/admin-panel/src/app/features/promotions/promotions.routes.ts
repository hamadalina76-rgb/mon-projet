// src/app/features/promotions/promotions.routes.ts
import { Routes } from '@angular/router';

export const PROMOTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./promotions-list/promotions-list.component').then(
        (m) => m.PromotionsListComponent
      ),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./promotion-form/promotion-form.component').then(
        (m) => m.PromotionFormComponent
      ),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./promotion-form/promotion-form.component').then(
        (m) => m.PromotionFormComponent
      ),
  },
  {
    path: ':id/analytics',
    loadComponent: () =>
      import('./promotion-analytics/promotion-analytics.component').then(
        (m) => m.PromotionAnalyticsComponent
      ),
  },
];
