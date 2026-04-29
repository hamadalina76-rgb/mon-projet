import { Page, Locator, expect } from '@playwright/test';

export class AdminPartnersPage {
  readonly page: Page;
  readonly partnerTable: Locator;
  readonly partnerRows: Locator;
  readonly searchInput: Locator;
  readonly addPartnerButton: Locator;
  readonly statusFilter: Locator;
  readonly typeFilter: Locator;

  constructor(page: Page) {
    this.page = page;
    this.partnerTable = page.locator('table, mat-table, [class*="partner-list"]');
    this.partnerRows = page.locator('tbody tr, mat-row, [class*="partner-row"]');
    this.searchInput = page.getByPlaceholder(/search|rechercher/i);
    this.addPartnerButton = page.getByRole('button', { name: /add|ajouter|create|créer/i });
    this.statusFilter = page.locator('mat-select, select').filter({ hasText: /status|statut/i });
    this.typeFilter = page.locator('mat-select, select').filter({ hasText: /type/i });
  }

  async goto() {
    await this.page.goto('/partners');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/partners/);
    await this.page.waitForLoadState('networkidle');
  }

  async searchPartner(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForLoadState('networkidle');
  }

  async approvePartner(index: number = 0) {
    await this.partnerRows.nth(index).click();
    await this.page.waitForLoadState('networkidle');
    const approveBtn = this.page.getByRole('button', { name: /approve|approuver/i });
    if (await approveBtn.isVisible()) {
      await approveBtn.click();
    }
  }
}
