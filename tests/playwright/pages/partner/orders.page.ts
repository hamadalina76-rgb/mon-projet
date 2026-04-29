import { Page, Locator, expect } from '@playwright/test';

export class PartnerOrdersPage {
  readonly page: Page;
  readonly orderList: Locator;
  readonly orderCards: Locator;
  readonly newOrdersBadge: Locator;
  readonly acceptButton: Locator;
  readonly rejectButton: Locator;
  readonly readyButton: Locator;
  readonly statusTabs: Locator;

  constructor(page: Page) {
    this.page = page;
    this.orderList = page.locator('[class*="order-list"], [class*="orders"]');
    this.orderCards = page.locator('[class*="order-card"], [class*="order-item"], mat-card');
    this.newOrdersBadge = page.locator('[class*="badge"], mat-badge');
    this.acceptButton = page.getByRole('button', { name: /accept|accepter/i });
    this.rejectButton = page.getByRole('button', { name: /reject|refuser/i });
    this.readyButton = page.getByRole('button', { name: /ready|prêt/i });
    this.statusTabs = page.locator('mat-tab-group, [class*="tab"]');
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
    return this.orderCards.count();
  }

  async acceptFirstOrder() {
    await this.orderCards.first().click();
    await this.acceptButton.first().click();
    await this.page.waitForLoadState('networkidle');
  }

  async markOrderReady() {
    await this.readyButton.first().click();
    await this.page.waitForLoadState('networkidle');
  }
}
