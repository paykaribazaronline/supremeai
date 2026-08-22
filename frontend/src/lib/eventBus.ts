/**
 * ✅ FRONTEND EVENT BUS - SupremeAI Integration Foundation
 * 
 * PROBLEM SOLVED: Components are deaf to each other
 * BEFORE: Chat doesn't know Voice is ready
 * AFTER: Any component can subscribe to any event
 * 
 * USAGE:
 *   import { eventBus, Events } from '@/lib/eventBus';
 *   
 *   // Listen
 *   const unsub = eventBus.subscribe(Events.CHAT_MESSAGE_SENT, (data) => {
 *     console.log('New message:', data);
 *   });
 *   
 *   // Emit
 *   eventBus.emit(Events.THEME_CHANGED, { theme: 'dark' });
 */

type EventCallback<T = any> = (data: T) => void;
type EventType = string;

class FrontendEventBus {
  private listeners = new Map<EventType, Set<EventCallback>>();
  private history: Array<{ type: EventType; data: any; timestamp: number }> = [];
  private maxHistory = 100;
  private debugMode = import.meta.env?.VITE_EVENT_BUS_DEBUG === 'true';

  /**
   * Subscribe to an event
   * @returns Unsubscribe function (React useEffect friendly)
   */
  subscribe<T = any>(event: EventType, callback: EventCallback<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    
    this.listeners.get(event)!.add(callback);
    
    if (this.debugMode) {
      console.log(`[EventBus] Subscribed to: ${event} (total listeners: ${this.listeners.get(event)!.size})`);
    }
    
    // Return cleanup function
    return () => {
      const callbacks = this.listeners.get(event);
      if (callbacks) {
        callbacks.delete(callback);
        if (this.debugMode) {
          console.log(`[EventBus] Unsubscribed from: ${event}`);
        }
      }
    };
  }

  /**
   * Emit an event to all subscribers
   */
  emit<T = any>(event: EventType, data?: T): void {
    // Store in history for debugging
    this.history.push({
      type: event,
      data,
      timestamp: Date.now(),
    });
    
    if (this.history.length > this.maxHistory) {
      this.history.shift();
    }
    
    if (this.debugMode) {
      console.log(`[EventBus] Emitting: ${event}`, data);
    }
    
    // Notify all subscribers (with error isolation)
    const callbacks = this.listeners.get(event);
    if (callbacks) {
      callbacks.forEach((callback) => {
        try {
          callback(data);
        } catch (error) {
          console.error(`[EventBus] Error in handler for ${event}:`, error);
        }
      });
    }
  }

  /**
   * Get recent event history (for debugging)
   */
  getHistory(): readonly typeof this.history {
    return this.history;
  }

  /**
   * Clear all listeners (for logout/cleanup)
   */
  clear(): void {
    this.listeners.clear();
    this.history = [];
    if (this.debugMode) {
      console.log('[EventBus] Cleared all listeners');
    }
  }

  /**
   * Get listener count for an event (for debugging)
   */
  getListenerCount(event: EventType): number {
    return this.listeners.get(event)?.size || 0;
  }
}

// Singleton instance
export const eventBus = new FrontendEventBus();

// ═══════════════════════════════════════════════════════════════
// PRE-DEFINED EVENT TYPES (Import these, NEVER use magic strings!)
// ═══════════════════════════════════════════════════════════════

