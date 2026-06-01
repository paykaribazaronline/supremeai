import { type Page } from '@playwright/test';

export async function autoAuth(page: Page) {
  const baseURL = process.env.BASE_URL || 'http://localhost:5173';
  await page.goto(`${baseURL}/login`);
  await page.waitForLoadState('networkidle');

  const email = process.env.E2E_ADMIN_EMAIL || 'test@supreme.ai';
  const password = process.env.E2E_ADMIN_PASSWORD || 'test123';

  const emailInput = page.locator('#email, input[type="email"], input[placeholder*="email" i], input[placeholder*="ইমেইল"]').first();
  const passwordInput = page.locator('#password, input[type="password"], input[placeholder*="password" i], input[placeholder*="পাসওয়ার্ড"]').first();
  const submitBtn = page.locator('button[type="submit"], button:has-text("Login"), button:has-text("লগইন")').first();

  if (await emailInput.count() > 0) {
    await emailInput.fill(email);
    await passwordInput.fill(password);
    await submitBtn.click();
    await page.waitForURL(`${baseURL}/**`, { timeout: 30000 });
    await page.waitForLoadState('networkidle');
  }
}
