# 📄 ফাইল: apps/studio-client/src/hooks/useWebSocket.ts

**প্রকার:** .ts  
**সাইজ:** 3,834 বাইট  
**আপডেট:** 2026-07-05T16:59:16.463206

---

## কোড

```ts
import { useState, useEffect, useRef, useCallback } from 'react';
import { getWebSocketBaseUrl } from '../utils/api';

type ConnectionStatus = 'connecting' | 'open' | 'closed' | 'error';

interface UseWebSocketOptions {
  url?: string;
  autoConnect?: boolean;
  reconnectAttempts?: number;
  reconnectInterval?: number;
  onMessage?: (data: any) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: Event) => void;
}

interface UseWebSocketReturn {
  status: ConnectionStatus;
  data: any | null;
  send: (message: unknown) => void;
  connect: () => void;
  disconnect: () => void;
  lastMessage: MessageEvent | null;
}

export function useWebSocket(options: UseWebSocketOptions = {}): UseWebSocketReturn {
  const {
    url,
    autoConnect = false,
    reconnectAttempts = 5,
    reconnectInterval = 3000,
    onMessage,
    onOpen,
    onClose,
    onError,
  } = options;

  const [status, setStatus] = useState<ConnectionStatus>('closed');
  const [data, setData] = useState<any | null>(null);
  const [lastMessage, setLastMessage] = useState<MessageEvent | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const attemptsRef = useRef(0);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const resolveUrl = useCallback(() => {
    if (url) return url;
    return `${getWebSocketBaseUrl()}/ws`;
  }, [url]);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    try {
      setStatus('connecting');
      const socketUrl = resolveUrl();
      const ws = new WebSocket(socketUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        if (!mountedRef.current) return;
        attemptsRef.current = 0;
        setStatus('open');
        onOpen?.();
      };

      ws.onmessage = (event: MessageEvent) => {
        if (!mountedRef.current) return;
        setLastMessage(event);
        try {
          const parsed = JSON.parse(event.data);
          setData(parsed);
          onMessage?.(parsed);
        } catch {
          setData(event.data);
          onMessage?.(event.data);
        }
      };

      ws.onclose = () => {
        if (!mountedRef.current) return;
        setStatus('closed');
        onClose?.();

        if (attemptsRef.current < reconnectAttempts) {
          attemptsRef.current += 1;
          reconnectTimerRef.current = setTimeout(() => {
            if (mountedRef.current) connect();
          }, reconnectInterval * attemptsRef.current);
        }
      };

      ws.onerror = (event: Event) => {
        if (!mountedRef.current) return;
        setStatus('error');
        onError?.(event);
      };
    } catch (err) {
      if (!mountedRef.current) return;
      setStatus('error');
    }
  }, [resolveUrl, reconnectAttempts, reconnectInterval, onMessage, onOpen, onClose, onError]);

  const disconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    attemptsRef.current = reconnectAttempts;
    wsRef.current?.close();
    wsRef.current = null;
    setStatus('closed');
  }, [reconnectAttempts]);

  const send = useCallback((message: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      const payload = typeof message === 'string' ? message : JSON.stringify(message);
      wsRef.current.send(payload);
    } else {
      console.warn('WebSocket is not connected. Cannot send message.');
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    if (autoConnect) {
      connect();
    }
    return () => {
      mountedRef.current = false;
      disconnect();
    };
  }, [autoConnect, connect, disconnect]);

  return { status, data, send, connect, disconnect, lastMessage };
}

```