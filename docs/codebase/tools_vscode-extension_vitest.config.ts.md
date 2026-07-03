# 📄 ফাইল: tools/vscode-extension/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 387 বাইট  
**আপডেট:** 2026-07-03T12:26:44.398038

---

## কোড

```ts
import { fileURLToPath, URL } from 'url';

export default {
  test: {
    environment: 'node',
    globals: true,
    include: ['test/**/*.test.ts'],
    setupFiles: ['./test/setup.ts'],
  },
  resolve: {
    alias: [
      { find: /^vscode$/, replacement: fileURLToPath(new URL('./test/mocks/vscode.ts', import.meta.url)) },
    ],
  },
  coverage: {
    provider: 'v8',
  },
} as any;

```