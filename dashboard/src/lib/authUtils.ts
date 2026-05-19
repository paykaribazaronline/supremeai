import { AuthUser } from '../types';

let _inMemoryToken: string | null = null;
let _inMemoryRefreshToken: string | null = null;

const AUTH_TOKEN_KEY = 'supremeai_token';
const FIREBASE_USER_KEY = 'supremeai_user';
const REFRESH_TOKEN_KEY = 'supremeai_refresh_token';

// Simple obfuscation to prevent plain-text inspection in browser storage
function obfuscate(value: string): string {
  if (!value) return '';
  try {
    return btoa(encodeURIComponent(value).split('').map((char, index) => {
      return String.fromCharCode(char.charCodeAt(0) ^ (index % 5 + 42));
    }).join(''));
  } catch (e) {
    return value;
  }
}

function deobfuscate(value: string): string {
  if (!value) return '';
  try {
    const decoded = atob(value);
    return decodeURIComponent(decoded.split('').map((char, index) => {
      return String.fromCharCode(char.charCodeAt(0) ^ (index % 5 + 42));
    }).join(''));
  } catch (e) {
    return value;
  }
}

// ─── Emulator stub data for /api/admin/* endpoints ───────────────────────────
// When USE_EMULATOR=true or VITE_API_URL is empty, all /api/admin/* and
// /api/self-healing/* fetch calls are intercepted here and return live JSON
// stub responses.  This eliminates 60+ "Optional endpoint unavailable" console
// errors and makes every admin page render without a backend running.

