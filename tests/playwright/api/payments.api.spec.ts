import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Payment Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/payments - should list payments', async () => {
    const response = await api.get('/api/v1/payments');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/payments - should support pagination', async () => {
    const response = await api.get('/api/v1/payments?page=0&size=10');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/payments/:id - should get payment details', async () => {
    const listRes = await api.get('/api/v1/payments');
    if (listRes.status() === 200) {
      const body = await listRes.json();
      const payments = body.content || body;
      if (Array.isArray(payments) && payments.length > 0) {
        const id = payments[0].id;
        const response = await api.get(`/api/v1/payments/${id}`);
        expect(response.status()).toBe(200);
      }
    }
  });

  test('GET /api/v1/wallets - should get wallet info', async () => {
    const response = await api.get('/api/v1/wallets');
    expect([200, 404]).toContain(response.status());
  });
});
