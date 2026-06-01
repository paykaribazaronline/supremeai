import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Admin Panel Tests', () => {
  test('Admin page loads successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/SupremeAI|NEUROLYNX|أم انگلیسی/i);
    await expect(page.locator('nav, header, main').first()).toBeVisible();
  });

  test('Admin dashboard is accessible', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/dashboard`);
    await expect(page.locator('main, .admin-dashboard, h1, h2').first()).toBeVisible();
  });

  test('Navigation menu is present', async ({ page }) => {
    await expect(page.locator('nav, .ant-menu, [role="navigation"], aside').first()).toBeVisible();
    const menuCount = await page.locator('a[href*="admin"], [role="menuitem"], .ant-menu-item').count();
    expect(menuCount).toBeGreaterThan(3);
  });

  test('API health check returns 200', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/health`).catch(() => null);
    expect(response?.status()).toBe(200);
  });
});
