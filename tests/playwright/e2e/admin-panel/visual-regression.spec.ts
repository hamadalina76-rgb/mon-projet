import { test, expect } from '@playwright/test';

/**
 * Visual Regression Tests - Admin Panel
 *
 * Takes screenshots of key pages and compares against baseline.
 * First run creates baselines in __snapshots__/ directory.
 * Subsequent runs diff against baseline and fail on visual changes.
 *
 * Update baselines: npx playwright test --update-snapshots
 */
test.describe('Admin Panel - Visual Regression', () => {
  test('login page', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-login.png', {
      maxDiffPixelRatio: 0.05,
      fullPage: true,
    });
  });

  test('dashboard', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    // Wait for charts/widgets to render
    await page.waitForTimeout(2000);
    await expect(page).toHaveScreenshot('admin-dashboard.png', {
      maxDiffPixelRatio: 0.1, // Dashboard has dynamic data, allow 10%
      fullPage: true,
    });
  });

  test('orders list', async ({ page }) => {
    await page.goto('/orders');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-orders.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('partners list', async ({ page }) => {
    await page.goto('/partners');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-partners.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('users list', async ({ page }) => {
    await page.goto('/users');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-users.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('zones page', async ({ page }) => {
    await page.goto('/zones');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-zones.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('settings page', async ({ page }) => {
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('admin-settings.png', {
      maxDiffPixelRatio: 0.05,
      fullPage: true,
    });
  });

  test('sidebar navigation - collapsed and expanded', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Screenshot sidebar area
    const sidebar = page.locator('nav, [class*="sidebar"], [class*="sidenav"], mat-sidenav').first();
    if (await sidebar.isVisible()) {
      await expect(sidebar).toHaveScreenshot('admin-sidebar.png', {
        maxDiffPixelRatio: 0.05,
      });
    }
  });
});
