# 📄 ফাইল: packages/ui-components/src/utils/api.ts

**প্রকার:** .ts  
**সাইজ:** 396 বাইট  
**আপডেট:** 2026-07-11T13:56:22.535949

---

## কোড

```ts
export const getApiBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    return import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_URL || 'http://localhost:8000';
  }

  if (import.meta.env.VITE_API_BASE) {
    return import.meta.env.VITE_API_BASE;
  }

  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }

  return window.location.origin;
};

```