export const Events = {
  // ─── AUTHENTICATION EVENTS ────────────────────────────────
  AUTH_LOGIN: 'auth:login',
  AUTH_LOGOUT: 'auth:logout',
  AUTH_TOKEN_REFRESHED: 'auth:token_refreshed',
  AUTH_MFA_REQUIRED: 'auth:mfa_required',
  AUTH_SESSION_EXPIRED: 'auth:session_expired',

  // ─── CHAT & CONVERSATION EVENTS ──────────────────────────
  CHAT_MESSAGE_SENT: 'chat:message_sent',
  CHAT_MESSAGE_RECEIVED: 'chat:message_received',
  CHAT_CONVERSATION_CREATED: 'chat:conversation_created',
  CHAT_STREAM_START: 'chat:stream_start',
  CHAT_STREAM_TOKEN: 'chat:stream_token',
  CHAT_STREAM_END: 'chat:stream_end',
  CHAT_ERROR: 'chat:error',

  // ─── THEME & UI EVENTS ───────────────────────────────────
  THEME_CHANGED: 'theme:changed',
  THEME_DARK_MODE: 'theme:dark_mode',
  THEME_LIGHT_MODE: 'theme:light_mode',
  SIDEBAR_TOGGLED: 'ui:sidebar_toggled',
  MODAL_OPENED: 'ui:modal_opened',
  MODAL_CLOSED: 'ui:modal_closed',

  // ─── SERVICE HEALTH & MONITORING ─────────────────────────
  SERVICE_HEALTH_CHANGED: 'service:health_changed',
  SERVICE_DOWN: 'service:down',
  SERVICE_RECOVERED: 'service:recovered',
  SERVICE_DEGRADED: 'service:degraded',
  METRICS_UPDATE_AVAILABLE: 'metrics:update_available',
  METRICS_REFRESH_REQUESTED: 'metrics:refresh_requested',

  // ─── COST & BILLING EVENTS ───────────────────────────────
  COST_THRESHOLD_REACHED: 'cost:threshold_reached',
  COST_BUDGET_WARNING: 'cost:budget_warning',
  BUDGET_EXHAUSTED: 'budget:exhausted',
  TOKEN_USAGE_UPDATED: 'cost:token_usage_updated',
  PAYMENT_REQUIRED: 'payment:required',

  // ─── BROWSER EVENTS ──────────────────────────────────────
  BROWSER_URL_CHANGED: 'browser:url_changed',
  BROWSER_PAGE_LOADED: 'browser:page_loaded',
  BROWSER_PAGE_CAPTURED: 'browser:page_captured',
  BROWSER_CONTENT_INGESTED: 'browser:content_ingested',
  BROWSER_SCREENSHOT_TAKEN: 'browser:screenshot_taken',
  BROWSER_ERROR: 'browser:error',
  IFRAME_CONSOLE_ERROR: 'iframe:console_error', // For AI self-healing

  // ─── EVOLUTION & LEARNING EVENTS ─────────────────────────
  SKILL_AUTO_CREATED: 'evolution:skill_auto_created',
  SKILL_APPROVAL_NEEDED: 'evolution:skill_approval_needed',
  PATTERN_DETECTED: 'evolution:pattern_detected',
  OPTIMIZATION_SUGGESTED: 'evolution:optimization_suggested',
  LEARNING_LOOP_COMPLETE: 'evolution:learning_complete',
  PROMPT_OPTIMIZED: 'evolution:prompt_optimized',

  // ─── SECURITY EVENTS ─────────────────────────────────────
  THREAT_DETECTED: 'security:threat_detected',
  THREAT_BLOCKED: 'security:threat_blocked',
  USER_BLOCKED: 'security:user_blocked',
  SUSPICIOUS_ACTIVITY: 'security:suspicious_activity',
  RATE_LIMIT_HIT: 'security:rate_limit_hit',

  // ─── VOICE & AUDIO EVENTS ────────────────────────────────
  VOICE_MESSAGE_READY: 'voice:message_ready',
  VOICE_TOGGLED: 'voice:toggled',
  VOICE_RECORDING_STARTED: 'voice:recording_started',
  VOICE_RECORDING_STOPPED: 'voice:recording_stopped',
  TTS_GENERATED: 'tts:generated',

  // ─── RAG & KNOWLEDGE EVENTS ──────────────────────────────
  RAG_CONTENT_UPDATED: 'rag:content_updated',
  RAG_INDEXING_COMPLETE: 'rag:indexing_complete',
  KNOWLEDGE_QUERY: 'knowledge:query',
  KNOWLEDGE_RESULT: 'knowledge:result',

  // ─── WORKSPACE & INTEGRATION EVENTS ──────────────────────
  INTEGRATION_CONNECTED: 'integration:connected',
  INTEGRATION_DISCONNECTED: 'integration:disconnected',
  WORKSPACE_CHANGED: 'workspace:changed',
  FILE_SAVED: 'workspace:file_saved',
  DEPLOYMENT_STATUS: 'deployment:status',

  // ─── ADMIN-SPECIFIC EVENTS ───────────────────────────────
  USER_ACTION_LOGGED: 'admin:user_action',
  SETTINGS_CHANGED: 'admin:settings_changed',
  BACKUP_COMPLETED: 'admin:backup_completed',
  SYSTEM_ALERT: 'admin:system_alert',

  // ─── HITL (HUMAN-IN-THE-LOOP) EVENTS ────────────────────
  HITL_REQUIRED: 'hitl:required',
  HITL_SESSION_STARTED: 'hitl:session_started',
  HITL_SESSION_ENDED: 'hitl:sessionEnded',
  TAKEOVER_REQUESTED: 'hitl:takeover_requested',
  CONTROL_RETURNED: 'hitl:control_returned',

} as const;

export type EventType = keyof typeof Events;

// Export type helpers
export interface BaseEventData {
  timestamp: number;
  source: string;
}

export interface AuthEventData extends BaseEventData {
  userId?: string;
  sessionId?: string;
}

export interface ServiceHealthData extends BaseEventData {
  serviceName: string;
  status: 'healthy' | 'degraded' | 'down';
  latency?: number;
  error?: string;
}

export interface CostEventData extends BaseEventData {
  currentCost: number;
  limit: number;
  threshold: number;
  service?: string;
}
