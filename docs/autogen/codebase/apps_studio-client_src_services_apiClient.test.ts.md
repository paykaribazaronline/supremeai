# 📄 ফাইল: apps/studio-client/src/services/apiClient.test.ts

**প্রকার:** .ts  
**সাইজ:** 1,814 বাইট  
**আপডেট:** 2026-07-08T01:44:17.719661

---

## কোড

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiClient, setApiConcurrency } from './apiClient';
import { useAdminStore } from '../store/adminStore';

// Mock getApiBaseUrl
vi.mock('../utils/api', () => ({
  getApiBaseUrl: () => 'http://localhost:8000'
}));

// Mock useAdminStore
vi.mock('../store/adminStore', () => ({
  useAdminStore: {
    getState: vi.fn(() => ({
      adminAuthenticated: true,
      handleAdminLogout: vi.fn(),
    }))
  }
}));

describe('apiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn();
    setApiConcurrency(3);
  });

  it('should include credentials and process successful response', async () => {
    const mockResponse = { data: 'success' };
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const result = await apiClient.get('/test');
    
    expect(global.fetch).toHaveBeenCalledWith('http://localhost:8000/test', expect.objectContaining({
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      method: 'GET'
    }));
    expect(result).toEqual(mockResponse);
  });

  it('should throw ApiError with status 401 on unauthorized access', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ detail: 'Unauthorized' }),
    });

    await expect(apiClient.get('/secure')).rejects.toThrow('Unauthorized');
  });

  it('should throw ApiError with status 429 on rate limit', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 429,
      json: async () => ({ detail: 'Too Many Requests' }),
    });

    await expect(apiClient.get('/rate-limit')).rejects.toThrow(/Rate limit exceeded/);
  });
});

```