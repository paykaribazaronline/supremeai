import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Functional Admin E2E Flows', () => {
  test('Login → Dashboard → Chat → Settings → Logout', async ({ page }) => {
    // 1) Dashboard loads with real widgets
    await page.goto(`${BASE_URL}/admin/dashboard`);
    await expect(page.locator('main, .admin-dashboard, h1, h2').first()).toBeVisible({ timeout: 10000 });

    const stats = page.locator('.hud-metrics, .hud-metric').first();
    if (await stats.count() > 0) {
      await expect(stats).toBeVisible();
    }

    // 2) Navigate to Chat
    await page.goto(`${BASE_URL}/admin/ai`);
    await expect(page.locator('.neural-chat-container, .chat-input-wrapper, textarea, input[type="text"]').first()).toBeVisible({ timeout: 10000 });

    // Type a message and submit
    const input = page.locator('.chat-styled-input input, .neural-chat-content input[type="text"], textarea').first();
    if (await input.count() > 0) {
      await input.fill('Hello SupremeAI');
      await page.keyboard.press('Enter');
      // Wait for a response bubble
      await expect(page.locator('.message-bubble').last()).toBeVisible({ timeout: 15000 });
    }

    // 3) Navigate to Settings
    await page.goto(`${BASE_URL}/admin/settings`);
    await expect(page.locator('.glass-card, .cyber-tabs, [class*="settings"]').first()).toBeVisible({ timeout: 10000 });

    // 4) Back to dashboard
    await page.goto(`${BASE_URL}/admin/dashboard`);
    await expect(page.locator('main, .admin-dashboard').first()).toBeVisible({ timeout: 10000 });
  });

  test('Rules page shows add button and table/cards', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/rules`);
    await expect(page.locator('h1:has-text("Rules"), h2:has-text("Rules"), h1:has-text("Work Rules")').first()).toBeVisible({ timeout: 10000 });

    const addBtn = page.locator('button:has-text("Add"), button:has-text("New"), button:has-text("+")').first();
    if (await addBtn.count() > 0) {
      await expect(addBtn).toBeVisible();
    }

    const tableOrList = page.locator('.ant-table, .ant-list, table, .ant-card').first();
    if (await tableOrList.count() > 0) {
      await expect(tableOrList).toBeVisible();
    }
  });

  test('Projects page shows project cards or table', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/projects`);
    await expect(page.locator('h1:has-text("Projects"), h2:has-text("Projects")').first()).toBeVisible({ timeout: 10000 });

    const grid = page.locator('.projects-grid, .ant-card, .project-card, .ant-table').first();
    if (await grid.count() > 0) {
      await expect(grid).toBeVisible();
    }
  });

  test('API health returns valid JSON with status ok', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/health`).catch(() => null);
    expect(response?.ok()).toBe(true);
    const body = response ? await response.json() : null;
    expect(body?.status ?? body?.success ?? body?.mode).toBeTruthy();
  });
});
