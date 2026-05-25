const { test, expect } = require('@playwright/test');

test.describe('Agent Orchestration Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/agents');
    await page.waitForLoadState('networkidle');
  });

  test('Agent dashboard loads', async ({ page }) => {
    await expect(page.locator('.agent-dashboard')).toBeVisible();
  });

  test('Agent status display', async ({ page }) => {
    await expect(page.locator('.agent-status')).toBeVisible();
  });

  test('Orchestration controls', async ({ page }) => {
    await page.click('.orchestrate-btn');
    await expect(page.locator('.orchestration-panel')).toBeVisible();
  });

  test('Agent logs', async ({ page }) => {
    await page.click('.logs-tab');
    await expect(page.locator('.agent-logs')).toBeVisible();
  });
});

test.describe('Learning System Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/learning');
    await page.waitForLoadState('networkidle');
  });

  test('Learning progress bar', async ({ page }) => {
    await expect(page.locator('.progress-bar')).toBeVisible();
  });

  test('Knowledge update button', async ({ page }) => {
    await page.click('.update-knowledge-btn');
    await expect(page.locator('.update-modal')).toBeVisible();
  });

  test('Training history', async ({ page }) => {
    await page.click('.training-history');
    await expect(page.locator('.history-table')).toBeVisible();
  });
});

test.describe('Report Generation Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/reports');
    await page.waitForLoadState('networkidle');
  });

  test('Report types display', async ({ page }) => {
    await expect(page.locator('.report-types')).toBeVisible();
  });

  test('Generate report', async ({ page }) => {
    await page.click('.generate-report-btn');
    await expect(page.locator('.report-form')).toBeVisible();
  });

  test('Export options', async ({ page }) => {
    await page.click('.export-options');
    await expect(page.locator('.export-dropdown')).toBeVisible();
  });
});

test.describe('Backup System Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/backup');
    await page.waitForLoadState('networkidle');
  });

  test('Backup status', async ({ page }) => {
    await expect(page.locator('.backup-status')).toBeVisible();
  });

  test('Create backup', async ({ page }) => {
    await page.click('.create-backup-btn');
    await expect(page.locator('.backup-progress')).toBeVisible();
  });

  test('Restore functionality', async ({ page }) => {
    await page.click('.restore-btn');
    await expect(page.locator('.restore-modal')).toBeVisible();
  });
});

test.describe('API Key Rotation Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/settings/api');
    await page.waitForLoadState('networkidle');
  });

  test('Provider keys display', async ({ page }) => {
    await expect(page.locator('.provider-keys')).toBeVisible();
  });

  test('Rotation settings', async ({ page }) => {
    await page.click('.rotation-settings');
    await expect(page.locator('.rotation-config')).toBeVisible();
  });

  test('Test connection', async ({ page }) => {
    await page.click('.test-connection-btn');
    await expect(page.locator('.connection-result')).toBeVisible();
  });
});