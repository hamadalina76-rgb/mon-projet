import { test, expect } from '@playwright/test';
import { AdminUsersPage } from '../../pages/admin/users.page';

test.describe('Admin Panel - Users Management', () => {
  test('should display users list', async ({ page }) => {
    const usersPage = new AdminUsersPage(page);
    await usersPage.goto();
    await usersPage.expectLoaded();
  });

  test('should search users', async ({ page }) => {
    const usersPage = new AdminUsersPage(page);
    await usersPage.goto();
    await usersPage.expectLoaded();

    if (await usersPage.searchInput.isVisible()) {
      await usersPage.searchUser('test');
      await page.waitForLoadState('networkidle');
    }
  });

  test('should view user details', async ({ page }) => {
    const usersPage = new AdminUsersPage(page);
    await usersPage.goto();
    await usersPage.expectLoaded();

    const count = await usersPage.getUserCount();
    if (count > 0) {
      await usersPage.clickUser(0);
      await page.waitForLoadState('networkidle');
    }
  });
});
