// Architectural Fix: In-memory circuit breaker state
const circuitBreakerState = {
  brokenUntil: 0, // Timestamp until which the circuit is open
  failureCount: 0,
  lastFailureTime: 0,
};

addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request))
})

addEventListener('scheduled', event => {
  event.waitUntil(checkHealthAndStore())
})

function getBackends() {
  const gcp_url = typeof env !== 'undefined' ? env.GCP_CLOUD_RUN_URL : (typeof GCP_CLOUD_RUN_URL !== 'undefined' ? GCP_CLOUD_RUN_URL : '');

  const gcp_weight = typeof env !== 'undefined' ? env.GCP_WEIGHT : (typeof GCP_WEIGHT !== 'undefined' ? GCP_WEIGHT : '50');

  const gcp_region = typeof env !== 'undefined' ? env.GCP_REGION : (typeof GCP_REGION !== 'undefined' ? GCP_REGION : 'us-central1');

  return [
    {
      name: 'gcp-cloud-run',
      url: gcp_url,
      health: gcp_url ? `${gcp_url}/health` : '',
      region: gcp_region,
      timeout: 5000,
      retries: 3,
      weight: parseInt(gcp_weight || '50', 10),
    }
  ].filter(b => b.url)
}

async function handleRequest(request) {
  const url = new URL(request.url)
  const backends = getBackends()

  if (backends.length === 0) {
    return new Response('No backends configured', { status: 503 })
  }

  // Architectural Fix: Implement Circuit Breaker logic
  if (Date.now() < circuitBreakerState.brokenUntil) {
    // Circuit is open, return emergency response without hitting KV or origin
    console.error('Circuit Breaker is open. Returning emergency fallback response.');
    // This can be a static page from R2, a simple message, or a data-driven response
    return new Response('Service temporarily unavailable. Please try again shortly.', { status: 503, headers: { 'Content-Type': 'text/plain' } });
  }

  const healthyBackends = await getHealthyBackendsFromKV(backends)
  // Architectural Fix #1: Add a fallback to all backends if none are healthy.
  if (healthyBackends.length === 0) {
    console.warn('All backends reported as unhealthy. Attempting to route to a backend as a last resort.');
    const backend = weightedPick(backends); // Fallback to all configured backends
    return forwardRequest(request, backend, url);
  }

  const backend = weightedPick(healthyBackends)
  const target = new URL(url.pathname + url.search, backend.url)

  try {
    const response = await fetch(target, {
      // Architectural Fix #2: Use a separate signal for retries within the worker.
      // This is a placeholder for a more complex retry logic if you were to implement it here.
      // For now, we just use the backend's timeout.
      method: request.method,
      headers: omitWranglerHeaders(request.headers),
      body: request.method !== 'GET' ? await request.text() : null,
      signal: AbortSignal.timeout(backend.timeout),
    })

    return new Response(response.body, {
      status: response.status,
      headers: omitHopByHopHeaders(new Headers(response.headers)),
    })
  } catch (err) {
    return new Response(`Backend ${backend.name} error: ${err.message}`, { status: 502 })
  }
}

async function forwardRequest(request, backend, originalUrl) {
  const target = new URL(originalUrl.pathname + originalUrl.search, backend.url);

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: omitWranglerHeaders(request.headers),
      body: request.method !== 'GET' && request.method !== 'HEAD' ? await request.text() : null,
      signal: AbortSignal.timeout(backend.timeout),
    });

    return new Response(response.body, {
      status: response.status,
      headers: omitHopByHopHeaders(new Headers(response.headers)),
    });
  } catch (err) {
    return new Response(`Last-resort routing to backend ${backend.name} failed: ${err.message}`, { status: 502 });
  }
}

async function getHealthyBackendsFromKV(backends) {
  try {
    const kv = typeof SUPREMEAI_KV !== 'undefined' ? SUPREMEAI_KV : (typeof env !== 'undefined' && env.SUPREMEAI_KV ? env.SUPREMEAI_KV : null);
    if (kv) {
      const cached = await kv.get('healthy_backends');
      if (cached) {
        const healthyNames = JSON.parse(cached);
        const filtered = backends.filter(b => healthyNames.includes(b.name));
        if (filtered.length > 0) {
          return filtered;
        }
      }
    }
  } catch (e) {
    console.error('KV read error:', e);
  }
  // Fallback to direct health check if KV is empty or fails
  const directlyChecked = await getHealthyBackends(backends);
  if (directlyChecked.length === 0 && backends.length > 0) {
    // All backends are unhealthy, trip the circuit breaker
    circuitBreakerState.failureCount++;
    circuitBreakerState.lastFailureTime = Date.now();
    // If it fails 3 times in a row, open the circuit for 1 minute
    if (circuitBreakerState.failureCount >= 3) {
      console.error('All backends unhealthy after direct check. Tripping circuit breaker for 60 seconds.');
      circuitBreakerState.brokenUntil = Date.now() + 60000; // Open for 60 seconds
      circuitBreakerState.failureCount = 0; // Reset count
    }
  }
  return directlyChecked;
}

async function checkHealthAndStore() {
  const backends = getBackends()
  if (backends.length === 0) return

  const healthyBackends = await getHealthyBackends(backends)
  const healthyNames = healthyBackends.map(b => b.name)

  const kv = typeof SUPREMEAI_KV !== 'undefined' ? SUPREMEAI_KV : (typeof env !== 'undefined' && env.SUPREMEAI_KV ? env.SUPREMEAI_KV : null);
  if (kv) {
    // আর্কিটেকচারাল ফিক্স #2: Add a TTL to prevent using stale data if the cron fails
    await kv.put('healthy_backends', JSON.stringify(healthyNames), {
      expirationTtl: 60 // Expire after 60 seconds
    });
    console.log('Saved healthy backends to KV:', healthyNames)
  }
}

async function getHealthyBackends(backends) {
  const results = await Promise.allSettled(
    backends.map(async backend => {
      for (let attempt = 0; attempt < backend.retries; attempt++) {
        try {
          const res = await fetch(backend.health, { signal: AbortSignal.timeout(backend.timeout) })
          if (res.ok) return backend
        } catch (_) {
          if (attempt === backend.retries - 1) return null
          await new Promise(r => setTimeout(r, 200 * (attempt + 1)))
        }
      }
      return null
    })
  )
  return results.filter(r => r.status === 'fulfilled' && r.value).map(r => r.value)
}

function weightedPick(backends) {
  const total = backends.reduce((sum, b) => sum + (b.weight || 0), 0)
  if (total === 0) return backends[Math.floor(Math.random() * backends.length)]
  let r = Math.random() * total
  for (const b of backends) {
    r -= b.weight || 0
    if (r <= 0) return b
  }
  return backends[backends.length - 1]
}

function omitWranglerHeaders(headers) {
  const allowlist = ['content-type', 'authorization', 'x-telegram-bot-token']
  const out = new Headers()
  headers.forEach((v, k) => { if (allowlist.includes(k.toLowerCase()) || !k.startsWith('cf-')) out.set(k, v) })
  return out
}

function omitHopByHopHeaders(headers) {
  const block = new Set(['connection', 'keep-alive', 'proxy-authenticate', 'proxy-authorization', 'te', 'trailer', 'transfer-encoding', 'upgrade'])
  const out = new Headers()
  headers.forEach((v, k) => { if (!block.has(k.toLowerCase())) out.set(k, v) })
  return out
}
