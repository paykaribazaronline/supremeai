import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

// বাংলা মন্তব্য: api.ts এখন build-time constants (BACKEND_URL ইত্যাদি) module-load-এ resolve করে।
// তাই প্রতিটি টেস্টে env সেট করার পর vi.resetModules() দিয়ে module পুনরায় import করা হচ্ছে।
const env = import.meta.env as unknown as Record<string, unknown>;

const loadApi = async () => {
  vi.resetModules();
  return await import('./api');
};

const setHostname = (hostname: string) => {
  // বাংলা মন্তব্য: jsdom-এ window.location রিডঅনলি, তাই defineProperty দিয়ে ওভাররাইড
  Object.defineProperty(window, 'location', {
    configurable: true,
    writable: true,
    value: { hostname, host: `${hostname}`, protocol: 'https:' },
  });
};

const ORIGINAL_LOCATION = window.location;

describe('api.ts — portal-ভিত্তিক backend resolution', () => {
  beforeEach(() => {
    delete env.VITE_API_BASE;
    delete env.VITE_API_URL;
    delete env.VITE_WS_BASE_URL;
    delete env.VITE_USER_BACKEND;
    delete env.VITE_ADMIN_BACKEND;
    delete env.VITE_PORTAL_TYPE;
    delete env.PROD;
    setHostname('localhost');
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: ORIGINAL_LOCATION,
    });
  });

  describe('BACKEND_URL', () => {
    it('user portal-এ ডিফল্ট user backend রিটার্ন করে', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      const { BACKEND_URL } = await loadApi();
      expect(BACKEND_URL).toBe('https://supremeai-backend.onrender.com');
    });

    it('admin portal-এ admin backend রিটার্ন করে', async () => {
      env.VITE_PORTAL_TYPE = 'admin';
      const { BACKEND_URL } = await loadApi();
      expect(BACKEND_URL).toBe('https://supremeai-admin.onrender.com');
    });

    it('VITE_USER_BACKEND override সম্মান করে', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      env.VITE_USER_BACKEND = 'https://user-override.example.com';
      const { BACKEND_URL } = await loadApi();
      expect(BACKEND_URL).toBe('https://user-override.example.com');
    });

    it('VITE_ADMIN_BACKEND override সম্মান করে', async () => {
      env.VITE_PORTAL_TYPE = 'admin';
      env.VITE_ADMIN_BACKEND = 'https://admin-override.example.com';
      const { BACKEND_URL } = await loadApi();
      expect(BACKEND_URL).toBe('https://admin-override.example.com');
    });

    it('user portal-এ VITE_API_BASE কে VITE_API_URL-এর চেয়ে অগ্রাধিকার দেয়', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      env.VITE_API_BASE = 'https://api.example.com';
      env.VITE_API_URL = 'https://fallback.example.com';
      const { BACKEND_URL } = await loadApi();
      expect(BACKEND_URL).toBe('https://api.example.com');
    });
  });

  describe('cross-portal isolation', () => {
    it('switchActiveBackend export আর নেই (cross-portal failover সরানো হয়েছে)', async () => {
      const api = await loadApi();
      expect((api as Record<string, unknown>).switchActiveBackend).toBeUndefined();
    });

    it('user portal কখনোই admin backend রিটার্ন করে না', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      setHostname('supremeai-lac.vercel.app');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).not.toContain('supremeai-admin');
    });

    it('admin portal কখনোই user backend রিটার্ন করে না', async () => {
      env.VITE_PORTAL_TYPE = 'admin';
      setHostname('localhost');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).toBe('https://supremeai-admin.onrender.com');
    });
  });

  describe('getApiBaseUrl', () => {
    it('Firebase hosting (web.app)-এ relative path রিটার্ন করে — CORS preflight এড়াতে', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      setHostname('supremeai-a.web.app');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).toBe('');
    });

    it('admin Firebase hosting-এও relative path রিটার্ন করে', async () => {
      env.VITE_PORTAL_TYPE = 'admin';
      setHostname('supremeai-admin.web.app');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).toBe('');
    });

    it('firebaseapp.com ডোমেইনেও relative path রিটার্ন করে', async () => {
      setHostname('supremeai-a.firebaseapp.com');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).toBe('');
    });

    it('Vercel ডোমেইনে সরাসরি user backend URL রিটার্ন করে', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      setHostname('supremeai-lac.vercel.app');
      const { getApiBaseUrl } = await loadApi();
      expect(getApiBaseUrl()).toBe('https://supremeai-backend.onrender.com');
    });
  });

  describe('getWebSocketBaseUrl', () => {
    it('VITE_WS_BASE_URL সেট থাকলে সেটিই রিটার্ন করে', async () => {
      env.VITE_WS_BASE_URL = 'wss://ws.example.com';
      const { getWebSocketBaseUrl } = await loadApi();
      expect(getWebSocketBaseUrl()).toBe('wss://ws.example.com');
    });

    it('Firebase hosting-এ portal backend-এর direct wss রিটার্ন করে', async () => {
      env.VITE_PORTAL_TYPE = 'admin';
      setHostname('supremeai-admin.web.app');
      const { getWebSocketBaseUrl } = await loadApi();
      expect(getWebSocketBaseUrl()).toBe('wss://supremeai-admin.onrender.com');
    });

    it('https backend URL কে wss-এ রূপান্তর করে', async () => {
      env.VITE_PORTAL_TYPE = 'user';
      setHostname('supremeai-lac.vercel.app');
      const { getWebSocketBaseUrl } = await loadApi();
      expect(getWebSocketBaseUrl()).toBe('wss://supremeai-backend.onrender.com');
    });
  });
});
