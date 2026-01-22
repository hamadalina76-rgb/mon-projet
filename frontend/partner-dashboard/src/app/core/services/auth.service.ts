// src/app/core/services/auth.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { User, LoginRequest, LoginResponse, RegisterRequest } from '@core/models/user.model';

const TOKEN_KEY = 'auth_token';
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

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.apiService.post<LoginResponse>('auth/partner/login', credentials).pipe(
      tap((response) => {
        this.setToken(response.token);
        this.setUser(response.user);
      })
    );
  }

  register(data: RegisterRequest): Observable<LoginResponse> {
    return this.apiService.post<LoginResponse>('auth/partner/register', data);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
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

  setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
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

  getUserRole(): string | null {
    const user = this.currentUserSignal();
    return user?.role || null;
  }

  getPartnerId(): string | null {
    const user = this.currentUserSignal();
    return user?.partnerId || null;
  }

  refreshToken(): Observable<LoginResponse> {
    return this.apiService.post<LoginResponse>('auth/refresh', {}).pipe(
      tap((response) => {
        this.setToken(response.token);
      })
    );
  }
}
