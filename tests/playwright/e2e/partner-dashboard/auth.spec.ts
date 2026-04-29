import { test, expect } from '@playwright/test';
import { TEST_ACCOUNTS } from '../../helpers/test-data';

test.describe('Partner Dashboard - Authentication', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should display login page', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();
  });

  test('should login with valid partner credentials', async ({ page }) => {
    await page.goto('/auth/login');
    await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.partner.email);
    await page.getByLabel(/password/i).fill(TEST_ACCOUNTS.partner.password);
    await page.getByRole('button', { name: /sign in|login|connexion/i }).click();
    await page.waitForURL('**/dashboard', { timeout: 15_000 });
    await expect(page).toHaveURL(/dashboard/);
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/auth/login');
    await page.getByLabel(/email/i).fill('wrong@test.com');
    await page.getByLabel(/password/i).fill('WrongPass123!');
    await page.getByRole('button', { name: /sign in|login|connexion/i }).click();

    const error = page.locator('[class*="error"], [class*="alert"], mat-error');
    await expect(error.first()).toBeVisible({ timeout: 5_000 });
  });

  test('should redirect unauthenticated users to login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/auth/);
  });
});
