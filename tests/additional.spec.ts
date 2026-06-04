import { test, expect } from '@playwright/test';

test.describe('SupremeAI Additional Validation Tests', () => {
  test('Page Load Performance Verification', async ({ page }) => {
    const startTime = Date.now();
    await page.goto('/');
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(5000); // verify load time under 5s
  });

  test('404 Page Redirection Handling', async ({ page }) => {
    await page.goto('/invalid-page-path-random');
    // redirects back to admin dashboard
    await expect(page).toHaveURL(/\/admin\/dashboard/);
  });
});
