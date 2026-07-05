# 📄 ফাইল: apps/web-chat/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 141 বাইট  
**আপডেট:** 2026-07-05T00:55:36.213324

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