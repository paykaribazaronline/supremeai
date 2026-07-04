# 📄 ফাইল: tests/e2e/chat.spec.ts

**প্রকার:** .ts  
**সাইজ:** 321 বাইট  
**আপডেট:** 2026-07-04T22:38:52.641870

---

## কোড

```ts
import { test, expect } from '@playwright/test';

test('Chat sends message', async ({ page }) => {
  await page.goto('/');
  await page.fill('[data-testid="chat-input"]', 'Hello SupremeAI!');
  await page.click('[data-testid="chat-submit"]');
  await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();
});

```