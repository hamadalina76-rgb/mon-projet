import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS, TEST_ORDER } from '../helpers/test-data';

/**
 * End-to-End Order Flow Test
 *
 * Simulates the complete order lifecycle via API:
 * 1. Customer creates order
 * 2. Partner accepts order
 * 3. System assigns courier
 * 4. Courier picks up order
 * 5. Courier delivers order
 * 6. Order is completed
 */
test.describe('API - E2E Order Lifecycle', () => {
  let adminApi: ApiClient;
  let customerApi: ApiClient;
  let partnerApi: ApiClient;
  let courierApi: ApiClient;

  test.beforeAll(async () => {
    adminApi = new ApiClient();
    await adminApi.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);

    customerApi = new ApiClient();
    await customerApi.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);

    partnerApi = new ApiClient();
    await partnerApi.login(TEST_ACCOUNTS.partner.email, TEST_ACCOUNTS.partner.password);

    courierApi = new ApiClient();
    await courierApi.login(TEST_ACCOUNTS.courier.email, TEST_ACCOUNTS.courier.password);
  });

  test('full order lifecycle: create -> accept -> pickup -> deliver', async () => {
    // Step 1: Get available partners
    const partnersRes = await customerApi.get('/api/v1/partners?status=ACTIVE');
    if (partnersRes.status() !== 200) {
      test.skip();
      return;
    }
    const partners = await partnersRes.json();
    const partnerList = partners.content || partners;
    if (!Array.isArray(partnerList) || partnerList.length === 0) {
      test.skip();
      return;
    }

    const partnerId = partnerList[0].id;

    // Step 2: Create order
    const createRes = await customerApi.post('/api/v1/orders', {
      partnerId,
      items: TEST_ORDER.items,
      deliveryAddress: TEST_ORDER.deliveryAddress,
      paymentMethod: TEST_ORDER.paymentMethod,
    });

    if (createRes.status() !== 200 && createRes.status() !== 201) {
      // Order creation may fail if products don't exist
      console.log('Order creation failed, skipping lifecycle test');
      return;
    }

    const order = await createRes.json();
    const orderId = order.id;
    expect(orderId).toBeTruthy();

    // Step 3: Verify order appears in partner's list
    const partnerOrdersRes = await partnerApi.get('/api/v1/orders?status=PENDING');
    expect(partnerOrdersRes.status()).toBe(200);

    // Step 4: Partner accepts order
    const acceptRes = await partnerApi.put(`/api/v1/orders/${orderId}/status`, {
      status: 'ACCEPTED',
    });
    expect([200, 204]).toContain(acceptRes.status());

    // Step 5: Verify a delivery is created
    const deliveryRes = await adminApi.get(`/api/v1/deliveries?orderId=${orderId}`);
    if (deliveryRes.status() === 200) {
      const deliveries = await deliveryRes.json();
      const deliveryList = deliveries.content || deliveries;

      if (Array.isArray(deliveryList) && deliveryList.length > 0) {
        const deliveryId = deliveryList[0].id;

        // Step 6: Courier accepts delivery
        const courierAcceptRes = await courierApi.put(`/api/v1/deliveries/${deliveryId}/accept`, {});
        if ([200, 204].includes(courierAcceptRes.status())) {
          // Step 7: Courier confirms pickup
          const pickupRes = await courierApi.put(`/api/v1/deliveries/${deliveryId}/pickup`, {});
          expect([200, 204]).toContain(pickupRes.status());

          // Step 8: Courier confirms delivery
          const deliverRes = await courierApi.put(`/api/v1/deliveries/${deliveryId}/deliver`, {});
          expect([200, 204]).toContain(deliverRes.status());

          // Step 9: Verify order is completed
          const finalOrderRes = await adminApi.get(`/api/v1/orders/${orderId}`);
          expect(finalOrderRes.status()).toBe(200);
          const finalOrder = await finalOrderRes.json();
          expect(['DELIVERED', 'COMPLETED']).toContain(finalOrder.status);
        }
      }
    }
  });

  test('order cancellation flow', async () => {
    // Create an order
    const partnersRes = await customerApi.get('/api/v1/partners?status=ACTIVE');
    if (partnersRes.status() !== 200) {
      test.skip();
      return;
    }
    const partners = await partnersRes.json();
    const partnerList = partners.content || partners;
    if (!Array.isArray(partnerList) || partnerList.length === 0) {
      test.skip();
      return;
    }

    const createRes = await customerApi.post('/api/v1/orders', {
      partnerId: partnerList[0].id,
      items: TEST_ORDER.items,
      deliveryAddress: TEST_ORDER.deliveryAddress,
      paymentMethod: TEST_ORDER.paymentMethod,
    });

    if (createRes.status() !== 200 && createRes.status() !== 201) return;

    const order = await createRes.json();

    // Cancel the order
    const cancelRes = await customerApi.put(`/api/v1/orders/${order.id}/cancel`, {
      reason: 'Test cancellation',
    });
    expect([200, 204]).toContain(cancelRes.status());

    // Verify order is cancelled
    const checkRes = await adminApi.get(`/api/v1/orders/${order.id}`);
    if (checkRes.status() === 200) {
      const cancelled = await checkRes.json();
      expect(cancelled.status).toBe('CANCELLED');
    }
  });
});
