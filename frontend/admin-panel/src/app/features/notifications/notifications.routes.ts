// src/app/features/notifications/notifications.routes.ts
import { Routes } from '@angular/router';

export const NOTIFICATIONS_ROUTES: Routes = [
  {
    path: 'send',
    loadComponent: () =>
      import('./send-notification/send-notification.component').then(
        (m) => m.SendNotificationComponent
      ),
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./notification-history/notification-history.component').then(
        (m) => m.NotificationHistoryComponent
      ),
  },
  {
    path: 'templates',
    loadComponent: () =>
      import('./templates/templates.component').then(
        (m) => m.TemplatesComponent
      ),
  },
  {
    path: '',
    redirectTo: 'send',
    pathMatch: 'full',
  },
];
