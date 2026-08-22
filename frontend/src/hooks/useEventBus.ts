/**
 * ✅ REACT HOOK FOR EVENT BUS
 * Provides easy subscription management with automatic cleanup
 */

import { useEffect, useRef, useCallback } from 'react';
import { eventBus, Events, type EventCallback } from '../lib/eventBus';

interface UseEventBusReturn {
  emit: typeof eventBus.emit;
  subscribe: typeof eventBus.subscribe;
  getListenerCount: typeof eventBus.getListenerCount;
}

/**
 * Hook for subscribing to events with automatic cleanup
 * @param event Event name from Events enum
 * @param callback Handler function
 * @param deps Optional dependency array for re-subscription
 */
export function useEventBus<T = any>(
  event: keyof typeof Events | string,
  callback: EventCallback<T>,
  deps: React.DependencyList = []
): UseEventBusReturn {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    const unsubscribe = eventBus.subscribe<T>(event, (data) => {
      callbackRef.current(data);
    });

    return unsubscribe;
  }, [event, ...deps]);

  return {
    emit: useCallback(eventBus.emit, []),
    subscribe: useCallback(eventBus.subscribe, []),
    getListenerCount: useCallback(eventBus.getListenerCount, []),
  };
}

/**
 * Hook for emitting events (convenience wrapper)
 */
export function useEventEmitter() {
  const emit = useCallback(<T = any>(event: keyof typeof Events | string, data?: T) => {
    eventBus.emit(event, data);
  }, []);

  return { emit };
}

/**
 * Hook for multiple event subscriptions
 */
export function useEventBusMulti(
  subscriptions: Array<{
    event: keyof typeof Events | string;
    handler: EventCallback;
  }>
): void {
  useEffect(() => {
    const unsubscribers = subscriptions.map(({ event, handler }) =>
      eventBus.subscribe(event, handler)
    );

    return () => unsubscribers.forEach(unsub => unsub());
  }, [subscriptions]);
}

export default useEventBus;
