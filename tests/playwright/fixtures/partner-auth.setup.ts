import { test as setup, expect } from '@playwright/test';
import { TEST_ACCOUNTS } from '../helpers/test-data';
import path from 'path';

const authFile = path.join(__dirname, '.auth', 'partner.json');

setup('authenticate as partner', async ({ page }) => {
  await page.goto('/auth/login');

  await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.partner.email);
  await page.getByLabel(/password/i).fill(TEST_ACCOUNTS.partner.password);
  await page.getByRole('button', { name: /sign in|login|connexion/i }).click();

  // Wait for redirect to dashboard
  await page.waitForURL('**/dashboard', { timeout: 15_000 });
  await expect(page).toHaveURL(/dashboard/);

  // Save auth state
  await page.context().storageState({ path: authFile });
});
