// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

/**
 * Auth interceptor that adds Authorization header to all requests
 * EXCEPT auth endpoints (/api/v1/auth/**)
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const url = req.url || '';
  const lowerUrl = url.toLowerCase();

  // Skip static assets and config/i18n files.
  const isStaticRequest =
    lowerUrl.includes('/assets/') ||
    lowerUrl.includes('assets/i18n/') ||
    lowerUrl.includes('assets/config/') ||
    lowerUrl.endsWith('favicon.svg') ||
    lowerUrl.endsWith('.json');

  // Skip auth endpoints - they don't need Authorization header
  const isAuthEndpoint = lowerUrl.includes('/api/v1/auth/');

  if (isStaticRequest || isAuthEndpoint) {
    return next(req);
  }

  const token = authService.getToken();

  if (token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(clonedReq);
  }

  return next(req);
};
