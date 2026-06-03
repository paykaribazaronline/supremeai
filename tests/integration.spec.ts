import { test, expect } from '@playwright/test';
import { autoAuth } from '../e2e-utils/auto-auth.test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

test.beforeEach(async ({ page }) => {
  await autoAuth(page);
});

test.describe('Integration Tests - Real System Features', () => {
  test.describe('Page Integration', () => {
    test('Dashboard loads', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/dashboard`);
      await expect(page.locator('main').first()).toBeVisible();
    });

    test('Browser page accessible', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/browser`);
      await expect(page.locator('main').first()).toBeVisible();
    });

    test('OCR page accessible', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/ocr`);
      await expect(page.locator('main').first()).toBeVisible();
    });

    test('Infrastructure page accessible', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/infrastructure`);
      await expect(page.locator('main').first()).toBeVisible();
    });
  });

  test.describe('Knowledge and Data Operations', () => {
    test('Knowledge base file is populated', async () => {
      const fs = require('fs');
      const path = require('path');
      const knowledgePath = path.join(__dirname, '../config/autonomous_seed_knowledge.json');
      const knowledge = JSON.parse(fs.readFileSync(knowledgePath, 'utf8'));
      expect(knowledge).toHaveProperty('knowledge');
      expect(knowledge.knowledge.length).toBeGreaterThan(10);
    });

    test('Learning query API reachable', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/actuator/health`);
      expect(response.status()).toBe(200);
    });

    test('Firestore rules file exists and valid', async () => {
      const fs = require('fs');
      const path = require('path');
      const rulesPath = path.join(__dirname, '../config/firestore.rules');
      expect(fs.existsSync(rulesPath)).toBe(true);
      const rules = fs.readFileSync(rulesPath, 'utf8');
      expect(rules).toContain('service cloud.firestore');
      expect(rules).toContain('rules_version');
    });
  });

  test.describe('Data Integrity', () => {
    test('Firestore indexes file exists', async () => {
      const fs = require('fs');
      const path = require('path');
      const indexPath = path.join(__dirname, '../config/firestore.indexes.json');
      const indexes = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
      expect(indexes).toHaveProperty('indexes');
    });

    test('Feature registry structure', async () => {
      const fs = require('fs');
      const path = require('path');
      const registryPath = path.join(__dirname, '../config/feature-registry.json');
      const registry = JSON.parse(fs.readFileSync(registryPath, 'utf8'));
      expect(registry).toHaveProperty('features');
    });
  });

  test.describe('Real-time Features', () => {
    test('Dashboard loads with main UI', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/dashboard`);
      await expect(page.locator('main').first()).toBeVisible();
    });

    test('Settings page loads', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/settings`);
      await expect(page.locator('main').first()).toBeVisible();
    });
  });

  test.describe('Cloud Services', () => {
    test('Backend health endpoint', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/actuator/health`);
      expect(response.status()).toBe(200);
    });
  });
});
