import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.describe('Golden path E2E', () => {
  test('loads home and navigates admin settings', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin`);
    await expect(page.locator('nav, [role="navigation"], aside').first()).toBeVisible({ timeout: 10_000 });

    const settingsLink = page.locator('a[href*="settings"], [role="menuitem"]:has-text("Settings"), .ant-menu-item:has-text("Settings")').first();
    if (await settingsLink.count() > 0) {
      await settingsLink.click();
      await expect(page).toHaveURL(/\/settings/);
      await expect(page.locator('.glass-card, .cyber-tabs, [class*="settings"]').first()).toBeVisible({ timeout: 10_000 });
    }
  });

  test('opens chat and sends message', async ({ page }) => {
    await page.goto(`${BASE_URL}/admin/ai`);
    await expect(page.locator('.neural-chat-container, .chat-input-wrapper, textarea, input[type="text"]').first()).toBeVisible({ timeout: 10_000 });

    const input = page.locator('.chat-styled-input input, .neural-chat-content input[type="text"], textarea').first();
    if (await input.count() > 0) {
      await input.fill('E2E check message');
      await page.keyboard.press('Enter');
      await expect(page.locator('.message-bubble').last()).toBeVisible({ timeout: 15000 });
      await page.screenshot({ path: 'tests/golden-e2e.png', fullPage: true });
    }
  });
});
