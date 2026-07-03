# 📄 ফাইল: tools/vscode-extension/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 287 বাইট  
**আপডেট:** 2026-07-03T11:34:56.016209

---

## কোড

```ts
import { fileURLToPath, URL } from 'url';

export default {
  test: {
    environment: 'node',
    globals: true,
    include: ['test/**/*.test.ts'],
  },
  alias: [
    { find: /^vscode$/, replacement: fileURLToPath(new URL('./test/mocks/vscode.ts', import.meta.url)) },
  ],
} as any;

```