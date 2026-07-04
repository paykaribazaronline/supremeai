# 📄 ফাইল: test-results/e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-Mobile-Chrome/error-context.md

**প্রকার:** .md  
**সাইজ:** 2,976 বাইট  
**আপডেট:** 2026-07-04T13:24:28.396112

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\visual.spec.ts >> Visual Regression Tests >> ConsentMatrixModal should match the approved snapshot
- Location: tests\e2e\visual.spec.ts:10:9

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('[data-testid="consent-matrix-modal"]')
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for locator('[data-testid="consent-matrix-modal"]')

```

```yaml
- complementary:
  - text: ▲ SupremeAI
  - navigation:
    - button "Sessions"
    - button "Workspace"
    - button "Auth Vault"
    - button "Automation"
    - button "Knowledge"
    - button "Secrets"
    - button "Usage"
    - button "Settings"
    - button "Site Actions"
    - button "LLM Gateway"
    - button "Admin Console"
  - text: Offline
  - button "Dark mode"
- main:
  - heading "What do you want to build today?" [level=1]
  - textbox "Give SupremeAI a task to work on..."
  - button "Start Session" [disabled]
  - heading "Recent sessions" [level=2]
  - text: 0 total
  - paragraph: No sessions yet. Start your first task above.
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | 
  3  | test.describe('Visual Regression Tests', () => {
  4  |     test('Homepage layout should be stable', async ({ page }) => {
  5  |         await page.goto('/');
  6  |         // পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
  7  |         await expect(page).toHaveScreenshot('homepage-stable.png', { fullPage: true });
  8  |     });
  9  | 
  10 |     test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {
  11 |         // একটি ডামি URL প্যারামিটার ব্যবহার করে মোডালটি দেখানো হচ্ছে
  12 |         await page.goto('/?showConsentModal=true');
  13 | 
  14 |         // একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে
  15 |         const modal = page.locator('[data-testid="consent-matrix-modal"]'); // এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন
> 16 |         await expect(modal).toBeVisible();
     |                             ^ Error: expect(locator).toBeVisible() failed
  17 | 
  18 |         // শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
  19 |         await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');
  20 |     });
  21 | });
```
```