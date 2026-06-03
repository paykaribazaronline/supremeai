import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Advanced Admin Panel Tests', () => {
  test('Security page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/security`);
    await expect(page.locator('main, h1, h2, .admin-page').first()).toBeVisible();
  });

  test('API key management loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/security/api-keys`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Learning dashboard loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/learning`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Projects page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/projects`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Users page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/users`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('AI Chat page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/ai`);
    await expect(page.locator('main, .ant-layout-content').first()).toBeVisible();
  });

  test('Settings page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('General settings loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('API configuration loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings/api`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Notification settings loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings/notifications`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });
});
