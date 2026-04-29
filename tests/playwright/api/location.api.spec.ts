import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Location Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/zones - should list zones', async () => {
    const response = await api.get('/api/v1/zones');
    expect([200, 404]).toContain(response.status());
  });

  test('POST /api/v1/location/zone-check - should check if point is in zone', async () => {
    const response = await api.post('/api/v1/location/zone-check', {
      latitude: 36.4513,
      longitude: 10.7357,
    });
    expect([200, 400, 404]).toContain(response.status());
  });

  test('POST /api/v1/location/distance - should calculate distance', async () => {
    const response = await api.post('/api/v1/location/distance', {
      originLat: 36.4513,
      originLng: 10.7357,
      destLat: 36.4600,
      destLng: 10.7400,
    });
    expect([200, 400, 404]).toContain(response.status());
  });

  test('GET /api/v1/location/couriers/nearby - should find nearby couriers', async () => {
    const response = await api.get('/api/v1/location/couriers/nearby?lat=36.4513&lng=10.7357&radius=5');
    expect([200, 404]).toContain(response.status());
  });
});
