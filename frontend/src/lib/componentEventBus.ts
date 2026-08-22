/**
 * ComponentEventBus - Lightweight Event System for Cross-Component Communication
 * 
 * This enables your 20+ crown jewel components to talk to each other!
 * 
 * @file frontend/src/lib/componentEventBus.ts
 * @description Central event bus for SupremeAI component integration
 * @version 1.0.0
 * 
 * USAGE:
 * ```typescript
 * // Listen for events
 * useEffect(() => {
 *   return componentEventBus.on('service:status-change', (data) => {
 *     console.log(`Service ${data.service} is now ${data.status}`);
 *   });
 * }, []);
 * 
 * // Emit events
 * componentEventBus.emitServiceStatusChange('backend', 'down');
 * ```
 */

// ════════════════════════════════════════════════════════════════════
// TYPES
// ════════════════════════════════════════════════════════════════════

export type EventType = 
  // Service & Infrastructure Events
  | 'service:status-change'
  | 'service:health-update'
  | 'deployment:status-update'
  | 'deployment:complete'
  
  // Browser & Navigation Events
  | 'browser:url-changed'
  | 'browser:page-loaded'
  | 'browser:screenshot-captured'
  
  // Security Events
  | 'security:scan-complete'
  | 'security:threat-detected'
  | 'security:vulnerability-found'
  
  // AI & Intelligence Events
  | 'ai:action-complete'
  | 'ai:context-needed'
  | 'ai:insight-generated'
  
  // Memory & Knowledge Events
  | 'memory:item-created'
  | 'memory:session-saved'
  | 'memory:context-retrieved'
  
  // Alert & Notification Events
  | 'alert:new-alert'
  | 'alert:acknowledged'
  | 'alert:cleared'
  
  // User Interaction Events
  | 'user:action-performed'
  | 'user:preference-changed'
  | 'user:feedback-submitted';

export type EventCallback<T = any> = (data: T) => void;
export type EventDataMap = {
  'service:status-change': { service: string; status: 'healthy' | 'degraded' | 'down'; latency?: number; timestamp: number };
  'browser:url-changed': { url: string; title?: string; timestamp: number };
  'security:scan-complete': { url: string; score: number; issues: string[]; timestamp: number };
  'ai:action-complete': { action: string; result: any; duration: number };
  'memory:item-created': { type: string; id: string; timestamp: number };
  'alert:new-alert': { id: string; severity: string; source: string; message: string };
  'deployment:status-update': { id: string; environment: string; status: string; progress?: number };
};

// ════════════════════════════════════════════════════════════════════
// EVENT BUS CLASS
// ════════════════════════════════════════════════════════════════════

class ComponentEventBus {
  private listeners = new Map<EventType, Set<EventCallback>>();
  private eventHistory: Array<{ type: EventType; data: any; timestamp: number }> = [];
  private maxHistorySize = 100;
  
  /**
   * Subscribe to an event
   * @param event - The event type to listen for
   * @param callback - Function to call when event fires
   * @returns Unsubscribe function (call to stop listening)
   */
  on<T = any>(event: EventType, callback: EventCallback<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);
    
