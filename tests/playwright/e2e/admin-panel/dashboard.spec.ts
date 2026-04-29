import { test, expect } from '@playwright/test';
import { AdminDashboardPage } from '../../pages/admin/dashboard.page';

test.describe('Admin Panel - Dashboard', () => {
  test('should load dashboard after login', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();
  });

  test('should display sidebar navigation', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await expect(dashboard.sidebar).toBeVisible();
  });

  test('should navigate to orders', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('orders');
    await expect(page).toHaveURL(/orders/);
  });

  test('should navigate to users', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('users');
    await expect(page).toHaveURL(/users/);
  });

  test('should navigate to partners', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('partners');
    await expect(page).toHaveURL(/partners/);
  });

  test('should navigate to payments', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('payments');
    await expect(page).toHaveURL(/payments/);
  });

  test('should navigate to zones', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('zones');
    await expect(page).toHaveURL(/zones/);
  });

  test('should navigate to analytics', async ({ page }) => {
    const dashboard = new AdminDashboardPage(page);
    await dashboard.goto();
    await dashboard.navigateTo('analytics');
    await expect(page).toHaveURL(/analytics/);
  });
});
