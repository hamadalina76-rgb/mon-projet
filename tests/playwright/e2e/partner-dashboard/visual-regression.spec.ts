import { test, expect } from '@playwright/test';

/**
 * Visual Regression Tests - Partner Dashboard
 *
 * First run creates baselines. Update with: npx playwright test --update-snapshots
 */
test.describe('Partner Dashboard - Visual Regression', () => {
  test('login page', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('partner-login.png', {
      maxDiffPixelRatio: 0.05,
      fullPage: true,
    });
  });

  test('dashboard', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await expect(page).toHaveScreenshot('partner-dashboard.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('orders page', async ({ page }) => {
    await page.goto('/orders');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('partner-orders.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('menu management page', async ({ page }) => {
    await page.goto('/menu');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('partner-menu.png', {
      maxDiffPixelRatio: 0.1,
      fullPage: true,
    });
  });

  test('profile page', async ({ page }) => {
    await page.goto('/profile');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('partner-profile.png', {
      maxDiffPixelRatio: 0.05,
      fullPage: true,
    });
  });

  test('analytics page', async ({ page }) => {
    await page.goto('/analytics');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await expect(page).toHaveScreenshot('partner-analytics.png', {
      maxDiffPixelRatio: 0.15,
      fullPage: true,
    });
  });
});
