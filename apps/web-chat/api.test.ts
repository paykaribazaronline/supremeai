import { describe, it, expect } from 'vitest';
import { api } from './api';

describe('api', () => {
    it('fetchQuota should return 87', async () => {
        const quota = await api.fetchQuota() as { remaining: number };
        expect(quota.remaining).toBe(87);
    });

    it('executeTask should handle errors gracefully', async () => {
        // Simple mock test since real fetch would fail
        const result = await api.executeTask('Hello', []);
        expect(result).toHaveProperty('error');
    });
});
