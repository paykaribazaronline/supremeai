const CACHE_NAME = 'supremeai-cache-v1';
const ASSETS_TO_CACHE = [
  '/404.html',
  '/admin-console.html',
  '/favicon.svg',
  '/index.html',
  '/manifest.json',
  '/monitoring-dashboard.html',
  '/performance-dashboard.html',
  '/sw.js',
  '/images/supreme-ai-icon.svg',
  '/images/supreme-ai-logo.svg',
  '/images/supreme-ai-monochrome.svg',
  '/images/supreme-ai-reversed.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(ASSETS_TO_CACHE))
  );
});

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => response || fetch(event.request))
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
