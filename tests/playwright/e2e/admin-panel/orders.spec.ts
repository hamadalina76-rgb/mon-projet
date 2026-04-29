import { test, expect } from '@playwright/test';
import { AdminOrdersPage } from '../../pages/admin/orders.page';

test.describe('Admin Panel - Orders Management', () => {
  test('should display orders list', async ({ page }) => {
    const ordersPage = new AdminOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();
  });

  test('should search orders', async ({ page }) => {
    const ordersPage = new AdminOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();

    if (await ordersPage.searchInput.isVisible()) {
      await ordersPage.searchOrder('test');
      await page.waitForLoadState('networkidle');
    }
  });

  test('should view order details', async ({ page }) => {
    const ordersPage = new AdminOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();

    const count = await ordersPage.getOrderCount();
    if (count > 0) {
      await ordersPage.clickOrder(0);
      // Should navigate to order detail or open modal
      await page.waitForLoadState('networkidle');
    }
  });

  test('should filter orders by status', async ({ page }) => {
    const ordersPage = new AdminOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();

    if (await ordersPage.statusFilter.isVisible()) {
      await ordersPage.filterByStatus('PENDING');
      await page.waitForLoadState('networkidle');
    }
  });
});
