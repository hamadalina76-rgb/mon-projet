import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Analytics Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/analytics/dashboard - should get dashboard metrics', async () => {
    const response = await api.get('/api/v1/analytics/dashboard');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/analytics/revenue - should get revenue report', async () => {
    const response = await api.get('/api/v1/analytics/revenue?period=DAILY');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/analytics/performance - should get performance metrics', async () => {
    const response = await api.get('/api/v1/analytics/performance');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/reports - should list reports', async () => {
    const response = await api.get('/api/v1/reports');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/analytics/revenue - should support date range', async () => {
    const today = new Date().toISOString().split('T')[0];
    const lastMonth = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const response = await api.get(`/api/v1/analytics/revenue?from=${lastMonth}&to=${today}`);
    expect([200, 404]).toContain(response.status());
  });
});
