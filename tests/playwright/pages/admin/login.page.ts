import { Page, Locator, expect } from '@playwright/test';

export class AdminLoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly forgotPasswordLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.getByLabel(/email/i);
    this.passwordInput = page.getByLabel(/password/i);
    this.loginButton = page.getByRole('button', { name: /sign in|login|connexion/i });
    this.errorMessage = page.locator('[class*="error"], [class*="alert-danger"], mat-error');
    this.forgotPasswordLink = page.getByRole('link', { name: /forgot|oublié/i });
  }

  async goto() {
    await this.page.goto('/auth/login');
  }

  async login(email: string, password: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async expectDashboardRedirect() {
    await this.page.waitForURL('**/dashboard', { timeout: 15_000 });
    await expect(this.page).toHaveURL(/dashboard/);
  }

  async expectError() {
    await expect(this.errorMessage.first()).toBeVisible({ timeout: 5_000 });
  }
}
