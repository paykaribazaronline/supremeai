# 📄 ফাইল: apps/studio-client/src/utils/api.ts

**প্রকার:** .ts  
**সাইজ:** 998 বাইট  
**আপডেট:** 2026-07-11T20:08:21.444849

---

## কোড

```ts
export const getApiBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    const url = import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_URL;
    if (!url && import.meta.env.PROD) throw new Error("API URL missing in production");
    return url || 'http://localhost:8000';
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
    const url = import.meta.env.VITE_WS_BASE_URL;
    if (!url && import.meta.env.PROD) throw new Error("WS URL missing in production");
    return url || 'ws://localhost:8000';
  }

  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
};

```