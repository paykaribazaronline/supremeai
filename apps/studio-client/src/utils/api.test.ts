import { describe, it, expect, beforeEach } from 'vitest';
import { RENDER_BACKENDS, switchActiveBackend, getApiBaseUrl, getWebSocketBaseUrl } from './api';

describe('api.ts', () => {
  beforeEach(() => {
    sessionStorage.clear();
    delete import.meta.env.VITE_API_BASE;
    delete import.meta.env.VITE_API_URL;
    delete import.meta.env.VITE_WS_BASE_URL;
    delete import.meta.env.VITE_PRIMARY_BACKEND;
    delete import.meta.env.VITE_SECONDARY_BACKEND;
    delete import.meta.env.PROD;
  });

  describe('switchActiveBackend', () => {
    it('toggles between backends', () => {
      const first = switchActiveBackend();
      const _second = switchActiveBackend();
      expect(first).not.toBe(_second);
    });

    it('returns same backend after two toggles', () => {
      const first = switchActiveBackend();
      const _second = switchActiveBackend();
      const third = switchActiveBackend();
      expect(first).toBe(third);
    });
  });

  describe('getApiBaseUrl', () => {
    it('returns primary backend in production when no env vars set', () => {
      import.meta.env.PROD = true;
      expect(getApiBaseUrl()).toBe(RENDER_BACKENDS[0]);
    });

    it('returns default primary backend when no env and not production', () => {
      expect(getApiBaseUrl()).toBe(RENDER_BACKENDS[0]);
    });

    it('prefers VITE_API_BASE over VITE_API_URL', () => {
      import.meta.env.VITE_API_BASE = 'https://api.example.com';
      import.meta.env.VITE_API_URL = 'https://fallback.example.com';
      expect(getApiBaseUrl()).toBe('https://api.example.com');
    });

    it('returns VITE_API_URL when VITE_API_BASE is not set', () => {
      import.meta.env.VITE_API_URL = 'https://fallback.example.com';
      expect(getApiBaseUrl()).toBe('https://fallback.example.com');
    });

    it('returns cached backend from sessionStorage', () => {
      sessionStorage.setItem('supremeai_active_backend', 'https://cached.example.com');
      expect(getApiBaseUrl()).toBe('https://cached.example.com');
    });
  });

  describe('getWebSocketBaseUrl', () => {
    it('returns env var when set', () => {
      import.meta.env.VITE_WS_BASE_URL = 'wss://ws.example.com';
      expect(getWebSocketBaseUrl()).toBe('wss://ws.example.com');
    });

    it('converts cached https backend to wss in production', () => {
      import.meta.env.PROD = true;
      sessionStorage.setItem('supremeai_active_backend', 'https://api.example.com');
      expect(getWebSocketBaseUrl()).toBe('wss://api.example.com');
    });

    it('falls back to default render backend wss protocol when no env and not production', () => {
      expect(getWebSocketBaseUrl()).toBe('wss://supremeai-backend.onrender.com');
    });
  });
});
