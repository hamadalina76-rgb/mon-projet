// src/app/core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

/**
 * Guard: requires authentication (valid JWT token)
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
  return false;
};

/**
 * Guard: requires completed partner profile
 * Redirects to /auth/complete-profile if profile is incomplete
 */
export const profileCompleteGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (authService.isProfileComplete()) {
    return true;
  }

  router.navigate(['/auth/complete-profile']);
  return false;
};

/**
 * Guard: prevents access to complete-profile if profile already completed
 * BUT allows access if status is DOCUMENTS_MISSING (partner needs to update profile)
 * Redirects to /dashboard if profile is already complete and status is not DOCUMENTS_MISSING
 */
export const incompleteProfileGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const partnerService = inject(PartnerService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/auth/login']);
    return false;
  }

  // If profile is not complete, allow access
  if (!authService.isProfileComplete()) {
    return true;
  }

  // If profile is complete, check partner status
  // Allow access if status is DOCUMENTS_MISSING (partner needs to update)
  const partnerId = authService.getPartnerId();
  if (partnerId) {
    return partnerService.getPartner(partnerId).pipe(
      map((partner: any) => {
        const status = partner?.status?.toLowerCase();
        // Allow access if status is DOCUMENTS_MISSING
        if (status === 'documents_missing') {
          return true;
        }
        // Otherwise redirect to dashboard
        router.navigate(['/dashboard']);
        return false;
      }),
      catchError(() => {
        // On error, allow access (safer to let user try)
        return of(true);
      })
    );
  }

  // If no partnerId but profile is complete, redirect to dashboard
  router.navigate(['/dashboard']);
  return false;
};
