// Centralized API Client for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাপ্লিকেশনের সেন্ট্রাল এপিআই ক্লায়েন্ট যা হেডার, টোকেন এবং সিকিউর রেট লিমিট (429) / ভ্যালিডেশন এরর ইন্টারসেপ্ট করে।

const API_BASE_URL = import.meta.env.VITE_API_BASE || 'http://localhost:8000';

export const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('supremeai_admin_token') || '';
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
      throw new Error(`Rate limit exceeded: ${errMsg}. Please wait before retrying.`);
    }
    if (res.status === 422) {
      console.error("Validation error (422) detected in payload schema.");
      throw new Error(`Validation Error: ${errMsg}`);
    }
    if (res.status === 401 || res.status === 403) {
      console.warn("Authorization failure (401/403). Session invalidated.");
    }
    throw new Error(errMsg);
  }
  return res.json();
};

export const apiClient = {
  get: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: 'GET',
      headers: getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },

  post: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  put: async <T>(path: string, body?: any, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      ...options,
    });
    return handleResponse(res);
  },

  delete: async <T>(path: string, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
      ...options,
    });
    return handleResponse(res);
  },
};
