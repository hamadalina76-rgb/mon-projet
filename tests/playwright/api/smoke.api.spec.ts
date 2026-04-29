import { test, expect } from '@playwright/test';
import { TEST_ACCOUNTS } from '../helpers/test-data';

const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';

/**
 * Smoke Tests - Quick Health Check
 *
 * Run these FIRST before the full suite to verify the environment is up.
 * If smoke tests fail, skip the rest.
 *
 * Usage: npx playwright test api/smoke.api.spec.ts
 */
test.describe('Smoke Tests - Environment Health', () => {
  test.describe.configure({ mode: 'serial' });

  test('API Gateway is reachable', async ({ request }) => {
    const response = await request.get(`${API_BASE}/actuator/health`, {
      timeout: 10_000,
    });
    expect([200, 404]).toContain(response.status());
  });

  test('Auth service responds', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: { email: 'smoke@test.com', password: 'smoke' },
      timeout: 10_000,
    });
    // Should get 400/401, NOT 500/502/503
    expect(response.status()).toBeLessThan(500);
  });

  test('Admin can login', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
      timeout: 10_000,
    });
    expect(response.status()).toBe(200);

    const body = await response.json();
    const token = body.accessToken || body.access_token || body.token;
    expect(token).toBeTruthy();
  });

  test('Order service responds', async ({ request }) => {
    // Login first
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const { access_token, accessToken, token } = await loginRes.json();
    const authToken = accessToken || access_token || token;

    const response = await request.get(`${API_BASE}/api/v1/orders?page=0&size=1`, {
      headers: { Authorization: `Bearer ${authToken}` },
      timeout: 10_000,
    });
    expect(response.status()).toBeLessThan(500);
  });

  test('Partner service responds', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const body = await loginRes.json();
    const authToken = body.accessToken || body.access_token || body.token;

    const response = await request.get(`${API_BASE}/api/v1/partners?page=0&size=1`, {
      headers: { Authorization: `Bearer ${authToken}` },
      timeout: 10_000,
    });
    expect(response.status()).toBeLessThan(500);
  });

  test('Delivery service responds', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const body = await loginRes.json();
    const authToken = body.accessToken || body.access_token || body.token;

    const response = await request.get(`${API_BASE}/api/v1/deliveries`, {
      headers: { Authorization: `Bearer ${authToken}` },
      timeout: 10_000,
    });
    expect(response.status()).toBeLessThan(500);
  });

  test('Payment service responds', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const body = await loginRes.json();
    const authToken = body.accessToken || body.access_token || body.token;

    const response = await request.get(`${API_BASE}/api/v1/payments`, {
      headers: { Authorization: `Bearer ${authToken}` },
      timeout: 10_000,
    });
    expect(response.status()).toBeLessThan(500);
  });

  test('Location service responds', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });
    const body = await loginRes.json();
    const authToken = body.accessToken || body.access_token || body.token;

    const response = await request.get(`${API_BASE}/api/v1/zones`, {
      headers: { Authorization: `Bearer ${authToken}` },
      timeout: 10_000,
    });
    expect(response.status()).toBeLessThan(500);
  });
});
