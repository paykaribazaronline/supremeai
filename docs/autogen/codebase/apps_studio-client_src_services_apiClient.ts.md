# 📄 ফাইল: apps/studio-client/src/services/apiClient.ts

**প্রকার:** .ts  
**সাইজ:** 4,454 বাইট  
**আপডেট:** 2026-07-11T11:05:10.278544

---

## কোড

```ts
// Centralized API Client for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাপ্লিকেশনের সেন্ট্রাল এপিআই ক্লায়েন্ট যা হেডার, টোকেন এবং সিকিউর রেট লিমিট (429) / ভ্যালিডেশন এরর ইন্টারসেপ্ট করে।

import { getApiBaseUrl } from '../utils/api';
import PQueue from 'p-queue';

// বাংলা মন্তব্য: কাস্টম এরর ক্লাস — status প্রপার্টি দিয়ে React Query retry ফাংশন সঠিকভাবে 401/403/429 চিহ্নিত করতে পারে
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// Dynamic concurrency queue
export const requestQueue = new PQueue({ concurrency: 3 }); // Default to 3, can be updated via config

export const setApiConcurrency = (concurrency: number) => {
  requestQueue.concurrency = concurrency;
};

export const getAuthHeaders = (): Record<string, string> => {
  return {
    'Content-Type': 'application/json'
    // Authorization header removed as per httpOnly cookie setup
  };
};

const handleResponse = async (res: Response) => {
  if (!res.ok) {
    let errMsg = `HTTP error! status: ${res.status}`;
    try {
      const errData = await res.json();
      errMsg = errData.detail || errMsg;
    } catch {
      // JSON parsing failure fallback
    }

    // 🛑 ZERO-GAP: Intercept specific critical HTTP exception statuses
    if (res.status === 429) {
      console.warn("Rate limit exceeded (429). Throttling client requests.");
      throw new ApiError(`Rate limit exceeded: ${errMsg}. Please wait before retrying.`, 429);
    }
    if (res.status === 402) {
      console.warn("Payment/Budget Required (402). CostGuard rejected the request.");
      throw new ApiError(`Budget Limit Exceeded: ${errMsg}`, 402);
    }
    if (res.status === 422) {
      console.error("Validation error (422) detected in payload schema.");
      throw new ApiError(`Validation Error: ${errMsg}`, 422);
    }
    if (res.status === 401 || res.status === 403) {
      console.warn("Authorization failure (401/403). Session invalidated.");
      throw new ApiError(errMsg, res.status);
    }
    throw new ApiError(errMsg, res.status);
  }
  return res.json();
};

// বাংলা মন্তব্য: throttledFetch — p-queue দিয়ে একসাথে অতিরিক্ত রিকোয়েস্ট না যাওয়ার নিশ্চয়তা
const throttledFetch = async (url: string, options: RequestInit): Promise<Response> => {
  return requestQueue.add(async () => {
    try {
      // credentials already set in interceptor, but we can enforce it here too
      options.credentials = 'include';
      return await fetch(url, options);
    } catch (e) {
      console.error(`[Queue Interceptor] Network failure for ${url}:`, e);
      throw e; // throw so the caller knows it failed, queue will proceed to next item
    }
  }) as Promise<Response>;
};

export const apiClient = {
  get: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'GET',
      headers: getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },

  post: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  put: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  delete: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },
};


```