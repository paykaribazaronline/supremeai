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
    
    // Check if real dashboard widgets like stats and latency graphs exist
    const statsContainer = page.locator('.hud-metrics, .stats-container, .hud-metric, .card').first();
    await expect(statsContainer).toBeVisible();
  });

  test('Navigation menu is present and interactive', async ({ page }) => {
    await expect(page.locator('nav, .ant-menu, [role="navigation"], aside').first()).toBeVisible();
    
    // Test navigating to settings section
    const settingsLink = page.locator('a[href*="settings"], [role="menuitem"]:has-text("Settings"), .ant-menu-item:has-text("Settings")').first();
    if (await settingsLink.count() > 0) {
      await settingsLink.click();
      await expect(page).toHaveURL(/\/settings/);
    }
  });

  test('API health check returns 200', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/health`).catch(() => null);
    expect(response?.status()).toBe(200);
  });

  test('Functional Test: Navigate to Rules and check for rules table', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/rules`);
    // Wait for the Rules page header to appear
    await expect(page.locator('h1:has-text("Work Rules"), h2:has-text("Work Rules")').first()).toBeVisible({ timeout: 10000 });
    
    // Check if the rules list or table is rendered
    const listOrTable = page.locator('.ant-table, .ant-list, table').first();
    await expect(listOrTable).toBeVisible();

    // Verify there is an add rule button
    const addButton = page.locator('button:has-text("Add Rule"), button:has-text("New")').first();
    await expect(addButton).toBeVisible();
  });

  test('Functional Test: Navigate to Projects and check for project cards', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/projects`);
    // Wait for Projects header
    await expect(page.locator('h1:has-text("Projects"), h2:has-text("Projects")').first()).toBeVisible({ timeout: 10000 });
    
    // Check if projects list/grid is visible
    const projectGrid = page.locator('.projects-grid, .ant-card, .project-card').first();
    await expect(projectGrid).toBeVisible();
  });
});
