# 📄 ফাইল: apps/web-chat/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 141 বাইট  
**আপডেট:** 2026-07-05T14:42:46.731820

---

## কোড

```ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
  },
});

```