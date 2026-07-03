# 📄 ফাইল: tests/e2e/chat.spec.ts

**প্রকার:** .ts  
**সাইজ:** 309 বাইট  
**আপডেট:** 2026-07-03T16:05:21.803632

---

## কোড

```ts
import { test, expect } from '@playwright/test';

test('Chat sends message', async ({ page }) => {
  await page.goto('/');
  await page.fill('input[type="text"]', 'Hello SupremeAI!');
  await page.click('button[type="submit"]');
  await expect(page.locator('.message')).toContainText('Hello SupremeAI!');
});

```