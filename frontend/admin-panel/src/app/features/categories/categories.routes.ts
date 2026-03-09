// src/app/features/categories/categories.routes.ts
import { Routes } from '@angular/router';

export const CATEGORIES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./categories-list/categories-list.component').then(
        (m) => m.CategoriesListComponent
      ),
  },
 
  {
    path: 'create',
    loadComponent: () =>
      import('./category-configuration/category-configuration.component').then(
        (m) => m.CategoryConfigurationComponent
      ),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./category-configuration/category-configuration.component').then(
        (m) => m.CategoryConfigurationComponent
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./category-detail/category-detail.component').then(
        (m) => m.CategoryDetailComponent
      ),
  },
];