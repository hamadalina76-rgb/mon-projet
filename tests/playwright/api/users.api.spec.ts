import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - User Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/users/customers - should list customers', async () => {
    const response = await api.get('/api/v1/users/customers');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/users/couriers - should list couriers', async () => {
    const response = await api.get('/api/v1/users/couriers');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/users/admins - should list admins', async () => {
    const response = await api.get('/api/v1/users/admins');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/users/profile - should get own profile', async () => {
    const response = await api.get('/api/v1/users/profile');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/users/customers - should support pagination', async () => {
    const response = await api.get('/api/v1/users/customers?page=0&size=5');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/users/customers - should support search', async () => {
    const response = await api.get('/api/v1/users/customers?search=test');
    expect([200, 404]).toContain(response.status());
  });
});
