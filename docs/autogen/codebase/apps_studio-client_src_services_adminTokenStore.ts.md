# 📄 ফাইল: apps/studio-client/src/services/adminTokenStore.ts

**প্রকার:** .ts  
**সাইজ:** 637 বাইট  
**আপডেট:** 2026-07-07T14:29:43.697840

---

## কোড

```ts
/**
 * In-memory admin token store.
 * This intentionally keeps admin tokens out of browser-local storage
 * to reduce exposure from XSS and persistent storage.
 */
const TOKEN_KEY = 'supreme_admin_token';

export const setAdminToken = (token: string) => {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem(TOKEN_KEY, token);
  }
};

export const getAdminToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return sessionStorage.getItem(TOKEN_KEY);
  }
  return null;
};

export const clearAdminToken = () => {
  if (typeof window !== 'undefined') {
    sessionStorage.removeItem(TOKEN_KEY);
  }
};

```