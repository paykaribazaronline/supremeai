import { test, expect } from '@playwright/test';

test('Chat sends message', async ({ page }) => {
  await page.goto('/');
  await page.fill('input[type="text"]', 'Hello SupremeAI!');
  await page.click('button[type="submit"]');
  await expect(page.locator('.message')).toContainText('Hello SupremeAI!');
});
