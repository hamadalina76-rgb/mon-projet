// src/app/core/guards/admin.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const userRole = authService.getUserRole();
  const adminRoles = ['SUPER_ADMIN', 'ADMIN', 'MODERATOR'];

  if (userRole && adminRoles.includes(userRole)) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};
