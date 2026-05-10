import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface SystemEvent {
  type: string;
  domainId?: string;
  progress?: number;
  fact?: string;
  message?: string;
  timestamp?: number;
}

export function useSystemWebSocket() {
  const [lastMessage, setLastMessage] = useState<SystemEvent | null>(null);
  const [connected, setConnected] = useState(false);
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    const connectWebSocket = () => {
      try {
        const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws'}://${window.location.host}/ws`;
        const socket = new SockJS(wsUrl);
        
        const stompClient = new Client({
          webSocketFactory: () => socket,
          reconnectDelay: 5000,
          onConnect: () => {
            setConnected(true);
            stompClient.subscribe('/topic/system-events', (message) => {
              try {
                const data = JSON.parse(message.body) as SystemEvent;
                setLastMessage(data);
              } catch (e) {
                console.error('Failed to parse WebSocket message', e);
              }
            });
          },
          onDisconnect: () => {
            setConnected(false);
          },
          onStompError: (frame) => {
            console.error('STOMP error', frame);
            setConnected(false);
          }
        });

        stompClient.activate();
        stompClientRef.current = stompClient;
      } catch (err) {
        console.error('WebSocket connection error', err);
      }
    };

    connectWebSocket();

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, []);

  return { lastMessage, connected };
}