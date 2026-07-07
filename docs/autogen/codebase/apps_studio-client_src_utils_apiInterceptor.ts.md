# 📄 ফাইল: apps/studio-client/src/utils/apiInterceptor.ts

**প্রকার:** .ts  
**সাইজ:** 2,355 বাইট  
**আপডেট:** 2026-07-07T21:58:43.497395

---

## কোড

```ts
import { getApiBaseUrl } from './api';
import { useAdminStore } from '../store/adminStore';

export function setupGlobalFetchInterceptor() {
  if (typeof window === 'undefined') return;

  const originalFetch = window.fetch;

  window.fetch = async function (...args) {
    const url = args[0];
    let options: any = args[1];
    const apiBase = getApiBaseUrl();

    if (typeof url === 'string' && url.startsWith(apiBase)) {
      options = options || {};
      // Ensure cookies are sent with every cross-origin API request
      options.credentials = 'include';
      args[1] = options;
    }

    try {
      const response = await originalFetch.apply(this, args);
      
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
           // Handle unauthorized access globally
           const store = useAdminStore.getState();
           if (store.adminAuthenticated) {
             store.handleAdminLogout();
           }
           if (typeof window !== 'undefined' && window.location.pathname.startsWith('/admin')) {
             // Redirect to login if on admin portal
             // Wait, handleAdminLogout already resets state. Let's redirect as user requested
             // Actually, if we just clear authenticated state, the App will render the login page.
           }
        }
        
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