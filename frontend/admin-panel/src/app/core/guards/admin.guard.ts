// src/app/core/guards/admin.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { AdminRole } from '@core/models/role.model';

const ALLOWED_ROLES: AdminRole[] = [
  'SUPER_ADMIN',
  'ADMIN',
  'FINANCE_ADMIN',
  'SUPPORT_ADMIN',
  'CONTENT_MODERATOR',
];

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const userRole = authService.getUserRole();

  if (userRole && ALLOWED_ROLES.includes(userRole)) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};
