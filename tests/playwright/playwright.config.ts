import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '.env') });

const ADMIN_BASE_URL = process.env.ADMIN_BASE_URL || 'http://localhost:4200';
const PARTNER_BASE_URL = process.env.PARTNER_BASE_URL || 'http://localhost:4201';
const API_GATEWAY_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';

export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },

  reporter: [
    ['html', { open: 'never' }],
    ['allure-playwright'],
    ['list'],
  ],

  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },

  projects: [
    // --- Auth setup (runs first, stores auth state) ---
    {
      name: 'admin-auth-setup',
      testDir: './fixtures',
      testMatch: 'admin-auth.setup.ts',
    },
    {
      name: 'partner-auth-setup',
      testDir: './fixtures',
      testMatch: 'partner-auth.setup.ts',
    },

    // --- Admin Panel E2E Tests ---
    {
      name: 'admin-panel',
      testDir: './e2e/admin-panel',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: ADMIN_BASE_URL,
        storageState: 'fixtures/.auth/admin.json',
      },
      dependencies: ['admin-auth-setup'],
    },

    // --- Partner Dashboard E2E Tests ---
    {
      name: 'partner-dashboard',
      testDir: './e2e/partner-dashboard',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: PARTNER_BASE_URL,
        storageState: 'fixtures/.auth/partner.json',
      },
      dependencies: ['partner-auth-setup'],
    },

    // --- API Tests (no browser needed) ---
    {
      name: 'api-tests',
      testDir: './api',
      use: {
        baseURL: API_GATEWAY_URL,
      },
    },
  ],
});
