// src/app/core/services/auth.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { 
  User, LoginRequest, AuthResponse, OtpResponse, 
  RegisterRequest, RegisterResponse, VerifyOtpRequest 
} from '@core/models/user.model';

const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'user_data';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUserSignal = signal<User | null>(null);
  
  currentUser = this.currentUserSignal.asReadonly();
  isLoggedIn = computed(() => !!this.currentUserSignal());

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const userData = localStorage.getItem(USER_KEY);
    if (userData) {
      try {
        this.currentUserSignal.set(JSON.parse(userData));
      } catch {
        this.logout();
      }
    }
  }

  // ==================== PHASE 1: REGISTRATION ====================
  
  /**
   * Register a new partner account
   * POST /api/v1/auth/register
   * Only creates auth account (email, password, name, phone, role=PARTNER)
   */
  register(data: RegisterRequest): Observable<RegisterResponse> {
    return this.apiService.post<RegisterResponse>('v1/auth/register', data);
  }

  /**
   * Check if email already exists
   * GET /api/v1/auth/check-email?email=xxx
   */
  checkEmail(email: string): Observable<{ exists: boolean }> {
    return this.apiService.get<{ exists: boolean }>('v1/auth/check-email', { email });
  }

  // ==================== PHASE 2: LOGIN (OTP FLOW) ====================

  /**
   * Step 1: Send login request → backend returns either:
   *   - AuthResponse (JWT tokens) if email already verified
   *   - OtpResponse (OTP sent) if email not yet verified (first login)
   * POST /api/v1/auth/login
   */
  login(credentials: LoginRequest): Observable<OtpResponse | AuthResponse> {
    return this.apiService.post<OtpResponse | AuthResponse>('v1/auth/login', credentials).pipe(
      tap((response: any) => {
        // If backend returned tokens directly (email already verified), save them
        if (response.access_token) {
          this.setToken(response.access_token);
          this.setRefreshToken(response.refresh_token);
          if (response.user) {
            this.setUser(response.user);
          }
        }
      })
    );
  }

  /**
   * Step 2: Verify OTP → get JWT tokens
   * POST /api/v1/auth/verify-otp
   */
  verifyOtp(data: VerifyOtpRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('v1/auth/verify-otp', data).pipe(
      tap((response) => {
        this.setToken(response.access_token);
        this.setRefreshToken(response.refresh_token);
        this.setUser(response.user);
      })
    );
  }

  /**
   * Resend OTP
   * POST /api/v1/auth/resend-otp
   */
  resendOtp(email: string): Observable<OtpResponse> {
    return this.apiService.post<OtpResponse>('v1/auth/resend-otp', { email });
  }

  // ==================== FORGOT / RESET PASSWORD ====================

  /**
   * Request password reset code (sent by email)
   * POST /api/v1/auth/forgot-password
   */
  requestPasswordReset(email: string): Observable<{ message?: string; email?: string }> {
    return this.apiService.post<{ message?: string; email?: string }>('v1/auth/forgot-password', { email });
  }

  /**
   * Reset password with OTP code received by email
   * POST /api/v1/auth/reset-password
   */
  resetPassword(data: { email: string; otpCode: string; newPassword: string }): Observable<{ message?: string }> {
    return this.apiService.post<{ message?: string }>('v1/auth/reset-password', data);
  }

  // ==================== SESSION MANAGEMENT ====================

  logout(): void {
    const token = this.getToken();
    if (token) {
      // Notify backend (fire and forget)
      this.apiService.post('v1/auth/logout', {}).subscribe({ error: () => {} });
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSignal.set(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  }

  setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  /**
   * Update the stored user (e.g., after profile completion)
   */
  updateStoredUser(updates: Partial<User>): void {
    const current = this.currentUserSignal();
    if (current) {
      const updated = { ...current, ...updates };
      this.setUser(updated);
    }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    // Check token expiration
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  /**
   * Check if user needs to complete partner profile
   */
  isProfileComplete(): boolean {
    const user = this.currentUserSignal();
    return !!user?.partnerId;
  }

  getUserRole(): string | null {
    const user = this.currentUserSignal();
    return user?.role || null;
  }

  getPartnerId(): number | null {
    const user = this.currentUserSignal();
    return user?.partnerId || null;
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    return this.apiService.post<AuthResponse>('v1/auth/refresh', { refreshToken }).pipe(
      tap((response) => {
        this.setToken(response.access_token);
        this.setRefreshToken(response.refresh_token);
      })
    );
  }
}
