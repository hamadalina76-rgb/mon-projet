import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS, TEST_PARTNER } from '../helpers/test-data';

test.describe('API - Partner Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/partners - should list partners', async () => {
    const response = await api.get('/api/v1/partners');
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body.content || body)).toBeTruthy();
  });

  test('GET /api/v1/partners - should support pagination', async () => {
    const response = await api.get('/api/v1/partners?page=0&size=5');
    expect(response.status()).toBe(200);
  });

  test('GET /api/v1/partners/:id - should get partner details', async () => {
    const listRes = await api.get('/api/v1/partners');
    const body = await listRes.json();
    const partners = body.content || body;

    if (Array.isArray(partners) && partners.length > 0) {
      const id = partners[0].id;
      const response = await api.get(`/api/v1/partners/${id}`);
      expect(response.status()).toBe(200);
    }
  });

  test('GET /api/v1/partners/:id/menu - should get partner menu', async () => {
    const listRes = await api.get('/api/v1/partners');
    const body = await listRes.json();
    const partners = body.content || body;

    if (Array.isArray(partners) && partners.length > 0) {
      const id = partners[0].id;
      const response = await api.get(`/api/v1/partners/${id}/menu`);
      expect([200, 404]).toContain(response.status());
    }
  });

  test('GET /api/v1/partners/nearby - should find nearby partners', async () => {
    const response = await api.get('/api/v1/partners/nearby?lat=36.4513&lng=10.7357&radius=10');
    expect([200, 404]).toContain(response.status());
  });
});
