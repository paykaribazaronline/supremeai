# 📄 ফাইল: apps/web-chat/api.test.ts

**প্রকার:** .ts  
**সাইজ:** 518 বাইট  
**আপডেট:** 2026-07-08T02:55:55.615339

---

## কোড

```ts
import { describe, it, expect } from 'vitest';
import { api } from './api';

describe('api', () => {
    it('fetchQuota should return 87', async () => {
        const quota = await api.fetchQuota() as { remaining: number };
        expect(quota.remaining).toBe(87);
    });

    it('executeTask should handle errors gracefully', async () => {
        // Simple mock test since real fetch would fail
        const result = await api.executeTask('Hello', []);
        expect(result).toHaveProperty('error');
    });
});

```