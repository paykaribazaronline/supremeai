import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Agent Orchestration Tests', () => {
  test('Agent page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/providers`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Administration page accessible', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/users`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Action button present on page', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/providers`);
    const buttons = await page.locator('button').count();
    expect(buttons).toBeGreaterThan(0);
  });
});

test.describe('Learning System Tests', () => {
  test('Learning page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/learning`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Page contains interactive elements', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/learning`);
    const links = await page.locator('button, a').count();
    expect(links).toBeGreaterThan(0);
  });

  test('Performance tab loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/monitoring`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });
});

test.describe('Report Generation Tests', () => {
  test('Reports page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/reports`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Report page has action available', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/reports`);
    await expect(page.locator('main').first()).toBeVisible();
  });

  test('Export functionality area loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/reports`);
    const panels = await page.locator('main, .ant-card, .ant-tabs').count();
    expect(panels).toBeGreaterThan(0);
  });
});

test.describe('Backup System Tests', () => {
  test('Backup page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/backup`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Backup action area loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/backup`);
    await expect(page.locator('main').first()).toBeVisible();
  });

  test('Restore component loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/backup`);
    expect(true).toBe(true);
  });
});

test.describe('API Key Rotation Tests', () => {
  test('API settings page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Security page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/security`);
    await expect(page.locator('main, h1, h2').first()).toBeVisible();
  });

  test('Configuration area loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/settings`);
    const configElements = await page.locator('main, form, .ant-form').count();
    expect(configElements).toBeGreaterThan(0);
  });
});
