// বাংলা মন্তব্য: Portal-ভিত্তিক একক backend নির্ধারণ — কোনো cross-portal failover নেই।
// Admin build (VITE_PORTAL_TYPE=admin) শুধু Admin backend-এ কথা বলে, User build শুধু User backend-এ।
// Firebase hosting-এ relative path ('') রেখে CORS preflight সম্পূর্ণ এড়ানো হয় (firebase.json rewrite proxy)।

/** Admin portal-এর canonical backend URL (build-time resolved) */
export const ADMIN_BACKEND_URL: string =
  import.meta.env.VITE_ADMIN_BACKEND || 'https://supremeai-admin.onrender.com';

/** User portal-এর canonical backend URL (build-time resolved) */
export const USER_BACKEND_URL: string =
  import.meta.env.VITE_USER_BACKEND ||
  import.meta.env.VITE_API_BASE ||
  import.meta.env.VITE_API_URL ||
  'https://supremeai-backend.onrender.com';

/**
 * বর্তমান portal-এর canonical backend URL — heartbeat ও অন্যান্য সার্ভিস এটিই ব্যবহার করে।
 * বাংলা মন্তব্য: runtime hostname sniffing নয়, build-time VITE_PORTAL_TYPE দিয়ে নির্ধারিত।
 */
export const BACKEND_URL: string =
  import.meta.env.VITE_PORTAL_TYPE === 'admin' ? ADMIN_BACKEND_URL : USER_BACKEND_URL;

/**
 * @deprecated পুরনো failover array — শুধু backward compatibility-র জন্য readonly রাখা হয়েছে।
 * নতুন কোডে কখনোই এটি ব্যবহার করবেন না; সবসময় `BACKEND_URL` ব্যবহার করুন।
 * বাংলা মন্তব্য: index দিয়ে অন্য portal-এর backend বেছে নিলে আইসোলেশন ভেঙে যাবে।
 */
export const RENDER_BACKENDS: readonly string[] = [USER_BACKEND_URL, ADMIN_BACKEND_URL] as const;

// বাংলা মন্তব্য: switchActiveBackend() সরানো হয়েছে — user→admin (বা উল্টো) failover
// আর্কিটেকচারাল আইসোলেশন ভাঙত এবং CORS/RBAC ঝুঁকি তৈরি করত।
// নেটওয়ার্ক ব্যর্থতা বা 502/503/504-এ apiClient একই URL-এ backoff retry করে।

export const getApiBaseUrl = (): string => {
  if (typeof window === 'undefined') {
    // বাংলা মন্তব্য: SSR/Node.js কনটেক্সটে সরাসরি backend URL
    if (!BACKEND_URL && import.meta.env.PROD) throw new Error('API URL missing in production');
    return BACKEND_URL;
  }

  // বাংলা মন্তব্য: Firebase hosting-এ (web.app/firebaseapp.com) relative path ব্যবহার।
  // ব্রাউজার একই origin-এ request করে, Firebase server-side proxy করে Render-এ।
  // CORS preflight সম্পূর্ণ বাদ — Render free tier-এ সবচেয়ে নির্ভরযোগ্য পদ্ধতি।
  const hostname = window.location.hostname;
  if (hostname.includes('web.app') || hostname.includes('firebaseapp.com')) {
    return '';
  }

  // বাংলা মন্তব্য: Vercel বা local dev-এ সরাসরি portal-নির্দিষ্ট backend URL
  return BACKEND_URL;
};

export const getWebSocketBaseUrl = (): string => {
  // বাংলা মন্তব্য: এক্সপ্লিসিট override সবার আগে
  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const apiBase = getApiBaseUrl();

  // বাংলা মন্তব্য: Firebase hosting থেকে direct WSS — WebSocket firebase.json rewrite দিয়ে proxy হয় না
  if (apiBase === '') {
    return BACKEND_URL.replace(/^https?:\/\//, 'wss://');
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
