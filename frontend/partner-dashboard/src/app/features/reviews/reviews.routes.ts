// src/app/features/reviews/reviews.routes.ts
import { Routes } from '@angular/router';

export const REVIEWS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./reviews-list/reviews-list.component').then(
        (m) => m.ReviewsListComponent
      ),
  },
];
