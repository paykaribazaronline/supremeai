# 📄 ফাইল: apps/studio-client/src/utils/api.ts

**প্রকার:** .ts  
**সাইজ:** 781 বাইট  
**আপডেট:** 2026-07-05T15:31:52.404322

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

export const getWebSocketBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    return import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8000';
  }

  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
};

```