    // Return unsubscribe function for cleanup
    return () => {
      this.listeners.get(event)?.delete(callback);
    };
  }
  
  /**
   * Subscribe to an event only once
   * @param event - The event type to listen for
   * @param callback - Function to call when event fires (will be removed after first call)
   * @returns Unsubscribe function
   */
  once<T = any>(event: EventType, callback: EventCallback<T>): () => let {
    const wrapper: EventCallback<T> = (data) => {
      callback(data);
      this.off(event, wrapper);
    };
    return this.on(event, wrapper);
  }
  
  /**
   * Unsubscribe from an event
   * @param event - The event type
   * @param callback - The specific callback to remove
   */
  off<T = any>(event: EventType, callback: EventCallback<T>): void {
    this.listeners.get(event)?.delete(callback);
  }
  
  /**
   * Emit an event to all subscribers
   * @param event - The event type to emit
   * @param data - Optional data to pass to subscribers
   */
  emit<T = any>(event: EventType, data?: T): void {
    // Store in history for debugging
    this.eventHistory.push({ type: event, data, timestamp: Date.now() });
    if (this.eventHistory.length > this.maxHistorySize) {
      this.eventHistory.shift();
    }
    
    // Notify all listeners
    const callbacks = this.listeners.get(event);
    if (callbacks && callbacks.size > 0) {
      callbacks.forEach(cb => {
        try {
          cb(data);
        } catch (error) {
          console.error(`[ComponentEventBus] Error in handler for ${event}:`, error);
        }
      });
    }
    
    // Debug logging in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[ComponentEventBus] 📤 ${event}`, {
        listenersCount: callbacks?.size || 0,
        data
      });
    }
  }
  
  /**
   * Remove all listeners for a specific event (or all events if no event specified)
   * @param event - Optional event type to clear
   */
  clear(event?: EventType): void {
    if (event) {
      this.listeners.delete(event);
    } else {
      this.listeners.clear();
    }
  }
  
  /**
   * Get recent event history (for debugging)
   * @param limit - Number of events to return
   */
  getHistory(limit = 20): typeof this.eventHistory {
    return this.eventHistory.slice(-limit);
  }
  
  /**
   * Get the number of listeners for a specific event
   * @param event - The event type to check
   */
  getListenerCount(event: EventType): number {
    return this.listeners.get(event)?.size || 0;
  }
  
  // ════════════════════════════════════════════════════════════════════
  // CONVENIENCE METHODS FOR COMMON SUPREMEAI EVENTS
  // ════════════════════════════════════════════════════════════════════
  
  /**
   * Service health status changed
   */
  emitServiceStatusChange(
    service: string, 
    status: 'healthy' | 'degraded' | 'down',
    extra?: Partial<EventDataMap['service:status-change']>
  ) {
    this.emit('service:status-change', {
      service,
      status,
      timestamp: Date.now(),
      ...extra
    });
  }
  
  /**
   * Browser URL changed (navigation)
   */
  emitBrowserUrlChange(url: string, title?: string) {
    this.emit('browser:url-changed', { url, title, timestamp: Date.now() });
  }
  
  /**
   * Security scan completed
   */
  emitSecurityScanComplete(result: {
    url: string;
    score: number;
    issues: string[];
  }) {
    this.emit('security:scan-complete', {
      ...result,
      timestamp: Date.now()
    });
    
    // Auto-trigger alert if score is low
    if (result.score < 70) {
      this.emitAlert(
        result.score < 50 ? 'critical' : 'error',
        'SecurityScanner',
        `Low security score (${result.score}/100) for ${result.url}`
      );
    }
  }
  
  /**
   * AI action completed
   */
  emitAIActionComplete(action: string, result: any, startTime: number) {
    this.emit('ai:action-complete', {
      action,
      result,
      duration: Date.now() - startTime
    });
  }
  
  /**
   * Memory item created/saved
   */
  emitMemoryItemCreated(type: string, id: string) {
    this.emit('memory:item-created', { type, id, timestamp: Date.now() });
  }
  
  /**
   * New alert notification
   */
  emitAlert(severity: 'info' | 'warning' | 'error' | 'critical', source: string, message: string) {
    const alertId = `alert-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    this.emit('alert:new-alert', {
      id: alertId,
      severity,
      source,
      message
    });
    
    return alertId;
  }
  
  /**
   * Deployment status updated
   */
  emitDeploymentStatusUpdate(
    id: string, 
    environment: string, 
    status: 'pending' | 'running' | 'success' | 'failed',
    progress?: number
  ) {
    this.emit('deployment:status-update', {
      id,
      environment,
      status,
      progress,
      timestamp: Date.now()
    });
    
    // Auto-emit alert for failed deployments
    if (status === 'failed') {
      this.emitAlert('error', 'DeploymentSystem', `Deployment ${id} failed in ${environment}`);
    } else if (status === 'success') {
      this.emitAlert('info', 'DeploymentSystem', `Deployment ${id} succeeded in ${environment}`);
    }
  }
}

// ════════════════════════════════════════════════════════════════════
// SINGLETON EXPORT
// ════════════════════════════════════════════════════════════════════

/**
 * Global singleton instance of the component event bus
 * Import and use anywhere in your application:
 * 
 * import { componentEventBus } from '@/lib/componentEventBus';
 */
export const componentEventBus = new ComponentEventBus();

// ════════════════════════════════════════════════════════════════════
// REACT HOOK INTEGRATION (Optional)
// ════════════════════════════════════════════════════════════════════

import { useEffect, useRef, useCallback } from 'react';

/**
 * React hook for subscribing to events with automatic cleanup
 * @param event - The event type to listen for
 * @param callback - Function to call when event fires
 * @param deps - Optional dependency array (re-subscribes when changed)
 * 
 * @example
 * ```tsx
 * const [serviceStatus, setServiceStatus] = useState('healthy');
 * useComponentEvent('service:status-change', (data) => {
 *   if (data.service === 'backend') setServiceStatus(data.status);
 * }, []);
 * ```
 */
export function useComponentEvent<T = any>(
  event: EventType, 
  callback: EventCallback<T>,
  deps: React.DependencyList = []
) {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;
  
  useEffect(() => {
    return componentEventBus.on<T>(event, (data) => {
      callbackRef.current(data);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [event, ...deps]);
}

/**
 * React hook for emitting events with memoized emitter
 * @returns Object with emit functions for common events
 * 
 * @example
 * ```tsx
 * const { emitAlert, emitUrlChange } = useComponentEventEmitter();
 * 
 * <button onClick={() => emitAlert('error', 'MyComponent', 'Something broke')}>
 *   Trigger Alert
 * </button>
 * ```
 */
export function useComponentEventEmitter() {
  return {
    emitServiceStatusChange: useCallback(componentEventBus.emitServiceStatusChange.bind(componentEventBus), []),
    emitBrowserUrlChange: useCallback(componentEventBus.emitBrowserUrlChange.bind(componentEventBus), []),
    emitSecurityScanComplete: useCallback(componentEventBus.emitSecurityScanComplete.bind(componentEventBus), []),
    emitAIActionComplete: useCallback(componentEventBus.emitAIActionComplete.bind(componentEventBus), []),
    emitMemoryItemCreated: useCallback(componentEventBus.emitMemoryItemCreated.bind(componentEventBus), []),
    emitAlert: useCallback(componentEventBus.emitAlert.bind(componentEventBus), []),
    emitDeploymentStatusUpdate: useCallback(componentEventBus.emitDeploymentStatusUpdate.bind(componentEventBus), []),
    emit: useCallback(componentEventBus.emit.bind(componentEventBus), []),
  };
}

export default componentEventBus;
