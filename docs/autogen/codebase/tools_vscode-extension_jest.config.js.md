# 📄 ফাইল: tools/vscode-extension/jest.config.js

**প্রকার:** .js  
**সাইজ:** 413 বাইট  
**আপডেট:** 2026-07-03T15:56:22.737086

---

## কোড

```js
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src', '<rootDir>/test'],
  testMatch: ['**/*.test.ts'],
  moduleFileExtensions: ['ts', 'js', 'json'],
  collectCoverageFrom: ['src/**/*.ts', '!src/**/*.d.ts'],
  coverageDirectory: 'coverage',
  transform: {
    '^.+\\.ts$': 'ts-jest',
  },
  moduleNameMapper: {
    '^vscode$': '<rootDir>/test/__mocks__/vscode.ts',
  },
};

```