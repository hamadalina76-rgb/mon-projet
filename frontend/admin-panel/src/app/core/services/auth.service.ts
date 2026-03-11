// src/app/core/services/auth.service.ts
import { Injectable, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of, switchMap } from 'rxjs';
import { ApiService } from './api.service';
import {
  AdminUser,
  LoginRequest,
  LoginResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ProfileResponse,
} from '@core/models/user.model';
import { AdminRole, ROLE_PERMISSIONS, Permission } from '@core/models/role.model';
import { environment } from '@environments/environment';

const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'user';
const REMEMBER_KEY = 'remember_me';

/** Rôles autorisés pour le panneau admin (exclut PARTNER, CUSTOMER, COURIER, etc.) */
const ADMIN_ROLES = ['SUPER_ADMIN', 'ADMIN', 'FINANCE_ADMIN', 'SUPPORT_ADMIN', 'CONTENT_MODERATOR'];

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSignal = signal<AdminUser | null>(null);
  private http = inject(HttpClient);

  currentUser = this.currentUserSignal.asReadonly();
  isLoggedIn = computed(() => !!this.currentUserSignal());

  constructor(
    private apiService: ApiService,
    private router: Router,
  ) {
    this.loadStoredUser();
  }

  // ---------------------------------------------------------------------------
  // Storage helpers (localStorage or sessionStorage based on "remember me")
  // ---------------------------------------------------------------------------
  private get storage(): Storage {
    return localStorage.getItem(REMEMBER_KEY) === 'true'
      ? localStorage
      : sessionStorage;
  }

  private loadStoredUser(): void {
    const userData =
      localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
    const token =
      localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);

    if (userData && token) {
      try {
        const user: AdminUser = JSON.parse(userData);
        const role = user?.role;
        if (!role || !ADMIN_ROLES.includes(role)) {
          this.clearStorage();
          return;
        }
        this.currentUserSignal.set(user);
      } catch {
        this.clearStorage();
      }
    }
  }

  private clearStorage(): void {
    [localStorage, sessionStorage].forEach((s) => {
      s.removeItem(TOKEN_KEY);
      s.removeItem(REFRESH_TOKEN_KEY);
      s.removeItem(USER_KEY);
    });
    localStorage.removeItem(REMEMBER_KEY);
  }

  // ---------------------------------------------------------------------------
  // Auth actions
  // ---------------------------------------------------------------------------
  login(credentials: LoginRequest): Observable<LoginResponse> {
    // Store remember preference
    if (credentials.rememberMe) {
      localStorage.setItem(REMEMBER_KEY, 'true');
    } else {
      localStorage.removeItem(REMEMBER_KEY);
    }

    return this.apiService
      .post<LoginResponse>('auth/admin/login', {
        email: credentials.email,
        password: credentials.password,
      })
      .pipe(
        switchMap((response) => {
          // Backend returns role as 'ADMIN' or 'SUPER_ADMIN' — validate admin access
          const backendRole = response.user.role;
          const allowedRoles = ['ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'SUPPORT_ADMIN', 'CONTENT_MODERATOR'];
          
          if (!allowedRoles.includes(backendRole)) {
            throw new Error('ACCESS_DENIED');
          }

          // Store tokens with debug logging
          console.log('[AuthService] Storing access token:', response.access_token?.substring(0, 20) + '...');
          this.setToken(response.access_token);
          this.setRefreshToken(response.refresh_token);
          console.log('[AuthService] Tokens stored in', credentials.rememberMe ? 'localStorage' : 'sessionStorage');

          // Map backend role to frontend AdminRole
          let frontendRole: AdminRole;
          if (backendRole === 'SUPER_ADMIN') {
            frontendRole = 'SUPER_ADMIN';
          } else if (backendRole === 'FINANCE_ADMIN') {
            frontendRole = 'FINANCE_ADMIN';
          } else if (backendRole === 'SUPPORT_ADMIN') {
            frontendRole = 'SUPPORT_ADMIN';
          } else if (backendRole === 'CONTENT_MODERATOR') {
            frontendRole = 'CONTENT_MODERATOR';
          } else {
            // Default 'ADMIN' from backend → use customPermissions, not SUPER_ADMIN
            frontendRole = 'ADMIN' as AdminRole;
          }

          // Fetch admin profile from user-service to get real customPermissions
          console.log('[AuthService] Fetching admin profile for user ID:', response.user.id);
          return this.http.get<any>(`${environment.apiUrl}/admins/by-user/${response.user.id}`).pipe(
            tap((adminProfile) => {
              // Convert module permissions to sidebar format (module -> module:view)
              let userPermissions: string[];
              
              if (frontendRole === 'SUPER_ADMIN') {
                // SUPER_ADMIN gets all permissions
                userPermissions = Object.values(ROLE_PERMISSIONS['SUPER_ADMIN']);
              } else if (adminProfile.customPermissions && adminProfile.customPermissions.length > 0) {
                // Use custom permissions from DB — convert modules to permission strings
                userPermissions = adminProfile.customPermissions.flatMap((module: string) => {
                  // For each module, generate common permission patterns
                  return [
                    `${module}:view`,
                    `${module}:create`,
                    `${module}:edit`,
                    `${module}:delete`,
                    `${module}:manage`
                  ];
                });
              } else {
                // Fallback: use role's default permissions
                userPermissions = Object.values(ROLE_PERMISSIONS[frontendRole]);
              }

              // Map backend user to AdminUser
              const adminUser: AdminUser = {
                id: String(response.user.id),
                email: response.user.email,
                firstName: response.user.firstName,
                lastName: response.user.lastName,
                role: frontendRole,
                permissions: userPermissions,
                avatar: response.user.profilePicture,
                status: 'ACTIVE',
                createdAt: new Date().toISOString(),
              };
              console.log('[AuthService] Admin profile loaded. Setting user:', adminUser.email);
              this.setUser(adminUser);
            }),
            switchMap(() => of(response)),
            catchError((error) => {
              // If profile fetch fails, fallback to role-based permissions
              console.warn('Failed to fetch admin profile, using role-based permissions', error);
              const adminUser: AdminUser = {
                id: String(response.user.id),
                email: response.user.email,
                firstName: response.user.firstName,
                lastName: response.user.lastName,
                role: frontendRole,
                permissions: Object.values(ROLE_PERMISSIONS[frontendRole]),
                avatar: response.user.profilePicture,
                status: 'ACTIVE',
                createdAt: new Date().toISOString(),
              };
              this.setUser(adminUser);
              return of(response);
            })
          );
        }),
      );
  }

  logout(): void {
    // Fire-and-forget server-side logout
    this.apiService.post('auth/logout', {}).pipe(catchError(() => of(null))).subscribe();
    this.clearStorage();
    this.currentUserSignal.set(null);
    this.router.navigate(['/auth/login']);
  }

  forgotPassword(data: ForgotPasswordRequest): Observable<any> {
    return this.apiService.post('auth/forgot-password', data);
  }

  resetPassword(data: ResetPasswordRequest): Observable<any> {
    return this.apiService.post('auth/reset-password', data);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<any> {
    const email = this.currentUserSignal()?.email || '';
    return this.http.post(`${environment.apiUrl}/auth/change-password`, {
      currentPassword,
      newPassword
    }, {
      headers: {
        'X-User-Email': email
      }
    });
  }

  /**
   * GET /api/v1/auth/profile - returns current user profile (firstName, lastName, phoneNumber, etc.)
   */
  getProfile(): Observable<ProfileResponse> {
    return this.apiService.get<ProfileResponse>('auth/profile');
  }

  /**
   * PUT /api/v1/auth/profile - update firstName, lastName, phoneNumber
   */
  updateProfile(firstName: string, lastName: string, phoneNumber?: string): Observable<ProfileResponse> {
    return this.apiService.put<ProfileResponse>('auth/profile', {
      firstName,
      lastName,
      phoneNumber: phoneNumber ?? ''
    });
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    return this.apiService
      .post<LoginResponse>('auth/refresh', { refreshToken })
      .pipe(
        tap((response) => {
          this.setToken(response.access_token);
          this.setRefreshToken(response.refresh_token);
        }),
      );
  }

  // ---------------------------------------------------------------------------
  // Token management
  // ---------------------------------------------------------------------------
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    this.storage.setItem(TOKEN_KEY, token);
    console.log('[AuthService] Token set in storage. Key:', TOKEN_KEY, 'Storage type:', this.storage === localStorage ? 'localStorage' : 'sessionStorage');
  }

  private getRefreshToken(): string | null {
    return (
      localStorage.getItem(REFRESH_TOKEN_KEY) ||
      sessionStorage.getItem(REFRESH_TOKEN_KEY)
    );
  }

  private setRefreshToken(token: string): void {
    this.storage.setItem(REFRESH_TOKEN_KEY, token);
  }

  setUser(user: AdminUser): void {
    this.storage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  // ---------------------------------------------------------------------------
  // RBAC helpers
  // ---------------------------------------------------------------------------
  getUserRole(): AdminRole | null {
    return this.currentUserSignal()?.role || null;
  }

  getUserPermissions(): string[] {
    return this.currentUserSignal()?.permissions || [];
  }

  hasPermission(permission: string): boolean {
    const role = this.getUserRole();
    if (role === 'SUPER_ADMIN') return true; // super admin bypasses all
    return this.getUserPermissions().includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some((p) => this.hasPermission(p));
  }

  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every((p) => this.hasPermission(p));
  }

  hasRole(role: AdminRole): boolean {
    return this.getUserRole() === role;
  }

  hasAnyRole(roles: AdminRole[]): boolean {
    const current = this.getUserRole();
    return current ? roles.includes(current) : false;
  }

  /**
   * Get the permissions granted by the user's role (static mapping)
   */
  getRolePermissions(): Permission[] {
    const role = this.getUserRole();
    return role ? ROLE_PERMISSIONS[role] : [];
  }
}
