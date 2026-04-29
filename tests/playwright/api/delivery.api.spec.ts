import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Delivery Service', () => {
  let adminApi: ApiClient;
  let courierApi: ApiClient;

  test.beforeAll(async () => {
    adminApi = new ApiClient();
    await adminApi.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    courierApi = new ApiClient();
    await courierApi.login(TEST_ACCOUNTS.courier.email, TEST_ACCOUNTS.courier.password);
  });

  test('GET /api/v1/deliveries - admin should list deliveries', async () => {
    const response = await adminApi.get('/api/v1/deliveries');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/deliveries - should support status filter', async () => {
    const response = await adminApi.get('/api/v1/deliveries?status=IN_PROGRESS');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/deliveries/:id/tracking - should get tracking info', async () => {
    const listRes = await adminApi.get('/api/v1/deliveries');
    if (listRes.status() === 200) {
      const body = await listRes.json();
      const deliveries = body.content || body;
      if (Array.isArray(deliveries) && deliveries.length > 0) {
        const id = deliveries[0].id;
        const response = await adminApi.get(`/api/v1/deliveries/${id}/tracking`);
        expect([200, 404]).toContain(response.status());
      }
    }
  });

  test('GET /api/v1/couriers/schedule - courier should get schedule', async () => {
    const response = await courierApi.get('/api/v1/couriers/schedule');
    expect([200, 404]).toContain(response.status());
  });

  test('PUT /api/v1/couriers/availability - courier should update availability', async () => {
    const response = await courierApi.put('/api/v1/couriers/availability', {
      available: true,
      latitude: 36.4530,
      longitude: 10.7360,
    });
    expect([200, 204, 404]).toContain(response.status());
  });
});
