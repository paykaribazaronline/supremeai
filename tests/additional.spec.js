const { test, expect } = require('@playwright/test');

test.describe('Additional Feature Tests', () => {
  test.describe('WebSocket Tests', () => {
    test('WebSocket connection', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await expect(page.locator('.websocket-status')).toBeVisible();
    });

    test('Real-time updates', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await page.waitForTimeout(2000);
      await expect(page.locator('.realtime-data')).toBeVisible();
    });
  });

  test.describe('Performance Tests', () => {
    test('Page load time', async ({ page }) => {
      const start = Date.now();
      await page.goto('/admin');
      const end = Date.now();
      const loadTime = end - start;
      expect(loadTime).toBeLessThan(3000);
    });

    test('API response time', async ({ request }) => {
      const start = Date.now();
      await request.fetch('/api/health');
      const end = Date.now();
      const responseTime = end - start;
      expect(responseTime).toBeLessThan(1000);
    });
  });

  test.describe('Security Tests', () => {
    test('XSS protection', async ({ page }) => {
      await page.goto('/admin');
      await page.evaluate(() => {
        document.body.innerHTML += '<script>alert("xss")</script>';
      });
      const logs = await page.evaluate(() => document.querySelectorAll('script').length);
      expect(logs).toBeLessThanOrEqual(1);
    });

    test('CSRF protection', async ({ page }) => {
      await page.goto('/admin/settings');
      const csrfToken = await page.getAttribute('meta[name="csrf-token"]', 'content');
      expect(csrfToken).toBeTruthy();
    });
  });

  test.describe('Accessibility Tests', () => {
    test('ARIA labels', async ({ page }) => {
      await page.goto('/admin');
      const ariaCount = await page.locator('[aria-label]').count();
      expect(ariaCount).toBeGreaterThan(0);
    });

    test('Keyboard navigation', async ({ page }) => {
      await page.goto('/admin');
      await page.keyboard.press('Tab');
      const focused = await page.evaluate(() => document.activeElement?.tagName);
      expect(focused).toBeTruthy();
    });
  });

  test.describe('Mobile Responsiveness', () => {
    test('Mobile view', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/admin');
      await expect(page.locator('.mobile-menu')).toBeVisible();
    });

    test('Tablet view', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/admin');
      await expect(page.locator('.tablet-layout')).toBeVisible();
    });
  });

  test.describe('Error Handling', () => {
    test('404 handling', async ({ page }) => {
      const response = await page.goto('/admin/nonexistent');
      expect(response?.status()).toBe(404);
    });

    test('API error handling', async ({ request }) => {
      const response = await request.fetch('/api/nonexistent');
      expect(response.status()).toBe(404);
    });
  });

  test.describe('Data Validation', () => {
    test('Input validation', async ({ page }) => {
      await page.goto('/admin/settings');
      await page.fill('#api-key', 'invalid-key-format');
      await page.click('.save-btn');
      await expect(page.locator('.error-message')).toBeVisible();
    });

    test('Form validation', async ({ page }) => {
      await page.goto('/admin/users');
      await page.click('.add-user-btn');
      await page.click('.save-user-btn');
      await expect(page.locator('.validation-error')).toBeVisible();
    });
  });
});