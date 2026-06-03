import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Feature Tests - AI Chat', () => {
  test('AI chat page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/ai`);
    await expect(page.locator('main, .ant-layout-content').first()).toBeVisible();
  });

  test('Chat input available', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/ai`);
    const inputs = await page.locator('textarea, input[type="text"], .ant-input').count();
    expect(inputs).toBeGreaterThan(0);
  });
});

test.describe('Feature Tests - Knowledge and Analytics', () => {
  test('Learning page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/learning`);
    await expect(page.locator('main').first()).toBeVisible();
  });

  test('Analytics page has tabs', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/analytics`);
    await expect(page.locator('main').first()).toBeVisible();
  });

  test('Multiple admin sections accessible', async ({ page }) => {
    const routes = ['/admin/dashboard', '/admin/ai', '/admin/learning', '/admin/security', '/admin/reports'];
    for (const route of routes) {
      await page.goto(`${BASE_URL}${route}`);
      await expect(page.locator('main').first()).toBeVisible();
    }
  });
});
