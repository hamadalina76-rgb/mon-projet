import { Page, Locator, expect } from '@playwright/test';

export class PartnerDashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly sidebar: Locator;
  readonly ordersLink: Locator;
  readonly menuLink: Locator;
  readonly analyticsLink: Locator;
  readonly reviewsLink: Locator;
  readonly promotionsLink: Locator;
  readonly profileLink: Locator;
  readonly financeLink: Locator;
  readonly notificationsLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1, h2, [class*="title"]').first();
    this.sidebar = page.locator('nav, [class*="sidebar"], [class*="sidenav"], mat-sidenav');
    this.ordersLink = page.getByRole('link', { name: /orders|commandes/i });
    this.menuLink = page.getByRole('link', { name: /menu/i });
    this.analyticsLink = page.getByRole('link', { name: /analytics|statistiques/i });
    this.reviewsLink = page.getByRole('link', { name: /reviews|avis/i });
    this.promotionsLink = page.getByRole('link', { name: /promotions/i });
    this.profileLink = page.getByRole('link', { name: /profile|profil/i });
    this.financeLink = page.getByRole('link', { name: /finance/i });
    this.notificationsLink = page.getByRole('link', { name: /notifications/i });
  }

  async goto() {
    await this.page.goto('/dashboard');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/dashboard/);
    await expect(this.sidebar).toBeVisible();
  }

  async navigateTo(section: string) {
    await this.page.getByRole('link', { name: new RegExp(section, 'i') }).click();
    await this.page.waitForLoadState('networkidle');
  }
}
