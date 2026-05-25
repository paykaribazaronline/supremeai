const { test, expect } = require('@playwright/test');

test.describe('Feature Tests - AI Chat', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/chat');
    await page.waitForLoadState('networkidle');
  });

  test('Chat interface loads', async ({ page }) => {
    await expect(page.locator('.chat-container')).toBeVisible();
  });

  test('Send message functionality', async ({ page }) => {
    const messageBox = page.locator('.message-input');
    const sendButton = page.locator('.send-button');
    
    await messageBox.fill('Hello AI!');
    await sendButton.click();
    
    await expect(page.locator('.chat-message')).toContainText('Hello AI!');
  });

  test('Clear chat functionality', async ({ page }) => {
    await page.click('.clear-chat-button');
    await expect(page.locator('.chat-container')).toBeEmpty();
  });

  test('Chat history sidebar', async ({ page }) => {
    await page.click('.history-sidebar');
    await expect(page.locator('.chat-history')).toBeVisible();
  });
});

test.describe('Feature Tests - Knowledge Search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/knowledge');
    await page.waitForLoadState('networkidle');
  });

  test('Search bar exists', async ({ page }) => {
    await expect(page.locator('.search-bar')).toBeVisible();
  });

  test('Search functionality', async ({ page }) => {
    await page.fill('.search-bar', 'AI');
    await page.press('.search-bar', 'Enter');
    await expect(page.locator('.search-results')).toBeVisible();
  });

  test('Filter by category', async ({ page }) => {
    await page.selectOption('.category-filter', 'ai-ml');
    await expect(page.locator('.filtered-results')).toBeVisible();
  });
});

test.describe('Feature Tests - Data Visualization', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/analytics');
    await page.waitForLoadState('networkidle');
  });

  test('Charts render', async ({ page }) => {
    await expect(page.locator('.chart-container')).toBeVisible();
  });

  test('Data export', async ({ page }) => {
    await page.click('.export-button');
    await expect(page.locator('.export-options')).toBeVisible();
  });
});