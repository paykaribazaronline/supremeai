# 📄 ফাইল: tools/vscode-extension/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 450 বাইট  
**আপডেট:** 2026-07-10T18:52:51.215521

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
    ssr: false,
    deps: {
      interopDefault: true,
    },
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