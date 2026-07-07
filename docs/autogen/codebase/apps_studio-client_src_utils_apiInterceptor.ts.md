# 📄 ফাইল: apps/studio-client/src/utils/apiInterceptor.ts

**প্রকার:** .ts  
**সাইজ:** 2,033 বাইট  
**আপডেট:** 2026-07-07T17:03:49.483847

---

## কোড

```ts
import { getAdminToken } from '../services/adminTokenStore';
import { getApiBaseUrl } from './api';

export function setupGlobalFetchInterceptor() {
  if (typeof window === 'undefined') return;

  const originalFetch = window.fetch;

  window.fetch = async function (...args) {
    const url = args[0];
    let options: any = args[1];
    const apiBase = getApiBaseUrl();

    if (typeof url === 'string' && url.startsWith(apiBase)) {
      const token = getAdminToken();
      
      if (url.includes('/admin-api/')) {
        if (!token) {
           console.warn("Blocked unauthorized API request to prevent storm:", url);
           return Promise.reject(new Error("No token found"));
        }
      }

      if (token) {
        options = options || {};
        options.headers = {
          ...options.headers,
          'Authorization': `Bearer ${token}`
        };
        args[1] = options;
      }
    }

    try {
      const response = await originalFetch.apply(this, args);
      
      if (!response.ok) {
        let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
        try {
          const clone = response.clone();
          const text = await clone.text();
          if (text) {
             const parsed = JSON.parse(text);
             if (parsed.error) errorMsg = parsed.error;
             else if (parsed.message) errorMsg = parsed.message;
             else if (parsed.detail) errorMsg = typeof parsed.detail === 'string' ? parsed.detail : JSON.stringify(parsed.detail);
             else errorMsg = text.slice(0, 50);
          }
        } catch (e) {
          // ignore parsing error
        }

        if ((window as any).showGlobalToast) {
          (window as any).showGlobalToast('error', errorMsg);
        }
      }
      
      return response;
    } catch (error) {
      if ((window as any).showGlobalToast) {
        (window as any).showGlobalToast('error', `Network Error: ${error instanceof Error ? error.message : 'Unknown'}`);
      }
      throw error;
    }
  };
}

```