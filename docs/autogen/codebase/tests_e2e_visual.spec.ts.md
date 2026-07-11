# 📄 ফাইল: tests/e2e/visual.spec.ts

**প্রকার:** .ts  
**সাইজ:** 1,414 বাইট  
**আপডেট:** 2026-07-11T18:21:34.987680

---

## কোড

```ts
import { test, expect } from '@playwright/test';

test.describe('Visual Regression Tests', () => {
    test('Homepage layout should be stable', async ({ page }) => {
        await page.goto('/');
        // পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
        await expect(page).toHaveScreenshot('homepage-stable.png', { fullPage: true });
    });

    test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {
        // একটি ডামি URL প্যারামিটার ব্যবহার করে মোডালটি দেখানো হচ্ছে
        await page.goto('/?showConsentModal=true');

        // একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে
        const modal = page.locator('[data-testid="consent-matrix-modal"]'); // এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন
        await expect(modal).toBeVisible();

        // শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন
        await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');
    });
});
```