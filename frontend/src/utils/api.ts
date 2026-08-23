// 🔧 DYNAMIC CONFIG: No fallback hardcoded URLs - Fail-Fast in production

// 🔬 Evolution v3.0: Enhanced API client with Retry + Circuit Breaker
// বাংলা মন্ত্য: Portal-ভিত্তিক একক backend নির্ধারণ — কোনো cross-portal failover নয়।

/**
 * 🔬 Circuit Breaker States for Frontend
 * CLOSED → Normal, OPEN → Failing, HALF_OPEN → Testing
 */
type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

interface CircuitBreakerConfig {
  name: string;
  failureThreshold: number;   // Failures before OPEN
  recoveryTimeoutMs: number;  // ms before HALF_OPEN attempt
}

class FrontendCircuitBreaker {
  private state: CircuitState = 'CLOSED';
  private failures = 0;
  private lastFailureTime = 0;
  private readonly config: CircuitBreakerConfig;

  constructor(config: CircuitBreakerConfig) {
    this.config = config;
  }

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    // Check if we should try recovery
    if (this.state === 'OPEN') {
      const elapsed = Date.now() - this.lastFailureTime;
      if (elapsed >= this.config.recoveryTimeoutMs) {
        this.state = 'HALF_OPEN';
      } else {
        throw new Error(`Circuit '${this.config.name}' is OPEN. Retry in ~${Math.ceil((this.config.recoveryTimeoutMs - elapsed) / 1000)}s`);
      }
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  private onSuccess(): void {
    this.failures = 0;
    if (this.state === 'HALF_OPEN') {
      this.state = 'CLOSED';
    }
  }

  private onFailure(): void {
    this.failures++;
    this.lastFailureTime = Date.now();
    if (this.failures >= this.config.failureThreshold) {
      this.state = 'OPEN';
      console.warn(`⚡ Circuit '${this.config.name}' opened after ${this.failures} failures`);
    }
  }

  getState(): CircuitState { return this.state; }
  getRecoveryTimeMs(): number {
    if (this.state !== 'OPEN') return 0;
    return Math.max(0, this.config.recoveryTimeoutMs - (Date.now() - this.lastFailureTime));
  }
}

// Pre-configured circuits
const apiCircuit = new FrontendCircuitBreaker({
  name: 'api_backend',
  failureThreshold: parseInt(import.meta.env.VITE_CIRCUIT_FAILURE_THRESHOLD || '5'),
  recoveryTimeoutMs: parseInt(import.meta.env.VITE_CIRCUIT_RECOVERY_MS || '30000'),
});

const wsCircuit = new FrontendCircuitBreaker({
  name: 'websocket',
  failureThreshold: 3,
  recoveryTimeoutMs: 15000,
});

/**
 * 🔬 Retry Configuration
 */
interface RetryConfig {
  maxRetries: number;
  baseDelayMs: number;
  maxDelayMs: number;
  retryableStatuses: number[];
}

const DEFAULT_RETRY: RetryConfig = {
  maxRetries: parseInt(import.meta.env.VITE_MAX_RETRIES || '3'),
  baseDelayMs: 500,
  maxDelayMs: 5000,
  retryableStatuses: [408, 429, 500, 502, 503, 504],
};

async function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function calculateBackoff(attempt: number, config: RetryConfig): number {
  // Exponential backoff with jitter
  const delay = Math.min(config.baseDelayMs * Math.pow(2, attempt), config.maxDelayMs);
  return delay + Math.random() * 200; // Jitter
}

/**
 * 🔬 Enhanced fetchWithRetry with Circuit Breaker integration
 */
export async function fetchWithRetry(
  url: string,
  options: RequestInit = {},
  retryConfig: Partial<RetryConfig> = {}
): Promise<Response> {
  const config = { ...DEFAULT_RETRY, ...retryConfig };
  let lastError: Error | null = null;

  for (let attempt = 0; attempt <= config.maxRetries; attempt++) {
    try {
      // Circuit breaker protection
      const response = await apiCircuit.execute(() => fetch(url, options));

      // Don't retry on success or non-retryable codes
      if (!config.retryableStatuses.includes(response.status)) {
        return response;
      }

      // Retry on rate-limit or server errors
      console.warn(`⚠️ Attempt ${attempt + 1}/${config.maxRetries + 1}: ${response.status} ${url}`);
      lastError = new Error(`HTTP ${response.status}`);

    } catch (error) {
      lastError = error as Error;
      console.warn(`⚠️ Attempt ${attempt + 1}/${config.maxRetries + 1}:`, error);
    }

    // Wait before retry (except after last attempt)
    if (attempt < config.maxRetries) {
      const delay = calculateBackoff(attempt, config);
      console.log(`🔄 Retrying in ${Math.round(delay)}ms...`);
      await sleep(delay);
    }
  }

  throw lastError || new Error('All retries exhausted');
}// বাংলা মন্তব্য: Portal-ভিত্তিক একক backend নির্ধারণ — কোনো cross-portal failover নেই।
// Admin build (VITE_PORTAL_TYPE=admin) শুধু Admin backend-এ কথা বলে, User build শুধু User backend-এ।
// Firebase hosting external rewrite proxy সাপোর্ট করে না, তাই Firebase-এ সরাসরি backend URL ব্যবহার হয় (CORS allow)।
// Vercel-এ relative path ('') রাখা হয় কারণ Vercel external rewrite proxy সাপোর্ট করে।

/** Admin portal-এর canonical backend URL (build-time resolved) */
export const ADMIN_BACKEND_URL: string =
  import.meta.env.VITE_ADMIN_BACKEND || '';

/** User portal-এর canonical backend URL (build-time resolved) */
export const USER_BACKEND_URL: string =
  import.meta.env.VITE_USER_BACKEND ||
  import.meta.env.VITE_API_BASE ||
  import.meta.env.VITE_API_URL || '';

// 🔬 Export circuits for monitoring
export const circuits = { api: apiCircuit, websocket: wsCircuit };
export type { CircuitState };

// 🔒 RUNTIME VALIDATION - Missing URLs = Error in production
if ((import.meta.env.PROD) && !ADMIN_BACKEND_URL && import.meta.env.VITE_PORTAL_TYPE === 'admin') {
  throw new Error('❌ VITE_ADMIN_BACKEND is required in production. Set it in render.yaml or .env');
}
if ((import.meta.env.PROD) && !USER_BACKEND_URL && import.meta.env.VITE_PORTAL_TYPE !== 'admin') {
  throw new Error('❌ VITE_USER_BACKEND or VITE_API_URL is required in production. Set it in render.yaml or .env');
}

/**
 * বর্তমান portal-এর canonical backend URL — heartbeat ও অন্যান্য সার্ভিস এটিই ব্যবহার করে।
 * বাংলা মন্তব্য: runtime hostname sniffing নয়, build-time VITE_PORTAL_TYPE দিয়ে নির্ধারিত।
 */
export const BACKEND_URL: string =
  import.meta.env.VITE_PORTAL_TYPE === 'admin' ? ADMIN_BACKEND_URL : USER_BACKEND_URL;

/**
 * @deprecated পুরনো failover array — শুধু backward compatibility-র জন্য readonly রাখা হয়েছে।
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

  const hostname = window.location.hostname;

  // 🔥 ফিক্স: Firebase Hosting rewrite দিয়ে external URL-এ proxy করা যায় না,
  // তাই Firebase (.web.app/.firebaseapp.com)-এ সরাসরি portal-নির্দিষ্ট backend URL ব্যবহার করি।
  // Backend CORS ইতিমধ্যে supremeai-admin.web.app allow করে রেখেছে।
  // 🔧 DYNAMIC: Configure via RELATIVE_PATH_HOSTS env var
  const relativePathHosts = (import.meta.env.RELATIVE_PATH_HOSTS || 'vercel.app,localhost').split(',');
  if (relativePathHosts.some(h => hostname.includes(h))) {
    return '';
  }

  // Firebase ও বাকি হোস্টে (local dev ইত্যাদি) সরাসরি backend URL
  return BACKEND_URL;
};

/**
 * 🔬 Evolution v3.0: Health check for backend connectivity
 */
export async function checkBackendHealth(): Promise<{
  healthy: boolean;
  latency?: number;
  error?: string;
}> {
  const start = performance.now();
  try {
    const response = await fetchWithRetry(`${getApiBaseUrl()}/health/live`, {
      method: 'GET',
      signal: AbortSignal.timeout(5000),
    });
    return {
      healthy: response.ok,
      latency: Math.round(performance.now() - start),
    };
  } catch (error) {
    return {
      healthy: false,
      latency: Math.round(performance.now() - start),
      error: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

export const getWebSocketBaseUrl = (): string => {
  // বাংলা মন্তব্য: এক্সপ্লিসিট override সবার আগে
  if (import.meta.env.VITE_WS_BASE_URL) {
    return import.meta.env.VITE_WS_BASE_URL;
  }

  const apiBase = getApiBaseUrl();

  // 🔥 ফিক্স: Firebase hosting-এ apiBase === '' (relative path)।
  // WebSocket Firebase rewrite proxy দিয়ে যায় না — সরাসরি Render-এর wss:// URL ব্যবহার করতে হবে।
  if (apiBase === '') {
    return BACKEND_URL.replace(/^https:\/\//, 'wss://');
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
