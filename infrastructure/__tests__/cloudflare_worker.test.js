// বাংলা মন্তব্য: Cloudflare edge worker-এর রাউটিং হেল্পারগুলোর রিয়েল ইউনিট টেস্ট।
// লক্ষ্য: ব্যাকএন্ড ডিসকভারি, weighted routing, ও হেডার স্যানিটাইজেশন যেন নীরবে ভেঙে না যায়।
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import worker from '../cloudflare_worker.js';

const { getBackends, handleRequest, weightedPick, omitWranglerHeaders, omitHopByHopHeaders } = worker;

describe('cloudflare_worker: getBackends', () => {
  afterEach(() => {
    // বাংলা মন্তব্য: গ্লোবাল env লিক এড়াতে প্রতিটি টেস্টের পর পরিষ্কার করা হয়।
    delete globalThis.GCP_CLOUD_RUN_URL;
    delete globalThis.GCP_WEIGHT;
    delete globalThis.GCP_REGION;
  });

  it('returns empty list when no backend URL configured', () => {
    expect(getBackends()).toEqual([]);
  });

  it('builds a backend entry with health URL and parsed weight', () => {
    globalThis.GCP_CLOUD_RUN_URL = 'https://api.example.com';
    globalThis.GCP_WEIGHT = '70';
    const backends = getBackends();
    expect(backends).toHaveLength(1);
    expect(backends[0]).toMatchObject({
      name: 'gcp-cloud-run',
      url: 'https://api.example.com',
      health: 'https://api.example.com/health',
      weight: 70,
    });
  });
});

describe('cloudflare_worker: handleRequest', () => {
  afterEach(() => {
    delete globalThis.GCP_CLOUD_RUN_URL;
  });

  it('returns 503 when no backends are configured', async () => {
    const res = await handleRequest(new Request('https://edge.example.com/api/ping'));
    expect(res.status).toBe(503);
    expect(await res.text()).toBe('No backends configured');
  });
});

describe('cloudflare_worker: weightedPick', () => {
  it('always returns the only backend', () => {
    const only = { name: 'a', weight: 50 };
    expect(weightedPick([only])).toBe(only);
  });

  it('falls back to a random pick when all weights are zero', () => {
    const backends = [{ name: 'a', weight: 0 }, { name: 'b', weight: 0 }];
    expect(backends).toContain(weightedPick(backends));
  });

  it('respects weights (weight 0 backend is never picked)', () => {
    const backends = [{ name: 'never', weight: 0 }, { name: 'always', weight: 100 }];
    for (let i = 0; i < 25; i++) {
      expect(weightedPick(backends).name).toBe('always');
    }
  });
});

describe('cloudflare_worker: header sanitization', () => {
  it('omitWranglerHeaders drops cf-* headers but keeps allowlisted ones', () => {
    const headers = new Headers();
    headers.set('authorization', 'Bearer token');
    headers.set('content-type', 'application/json');
    headers.set('cf-connecting-ip', '1.2.3.4');
    const out = omitWranglerHeaders(headers);
    expect(out.get('authorization')).toBe('Bearer token');
    expect(out.get('content-type')).toBe('application/json');
    expect(out.get('cf-connecting-ip')).toBeNull();
  });

  it('omitHopByHopHeaders removes hop-by-hop headers', () => {
    const headers = new Headers();
    headers.set('connection', 'keep-alive');
    headers.set('transfer-encoding', 'chunked');
    headers.set('x-custom', 'keep-me');
    const out = omitHopByHopHeaders(headers);
    expect(out.get('connection')).toBeNull();
    expect(out.get('transfer-encoding')).toBeNull();
    expect(out.get('x-custom')).toBe('keep-me');
  });
});
