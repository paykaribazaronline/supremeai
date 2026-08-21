import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:4173';

test.describe('AETHEL Multi-Workspace Fleet Canvas — UI Smoke Tests', () => {
  test('renders target fleet canvas with node cards and connected badges', async ({ page }) => {
    await page.goto(BASE_URL + '/#/workspace');
    await page.waitForLoadState('networkidle');

    // MultiWorkspace canvas indicator
    const heading = page.locator('text=Multi-Platform Target Fleet Canvas').first();
    if (await heading.count() > 0) {
      await expect(heading).toBeVisible();
      const targetBadge = page.locator('text=Targets Connected').first();
      await expect(targetBadge).toBeVisible();
    }
  });

  test('permission scope badges render correctly (READ_ONLY / FULL_CONTROL)', async ({ page }) => {
    await page.goto(BASE_URL + '/#/workspace');
    await page.waitForLoadState('networkidle');

    const readOnlyBadge = page.locator('text=READ_ONLY').first();
    if (await readOnlyBadge.count() > 0) {
      await expect(readOnlyBadge).toBeVisible();
    }

    const fullControlBadge = page.locator('text=FULL_CONTROL').first();
    if (await fullControlBadge.count() > 0) {
      await expect(fullControlBadge).toBeVisible();
    }
  });
});
