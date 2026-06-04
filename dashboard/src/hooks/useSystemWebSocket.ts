import { Client, IMessage } from "@stomp/stompjs";
import { useState, useEffect, useRef, useCallback } from "react";
import SockJS from "sockjs-client";

interface SystemEvent {
  type: string;
  [key: string]: any;
}

export function useSystemWebSocket(
  topics: string[] = ["/topic/system-events"],
) {
  const [messages, setMessages] = useState<Record<string, SystemEvent>>({});
  const [connected, setConnected] = useState(false);
  const stompClientRef = useRef<Client | null>(null);

  const handleMessage = useCallback((topic: string, message: IMessage) => {
    try {
      const data = JSON.parse(message.body) as SystemEvent;
      setMessages((prev) => ({
        ...prev,
        [topic]: data,
      }));
    } catch (e) {
      console.error(`Failed to parse WebSocket message from ${topic}`, e);
    }
  }, []);

  useEffect(() => {
    const connectWebSocket = () => {
      try {
        const wsUrl = `${window.location.protocol === "https:" ? "wss:" : "ws"}://${window.location.host}/ws`;
        const socket = new SockJS(wsUrl);

        const stompClient = new Client({
          webSocketFactory: () => socket,
          reconnectDelay: 5000,
          onConnect: () => {
            setConnected(true);
            topics.forEach((topic) => {
              stompClient.subscribe(topic, (message) =>
                handleMessage(topic, message),
              );
            });
          },
          onDisconnect: () => {
            setConnected(false);
          },
          onStompError: (frame) => {
            console.error("STOMP error", frame);
            setConnected(false);
          },
        });

        stompClient.activate();
        stompClientRef.current = stompClient;
      } catch (err) {
        console.error("WebSocket connection error", err);
      }
    };

    connectWebSocket();

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [topics, handleMessage]);

  return { messages, connected, lastMessage: messages[topics[0]] || null };
}
