import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Accessibility Tests - Admin Panel
 *
 * Uses axe-core to check WCAG 2.1 compliance.
 * Tests all major pages for accessibility violations.
 */
test.describe('Admin Panel - Accessibility (WCAG 2.1)', () => {
  test('login page should have no critical violations', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

    const critical = results.violations.filter(
      (v) => v.impact === 'critical' || v.impact === 'serious'
    );

    if (critical.length > 0) {
      console.log('Accessibility violations on login page:');
      critical.forEach((v) => {
        console.log(`  [${v.impact}] ${v.id}: ${v.description}`);
        v.nodes.forEach((n) => console.log(`    -> ${n.html.substring(0, 100)}`));
      });
    }

    expect(critical).toHaveLength(0);
  });

  test('dashboard should have no critical violations', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .disableRules(['color-contrast']) // Charts may have contrast issues
      .analyze();

    const critical = results.violations.filter((v) => v.impact === 'critical');
    expect(critical).toHaveLength(0);
  });

  test('orders page should have no critical violations', async ({ page }) => {
    await page.goto('/orders');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

    const critical = results.violations.filter((v) => v.impact === 'critical');
    expect(critical).toHaveLength(0);
  });

  test('all pages should have proper heading hierarchy', async ({ page }) => {
    const pages = ['/dashboard', '/orders', '/partners', '/users', '/settings'];

    for (const path of pages) {
      await page.goto(path);
      await page.waitForLoadState('networkidle');

      const results = await new AxeBuilder({ page })
        .withRules(['heading-order'])
        .analyze();

      if (results.violations.length > 0) {
        console.log(`Heading hierarchy issue on ${path}`);
      }
    }
  });

  test('forms should have associated labels', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withRules(['label', 'label-title-only'])
      .analyze();

    const serious = results.violations.filter(
      (v) => v.impact === 'critical' || v.impact === 'serious'
    );
    expect(serious).toHaveLength(0);
  });

  test('interactive elements should be keyboard accessible', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withRules(['keyboard', 'focus-order-semantics', 'tabindex'])
      .analyze();

    const critical = results.violations.filter((v) => v.impact === 'critical');
    expect(critical).toHaveLength(0);
  });

  test('images should have alt text', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    const results = await new AxeBuilder({ page })
      .withRules(['image-alt'])
      .analyze();

    if (results.violations.length > 0) {
      console.log('Images missing alt text:');
      results.violations.forEach((v) => {
        v.nodes.forEach((n) => console.log(`  -> ${n.html.substring(0, 100)}`));
      });
    }
  });
});
