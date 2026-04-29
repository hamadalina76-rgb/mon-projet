import { test, expect } from '@playwright/test';
import { AdminLoginPage } from '../../pages/admin/login.page';
import { TEST_ACCOUNTS } from '../../helpers/test-data';

test.describe('Admin Panel - Authentication', () => {
  test.use({ storageState: { cookies: [], origins: [] } }); // No pre-auth

  test('should display login page', async ({ page }) => {
    const loginPage = new AdminLoginPage(page);
    await loginPage.goto();

    await expect(loginPage.emailInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
    await expect(loginPage.loginButton).toBeVisible();
  });

  test('should login with valid admin credentials', async ({ page }) => {
    const loginPage = new AdminLoginPage(page);
    await loginPage.goto();
    await loginPage.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
    await loginPage.expectDashboardRedirect();
  });

  test('should show error with invalid credentials', async ({ page }) => {
    const loginPage = new AdminLoginPage(page);
    await loginPage.goto();
    await loginPage.login('invalid@test.com', 'WrongPassword123!');
    await loginPage.expectError();
  });

  test('should show error with empty fields', async ({ page }) => {
    const loginPage = new AdminLoginPage(page);
    await loginPage.goto();
    await loginPage.loginButton.click();

    // Form validation should prevent submission or show error
    const url = page.url();
    expect(url).toContain('auth');
  });

  test('should redirect unauthenticated users to login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/auth/);
  });
});
