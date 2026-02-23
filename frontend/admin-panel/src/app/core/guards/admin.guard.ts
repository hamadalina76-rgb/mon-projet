// src/app/core/guards/admin.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { AdminRole } from '@core/models/role.model';

/**
 * Rôles autorisés pour le panneau admin UNIQUEMENT.
 * Exclus explicitement : CUSTOMER (client), COURIER (livreur), PARTNER (partenaire), etc.
 */
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

  // Rôle non admin (partner, client, livreur) → déconnexion et redirection
  authService.logout();
  return false;
};
