// src/app/core/guards/permission.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { AdminRole } from '@core/models/role.model';

export const permissionGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check role-based access
  const requiredRoles = route.data['roles'] as AdminRole[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    if (!authService.hasAnyRole(requiredRoles)) {
      router.navigate(['/dashboard']);
      return false;
    }
  }

  // Check permission-based access
  const requiredPermissions = route.data['permissions'] as string[] | undefined;
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true;
  }

  if (authService.hasAnyPermission(requiredPermissions)) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