function emulatorStub(url: string, fullUrl: string): Response {
  const body = (data: unknown) => JSON.stringify({ success: true, data });

  // ── Generic health endpoints ───────────────────────────────────────────
  if (url === '/api/self-healing/status') {
    return jsonOk({
      enabled: true,
      lastCheck: new Date().toISOString(),
      totalHealingEvents: 0,
      successfulHealingEvents: 0,
      currentHealthStatus: 'healthy'
    });
  }

  if (url.startsWith('/api/self-healing/detect')) {
    return jsonOk({
      suggestion: 'No known fix found in local knowledge base. Logged for next session.',
      confidence: 0.4
    });
  }

  if (url.startsWith('/api/self-healing/health-check/now')) {
    return jsonOk({ ok: true, summary: { active: 0, inactive: 0 } });
  }

  if (url === '/api/self-healing/history?limit=50') {
    return jsonOk([]);
  }

  if (url === '/api/self-healing/history?limit=50') {
    return jsonOk([]);
  }

  // ── Provider test / rankings / discover ─────────────────────────────────
  if (url === '/api/admin/providers/test-key') {
    return jsonOk({ ok: true, message: 'Provider test key stub — emulator mode' });
  }

  if (url === '/api/admin/providers/rankings') {
    return jsonOk({ rankings: {} });
  }

  if (url === '/api/admin/providers/discover') {
    return jsonOk({ suggestions: [] });
  }

  if (url.startsWith('/api/admin/providers') && url.endsWith('/roles')) {
    return jsonOk({ roles: ['user', 'admin'] });
  }

  // ── User management ────────────────────────────────────────────────────
  if (url === '/api/admin/users/configured') {
    return jsonOk([]);
  }

  if (url === '/api/admin/users' || url.startsWith('/api/admin/users?') || url.startsWith('/api/users?')) {
    return jsonOk([]);
  }

  if (url.match(/^\/api\/admin\/users\/[^/]+\/tier$/)) {
    return jsonOk({ ok: true });
  }

  if (url === '/api/admin/users/create') {
    return jsonOk({ ok: true, uid: 'emulator-user-' + Date.now() });
  }

  if (url.match(/^\/api\/admin\/users\/[^/]+\/deactivate$/)) {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/users\/[^/]+\/reactivate$/)) {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/users\/[^/]+$/)) {
    return jsonOk({ user: { uid: 'emulator', email: 'emulator@dev.local' } });
  }

  // ── System configuration ───────────────────────────────────────────────
  if (url.startsWith('/api/admin/config')) {
    return jsonOk({
      appName: 'SupremeAI Dev',
      maintenanceMode: false,
      allowRegistration: false,
      features: {},
      limits: {},
      branding: {},
      ai: {},
      tierQuotas: { FREE: 100, PRO: 1000, ADMIN: -1 },
      tierMaxApis: { FREE: 1, PRO: 5, ADMIN: 20 },
      tierMaxSimulatorInstalls: { FREE: 1, PRO: 3, ADMIN: 10 }
    });
  }

  // ── Dashboard contract ─────────────────────────────────────────────────
  if (url === '/api/admin/dashboard/contract') {
    return jsonOk({
      stats: {
        systemHealthScore: 100,
        systemHealthStatus: 'healthy',
        systemHealthReason: 'Emulator mode — all systems nominal'
      }
    });
  }

  // ── Security / cyber ──────────────────────────────────────────────────
  if (url === '/api/admin/security/cyber/audit') {
    return jsonOk({ reportId: 'emulator-audit-' + Date.now(), findings: [], timestamp: new Date().toISOString() });
  }

  if (url === '/api/admin/security/cyber/learn') {
    return jsonOk({ ok: true, message: 'Learning cycle started in emulator mode' });
  }

  if (url === '/api/admin/security/cyber/config') {
    return jsonOk({ autonomousLearningEnabled: false, autonomousAuditEnabled: false });
  }

  if (url === '/api/admin/security/cyber/skills') {
    return jsonOk([]);
  }

  if (url === '/api/admin/security/cyber/protections') {
    return jsonOk([]);
  }

  // ── Infra / advice ─────────────────────────────────────────────────────
  if (url === '/api/admin/infrastructure/advice') {
    return jsonOk([]);
  }

  if (url === '/api/admin/infrastructure/generate-advice') {
    return jsonOk({ ok: true });
  }

  if (url === '/api/admin/infra/advice') {
    return jsonOk([]);
  }

  // ── Quotas ──────────────────────────────────────────────────────────────
  if (url === '/api/admin/quotas/config') {
    return jsonOk({
      tierQuotas: { FREE: 100, PRO: 1000, ADMIN: -1 },
      tierMaxApis: { FREE: 1, PRO: 5, ADMIN: 20 },
      tierMaxSimulatorInstalls: { FREE: 1, PRO: 3, ADMIN: 10 }
    });
  }

  if (url === '/api/admin/quotas/usage') {
    return jsonOk([]);
  }

  if (url.match(/^\/api\/admin\/quotas\/reset\/.+$/)) {
    return jsonOk({ ok: true });
  }

  // ── Logs ────────────────────────────────────────────────────────────────
  if (url.startsWith('/api/admin/logs')) {
    return jsonOk({ logs: [] });
  }

  if (url === '/api/admin/logs/clear') {
    return jsonOk({ ok: true });
  }

  // ── Learning ───────────────────────────────────────────────────────────
  if (url === '/api/admin/learning/status') {
    return jsonOk({
      isRunning: true,
      mode: 'AUTO',
      lastTrigger: new Date().toISOString(),
      intervalMinutes: 30,
      priority: 'HIGH',
      confidence: 0.9
    });
  }

  if (url === '/api/admin/learning/mode') {
    return jsonOk({ ok: true, currentMode: 'AUTO' });
  }

  if (url === '/api/admin/learning/interval') {
    return jsonOk({ ok: true, interval: 30 });
  }

  if (url === '/api/admin/learning/trigger') {
    return jsonOk({ ok: true });
  }

  if (url === '/api/admin/learning/resume') {
    return jsonOk({ ok: true });
  }

  if (url === '/api/admin/learning/emergency-pause') {
    return jsonOk({ ok: true });
  }

  // ── Knowledge ──────────────────────────────────────────────────────────
  if (url === '/api/admin/knowledge/domains/configured') {
    return jsonOk([]);
  }

  if (url === '/api/admin/knowledge/domains' || url.startsWith('/api/admin/knowledge/domains?')) {
    return jsonOk([]);
  }

  if (url === '/api/admin/knowledge/recommendations') {
    return jsonOk([]);
  }

  if (url === '/api/admin/knowledge/snapshot') {
    return jsonOk({
      totalKnowledgeNodes: 0,
      topLearningDomains: [],
      lastDiscoveryTime: null,
      discoveryEfficiency: 'n/a'
    });
  }

  if (url === '/api/admin/knowledge/recent') {
    return jsonOk([]);
  }

  if (url.startsWith('/api/admin/knowledge/domains/') && url.endsWith('/process')) {
    return jsonOk({ factsDiscovered: 0 });
  }

  if (url.match(/^\/api\/admin\/knowledge\/recommendations\/[^/]+\/approve/)) {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/knowledge\/recommendations\/[^/]+\/decline/)) {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/knowledge\/domains\/[^/]+$/)) {
    return jsonOk({ id: 'emulator-domain', name: 'Emulator' });
  }

  // ── Learning sources ───────────────────────────────────────────────────
  if (url === '/api/admin/learning/sources') {
    return jsonOk([]);
  }

  if (url === '/api/admin/learning/sources/detect-focus') {
    return jsonOk([]);
  }

  if (url.match(/^\/api\/admin\/learning\/sources\/[^/]+$/)) {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/learning\/sources\/[^/]+\/toggle$/)) {
    return jsonOk({ ok: true });
  }

  // ── System work rules ──────────────────────────────────────────────────
  if (url === '/api/admin/system-work-rules') {
    return jsonOk([]);
  }

  if (url === '/api/admin/system-work-rules/seed/defaults') {
    return jsonOk({ ok: true });
  }

  if (url.match(/^\/api\/admin\/system-work-rules\/[^/]+$/)) {
    return jsonOk([]);
  }

  if (url.match(/^\/api\/admin\/system-work-rules\/sync\/[^/]+$/)) {
    return jsonOk({ lastSyncStatus: 'DEV_MODE', lastSyncedAt: new Date().toISOString() });
  }

  // ── VPN ────────────────────────────────────────────────────────────────
  if (url.startsWith('/api/admin/vpn')) {
    return jsonOk([]);
  }

  // ── Backup ─────────────────────────────────────────────────────────────
  if (url === '/api/admin/backup/list') {
    return jsonOk([]);
  }

  if (url === '/api/admin/backup/trigger') {
    return jsonOk({ ok: true, jobId: 'emulator-backup-' + Date.now() });
  }

  if (url.match(/^\/api\/admin\/backup\/[^/]+$/)) {
    return jsonOk({ ok: true });
  }

  // ── Control / King mode ────────────────────────────────────────────────
  if (url.startsWith('/api/admin/control')) {
    return jsonOk({
      mode: 'AUTO',
      isRunning: true,
      pendingCount: 0,
      lastModeChange: '',
      uptime: ''
    });
  }

  // ── Rules / Plans / Chat ───────────────────────────────────────────────
  if (url === '/api/admin/rules') return jsonOk([]);
  if (url === '/api/admin/plans') return jsonOk([]);
  if (url === '/api/admin/chat/actions/pending') return jsonOk([]);

  // ── Improvements / Suggestion Service ─────────────────────────────────
  if (url.includes('/api/admin/improvements')) {
    return jsonOk({ pending: [], approved: [], rejected: [] });
  }

  // ── System metrics ─────────────────────────────────────────────────────
  if (url === '/api/system/metrics/resources') {
    return jsonOk({
      memoryUsed: 0, memoryMax: 0, cpuLoad: 0,
      availableProcessors: 1, dbActiveConnections: 0,
      redisStatus: 'dev'
    });
  }

  // ── AI agents / reporting ──────────────────────────────────────────────
  if (url === '/api/ai-agents/stats') {
    return jsonOk({
      totalAgents: 0, activeAgents: 0,
      idleAgents: 0, failedAgents: 0,
      avgTaskCompletionTime: 0
    });
  }

  if (url === '/api/apikeys/reports') return jsonOk({});
  if (url === '/api/activity/summary') return jsonOk([]);

  // ── Health check ───────────────────────────────────────────────────────
  if (url === '/healthCheck' || url === '/api/health') {
    return jsonOk({ status: 'ok', mode: 'emulator', timestamp: new Date().toISOString() });
  }

  // ── Smart Providers (AI models) ────────────────────────────────────────
  if (url.startsWith('/getConfiguredProviders') || url === '/api/admin/providers/configured') {
    return jsonOk({
      providers: [
        { id: 'firebase-gemini', name: 'Firebase Gemini API', type: 'firebase', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: true, models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro-vision'], roles: ['general_chat', 'reasoning', 'multimodal', 'vision'], source: 'env' },
        { id: 'vertex-ai', name: 'Vertex AI (Google Cloud)', type: 'vertex', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: true, models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'claude-3-5-sonnet', 'text-embedding-004'], roles: ['coding', 'reasoning', 'security', 'embedding'], source: 'env' },
        { id: 'cloudrun-deepseek-pro', name: 'DeepSeek Pro (Cloud Run)', type: 'ollama', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: false, endpoint: 'https://supreme-ai-deepseek-pro-lhlwyikwlq-uc.a.run.app', models: ['deepseek-coder-v2', 'deepseek-r1'], roles: ['coding', 'reasoning'], source: 'cloudrun' },
        { id: 'cloudrun-llama-3-1', name: 'Llama 3.1 (Cloud Run)', type: 'ollama', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: false, endpoint: 'https://supreme-ai-llama-3-1-lhlwyikwlq-uc.a.run.app', models: ['llama-3.1-8b', 'llama-3.1-70b'], roles: ['general_chat', 'reasoning', 'coding'], source: 'cloudrun' },
        { id: 'cloudrun-phi-3', name: 'Phi-3 (Cloud Run)', type: 'ollama', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: false, endpoint: 'https://supreme-ai-phi-3-lhlwyikwlq-uc.a.run.app', models: ['phi-3-mini', 'phi-3-medium'], roles: ['fast_chat', 'general_chat'], source: 'cloudrun' },
        { id: 'cloudrun-qwen-coder', name: 'Qwen Coder (Cloud Run)', type: 'ollama', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: false, endpoint: 'https://supreme-ai-qwen-coder-lhlwyikwlq-uc.a.run.app', models: ['qwen-2.5-coder-7b', 'qwen-2.5-7b'], roles: ['coding', 'reasoning'], source: 'cloudrun' },
        { id: 'cloudrun-nomic-embed', name: 'Nomic Embed (Cloud Run)', type: 'ollama', deploymentSource: 'gcloud', status: 'active', apiKeyConfigured: false, endpoint: 'https://supreme-ai-nomic-embed-lhlwyikwlq-uc.a.run.app', models: ['nomic-embed-text-v1.5'], roles: ['embedding'], source: 'cloudrun' },
      ],
      total: 7,
      active: 7,
      sources: ['env', 'cloudrun'],
    });
  }

  if (url.startsWith('/getProviderHealthStats') || url === '/api/admin/providers/health-stats') {
    return jsonOk({ total: 7, active: 7, error: 0, dead: 0, bySource: { env: 2, cloudrun: 5 } });
  }

  // ── Projects (Deployment tab) ──────────────────────────────────────────
  if (url === '/projects' || url === '/api/projects') {
    return jsonOk([]);
  }

  // ── Chat ────────────────────────────────────────────────────────────────
  if (url === '/chat/send' || url === '/api/chat/send') {
    return jsonOk({ success: true, message: 'Received in emulator', agent_name: 'Emulator', confidence: 1.0, intent: 'NORMAL' });
  }

  // ── Fallback: return 200 emulator payload ───────────────────────────────
  return new Response(body({ emulator: true, url }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', 'X-Emulator-Stub': 'true' },
  });
}

