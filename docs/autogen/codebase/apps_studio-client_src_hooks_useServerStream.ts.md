# 📄 ফাইল: apps/studio-client/src/hooks/useServerStream.ts

**প্রকার:** .ts  
**সাইজ:** 2,921 বাইট  
**আপডেট:** 2026-07-11T09:15:34.099812

---

## কোড

```ts
import { useEffect, useState } from 'react';
import { useStore } from '../store/useStore';
import { getApiBaseUrl } from '../utils/api';

export type ServerStreamStatus = 'connecting' | 'connected' | 'disconnected';

export const useServerStream = () => {
  const { setServerStatus, fetchGateStatus } = useStore();
  const [streamStatus, setStreamStatus] = useState<ServerStreamStatus>('connecting');

  useEffect(() => {
    const API_BASE_URL = getApiBaseUrl();
    const sseEndpoint = `${API_BASE_URL}/api/task/stream`;
    let eventSource: EventSource | null = null;
    let reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    let isMounted = true;

    const connect = () => {
      if (!isMounted) return;
      if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error("🔴 Max SSE reconnect attempts reached. Giving up.");
        setServerStatus(false);
        setStreamStatus('disconnected');
        return;
      }

      console.log("🔌 Initializing SupremeAI Unified Lifespan SSE Stream...");
      setStreamStatus(reconnectAttempts > 0 ? 'connecting' : 'connecting');
      eventSource = new EventSource(sseEndpoint);

      eventSource.onopen = () => {
        if (!isMounted) return;
        console.log("🟢 SSE Stream connected.");
        setServerStatus(true);
        setStreamStatus('connected');
        fetchGateStatus();
        reconnectAttempts = 0;
      };

      eventSource.onerror = () => {
        eventSource?.close();
        if (!isMounted) return;
        console.error("🔴 [SYSTEM CRITICAL] SSE Stream severed. SupremeAI Server is OFFLINE.");
        setServerStatus(false);
        setStreamStatus('disconnected');
        reconnectAttempts++;
        const backoff = Math.min(1000 * 2 ** reconnectAttempts, 30000);
        const jitter = Math.random() * 500;
        console.log(`🔄 SSE Reconnecting in ${(backoff + jitter) / 1000}s (attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`);
        
        if (isMounted) {
          setStreamStatus('connecting'); // Transition back to connecting during wait
          reconnectTimeout = setTimeout(connect, backoff + jitter);
        }
      };

      eventSource.onmessage = (e) => {
        if (e.data && (e.data.includes('auth_error') || e.data.includes('401'))) {
           console.error("🔴 SSE Auth Error: Closing stream to prevent storm.");
           eventSource?.close();
           if (isMounted) {
             setServerStatus(false);
             setStreamStatus('disconnected');
           }
        }
      };
    };

    connect();

    return () => {
      isMounted = false;
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
      console.log("🔌 Cleaning up SSE Stream...");
      eventSource?.close();
    };
  }, [setServerStatus, fetchGateStatus]);

  return { streamStatus };
};

```