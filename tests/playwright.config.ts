const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './',
  testMatch: /.*\.spec\.(ts|js)$/,
  timeout: 60000,
  retries: process.env.CI ? 1 : 0,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  reporter: [['html', { outputFolder: 'playwright-report' }]],
});
