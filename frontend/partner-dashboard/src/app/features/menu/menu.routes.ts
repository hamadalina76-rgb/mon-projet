import { Routes } from '@angular/router';

export const MENU_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./menu-list/menu-list.component').then((m) => m.MenuListComponent),
  },
  {
    path: 'categories/new',
    loadComponent: () =>
      import('./category-manager/category-manager.component').then((m) => m.CategoryManagerComponent),
  },
  {
    path: 'categories/:id/edit',
    loadComponent: () =>
      import('./category-manager/category-manager.component').then((m) => m.CategoryManagerComponent),
  },
  {
    path: 'products/new',
    loadComponent: () =>
      import('./product-form/product-form.component').then((m) => m.ProductFormComponent),
  },
  {
    path: 'products/:id/edit',
    loadComponent: () =>
      import('./product-form/product-form.component').then((m) => m.ProductFormComponent),
  },
];
