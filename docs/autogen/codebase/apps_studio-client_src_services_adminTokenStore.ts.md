# 📄 ফাইল: apps/studio-client/src/services/adminTokenStore.ts

**প্রকার:** .ts  
**সাইজ:** 1,139 বাইট  
**আপডেট:** 2026-07-11T13:13:34.534627

---

## কোড

```ts
/**
 * In-memory admin session metadata store.
 * This intentionally keeps admin tokens out of browser-local storage
 * (since we use httpOnly cookies) but stores non-sensitive session metadata.
 */
const METADATA_KEY = 'supreme_admin_metadata';

export interface AdminSessionMetadata {
  role?: string;
  permissions?: string[];
  expiry_timestamp?: number;
}

export const setAdminMetadata = (metadata: AdminSessionMetadata) => {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem(METADATA_KEY, JSON.stringify(metadata));
  }
};

export const getAdminMetadata = (): AdminSessionMetadata | null => {
  if (typeof window !== 'undefined') {
    const data = sessionStorage.getItem(METADATA_KEY);
    if (data) {
      try {
        return JSON.parse(data);
      } catch (e) {
        return null;
      }
    }
  }
  return null;
};

export const clearAdminMetadata = () => {
  if (typeof window !== 'undefined') {
    sessionStorage.removeItem(METADATA_KEY);
  }
};

// Legacy support to prevent build errors in components that haven't migrated to apiClient yet
export const getAdminToken = (): string => {
  return "";
};

```