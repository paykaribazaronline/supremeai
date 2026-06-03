import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('API Endpoint Tests', () => {
  test('Provider list endpoint', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/providers`);
    expect(response.status()).toBeLessThan(500);
  });

  test('Provider health check', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/providers/health`);
    expect(response.status()).toBeLessThan(500);
  });

  test('Agent status endpoint', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/agents/status`);
    expect(response.status()).toBeLessThan(500);
  });

  test('Learning progress endpoint', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/api/learning/progress`);
    expect(response.status()).toBeLessThan(500);
  });

  test('Health endpoint returns ok', async ({ page }) => {
    const response = await page.request.get(`${BASE_URL}/actuator/health`);
    expect(response.status()).toBe(200);
  });
});
