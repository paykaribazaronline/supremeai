const { test, expect } = require('@playwright/test');

test.describe('Advanced Admin Panel Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/');
    await page.waitForLoadState('networkidle');
  });

  test.describe('Security Features', () => {
    test('Security page loads', async ({ page }) => {
      await page.goto('/admin/security');
      await expect(page.locator('h1, h2, .security-header')).toBeVisible();
    });

    test('API key validation', async ({ page }) => {
      await page.goto('/admin/security/api-keys');
      const addButton = page.locator('button:has-text("Add")');
      await expect(addButton).toBeVisible();
    });

    test('Rate limiting settings', async ({ page }) => {
      await page.goto('/admin/security/rate-limit');
      await expect(page.locator('.rate-limit-config')).toBeVisible();
    });
  });

  test.describe('Learning System', () => {
    test('Learning dashboard loads', async ({ page }) => {
      await page.goto('/admin/learning');
      await expect(page.locator('.learning-dashboard')).toBeVisible();
    });

    test('Knowledge base viewer', async ({ page }) => {
      await page.goto('/admin/learning/knowledge');
      await expect(page.locator('.knowledge-viewer')).toBeVisible();
    });

    test('Training logs', async ({ page }) => {
      await page.goto('/admin/learning/logs');
      await expect(page.locator('.training-logs')).toBeVisible();
    });
  });

  test.describe('Project Management', () => {
    test('Projects page loads', async ({ page }) => {
      await page.goto('/admin/projects');
      await expect(page.locator('.projects-grid')).toBeVisible();
    });

    test('Create new project', async ({ page }) => {
      await page.goto('/admin/projects');
      await page.click('button:has-text("New Project")');
      await expect(page.locator('.modal')).toBeVisible();
    });

    test('Project settings', async ({ page }) => {
      await page.goto('/admin/projects');
      const settingsBtn = page.locator('.project-item:first-child .settings-btn');
      if (await settingsBtn.count() > 0) {
        await settingsBtn.click();
        await expect(page.locator('.settings-modal')).toBeVisible();
      }
    });
  });

  test.describe('User Management', () => {
    test('Users page loads', async ({ page }) => {
      await page.goto('/admin/users');
      await expect(page.locator('.users-table')).toBeVisible();
    });

    test('Add new user', async ({ page }) => {
      await page.goto('/admin/users');
      await page.click('button:has-text("Add User")');
      await expect(page.locator('.user-form')).toBeVisible();
    });

    test('User roles', async ({ page }) => {
      await page.goto('/admin/users');
      await expect(page.locator('.role-selector')).toBeVisible();
    });
  });

  test.describe('Knowledge Management', () => {
    test('Knowledge base page', async ({ page }) => {
      await page.goto('/admin/knowledge');
      await expect(page.locator('.knowledge-base')).toBeVisible();
    });

    test('Import knowledge', async ({ page }) => {
      await page.goto('/admin/knowledge');
      await page.click('button:has-text("Import")');
      await expect(page.locator('.import-modal')).toBeVisible();
    });

    test('Export knowledge', async ({ page }) => {
      await page.goto('/admin/knowledge');
      await page.click('button:has-text("Export")');
      await expect(page.locator('.export-options')).toBeVisible();
    });
  });

  test.describe('Settings', () => {
    test('General settings', async ({ page }) => {
      await page.goto('/admin/settings');
      await expect(page.locator('.general-settings')).toBeVisible();
    });

    test('API configuration', async ({ page }) => {
      await page.goto('/admin/settings/api');
      await expect(page.locator('.api-config')).toBeVisible();
    });

    test('Notification settings', async ({ page }) => {
      await page.goto('/admin/settings/notifications');
      await expect(page.locator('.notification-settings')).toBeVisible();
    });
  });
});