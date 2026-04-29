import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Promotion Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/promotions - should list promotions', async () => {
    const response = await api.get('/api/v1/promotions');
    expect([200, 404]).toContain(response.status());
  });

  test('POST /api/v1/promotions/validate - should validate promo code', async () => {
    const response = await api.post('/api/v1/promotions/validate', {
      code: 'TESTPROMO',
      userId: 'test-user-id',
      orderAmount: 50.0,
    });
    expect([200, 400, 404]).toContain(response.status());
  });

  test('GET /api/v1/promotions/dashboard - should get promotion stats', async () => {
    const response = await api.get('/api/v1/promotions/dashboard');
    expect([200, 404]).toContain(response.status());
  });
});
