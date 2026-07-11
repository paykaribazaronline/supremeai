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
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const mountedRef = useRef(true);
  
  // বাংলা মন্তব্য: P2 Fix — reconnectAttempts এবং reconnectInterval কে ref এ রাখা হয়েছে যাতে useCallback dependency list পরিবর্তন না হয়
  const reconnectAttemptsRef = useRef(reconnectAttempts);
  const reconnectIntervalRef = useRef(reconnectInterval);
  
  // Sync refs when options change
  useEffect(() => {
    reconnectAttemptsRef.current = reconnectAttempts;
    reconnectIntervalRef.current = reconnectInterval;
  }, [reconnectAttempts, reconnectInterval]);

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

        // বাংলা মন্তব্য: P2 Fix — Heartbeat ping প্রতি 30s এ পাঠানো হয় zombie connections detect করতে।
        heartbeatRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));
          }
        }, 30000);
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
        if (heartbeatRef.current) {
          clearInterval(heartbeatRef.current);
          heartbeatRef.current = null;
        }
        setStatus('closed');
        onClose?.();

        if (attemptsRef.current < reconnectAttemptsRef.current) {
          attemptsRef.current += 1;
          reconnectTimerRef.current = setTimeout(() => {
            if (mountedRef.current) connect();
          }, reconnectIntervalRef.current * attemptsRef.current);
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
  }, [resolveUrl, onMessage, onOpen, onClose, onError]);

  const disconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (heartbeatRef.current) {
      clearInterval(heartbeatRef.current);
      heartbeatRef.current = null;
    }
    attemptsRef.current = reconnectAttemptsRef.current;
    if (wsRef.current) {
      if (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING) {
        wsRef.current.close(1000, 'Component unmounted');
      }
    }
    wsRef.current = null;
    setStatus('closed');
  }, []);

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
