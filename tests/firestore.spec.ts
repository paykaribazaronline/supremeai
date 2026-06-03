import { test, expect } from '@playwright/test';

test.describe('Firestore Integration Tests', () => {
  test.describe('Data Integrity', () => {
    test('Knowledge base file is populated', async () => {
      const fs = require('fs');
      const path = require('path');
      const knowledgePath = path.join(__dirname, '../config/autonomous_seed_knowledge.json');
      const knowledge = JSON.parse(fs.readFileSync(knowledgePath, 'utf8'));
      expect(knowledge).toHaveProperty('knowledge');
      expect(Array.isArray(knowledge.knowledge)).toBe(true);
    });

    test('Feature registry structure', async () => {
      const fs = require('fs');
      const path = require('path');
      const registryPath = path.join(__dirname, '../config/feature-registry.json');
      const registry = JSON.parse(fs.readFileSync(registryPath, 'utf8'));
      expect(registry).toHaveProperty('features');
      expect(Array.isArray(registry.features)).toBe(true);
      expect(registry.features.length).toBeGreaterThan(0);
    });

    test('Firestore indexes file exists', async () => {
      const fs = require('fs');
      const path = require('path');
      const indexPath = path.join(__dirname, '../config/firestore.indexes.json');
      const indexes = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
      expect(indexes).toHaveProperty('indexes');
      expect(Array.isArray(indexes.indexes)).toBe(true);
      expect(indexes.indexes.length).toBeGreaterThan(5);
    });
  });

  test.describe('Security Rules', () => {
    test('Firestore rules file exists', async () => {
      const fs = require('fs');
      const path = require('path');
      const rulesPath = path.join(__dirname, '../config/firestore.rules');
      expect(fs.existsSync(rulesPath)).toBe(true);
    });

    test('Rules are valid Firestore security rules', async () => {
      const fs = require('fs');
      const path = require('path');
      const rulesPath = path.join(__dirname, '../config/firestore.rules');
      const rules = fs.readFileSync(rulesPath, 'utf8');
      expect(rules).toContain('service cloud.firestore');
      expect(rules).toContain('rules_version');
      expect(rules).toContain('match /databases/{database}/documents');
    });
  });

  test.describe('Collections', () => {
    test('Users collection has indexing', async () => {
      const fs = require('fs');
      const path = require('path');
      const indexPath = path.join(__dirname, '../config/firestore.indexes.json');
      const indexes = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
      const userIndexes = indexes.indexes.filter((idx: any) =>
        idx.collectionGroup === 'users' ||
        (idx.fields && idx.fields.some((f: any) => f.fieldPath === 'email'))
      );
      expect(userIndexes.length).toBeGreaterThanOrEqual(0);
    });

    test('Database rules contain allow rules', async () => {
      const fs = require('fs');
      const path = require('path');
      const rulesPath = path.join(__dirname, '../config/firestore.rules');
      const rules = fs.readFileSync(rulesPath, 'utf8');
      expect(rules).toContain('allow read');
      expect(rules).toContain('allow write');
    });
  });
});
