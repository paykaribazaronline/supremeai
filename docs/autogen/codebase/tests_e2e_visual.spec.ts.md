# 📄 ফাইল: tests/e2e/visual.spec.ts

**প্রকার:** .ts  
**সাইজ:** 1,256 বাইট  
**আপডেট:** 2026-07-04T05:33:40.957236

---

## কোড

```ts
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

```