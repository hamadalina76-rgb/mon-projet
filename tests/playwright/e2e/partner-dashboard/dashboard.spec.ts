import { test, expect } from '@playwright/test';
import { PartnerDashboardPage } from '../../pages/partner/dashboard.page';

test.describe('Partner Dashboard - Main', () => {
  test('should load dashboard', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();
  });

  test('should display sidebar navigation', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await expect(dashboard.sidebar).toBeVisible();
  });

  test('should navigate to orders', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('orders');
    await expect(page).toHaveURL(/orders/);
  });

  test('should navigate to menu', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('menu');
    await expect(page).toHaveURL(/menu/);
  });

  test('should navigate to analytics', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('analytics');
    await expect(page).toHaveURL(/analytics/);
  });

  test('should navigate to profile', async ({ page }) => {
    const dashboard = new PartnerDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('profile');
    await expect(page).toHaveURL(/profile/);
  });
});
