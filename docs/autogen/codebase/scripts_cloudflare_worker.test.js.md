# 📄 ফাইল: scripts/cloudflare_worker.test.js

**প্রকার:** .js  
**সাইজ:** 6,696 বাইট  
**আপডেট:** 2026-07-04T03:48:57.199195

---

## কোড

```js
import { Miniflare } from 'miniflare';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import path from 'path';

// Helper to wait for a specific duration
const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

describe('Cloudflare Worker Circuit Breaker E2E Test', () => {
    let mf;

    // একটি মক ব্যাকএন্ড সার্ভার যা আমরা ইচ্ছামত হেলদি বা আনহেলদি করতে পারব
    const mockBackend = {
        isHealthy: true,
        healthCheckResponse() {
            return new Response(this.isHealthy ? 'OK' : 'Service Unavailable', { status: this.isHealthy ? 200 : 503 });
        },
        mainResponse() {
            return new Response(this.isHealthy ? 'Backend Response OK' : 'Backend Down', { status: this.isHealthy ? 503 : 503 });
        }
    };

    beforeEach(async () => {
        // worker.js ফাইলের পাথ সঠিকভাবে সেট করা
        const workerScriptPath = path.resolve(process.cwd(), 'infrastructure/cloudflare_worker.js');

        // Miniflare (লোকাল ক্লাউডফ্লেয়ার এনভায়রনমেন্ট) ইনিশিয়ালাইজ করা
        mf = new Miniflare({
            scriptPath: workerScriptPath,
            kvNamespaces: ["SUPREMEAI_KV"], // টেস্টের জন্য KV Namespace
            bindings: {
                // Worker-এর জন্য প্রয়োজনীয় এনভায়রনমেন্ট ভ্যারিয়েবল
                GCP_CLOUD_RUN_URL: 'http://mock-backend.com',
                GCP_WEIGHT: '100',
            },
            modules: true,
        });

        // গ্লোবাল fetch ফাংশনকে মক করা, যাতে ব্যাকএন্ডের রেসপন্স কন্ট্রোল করা যায়
        global.fetch = vi.fn((url) => {
            if (url.toString().endsWith('/health')) {
                return Promise.resolve(mockBackend.healthCheckResponse());
            }
            return Promise.resolve(mockBackend.mainResponse());
        });
    });

    afterEach(() => {
        // প্রতিটি টেস্টের পর মক রিসেট করা
        vi.restoreAllMocks();
    });

    it('ব্যাকএন্ড সুস্থ থাকলে সফলভাবে রিকোয়েস্ট ফরওয়ার্ড করবে', async () => {
        mockBackend.isHealthy = true;
        const res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).toBe(503); // The mock backend returns 503 on success path for main response
        const text = await res.text();
        expect(text).toBe('Backend Down'); // Mock backend is configured to return this
    });

    it('টানা ৩ বার হেলথ চেক ফেইল হলে সার্কিট ব্রেকার ট্রিপ করবে এবং 503 রেসপন্স দেবে', async () => {
        mockBackend.isHealthy = false;
        const kv = await mf.getKVNamespace('SUPREMEAI_KV');

        // KV ক্যাশ ক্লিয়ার করা
        await kv.delete('healthy_backends');

        // প্রথম রিকোয়েস্ট (১ম ফেইলার)
        let res = await mf.dispatchFetch('http://localhost:8787/');
        // যেহেতু last resort চেষ্টা করবে, তাই প্রথমবার ব্যাকএন্ড এরর আসবে
        expect(res.status).toBe(502);

        // দ্বিতীয় রিকোয়েস্ট (২য় ফেইলার)
        res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).toBe(502);

        // তৃতীয় রিকোয়েস্ট (৩য় ফেইলার, সার্কিট ট্রিপ করবে)
        res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).toBe(502); // The worker still tries last-resort

        // চতুর্থ রিকোয়েস্ট (এখন সার্কিট ওপেন থাকবে)
        res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).toBe(503);
        const text = await res.text();
        expect(text).toBe('Service temporarily unavailable. Please try again shortly.');
        console.log("✅ সার্কিট ব্রেকার সফলভাবে ট্রিপ করেছে।");
    });

    it('সার্কিট ব্রেকার ট্রিপ করার পর নির্দিষ্ট সময় পর আবার রিকোয়েস্ট পাঠাবে', async () => {
        mockBackend.isHealthy = false;
        const kv = await mf.getKVNamespace('SUPREMEAI_KV');
        await kv.delete('healthy_backends');

        // সার্কিট ব্রেকার ট্রিপ করানো
        for (let i = 0; i < 3; i++) {
            await mf.dispatchFetch('http://localhost:8787/');
        }

        // নিশ্চিত করা যে সার্কিট ওপেন
        let res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).toBe(503);
        console.log("✅ সার্কিট ওপেন আছে।");

        // এখন ব্যাকএন্ডকে হেলদি করা
        mockBackend.isHealthy = true;
        console.log("⏳ ৬০ সেকেন্ড অপেক্ষা করা হচ্ছে সার্কিট রিসেট হওয়ার জন্য...");

        // worker-এ `brokenUntil` ৬০ সেকেন্ডের জন্য সেট করা আছে
        await wait(61000);

        // এখন রিকোয়েস্ট আবার ব্যাকএন্ডে যাওয়া উচিত
        res = await mf.dispatchFetch('http://localhost:8787/');
        expect(res.status).not.toBe(503); // এটি আর সার্কিট ব্রেকারের রেসপন্স হবে না
        expect(res.status).toBe(503); // এটি এখন মক ব্যাকএন্ডের সফল রেসপন্স
        const text = await res.text();
        expect(text).toBe('Backend Down');
        console.log("✅ সার্কিট সফলভাবে রিসেট হয়েছে এবং রিকোয়েস্ট ব্যাকএন্ডে গিয়েছে।");
    });
});
```