import { test, expect } from '@playwright/test';
import { TEST_ACCOUNTS } from '../helpers/test-data';

const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';

test.describe('API - Auth Service', () => {
  test('POST /api/v1/auth/login - should login with valid credentials', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.access_token || body.accessToken || body.token).toBeTruthy();
  });

  test('POST /api/v1/auth/login - should reject invalid credentials', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: 'nonexistent@test.com',
        password: 'WrongPassword123!',
      },
    });

    expect(response.status()).toBeGreaterThanOrEqual(400);
    expect(response.status()).toBeLessThan(500);
  });

  test('POST /api/v1/auth/login - should reject empty email', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: '',
        password: 'SomePassword123!',
      },
    });

    expect(response.status()).toBeGreaterThanOrEqual(400);
  });

  test('POST /api/v1/auth/login - should reject empty password', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: '',
      },
    });

    expect(response.status()).toBeGreaterThanOrEqual(400);
  });

  test('GET /api/v1/auth/profile - should return user info with valid token', async ({ request }) => {
    // Login first
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const { access_token, accessToken, token } = await loginRes.json();
    const authToken = access_token || accessToken || token;

    // Get user info
    const response = await request.get(`${API_BASE}/api/v1/auth/profile`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.email).toBe(TEST_ACCOUNTS.admin.email);
  });

  test('GET /api/v1/auth/profile - should reject invalid token', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v1/auth/profile`, {
      headers: { Authorization: 'Bearer invalid-token' },
    });

    expect(response.status()).toBeGreaterThanOrEqual(401);
  });

  test('POST /api/v1/auth/refresh - should refresh valid token', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const body = await loginRes.json();
    const refreshToken = body.refresh_token || body.refreshToken;

    if (refreshToken) {
      const response = await request.post(`${API_BASE}/api/v1/auth/refresh`, {
        data: { refreshToken },
      });
      expect(response.status()).toBe(200);
    }
  });
});
