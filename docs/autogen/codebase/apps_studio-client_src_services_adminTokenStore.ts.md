# 📄 ফাইল: apps/studio-client/src/services/adminTokenStore.ts

**প্রকার:** .ts  
**সাইজ:** 372 বাইট  
**আপডেট:** 2026-07-03T15:03:57.578828

---

## কোড

```ts
/**
 * In-memory admin token store.
 * This intentionally keeps admin tokens out of browser-local storage
 * to reduce exposure from XSS and persistent storage.
 */
let adminToken = '';

export const setAdminToken = (token: string) => {
  adminToken = token;
};

export const getAdminToken = () => adminToken;

export const clearAdminToken = () => {
  adminToken = '';
};

```