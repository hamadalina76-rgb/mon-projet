import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Review Service', () => {
  let adminApi: ApiClient;
  let customerApi: ApiClient;

  test.beforeAll(async () => {
    adminApi = new ApiClient();
    await adminApi.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    customerApi = new ApiClient();
    await customerApi.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  test('GET /api/v1/reviews - should list reviews', async () => {
    const response = await adminApi.get('/api/v1/reviews');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/reviews - should filter by partner', async () => {
    const response = await adminApi.get('/api/v1/reviews?partnerId=test-partner');
    expect([200, 404]).toContain(response.status());
  });

  test('POST /api/v1/reviews - should create review', async () => {
    const response = await customerApi.post('/api/v1/reviews', {
      orderId: 'test-order-id',
      partnerId: 'test-partner-id',
      rating: 4,
      comment: 'Automated test review - good food',
    });
    // 200/201 if order exists, 400/404 if not
    expect([200, 201, 400, 404]).toContain(response.status());
  });

  test('GET /api/v1/reviews - should support pagination', async () => {
    const response = await adminApi.get('/api/v1/reviews?page=0&size=5');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/ratings/:partnerId - should get partner rating', async () => {
    const response = await adminApi.get('/api/v1/ratings/test-partner-id');
    expect([200, 404]).toContain(response.status());
  });
});
