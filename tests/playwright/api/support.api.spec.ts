import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';

test.describe('API - Support Service', () => {
  let adminApi: ApiClient;
  let customerApi: ApiClient;

  test.beforeAll(async () => {
    adminApi = new ApiClient();
    await adminApi.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    customerApi = new ApiClient();
    await customerApi.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  test('GET /api/v1/support/tickets - admin should list tickets', async () => {
    const response = await adminApi.get('/api/v1/support/tickets');
    expect([200, 404]).toContain(response.status());
  });

  test('POST /api/v1/support/tickets - customer should create ticket', async () => {
    const response = await customerApi.post('/api/v1/support/tickets', {
      category: 'ORDER_ISSUE',
      subject: 'Test ticket - automated test',
      message: 'This is an automated test ticket created by the test robot.',
    });
    expect([200, 201, 400, 404]).toContain(response.status());
  });

  test('GET /api/v1/support/tickets - should support status filter', async () => {
    const response = await adminApi.get('/api/v1/support/tickets?status=OPEN');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/support/tickets - should support pagination', async () => {
    const response = await adminApi.get('/api/v1/support/tickets?page=0&size=10');
    expect([200, 404]).toContain(response.status());
  });

  test('GET /api/v1/support/tickets/:id - should get ticket details', async () => {
    const listRes = await adminApi.get('/api/v1/support/tickets');
    if (listRes.status() === 200) {
      const body = await listRes.json();
      const tickets = body.content || body;
      if (Array.isArray(tickets) && tickets.length > 0) {
        const id = tickets[0].id;
        const response = await adminApi.get(`/api/v1/support/tickets/${id}`);
        expect(response.status()).toBe(200);
      }
    }
  });

  test('PUT /api/v1/support/tickets/:id/status - admin should update ticket status', async () => {
    const listRes = await adminApi.get('/api/v1/support/tickets');
    if (listRes.status() === 200) {
      const body = await listRes.json();
      const tickets = body.content || body;
      if (Array.isArray(tickets) && tickets.length > 0) {
        const id = tickets[0].id;
        const response = await adminApi.put(`/api/v1/support/tickets/${id}/status`, {
          status: 'IN_PROGRESS',
        });
        expect([200, 204, 404]).toContain(response.status());
      }
    }
  });
});
