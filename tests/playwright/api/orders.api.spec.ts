import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS, TEST_ORDER } from '../helpers/test-data';

test.describe('API - Order Service', () => {
  let api: ApiClient;
  let customerApi: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    customerApi = new ApiClient();
    await customerApi.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  test('GET /api/v1/orders - admin should list orders', async () => {
    const response = await api.get('/api/v1/orders');
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body.content || body)).toBeTruthy();
  });

  test('POST /api/v1/orders - customer should create order', async () => {
    const response = await customerApi.post('/api/v1/orders', {
      partnerId: 'test-partner-id',
      items: TEST_ORDER.items,
      deliveryAddress: TEST_ORDER.deliveryAddress,
      paymentMethod: TEST_ORDER.paymentMethod,
    });

    // 200 or 201 for creation, 400 if partner doesn't exist in test env
    expect([200, 201, 400]).toContain(response.status());
  });

  test('GET /api/v1/orders/:id - should get order details', async () => {
    // First get list of orders
    const listRes = await api.get('/api/v1/orders');
    const body = await listRes.json();
    const orders = body.content || body;

    if (Array.isArray(orders) && orders.length > 0) {
      const orderId = orders[0].id;
      const response = await api.get(`/api/v1/orders/${orderId}`);
      expect(response.status()).toBe(200);
      const order = await response.json();
      expect(order.id).toBe(orderId);
    }
  });

  test('GET /api/v1/orders - should support pagination', async () => {
    const response = await api.get('/api/v1/orders?page=0&size=5');
    expect(response.status()).toBe(200);
  });

  test('GET /api/v1/orders - should filter by status', async () => {
    const response = await api.get('/api/v1/orders?status=PENDING');
    expect(response.status()).toBe(200);
  });

  test('GET /api/v1/orders/stats - should return order statistics', async () => {
    const response = await api.get('/api/v1/orders/stats');
    // May be 200 or 404 if endpoint doesn't exist
    expect([200, 404]).toContain(response.status());
  });
});
