# 📄 ফাইল: tools/vscode-extension/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 337 বাইট  
**আপডেট:** 2026-07-03T11:28:24.942637

---

## কোড

```ts
import { defineConfig } from 'vitest/config';
import { fileURLToPath, URL } from 'url';

export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    include: ['test/**/*.test.ts'],
  },
  resolve: {
    alias: {
      vscode: fileURLToPath(new URL('./test/mocks/vscode.ts', import.meta.url)),
    },
  },
});

```