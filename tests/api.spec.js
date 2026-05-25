const { test, expect } = require('@playwright/test');

test.describe('API Tests', () => {
  const BASE_URL = 'https://supremeai-a.web.app';

  test.describe('Health Check Endpoints', () => {
    test('API health endpoint returns 200', async ({ request }) => {
      const response = await request(`${BASE_URL}/api/health`);
      expect(response.status()).toBe(200);
    });

    test('System health endpoint returns 200', async ({ request }) => {
      const response = await request(`${BASE_URL}/api/system-health`);
      expect(response.status()).toBe(200);
    });
  });

  test.describe('Authentication API', () => {
    test('Auth endpoint exists', async ({ request }) => {
      const response = await request(`${BASE_URL}/api/auth`);
      expect(response.status()).not.toBe(404);
    });
  });

  test.describe('Firestore Integration', () => {
    test('Firestore rules are deployed', async ({ request }) => {
      const response = await fetch('https://firestore.googleapis.com/v1/projects/supremeai-a/databases/(default)/firestore/indexes');
      expect(response.status()).not.toBe(404);
    });
  });
});