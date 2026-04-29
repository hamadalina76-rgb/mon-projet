import { Page, Locator, expect } from '@playwright/test';

export class PartnerMenuPage {
  readonly page: Page;
  readonly menuCategories: Locator;
  readonly productCards: Locator;
  readonly addProductButton: Locator;
  readonly addCategoryButton: Locator;
  readonly productNameInput: Locator;
  readonly productPriceInput: Locator;
  readonly productDescriptionInput: Locator;
  readonly saveButton: Locator;
  readonly deleteButton: Locator;
  readonly toggleAvailability: Locator;

  constructor(page: Page) {
    this.page = page;
    this.menuCategories = page.locator('[class*="category"], mat-expansion-panel');
    this.productCards = page.locator('[class*="product-card"], [class*="product-item"], mat-card');
    this.addProductButton = page.getByRole('button', { name: /add product|ajouter produit/i });
    this.addCategoryButton = page.getByRole('button', { name: /add category|ajouter catégorie/i });
    this.productNameInput = page.getByLabel(/name|nom/i);
    this.productPriceInput = page.getByLabel(/price|prix/i);
    this.productDescriptionInput = page.getByLabel(/description/i);
    this.saveButton = page.getByRole('button', { name: /save|enregistrer/i });
    this.deleteButton = page.getByRole('button', { name: /delete|supprimer/i });
    this.toggleAvailability = page.locator('mat-slide-toggle, [class*="toggle"]');
  }

  async goto() {
    await this.page.goto('/menu');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/menu/);
    await this.page.waitForLoadState('networkidle');
  }

  async addProduct(name: string, price: number, description: string) {
    await this.addProductButton.click();
    await this.productNameInput.fill(name);
    await this.productPriceInput.fill(price.toString());
    await this.productDescriptionInput.fill(description);
    await this.saveButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  async getProductCount(): Promise<number> {
    return this.productCards.count();
  }

  async toggleProductAvailability(index: number = 0) {
    await this.toggleAvailability.nth(index).click();
    await this.page.waitForLoadState('networkidle');
  }
}
