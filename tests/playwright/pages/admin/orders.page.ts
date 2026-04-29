import { Page, Locator, expect } from '@playwright/test';

export class AdminOrdersPage {
  readonly page: Page;
  readonly orderTable: Locator;
  readonly orderRows: Locator;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly refreshButton: Locator;
  readonly exportButton: Locator;
  readonly paginationNext: Locator;

  constructor(page: Page) {
    this.page = page;
    this.orderTable = page.locator('table, [class*="order-list"], mat-table');
    this.orderRows = page.locator('tbody tr, mat-row, [class*="order-row"]');
    this.searchInput = page.getByPlaceholder(/search|rechercher/i);
    this.statusFilter = page.locator('mat-select, select').filter({ hasText: /status|statut/i });
    this.refreshButton = page.getByRole('button', { name: /refresh|actualiser/i });
    this.exportButton = page.getByRole('button', { name: /export/i });
    this.paginationNext = page.getByRole('button', { name: /next|suivant/i });
  }

  async goto() {
    await this.page.goto('/orders');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/orders/);
    await this.page.waitForLoadState('networkidle');
  }

  async getOrderCount(): Promise<number> {
    await this.page.waitForLoadState('networkidle');
    return this.orderRows.count();
  }

  async searchOrder(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForLoadState('networkidle');
  }

  async clickOrder(index: number = 0) {
    await this.orderRows.nth(index).click();
    await this.page.waitForLoadState('networkidle');
  }

  async filterByStatus(status: string) {
    await this.statusFilter.click();
    await this.page.getByRole('option', { name: new RegExp(status, 'i') }).click();
    await this.page.waitForLoadState('networkidle');
  }
}
