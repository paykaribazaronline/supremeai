# 📄 ফাইল: apps/studio-client/src/services/apiClient.ts

**প্রকার:** .ts  
**সাইজ:** 4,512 বাইট  
**আপডেট:** 2026-07-07T16:18:57.186953

---

## কোড

```ts
// Centralized API Client for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাপ্লিকেশনের সেন্ট্রাল এপিআই ক্লায়েন্ট যা হেডার, টোকেন এবং সিকিউর রেট লিমিট (429) / ভ্যালিডেশন এরর ইন্টারসেপ্ট করে।

import { getApiBaseUrl } from '../utils/api';
import { getAdminToken } from './adminTokenStore';

// বাংলা মন্তব্য: কাস্টম এরর ক্লাস — status প্রপার্টি দিয়ে React Query retry ফাংশন সঠিকভাবে 401/403/429 চিহ্নিত করতে পারে
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// বাংলা মন্তব্য: কনকারেন্সি লিমিটার — একসাথে সর্বোচ্চ MAX_CONCURRENT টি রিকোয়েস্ট যাবে, বাকিগুলো কিউতে থাকবে
const MAX_CONCURRENT = 3;
let activeRequests = 0;
const requestQueue: Array<() => void> = [];

function enqueue(): Promise<void> {
  if (activeRequests < MAX_CONCURRENT) {
    activeRequests++;
    return Promise.resolve();
  }
  return new Promise<void>((resolve) => {
    requestQueue.push(() => {
      activeRequests++;
      resolve();
    });
  });
}

function dequeue(): void {
  activeRequests--;
  if (requestQueue.length > 0) {
    const next = requestQueue.shift();
    next?.();
  }
}

export const getAuthHeaders = (): Record<string, string> => {
  const token = getAdminToken();
  return {
    'Content-Type': 'application/json',
    'Authorization': token ? `Bearer ${token}` : '',
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

// বাংলা মন্তব্য: throttledFetch — কিউ দিয়ে একসাথে অতিরিক্ত রিকোয়েস্ট না যাওয়ার নিশ্চয়তা
const throttledFetch = async (url: string, options: RequestInit): Promise<Response> => {
  await enqueue();
  try {
    return await fetch(url, options);
  } finally {
    dequeue();
  }
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