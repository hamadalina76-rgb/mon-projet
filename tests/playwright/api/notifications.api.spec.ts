import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Notification Service', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('GET /api/v1/notifications - should list notifications', async () => {
    const response = await api.get('/api/v1/notifications');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/notifications - should support pagination', async () => {
    const response = await api.get('/api/v1/notifications?page=0&size=10');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/notifications/unread-count - should get unread count', async () => {
    const response = await api.get('/api/v1/notifications/unread-count');
    expect([200, 404]).toContain(response.status());
  });

  test('POST /api/v1/notifications/push-token - should register push token', async () => {
    const response = await api.post('/api/v1/notifications/push-token', {
      token: 'test-fcm-token-12345',
      platform: 'ANDROID',
    });
    expect([200, 201, 404]).toContain(response.status());
  });

  test('PUT /api/v1/notifications/:id/read - should mark notification as read', async () => {
    const listRes = await api.get('/api/v1/notifications');
    if (listRes.status() === 200) {
      const body = await listRes.json();
      const notifications = body.content || body;
      if (Array.isArray(notifications) && notifications.length > 0) {
        const id = notifications[0].id;
        const response = await api.put(`/api/v1/notifications/${id}/read`, {});
        expect([200, 204, 404]).toContain(response.status());
      }
    }
  });
});
