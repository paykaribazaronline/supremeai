const { test } = require('@playwright/test');

const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL || 'admin@supreme.ai';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD || 'admin123';
const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

async function loginAsAdmin(page) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');

  const emailInput = page.locator('input[type="email"], input[placeholder*="email" i], input[placeholder*="ইমেইল"]');
  const passwordInput = page.locator('input[type="password"], input[placeholder*="password" i], input[placeholder*="পাসওয়ার্ড"]');
  const submitBtn = page.locator('button:has-text("Login"), button:has-text("লগইন"), button[type="submit"]');

  await emailInput.fill(ADMIN_EMAIL);
  await passwordInput.fill(ADMIN_PASSWORD);
  await submitBtn.click();

  await page.waitForURL(`${BASE_URL}/**`, { timeout: 30000 });
  await page.waitForLoadState('networkidle');
}

async function loginAsGuest(page) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');

  const guestBtn = page.locator('button:has-text("Continue as Guest"), button:has-text("গেস্ট")');
  if (await guestBtn.count() > 0) {
    await guestBtn.click();
    await page.waitForURL(`${BASE_URL}/**`, { timeout: 20000 });
    await page.waitForLoadState('networkidle');
  }
}

test.beforeEach(async ({ page }) => {
  await loginAsAdmin(page);
});

module.exports = { loginAsAdmin, loginAsGuest, ADMIN_EMAIL, ADMIN_PASSWORD, BASE_URL };
