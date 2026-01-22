// src/app/features/profile/profile.routes.ts
import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./profile-settings/profile-settings.component').then(
        (m) => m.ProfileSettingsComponent
      ),
  },
  {
    path: 'business',
    loadComponent: () =>
      import('./business-info/business-info.component').then(
        (m) => m.BusinessInfoComponent
      ),
  },
  {
    path: 'hours',
    loadComponent: () =>
      import('./opening-hours/opening-hours.component').then(
        (m) => m.OpeningHoursComponent
      ),
  },
  {
    path: 'zones',
    loadComponent: () =>
      import('./delivery-zones/delivery-zones.component').then(
        (m) => m.DeliveryZonesComponent
      ),
  },
];
