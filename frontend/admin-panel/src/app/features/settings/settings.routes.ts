// src/app/features/settings/settings.routes.ts
import { Routes } from '@angular/router';

export const SETTINGS_ROUTES: Routes = [
  {
    path: 'general',
    loadComponent: () =>
      import('./general-settings/general-settings.component').then(
        (m) => m.GeneralSettingsComponent
      ),
  },
  {
    path: 'commission',
    loadComponent: () =>
      import('../payments/commission-settings/commission-settings.component').then(
        (m) => m.CommissionSettingsComponent
      ),
  },
  {
    path: 'payment',
    loadComponent: () =>
      import('./payment-settings/payment-settings.component').then(
        (m) => m.PaymentSettingsComponent
      ),
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./notification-settings/notification-settings.component').then(
        (m) => m.NotificationSettingsComponent
      ),
  },
  {
    path: 'email-templates',
    loadComponent: () =>
      import('./email-templates/email-templates.component').then(
        (m) => m.EmailTemplatesComponent
      ),
  },
  {
    path: '',
    redirectTo: 'general',
    pathMatch: 'full',
  },
];
