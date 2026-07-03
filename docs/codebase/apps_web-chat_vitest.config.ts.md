# 📄 ফাইল: apps/web-chat/vitest.config.ts

**প্রকার:** .ts  
**সাইজ:** 141 বাইট  
**আপডেট:** 2026-07-03T11:28:24.929100

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