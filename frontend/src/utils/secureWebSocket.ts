/**
 * secureWebSocket.ts
 *
 * SECURITY FIX (audit S-2): Token must NEVER be passed as a URL query
 * parameter — it gets recorded in browser history, server access logs, and
 * proxy logs. Instead we use the "first-message auth" pattern:
 *
 *   1. Open the WebSocket without any credentials in the URL.
 *   2. As soon as the connection opens, send a JSON auth frame:
 *      { "type": "auth", "token": "<bearer>" }
 *   3. Backend must close the socket (code 4001) if the auth frame is not
 *      received within the handshake timeout.
 *
 * Usage:
 *   import { createSecureWebSocket, getAuthToken } from '@/utils/secureWebSocket';
 *   const ws = createSecureWebSocket(url, {
 *     onOpen: (ws) => { ... },
 *     onMessage: (event) => { ... },
 *     onClose: (event) => { ... },
 *     onError: (event) => { ... },
 *   });
 */

// Token key constants — must match authStore.ts
const USER_TOKEN_KEY = 'supremeai_auth_token';
const ADMIN_TOKEN_KEY = 'supreme_admin_jwt';
const LEGACY_ADMIN_TOKEN_KEY = 'adminToken';

/**
 * Reads the best available auth token from the in-memory apiClient cache
 * first, falling back to localStorage. Prefers user token over admin token.
 * Returns null if no token is found — callers should abort the connection.
 */
export function getAuthToken(): string | null {
  try {
    return (
      localStorage.getItem(USER_TOKEN_KEY) ||
      localStorage.getItem(ADMIN_TOKEN_KEY) ||
      localStorage.getItem(LEGACY_ADMIN_TOKEN_KEY) ||
      null
    );
  } catch (e) {
    console.warn("[secureWebSocket] getAuthToken failed:", e);
    return null;
  }
}

export interface SecureWsCallbacks {
  onOpen?: (ws: WebSocket) => void;
  onMessage?: (event: MessageEvent) => void;
  onClose?: (event: CloseEvent) => void;
  onError?: (event: Event) => void;
}

/**
 * Opens a WebSocket to `url` (which must NOT contain a token query param).
 * Immediately sends the auth frame on open.
 */
export function createSecureWebSocket(
  url: string,
  callbacks: SecureWsCallbacks
): WebSocket {
  // Guard: strip any accidental ?token= that callers might pass.
  const safeUrl = url.replace(/([?&])token=[^&]*/g, '$1').replace(/[?&]$/, '');

  const ws = new WebSocket(safeUrl);

  ws.onopen = () => {
    const token = getAuthToken();
    if (token) {
      // First-message auth frame — never touches the URL.
      ws.send(JSON.stringify({ type: 'auth', token }));
    }
    callbacks.onOpen?.(ws);
  };

  ws.onmessage = (event) => callbacks.onMessage?.(event);
  ws.onclose = (event) => callbacks.onClose?.(event);
  ws.onerror = (event) => callbacks.onError?.(event);

  return ws;
}

/**
 * Exponential backoff helper for reconnect loops.
 * Returns delay in ms: base * 2^attempt + random jitter, capped at maxMs.
 */
export function getReconnectDelay(
  attempt: number,
  baseMs = 1_000,
  maxMs = 30_000
): number {
  const exp = Math.min(attempt, 10); // prevent overflow
  const delay = baseMs * Math.pow(2, exp);
  const jitter = Math.random() * baseMs;
  return Math.min(delay + jitter, maxMs);
}
