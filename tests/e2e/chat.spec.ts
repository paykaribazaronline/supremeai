import { test, expect } from '@playwright/test';

test('Chat sends message', async ({ page }) => {
  await page.goto('/');
  await page.fill('[data-testid="chat-input"]', 'Hello SupremeAI!');
  await page.click('[data-testid="chat-submit"]');
  await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();
});
