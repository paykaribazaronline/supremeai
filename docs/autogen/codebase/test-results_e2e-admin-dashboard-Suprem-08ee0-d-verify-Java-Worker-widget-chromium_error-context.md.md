# 📄 ফাইল: test-results/e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-chromium/error-context.md

**প্রকার:** .md  
**সাইজ:** 3,047 বাইট  
**আপডেট:** 2026-07-04T23:04:45.104572

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\admin-dashboard.spec.ts >> SupremeAI Nexus E2E Flow >> should load the dashboard and verify Java Worker widget
- Location: tests\e2e\admin-dashboard.spec.ts:5:7

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByText('SupremeAI')
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for getByText('SupremeAI')

```

```yaml
- heading "Dashboard Module Failure" [level=2]
- paragraph: A critical module in the admin dashboard has crashed. The rest of the system remains intact.
- text: React is not defined
- button "Reboot Dashboard Module"
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | 
  3  | test.describe('SupremeAI Nexus E2E Flow', () => {
  4  | 
  5  |   test('should load the dashboard and verify Java Worker widget', async ({ page }) => {
  6  |     // 1. Load the admin dashboard
  7  |     await page.goto('/admin'); // Assumes routing allows direct /admin access
  8  | 
  9  |     // 2. Verify Nexus Header exists
> 10 |     await expect(page.getByText('SupremeAI')).toBeVisible();
     |                                               ^ Error: expect(locator).toBeVisible() failed
  11 | 
  12 |     // 3. Verify Java Background Worker widget is rendered
  13 |     const workerWidget = page.locator('text=Java Background Worker');
  14 |     await expect(workerWidget).toBeVisible({ timeout: 10000 });
  15 | 
  16 |     // 4. Verify healthy status indicator
  17 |     await expect(page.locator('text=HEALTHY').first()).toBeVisible();
  18 | 
  19 |     // 5. Verify metrics blocks are present (CPU, Memory, Active Tasks)
  20 |     await expect(page.locator('text=CPU Load')).toBeVisible();
  21 |     await expect(page.locator('text=Memory')).toBeVisible();
  22 |     await expect(page.locator('text=Active Tasks')).toBeVisible();
  23 |   });
  24 | 
  25 |   test('should be able to submit an orchestration command via chat', async ({ page }) => {
  26 |     await page.goto('/admin');
  27 | 
  28 |     // Find the chat input
  29 |     const chatInput = page.getByPlaceholder('[SupremeAI Nexus Command...]');
  30 |     await expect(chatInput).toBeVisible();
  31 | 
  32 |     // Type a command that would theoretically trigger a background Java task
  33 |     await chatInput.fill('Run full system security audit');
  34 |     await chatInput.press('Enter');
  35 | 
  36 |     // Verify the message appears in the chat stream
  37 |     await expect(page.getByText('Admin: Run full system security audit')).toBeVisible();
  38 | 
  39 |     // Verify SupremeAI's immediate ACK response appears
  40 |     await expect(page.getByText('Processing command "Run full system security audit"... Authorization confirmed.')).toBeVisible();
  41 |   });
  42 | 
  43 | });
  44 | 
```
```