function jsonOk(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, data }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}


export const authUtils = {
  getToken(): string | null {
    if (_inMemoryToken) return _inMemoryToken;
    const raw = sessionStorage.getItem(AUTH_TOKEN_KEY);
    if (raw) {
      const token = deobfuscate(raw);
      _inMemoryToken = token;
      return token;
    }
    return 'GUEST_MODE';
  },

  getRefreshToken(): string | null {
    if (_inMemoryRefreshToken) return _inMemoryRefreshToken;
    const raw = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    if (raw) {
      const token = deobfuscate(raw);
      _inMemoryRefreshToken = token;
      return token;
    }
    return null;
  },

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'admin' || user?.tier === 'admin' || user?.tier === 'ADMIN';
  },

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && token !== 'GUEST_MODE';
  },

  isGuest(): boolean {
    return this.getToken() === 'GUEST_MODE';
  },

  clearAuth() {
    _inMemoryToken = null;
    _inMemoryRefreshToken = null;
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(FIREBASE_USER_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    
    // Clear legacy local storage tokens for security cleanup
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(FIREBASE_USER_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  async logout() {
    try {
      const { firebaseSignOutFn } = await import('./firebase');
      await firebaseSignOutFn();
    } catch (e) {
      console.warn('Firebase logout failed', e);
    }
    this.clearAuth();
    window.location.href = '/login';
  },

  getCurrentUser(): AuthUser | null {
    const raw = sessionStorage.getItem(FIREBASE_USER_KEY);
    if (!raw) return null;
    try {
      const userStr = deobfuscate(raw);
      return JSON.parse(userStr);
    } catch (e) {
      return null;
    }
  },

  setToken(token: string) {
    _inMemoryToken = token;
    sessionStorage.setItem(AUTH_TOKEN_KEY, obfuscate(token));
  },

  setRefreshToken(token: string) {
    _inMemoryRefreshToken = token;
    sessionStorage.setItem(REFRESH_TOKEN_KEY, obfuscate(token));
  },

  setCurrentUser(user: any) {
    const userStr = JSON.stringify(user);
    sessionStorage.setItem(FIREBASE_USER_KEY, obfuscate(userStr));
  },

  getAuthHeaders(): HeadersInit {
    const token = this.getToken();
    if (token && token !== 'GUEST_MODE') {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  },

  async fetchWithAuth(url: string, options: any = {}) {
    const token = this.getToken();
    const API_BASE = import.meta.env.VITE_API_URL || '';
    const fullUrl = url.startsWith('/') && API_BASE ? `${API_BASE}${url}` : url;

    // ── Emulator / Dev Mode: stub all /api/admin/* and /api/self-healing/* calls ──
    // This prevents 50+ console errors when the backend is not running locally.
  const isEmulator = import.meta.env.VITE_USE_EMULATOR === 'true' || !API_BASE;
    if (isEmulator && (
      url.startsWith('/api/admin/') ||
      url.startsWith('/api/self-healing/') ||
      url.startsWith('/api/knowledge/') ||
      url.startsWith('/getConfiguredProviders') ||
      url.startsWith('/getProviderHealthStats') ||
      url === '/healthCheck' ||
      url === '/api/health'
    )) {
      return emulatorStub(url, fullUrl);
    }

    const headers = new Headers(options.headers || {});
    if (token && token !== 'GUEST_MODE') {
      headers.set('Authorization', `Bearer ${token}`);
    } else {
      // In Guest mode, we still send GUEST_MODE header to tell the backend
      headers.set('X-Guest-Access', 'true');
    }

    try {
      let response = await fetch(fullUrl, { ...options, headers });

      // Handle Token Expiry
      if (response.status === 401 && token !== 'GUEST_MODE') {
        try {
          const { refreshAccessToken } = await import('./firebase');
          const newToken = await refreshAccessToken();
          headers.set('Authorization', `Bearer ${newToken}`);
          response = await fetch(fullUrl, { ...options, headers });
        } catch (err) {
          console.error('Session expired, logging out...');
          this.clearAuth();
          window.location.href = '/';
        }
      }

      return response;
    } catch (err) {
      // Emulator mode: expected for many admin routes during development
      if (!fullUrl.includes('/health') && !fullUrl.includes('providers')) {
        console.debug('[fetchWithAuth] Optional endpoint unavailable:', fullUrl);
      }
      return new Response(null, { status: 503, statusText: 'Emulator' });
    }
  }
};

export const fetchWithAuth = authUtils.fetchWithAuth.bind(authUtils);
export const getAuthHeaders = authUtils.getAuthHeaders.bind(authUtils);
export default authUtils;

