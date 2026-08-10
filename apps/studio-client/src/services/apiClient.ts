// Centralized API Client for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাপ্লিকেশনের সেন্ট্রাল এপিআই ক্লায়েন্ট যা হেডার, টোকেন এবং সিকিউর রেট লিমিট (429) / ভ্যালিডেশন এরর ইন্টারসেপ্ট করে।

import { getApiBaseUrl } from '../utils/api';
import { getDeviceFingerprint } from '../utils/deviceFingerprint';
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

let cachedToken: string | null = null;

export const updateTokenCache = (token: string | null) => {
  cachedToken = token;
};

const isDev = () => {
  return typeof process !== 'undefined' && (process.env.NODE_ENV === 'development' || process.env.VITE_ENV === 'development');
};

const getCSRFToken = (): string => {
  // Get CSRF token from cookie or meta tag
  const metaTag = document.querySelector('meta[name="csrf-token"]');
  if (metaTag) return metaTag.getAttribute('content') || '';
  const cookieMatch = document.cookie.match(/csrf_token=([^;]+)/);
  return cookieMatch ? cookieMatch[1] : '';
};

export const getAuthHeaders = async (): Promise<Record<string, string>> => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // 🟢 Sprint 5: Backend API Integration
  if (cachedToken === null) {
    cachedToken = localStorage.getItem('supremeai_auth_token') || '';
  }

  if (cachedToken) {
    headers['Authorization'] = `Bearer ${cachedToken}`;
  }

  // 🔐 CSRF Protection
  const csrfToken = getCSRFToken();
  if (csrfToken) {
    headers['X-CSRF-Token'] = csrfToken;
  }

  // 🔐 Phase 2: Hybrid Fingerprint Login — AntiHackingContextMiddleware ব্যবহার করে
  // IP/country-এর পাশাপাশি তৃতীয় কনটেক্সট সিগন্যাল হিসেবে
  try {
    headers['X-Device-Fingerprint'] = await getDeviceFingerprint();
  } catch {
    // বাংলা: WebCrypto অনুপস্থিত থাকলে (পুরনো ব্রাউজার) নীরবে বাদ দেওয়া হচ্ছে — request ব্লক হবে না
  }

  return headers;
};

const handleResponse = async (res: Response) => {
  // 🔐 Phase 2 JIT-OTP Interceptor — Status 202 Accepted means JIT OTP is required
  if (res.status === 202) {
    const data = await res.json().catch(() => ({}));
    return {
      success: false,
      requiresOTP: true,
      message: data.message || 'JIT OTP verification required',
      data,
    };
  }

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
      if (isDev()) console.warn("Rate limit exceeded (429). Throttling client requests.");
      throw new ApiError(`Rate limit exceeded: ${errMsg}. Please wait before retrying.`, 429);
    }
    if (res.status === 402) {
      if (isDev()) console.warn("Payment/Budget Required (402). CostGuard rejected the request.");
      throw new ApiError(`Budget Limit Exceeded: ${errMsg}`, 402);
    }
    if (res.status === 422) {
      if (isDev()) console.error("Validation error (422) detected in payload schema.");
      throw new ApiError(`Validation Error: ${errMsg}`, 422);
    }
    if (res.status === 401 || res.status === 403) {
      if (isDev()) console.warn("Authorization failure (401/403). Session invalidated.");
      throw new ApiError(errMsg, res.status);
    }
    throw new ApiError(errMsg, res.status);
  }
  return res.json();
};

// বাংলা মন্তব্য: রেন্ডার ফ্রি টিয়ার কোল্ড স্টার্ট (Wake up time ৩০-৫০ সেকেন্ড) সামলাতে এপিআই রিকোয়েস্ট টাইমআউট ৬০ সেকেন্ডে বৃদ্ধি করা হলো।
const DEFAULT_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 60000);

const fetchWithTimeout = async (url: string, options: RequestInit, timeoutMs = DEFAULT_TIMEOUT_MS): Promise<Response> => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  // বাংলা মন্তব্য: JSDOM এবং node-fetch-এর সাথে AbortSignal টাইপ অমিল এড়াতে টেস্ট এনভায়রনমেন্টে signal বাদ দেওয়া হচ্ছে।
  const fetchOptions: RequestInit = { ...options };
  const isTest = typeof process !== 'undefined' && (process.env.NODE_ENV === 'test' || process.env.VITEST === 'true');
  if (!isTest) {
    fetchOptions.signal = controller.signal;
  }

  try {
    return await fetch(url, fetchOptions);
  } catch (e) {
    if (controller.signal.aborted) {
      throw new Error(`Request timed out after ${timeoutMs}ms: ${url}`);
    }
    throw e;
  } finally {
    clearTimeout(timer);
  }
};

// বাংলা মন্তব্য: throttledFetch — p-queue দিয়ে একসাথে অতিরিক্ত রিকোয়েস্ট না যাওয়ার নিশ্চয়তা
const throttledFetch = async (url: string, options: RequestInit): Promise<Response> => {
  return requestQueue.add(async () => {
    const currentUrl = url;
    let attempts = 0;
    options.credentials = 'include';

    while (attempts < 2) {
      try {
        const res = await fetchWithTimeout(currentUrl, options);
        // 502/503/504 মানে রেন্ডার সার্ভার স্লিপিং বা ডাউন — একই backend-এ রিট্রাই করব
        if (res.status >= 502 && res.status <= 504) {
          throw new Error("Server sleeping or down (50x)");
        }
        return res;
      } catch (e: unknown) {
        attempts++;
        if (attempts >= 2) {
          if (isDev()) console.error(`[Queue Interceptor] Network failure for ${currentUrl} after 2 attempts:`, e);
          throw e;
        }

        // বাংলা মন্তব্য: একই URL-এ backoff retry — backend কখনোই পাল্টানো হয় না (portal isolation)।
        // Render free tier cold start (৩০-৫০ সেকেন্ড) সামলাতে delay বাড়ানো হলো।
        const delayMs = 2000 * attempts;
        if (isDev()) console.warn(`[Retry] Network error: ${(e as Error).message}. Retrying same backend in ${delayMs}ms...`);
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }
    throw new Error("Backend request failed after retries");
  }) as Promise<Response>;
};

export const apiClient = {
  get: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'GET',
      headers: await getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },

  post: async <T>(path: string, body?: unknown, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: await getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  put: async <T>(path: string, body?: unknown, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'PUT',
      headers: await getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  delete: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'DELETE',
      headers: await getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },

  performSensitiveAction: async <T>(path: string, body?: unknown, otpCode?: string): Promise<T> => {
    const headers = await getAuthHeaders();
    if (otpCode) {
      headers['X-JIT-OTP'] = otpCode;
    }
    const res = await throttledFetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    return handleResponse(res);
  },
};
