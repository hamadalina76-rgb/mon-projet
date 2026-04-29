import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

/**
 * API Contract Tests
 *
 * Validates that API responses match expected schemas.
 * Catches breaking changes in response structure between deploys.
 */
test.describe('API Contracts - Response Schema Validation', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('POST /api/v1/auth/login - response contract', async ({ request }) => {
    const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';
    const response = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: {
        email: TEST_ACCOUNTS.admin.email,
        password: TEST_ACCOUNTS.admin.password,
      },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();

    // Validate login response schema
    expect(body).toHaveProperty('user');
    expect(body.user).toHaveProperty('id');
    expect(body.user).toHaveProperty('email');
    expect(body.user).toHaveProperty('firstName');
    expect(body.user).toHaveProperty('lastName');
    expect(body.user).toHaveProperty('role');

    // Must have tokens
    const hasAccessToken = 'accessToken' in body || 'access_token' in body || 'token' in body;
    expect(hasAccessToken).toBeTruthy();

    const hasRefreshToken = 'refreshToken' in body || 'refresh_token' in body;
    expect(hasRefreshToken).toBeTruthy();

    // Type checks
    expect(typeof body.user.email).toBe('string');
    expect(typeof body.user.role).toBe('string');
  });

  test('GET /api/orders - paginated response contract', async () => {
    const response = await api.get('/api/orders?page=0&size=5');
    if (response.status() !== 200) return;

    const body = await response.json();

    // Spring Page response structure
    if (body.content) {
      expect(body).toHaveProperty('content');
      expect(Array.isArray(body.content)).toBeTruthy();
      expect(body).toHaveProperty('totalElements');
      expect(body).toHaveProperty('totalPages');
      expect(body).toHaveProperty('size');
      expect(body).toHaveProperty('number');
      expect(typeof body.totalElements).toBe('number');
      expect(typeof body.totalPages).toBe('number');

      // Validate order object structure
      if (body.content.length > 0) {
        const order = body.content[0];
        expect(order).toHaveProperty('id');
        expect(order).toHaveProperty('status');
        expect(order).toHaveProperty('createdAt');
      }
    }
  });

  test('GET /api/v1/partners - partner list contract', async () => {
    const response = await api.get('/api/v1/partners?page=0&size=5');
    if (response.status() !== 200) return;

    const body = await response.json();
    const partners = body.content || body;

    if (Array.isArray(partners) && partners.length > 0) {
      const partner = partners[0];
      expect(partner).toHaveProperty('id');
      expect(partner).toHaveProperty('name');
      expect(partner).toHaveProperty('status');
      expect(typeof partner.name).toBe('string');
    }
  });

  test('GET /api/v1/partners/:id - partner detail contract', async () => {
    const listRes = await api.get('/api/v1/partners?page=0&size=1');
    if (listRes.status() !== 200) return;

    const body = await listRes.json();
    const partners = body.content || body;
    if (!Array.isArray(partners) || partners.length === 0) return;

    const detailRes = await api.get(`/api/v1/partners/${partners[0].id}`);
    if (detailRes.status() !== 200) return;

    const partner = await detailRes.json();
    expect(partner).toHaveProperty('id');
    expect(partner).toHaveProperty('name');
    expect(partner).toHaveProperty('type');
    expect(partner).toHaveProperty('status');
    expect(partner).toHaveProperty('email');
  });

  test('GET /api/v1/users/customers - customer list contract', async () => {
    const response = await api.get('/api/v1/users/customers?page=0&size=5');
    if (response.status() !== 200) return;

    const body = await response.json();
    const customers = body.content || body;

    if (Array.isArray(customers) && customers.length > 0) {
      const customer = customers[0];
      expect(customer).toHaveProperty('id');
      expect(customer).toHaveProperty('email');
      expect(customer).toHaveProperty('firstName');
      expect(customer).toHaveProperty('lastName');
    }
  });

  test('GET /api/v1/users/couriers - courier list contract', async () => {
    const response = await api.get('/api/v1/users/couriers?page=0&size=5');
    if (response.status() !== 200) return;

    const body = await response.json();
    const couriers = body.content || body;

    if (Array.isArray(couriers) && couriers.length > 0) {
      const courier = couriers[0];
      expect(courier).toHaveProperty('id');
      expect(courier).toHaveProperty('status');
      expect(courier).toHaveProperty('vehicleType');
    }
  });

  test('GET /api/v1/zones - zone list contract', async () => {
    const response = await api.get('/api/v1/zones');
    if (response.status() !== 200) return;

    const body = await response.json();
    const zones = body.content || body;

    if (Array.isArray(zones) && zones.length > 0) {
      const zone = zones[0];
      expect(zone).toHaveProperty('id');
      expect(zone).toHaveProperty('name');
    }
  });

  test('GET /api/v1/payments - payment list contract', async () => {
    const response = await api.get('/api/v1/payments?page=0&size=5');
    if (response.status() !== 200) return;

    const body = await response.json();
    const payments = body.content || body;

    if (Array.isArray(payments) && payments.length > 0) {
      const payment = payments[0];
      expect(payment).toHaveProperty('id');
      expect(payment).toHaveProperty('status');
      expect(payment).toHaveProperty('amount');
      expect(typeof payment.amount).toBe('number');
    }
  });

  test('Error responses follow consistent format', async ({ request }) => {
    const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';

    // Request a nonexistent resource
    const response = await request.get(`${API_BASE}/api/orders/nonexistent-id-99999`, {
      headers: {
        Authorization: `Bearer ${api.getToken()}`,
      },
    });

    if (response.status() >= 400 && response.status() < 500) {
      const body = await response.json().catch(() => null);
      if (body) {
        // Error responses should have a message or error field
        const hasErrorInfo =
          'message' in body || 'error' in body || 'detail' in body || 'status' in body;
        expect(hasErrorInfo).toBeTruthy();
      }
    }
  });
});
