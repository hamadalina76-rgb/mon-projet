// src/app/features/zones/zones.routes.ts
import { Routes } from '@angular/router';

export const ZONES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./zones-list/zones-list.component').then(
        (m) => m.ZonesListComponent
      ),
  },
  {
    path: 'editor',
    loadComponent: () =>
      import('./zone-editor/zone-editor.component').then(
        (m) => m.ZoneEditorComponent
      ),
  },
  {
    path: 'map-editor',
    loadComponent: () =>
      import('./zone-map-editor/zone-map-editor.component').then(
        (m) => m.ZoneMapEditorComponent
      ),
  },
  {
    path: ':id/map-edit',
    loadComponent: () =>
      import('./zone-map-editor/zone-map-editor.component').then(
        (m) => m.ZoneMapEditorComponent
      ),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./zone-editor/zone-editor.component').then(
        (m) => m.ZoneEditorComponent
      ),
  },
];
