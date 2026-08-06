// বাংলা মন্তব্য: ফায়ারবেস হোস্টিং রিরাইট ও রেন্ডার ব্যাকএন্ড সিঙ্ক (User Backend vs Admin Backend)
export const RENDER_BACKENDS = [
  import.meta.env.VITE_PRIMARY_BACKEND || 'https://supremeai-backend.onrender.com', // Primary User Backend
  import.meta.env.VITE_SECONDARY_BACKEND || 'https://supremeai-admin.onrender.com' // Admin Backend
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

  // বাংলা মন্তব্য: অ্যাডমিন ডোমেইন বা অ্যাডমিন পোর্টেলে রেন্ডার ব্যাকএন্ড ব্যবহার
  if (import.meta.env.VITE_PORTAL_TYPE === 'admin' || window.location.hostname.includes('admin')) {
    return 'https://supremeai-admin.onrender.com';
  }

  // বাংলা মন্তব্য: ফায়ারবেস হোস্টিং ডোমেইনে (web.app) থাকলে রিলেটিভ পাথ ('') ব্যবহার করা যেন firebase.json রিরাইট প্রক্সি কাজ করে
  if (window.location.hostname.includes('web.app') || window.location.hostname.includes('firebaseapp.com')) {
    return '';
  }

  if (import.meta.env.VITE_API_BASE) return import.meta.env.VITE_API_BASE;
  if (import.meta.env.VITE_API_URL) return import.meta.env.VITE_API_URL;

  // বাংলা মন্তব্য: পুরনো লোকালহোল্ড সেশন ক্যাশ মুছে দেওয়া
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
  // বাংলা মন্তব্য: প্রোডাকশন ক্লাউড ডোমেইনের জন্য WSS এন্ডপয়েন্ট (Firebase Web App -> Render WSS Backend)
  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const apiBase = getApiBaseUrl();
  if (apiBase === '' && typeof window !== 'undefined') {
    // ফায়ারবেস হোস্টিং থেকে রেন্ডার WSS ব্যাকএন্ডে ডিরেক্ট সকেট কানেকশন
    const isAdmin = window.location.hostname.includes('admin');
    return isAdmin ? 'wss://supremeai-admin.onrender.com' : 'wss://supremeai-backend.onrender.com';
  }

  if (apiBase.startsWith('https://')) {
    return apiBase.replace(/^https:\/\//, 'wss://');
  }
  if (apiBase.startsWith('http://')) {
    return apiBase.replace(/^http:\/\//, 'ws://');
  }

  const protocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = typeof window !== 'undefined' ? window.location.host : 'localhost:8000';
  return `${protocol}//${host}`;
};
