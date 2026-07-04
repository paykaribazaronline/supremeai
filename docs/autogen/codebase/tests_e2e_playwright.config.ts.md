# 📄 ফাইল: tests/e2e/playwright.config.ts

**প্রকার:** .ts  
**সাইজ:** 4,729 বাইট  
**আপডেট:** 2026-07-04T03:48:57.287190

---

## কোড

```ts
import { defineConfig, devices } from '@playwright/test';

/**
 * আপনার Playwright E2E টেস্ট কনফিগারেশন
 * এটি স্বয়ংক্রিয়ভাবে ডেভ সার্ভার চালু করবে এবং তারপর E2E টেস্ট রান করবে।
 * 
 * CI-তে রান করার জন্য: pnpm exec playwright test
 * লোকালে চালানোর জন্য: pnpm exec playwright test --headed
 */

export default defineConfig({
  testDir: './tests',
  testMatch: '**/*.spec.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results/e2e-report.json' }],
    ['list'],
  ],
import { test, expect } from '@playwright/test';

test.describe('Visual Regression Tests', () => {
  test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {
    // টেস্টের জন্য মোডালটি দেখানোর ব্যবস্থা করুন
    // এটি একটি নির্দিষ্ট URL-এ গিয়ে বা কোনো বাটনে ক্লিক করে করা যেতে পারে
    await page.goto('/?showConsentModal=true'); // উদাহরণস্বরূপ URL

    const modal = page.getByTestId('consent-matrix-modal'); // data-testid ব্যবহার করা হচ্ছে
    await expect(modal).toBeVisible();

    // মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
    await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');
  });

  test('Homepage layout should be stable', async ({ page }) => {
    await page.goto('/');
    // পুরো পেজের স্ক্রিনশট নিন
    await expect(page).toHaveScreenshot('homepage.png');
  });
});
import { test, expect } from '@playwright/test';

test.describe('Visual Regression Tests', () => {
  test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {
    // টেস্টের জন্য মোডালটি দেখানোর ব্যবস্থা করুন
    // এটি একটি নির্দিষ্ট URL-এ গিয়ে বা কোনো বাটনে ক্লিক করে করা যেতে পারে
    await page.goto('/?showConsentModal=true'); // উদাহরণস্বরূপ URL

    const modal = page.getByTestId('consent-matrix-modal'); // data-testid ব্যবহার করা হচ্ছে
    await expect(modal).toBeVisible();

    // মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
    await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');
  });

  test('Homepage layout should be stable', async ({ page }) => {
    await page.goto('/');
    // পুরো পেজের স্ক্রিনশট নিন
    await expect(page).toHaveScreenshot('homepage.png');
  });
});

  expect: {
    // Visual Regression Test-এর জন্য ডিফল্ট সেটিংস
    toHaveScreenshot: { maxDiffPixels: 100, threshold: 0.2 },
  },
  
  use: {
    // বাংলা মন্তব্য: ডেভেলপমেন্ট সার্ভারের জন্য ডিফল্ট URL
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    // মোবাইল ডিভাইসের জন্য টেস্ট
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  // বাংলা মন্তব্য: ডেভেলপমেন্ট সার্ভার চালু করা, এটি ব্যাকগ্রাউন্ডে থাকবে সমস্ত টেস্ট জুড়ে
  webServer: {
    command: 'pnpm --dir apps/studio-client dev --host 0.0.0.0 --port 5173',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});

```