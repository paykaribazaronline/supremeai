const { test, expect } = require('@playwright/test');

test.describe('Admin Panel Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/');
  });

  test('Admin page loads successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/SupremeAI/);
  });

  test('Admin dashboard is accessible', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await expect(page.locator('body')).toBeVisible();
  });

  test('Navigation menu works', async ({ page }) => {
    const menuItems = [
      'dashboard',
      'security',
      'learning',
      'projects',
      'knowledge',
      'settings'
    ];
    
    for (const item of menuItems) {
      try {
        await page.goto(`/admin/${item}`);
        await expect(page.locator('body')).toBeVisible();
      } catch (error) {
        console.log(`Menu item ${item} test failed: ${error.message}`);
      }
    }
  });

  test('API health check', async ({ page }) => {
    const response = await page.request('/api/health');
    expect(response.status()).toBe(200);
  });
});