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
  
  // Skip auth endpoints - they don't need Authorization header
  const isAuthEndpoint = req.url.includes('/api/v1/auth/');
  
  if (isAuthEndpoint) {
    console.log('[AuthInterceptor] Skipping auth endpoint:', req.url);
    return next(req);
  }
  
  const token = authService.getToken();

  if (token) {
    console.log('[AuthInterceptor] Adding Authorization header to:', req.method, req.url);
    console.log('[AuthInterceptor] Token (first 20 chars):', token.substring(0, 20) + '...');
    
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(clonedReq);
  } else {
    console.log('[AuthInterceptor] No token found for:', req.method, req.url);
  }

  return next(req);
};
