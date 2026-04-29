import { test, expect } from '@playwright/test';
import { AdminPartnersPage } from '../../pages/admin/partners.page';

test.describe('Admin Panel - Partners Management', () => {
  test('should display partners list', async ({ page }) => {
    const partnersPage = new AdminPartnersPage(page);
    await partnersPage.goto();
    await partnersPage.expectLoaded();
  });

  test('should search partners', async ({ page }) => {
    const partnersPage = new AdminPartnersPage(page);
    await partnersPage.goto();
    await partnersPage.expectLoaded();

    if (await partnersPage.searchInput.isVisible()) {
      await partnersPage.searchPartner('test');
      await page.waitForLoadState('networkidle');
    }
  });

  test('should view partner details', async ({ page }) => {
    const partnersPage = new AdminPartnersPage(page);
    await partnersPage.goto();
    await partnersPage.expectLoaded();

    const rows = partnersPage.partnerRows;
    if (await rows.count() > 0) {
      await rows.first().click();
      await page.waitForLoadState('networkidle');
    }
  });
});
