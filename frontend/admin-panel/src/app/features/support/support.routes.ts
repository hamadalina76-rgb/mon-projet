// src/app/features/support/support.routes.ts
import { Routes } from '@angular/router';

export const SUPPORT_ROUTES: Routes = [
  {
    path: 'tickets',
    loadComponent: () =>
      import('./tickets-list/tickets-list.component').then(
        (m) => m.TicketsListComponent
      ),
  },
  {
    path: 'tickets/:id',
    loadComponent: () =>
      import('./ticket-detail/ticket-detail.component').then(
        (m) => m.TicketDetailComponent
      ),
  },
  {
    path: 'chat',
    loadComponent: () =>
      import('./live-chat/live-chat.component').then((m) => m.LiveChatComponent),
  },
  {
    path: 'faq',
    loadComponent: () =>
      import('./faq-manager/faq-manager.component').then(
        (m) => m.FaqManagerComponent
      ),
  },
  {
    path: '',
    redirectTo: 'tickets',
    pathMatch: 'full',
  },
];
