// src/app/core/guards/permission.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

export const permissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredPermissions = route.data['permissions'] as string[];

  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true;
  }

  const userPermissions = authService.getUserPermissions();
  const hasPermission = requiredPermissions.some((p) =>
    userPermissions.includes(p)
  );

  if (hasPermission) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
