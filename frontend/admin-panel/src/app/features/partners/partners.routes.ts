// src/app/features/partners/partners.routes.ts
import { Routes } from '@angular/router';

export const PARTNERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./partners-list/partners-list.component').then(
        (m) => m.PartnersListComponent
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./partner-detail/partner-detail.component').then(
        (m) => m.PartnerDetailComponent
      ),
  },
  {
    path: ':id/approval',
    loadComponent: () =>
      import('./partner-approval/partner-approval.component').then(
        (m) => m.PartnerApprovalComponent
      ),
  },
  {
    path: ':id/verification',
    loadComponent: () =>
      import('./partner-verification/partner-verification.component').then(
        (m) => m.PartnerVerificationComponent
      ),
  },
];
