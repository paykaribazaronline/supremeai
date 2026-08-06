// বাংলা মন্তব্য: রোল-ভিত্তিক ব্যাকএন্ড ইউআরএল সেপারেশন (User Backend vs Admin Backend)
export const RENDER_BACKENDS = [
  import.meta.env.VITE_PRIMARY_BACKEND || 'https://supremeai-backend.onrender.com', // Primary User Backend (srv-d9d3n58js32c738n79k0)
  import.meta.env.VITE_SECONDARY_BACKEND || 'https://supremeai-admin.onrender.com' // Admin Backend (srv-d9fg48bh523c73f63bb0)
];

export const switchActiveBackend = (): string => {
  const current = sessionStorage.getItem('supremeai_active_backend') || RENDER_BACKENDS[0];
  const next = current === RENDER_BACKENDS[0] ? RENDER_BACKENDS[1] : RENDER_BACKENDS[0];
  sessionStorage.setItem('supremeai_active_backend', next);
  console.error(`[Failover] Switched backend to: ${next}`);
  return next;
};

export const getApiBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    const url = import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_URL;
    if (!url && import.meta.env.PROD) throw new Error("API URL missing in production");
    return url || RENDER_BACKENDS[0];
  }

  // Build-time explicit env override
  if (import.meta.env.VITE_API_BASE) return import.meta.env.VITE_API_BASE;
  if (import.meta.env.VITE_API_URL) return import.meta.env.VITE_API_URL;

  // Check portal build type (admin vs user)
  if (import.meta.env.VITE_PORTAL_TYPE === 'admin' || window.location.hostname.includes('admin')) {
    return 'https://supremeai-admin.onrender.com';
  }

  const cached = sessionStorage.getItem('supremeai_active_backend');
  const isSafeCachedBackend =
    !!cached && /^https:\/\//.test(cached) && !/localhost|127\.0\.0\.1/.test(cached);
  if (isSafeCachedBackend) {
    return cached as string;
  }
  if (cached) {
    sessionStorage.removeItem('supremeai_active_backend');
  }

  return RENDER_BACKENDS[0];
};

export const getWebSocketBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    const url = import.meta.env.VITE_WS_BASE_URL;
    if (!url && import.meta.env.PROD) throw new Error("WS URL missing in production");
    return url || RENDER_BACKENDS[0].replace(/^http/, 'ws');
  }

  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';

  // If in production, use the active backend domain for WebSockets
  if (import.meta.env.PROD) {
    const apiBase = getApiBaseUrl();
    if (apiBase.startsWith('http')) {
      return apiBase.replace(/^http/, 'ws');
    }
  }

  return `${protocol}//${window.location.host}`;
};
