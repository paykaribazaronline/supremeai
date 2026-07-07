const CACHE_NAME = 'supremeai-pwa-cache-v2';

// বাংলা মন্তব্য: যেসব রিসোর্স ক্যাশ করা হবে — শুধু নিশ্চিত ফাইলগুলো রাখা হয়েছে
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      // বাংলা মন্তব্য: addAll ব্যবহার না করে একটা একটা করে ক্যাশ করা হচ্ছে — কোনো একটা ফেইল করলেও বাকিগুলো ক্যাশ হবে
      const results = await Promise.allSettled(
        PRECACHE_URLS.map((url) =>
          fetch(url)
            .then((res) => {
              if (res.ok) return cache.put(url, res);
              console.debug(`[SW] Skipping cache for ${url}: ${res.status}`);
            })
            .catch((err) => console.debug(`[SW] Failed to fetch ${url}:`, err))
        )
      );
      console.debug('[SW] Precache results:', results.map((r) => r.status));
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.filter((name) => name !== CACHE_NAME).map((name) => caches.delete(name))
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') {
    // For POST requests, ideally we'd queue them using Background Sync API
    return;
  }

  // বাংলা মন্তব্য: API রেসপন্স ক্যাশ করলে stale ডেটা দেখাবে — শুধু স্ট্যাটিক অ্যাসেট ক্যাশ করা হবে
  const url = new URL(event.request.url);
  if (url.pathname.startsWith('/admin-api/') || url.pathname.startsWith('/api/')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        // Cache successful GET responses
        if (response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseClone);
          });
        }
        return response;
      })
      .catch(() => {
        // Fallback to cache on network failure
        return caches.match(event.request).then((response) => {
          if (response) {
            return response;
          }
          // বাংলা মন্তব্য: HTML রিকোয়েস্ট হলে ক্যাশ করা index.html ফেরত দেওয়া হবে (SPA ফলব্যাক)
          if (event.request.headers.get('accept')?.includes('text/html')) {
            return caches.match('/index.html');
          }
        });
      })
  );
});

// Background Sync
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-offline-actions') {
    event.waitUntil(syncOfflineActions());
  }
});

async function syncOfflineActions() {
  console.log('Background Sync: Triggering offline sync to backend');
  try {
    const response = await fetch('/api/offline/sync', { method: 'POST' });
    if (!response.ok) {
      throw new Error('Sync failed');
    }
  } catch (error) {
    console.error('Background sync failed:', error);
    throw error;
  }
}
