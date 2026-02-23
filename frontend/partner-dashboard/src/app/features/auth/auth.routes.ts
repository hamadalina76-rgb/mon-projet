// src/app/features/auth/auth.routes.ts
import { Routes } from '@angular/router';
import { authGuard, incompleteProfileGuard } from '@core/guards/auth.guard';
import { partnerGuard } from '@core/guards/partner.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@layout/auth-layout/auth-layout.component').then(
        (m) => m.AuthLayoutComponent
      ),
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./register/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'registration-success',
        loadComponent: () =>
          import('./registration-success/registration-success.component').then((m) => m.RegistrationSuccessComponent),
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent),
      },
      {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full',
      },
    ],
  },
  {
    // Complete profile is outside the auth layout (it has its own layout since user is authenticated)
    path: 'complete-profile',
    canActivate: [authGuard, partnerGuard, incompleteProfileGuard],
    loadComponent: () =>
      import('./complete-profile/complete-profile.component').then(
        (m) => m.CompleteProfileComponent
      ),
  },
];
