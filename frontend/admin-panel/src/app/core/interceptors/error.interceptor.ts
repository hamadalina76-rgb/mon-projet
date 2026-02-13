// src/app/core/interceptors/error.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '@core/services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastr = inject(ToastrService);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Skip interceptor toast for auth endpoints (login handles its own errors)
      const isAuthRequest = req.url.includes('/auth/');

      let errorMessage = 'Une erreur est survenue';

      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Requête invalide';
          break;
        case 401:
          if (!isAuthRequest) {
            errorMessage = 'Session expirée';
            authService.logout();
            router.navigate(['/auth/login']);
          }
          break;
        case 403:
          errorMessage = 'Accès non autorisé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 500:
          errorMessage = error.error?.message || 'Erreur serveur';
          break;
      }

      if (!isAuthRequest) {
        toastr.error(errorMessage, 'Erreur');
      }
      return throwError(() => error);
    })
  );
};
