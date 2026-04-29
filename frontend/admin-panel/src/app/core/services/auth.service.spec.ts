import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ApiService } from './api.service';
import { of, throwError } from 'rxjs';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;
  let router: Router;

  const mockLoginResponse = {
    access_token: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo5OTk5OTk5OTk5fQ.test',
    refresh_token: 'mock-refresh-token',
    user: {
      id: '1',
      email: 'admin@speedline-test.com',
      firstName: 'Test',
      lastName: 'Admin',
      role: 'SUPER_ADMIN',
      profilePicture: null,
    },
  };

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj('ApiService', ['get', 'post', 'put']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        AuthService,
        { provide: ApiService, useValue: apiServiceSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    // Clear storage before each test
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should store tokens and user on successful login', (done) => {
      apiServiceSpy.post.and.returnValue(of(mockLoginResponse));

      service.login({ email: 'admin@test.com', password: 'pass', rememberMe: false }).subscribe({
        next: () => {
          expect(service.isLoggedIn()).toBeTrue();
          expect(service.currentUser()?.email).toBe('admin@speedline-test.com');
          done();
        },
      });

      // Handle the admin profile fetch
      const req = httpMock.expectOne((r) => r.url.includes('/admins/by-user/'));
      req.flush({ customPermissions: ['dashboard', 'users'] });
    });

    it('should reject non-admin roles', (done) => {
      const customerResponse = {
        ...mockLoginResponse,
        user: { ...mockLoginResponse.user, role: 'CUSTOMER' },
      };
      apiServiceSpy.post.and.returnValue(of(customerResponse));

      service.login({ email: 'customer@test.com', password: 'pass', rememberMe: false }).subscribe({
        error: (err) => {
          expect(err.message).toBe('ACCESS_DENIED');
          done();
        },
      });
    });

    it('should use localStorage when rememberMe is true', (done) => {
      apiServiceSpy.post.and.returnValue(of(mockLoginResponse));

      service.login({ email: 'admin@test.com', password: 'pass', rememberMe: true }).subscribe({
        next: () => {
          expect(localStorage.getItem('remember_me')).toBe('true');
          done();
        },
      });

      const req = httpMock.expectOne((r) => r.url.includes('/admins/by-user/'));
      req.flush({ customPermissions: [] });
    });
  });

  describe('logout', () => {
    it('should clear tokens and user', () => {
      apiServiceSpy.post.and.returnValue(of(null));
      spyOn(router, 'navigate');

      sessionStorage.setItem('access_token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ email: 'test@test.com', role: 'SUPER_ADMIN' }));

      service.logout();

      expect(service.isLoggedIn()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
    });
  });

  describe('isAuthenticated', () => {
    it('should return false when no token', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should return false for expired token', () => {
      // Token with exp = 1 (1970)
      const expiredToken = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.test';
      sessionStorage.setItem('access_token', expiredToken);
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should return true for valid non-expired token', () => {
      // Token with exp far in the future
      const payload = btoa(JSON.stringify({ sub: 'test', exp: 9999999999 }));
      const validToken = `eyJhbGciOiJIUzI1NiJ9.${payload}.test`;
      sessionStorage.setItem('access_token', validToken);
      expect(service.isAuthenticated()).toBeTrue();
    });
  });

  describe('RBAC', () => {
    beforeEach(() => {
      service.setUser({
        id: '1',
        email: 'admin@test.com',
        firstName: 'Test',
        lastName: 'Admin',
        role: 'SUPER_ADMIN',
        permissions: ['dashboard:view', 'users:view', 'users:create'],
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      });
    });

    it('should return user role', () => {
      expect(service.getUserRole()).toBe('SUPER_ADMIN');
    });

    it('should return user permissions', () => {
      expect(service.getUserPermissions()).toContain('dashboard:view');
    });

    it('should SUPER_ADMIN bypass all permission checks', () => {
      expect(service.hasPermission('anything:random')).toBeTrue();
    });

    it('should check hasAnyPermission', () => {
      expect(service.hasAnyPermission(['dashboard:view', 'nonexistent:perm'])).toBeTrue();
    });

    it('should check hasAllPermissions', () => {
      expect(service.hasAllPermissions(['dashboard:view', 'users:view'])).toBeTrue();
    });

    it('should check hasRole', () => {
      expect(service.hasRole('SUPER_ADMIN')).toBeTrue();
      expect(service.hasRole('ADMIN' as any)).toBeFalse();
    });

    it('should check hasAnyRole', () => {
      expect(service.hasAnyRole(['SUPER_ADMIN', 'ADMIN' as any])).toBeTrue();
    });
  });

  describe('token management', () => {
    it('should get and set token', () => {
      service.setToken('my-token');
      expect(service.getToken()).toBe('my-token');
    });
  });
});
