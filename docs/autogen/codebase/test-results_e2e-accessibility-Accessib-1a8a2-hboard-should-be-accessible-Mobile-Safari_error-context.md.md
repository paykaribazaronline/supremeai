# 📄 ফাইল: test-results/e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Safari/error-context.md

**প্রকার:** .md  
**সাইজ:** 2,219 বাইট  
**আপডেট:** 2026-07-07T07:10:31.743582

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\accessibility.spec.ts >> Accessibility Tests (WCAG) >> Admin Dashboard should be accessible
- Location: tests\e2e\accessibility.spec.ts:19:9

# Error details

```
TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.
Call log:
  - waiting for locator('text=Admin Gate') to be visible

```

# Page snapshot

```yaml
- generic [ref=e7]: Loading SupremeAI...
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | import AxeBuilder from '@axe-core/playwright';
  3  | 
  4  | test.describe('Accessibility Tests (WCAG)', () => {
  5  |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {
  6  |         await page.goto('/');
  7  |         await page.waitForSelector('[data-testid="dashboard-sidebar"]', { state: 'visible', timeout: 15000 });
  8  | 
  9  |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
  10 | 
  11 |         // কোনো ভায়োলেশন থাকলে তা প্রিন্ট করার জন্য একটি সহায়ক লগ
  12 |         if (accessibilityScanResults.violations.length > 0) {
  13 |             console.log('Accessibility violations found on homepage:', JSON.stringify(accessibilityScanResults.violations, null, 2));
  14 |         }
  15 | 
  16 |         expect(accessibilityScanResults.violations).toEqual([]);
  17 |     });
  18 | 
  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {
  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL
> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });
     |                    ^ TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.
  22 | 
  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
  24 | 
  25 |         expect(accessibilityScanResults.violations).toEqual([]);
  26 |     });
  27 | });
```
```