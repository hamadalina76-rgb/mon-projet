// src/app/core/guards/partner.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

/**
 * Rôles autorisés pour le tableau de bord partenaire UNIQUEMENT.
 * Exclus explicitement : CUSTOMER (client), COURIER (livreur), ADMIN, SUPER_ADMIN, etc.
 */
const PARTNER_ROLES = ['PARTNER', 'PARTNER_OWNER', 'PARTNER_MANAGER', 'PARTNER_STAFF'];

/**
 * Guard: vérifie que l'utilisateur connecté est un partenaire.
 * Refuse : client (CUSTOMER), livreur (COURIER), admin (ADMIN, SUPER_ADMIN, etc.).
 */
export const partnerGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/auth/login'], {
      queryParams: { returnUrl: state.url },
    });
    return false;
  }

  const userRole = authService.getUserRole();

  if (userRole && PARTNER_ROLES.includes(userRole)) {
    return true;
  }

  // Rôle non partenaire (admin, client, livreur, etc.) → accès refusé
  authService.logout();
  router.navigate(['/auth/login'], {
    queryParams: { error: 'access_denied', message: 'Cette application est réservée aux partenaires.' },
  });
  return false;
};
