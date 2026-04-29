import { Page, Locator, expect } from '@playwright/test';

export class AdminDashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly sidebar: Locator;
  readonly ordersLink: Locator;
  readonly usersLink: Locator;
  readonly partnersLink: Locator;
  readonly paymentsLink: Locator;
  readonly deliveryLink: Locator;
  readonly analyticsLink: Locator;
  readonly promotionsLink: Locator;
  readonly supportLink: Locator;
  readonly zonesLink: Locator;
  readonly settingsLink: Locator;
  readonly logoutButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1, h2, [class*="title"]').first();
    this.sidebar = page.locator('nav, [class*="sidebar"], [class*="sidenav"], mat-sidenav');
    this.ordersLink = page.getByRole('link', { name: /orders|commandes/i });
    this.usersLink = page.getByRole('link', { name: /users|utilisateurs/i });
    this.partnersLink = page.getByRole('link', { name: /partners|partenaires/i });
    this.paymentsLink = page.getByRole('link', { name: /payments|paiements/i });
    this.deliveryLink = page.getByRole('link', { name: /delivery|dispatch|livraison/i });
    this.analyticsLink = page.getByRole('link', { name: /analytics|statistiques/i });
    this.promotionsLink = page.getByRole('link', { name: /promotions/i });
    this.supportLink = page.getByRole('link', { name: /support/i });
    this.zonesLink = page.getByRole('link', { name: /zones/i });
    this.settingsLink = page.getByRole('link', { name: /settings|paramètres/i });
    this.logoutButton = page.getByRole('button', { name: /logout|déconnexion/i });
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
