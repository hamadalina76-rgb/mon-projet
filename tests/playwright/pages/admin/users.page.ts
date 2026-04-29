import { Page, Locator, expect } from '@playwright/test';

export class AdminUsersPage {
  readonly page: Page;
  readonly userTable: Locator;
  readonly userRows: Locator;
  readonly searchInput: Locator;
  readonly addUserButton: Locator;
  readonly roleFilter: Locator;
  readonly statusFilter: Locator;

  constructor(page: Page) {
    this.page = page;
    this.userTable = page.locator('table, mat-table, [class*="user-list"]');
    this.userRows = page.locator('tbody tr, mat-row, [class*="user-row"]');
    this.searchInput = page.getByPlaceholder(/search|rechercher/i);
    this.addUserButton = page.getByRole('button', { name: /add|ajouter|create|créer/i });
    this.roleFilter = page.locator('mat-select, select').filter({ hasText: /role|rôle/i });
    this.statusFilter = page.locator('mat-select, select').filter({ hasText: /status|statut/i });
  }

  async goto() {
    await this.page.goto('/users');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/users/);
    await this.page.waitForLoadState('networkidle');
  }

  async searchUser(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForLoadState('networkidle');
  }

  async getUserCount(): Promise<number> {
    await this.page.waitForLoadState('networkidle');
    return this.userRows.count();
  }

  async clickUser(index: number = 0) {
    await this.userRows.nth(index).click();
    await this.page.waitForLoadState('networkidle');
  }
}
