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
  {
    path: 'moderation',
    loadComponent: () =>
      import('./review-moderation/review-moderation.component').then(
        (m) => m.ReviewModerationComponent
      ),
  },
  {
    path: 'flagged',
    loadComponent: () =>
      import('./flagged-reviews/flagged-reviews.component').then(
        (m) => m.FlaggedReviewsComponent
      ),
  },
];
