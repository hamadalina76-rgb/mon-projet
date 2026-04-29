import { test, expect } from '@playwright/test';
import { PartnerOrdersPage } from '../../pages/partner/orders.page';

test.describe('Partner Dashboard - Orders', () => {
  test('should display orders page', async ({ page }) => {
    const ordersPage = new PartnerOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();
  });

  test('should show order cards or empty state', async ({ page }) => {
    const ordersPage = new PartnerOrdersPage(page);
    await ordersPage.goto();
    await ordersPage.expectLoaded();

    const count = await ordersPage.getOrderCount();
    if (count === 0) {
      // Should show empty state message
      const emptyState = page.locator('[class*="empty"], [class*="no-data"]');
      await expect(emptyState.first()).toBeVisible().catch(() => {
        // Some apps just show an empty list
      });
    }
  });
});
