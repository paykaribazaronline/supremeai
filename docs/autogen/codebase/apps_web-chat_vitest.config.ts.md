# 📄 ফাইল: apps/web-chat/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 141 বাইট  
**আপডেট:** 2026-07-04T08:12:03.225101

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