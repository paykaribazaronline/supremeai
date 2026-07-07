# 📄 ফাইল: test-results/e2e-chat-Chat-sends-message-Mobile-Chrome/error-context.md

**প্রকার:** .md  
**সাইজ:** 1,181 বাইট  
**আপডেট:** 2026-07-07T11:35:20.616511

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\chat.spec.ts >> Chat sends message
- Location: tests\e2e\chat.spec.ts:3:5

# Error details

```
Test timeout of 30000ms exceeded.
```

```
Error: page.click: Test timeout of 30000ms exceeded.
Call log:
  - waiting for locator('[data-testid="tab-chat"]')

```

# Page snapshot

```yaml
- generic [ref=e7]: Loading SupremeAI...
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | 
  3  | test('Chat sends message', async ({ page }) => {
  4  |   await page.goto('/#/workspace');
> 5  |   await page.click('[data-testid="tab-chat"]');
     |              ^ Error: page.click: Test timeout of 30000ms exceeded.
  6  |   await page.waitForSelector('[data-testid="chat-input"]', { state: 'visible', timeout: 15000 });
  7  |   await page.fill('[data-testid="chat-input"]', 'Hello SupremeAI!');
  8  |   await page.click('[data-testid="chat-submit"]');
  9  |   await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();
  10 | });
  11 | 
```
```