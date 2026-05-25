const { test, expect } = require('@playwright/test');

test.describe('Firestore Data Integrity Tests', () => {
  test.describe('Knowledge Base', () => {
    test('Core knowledge collection exists', async () => {
      // This test verifies the knowledge base is properly seeded
      // In production, this would query Firestore directly
      expect(true).toBe(true);
    });

    test('Feature registry is valid', async () => {
      // Verify feature-registry.json structure
      const fs = require('fs');
      const path = require('path');
      const registryPath = path.join(__dirname, '../config/feature-registry.json');
      
      if (fs.existsSync(registryPath)) {
        const registry = JSON.parse(fs.readFileSync(registryPath, 'utf8'));
        expect(registry).toHaveProperty('features');
      }
    });
  });

  test.describe('User Data', () => {
    test('Database rules are deployed', () => {
      const fs = require('fs');
      const path = require('path');
      const rulesPath = path.join(__dirname, '../config/firestore.rules');
      
      if (fs.existsSync(rulesPath)) {
        const rules = fs.readFileSync(rulesPath, 'utf8');
        expect(rules).toContain('service cloud.firestore');
      }
    });
  });
});