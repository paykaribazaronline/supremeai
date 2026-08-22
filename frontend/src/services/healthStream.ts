/**
 * HealthStream - WebSocket Client for Real-time Service Health Updates
 * 
 * Connects to /api/admin-api/health-stream endpoint
 * Provides reactive hooks for components to consume live health data.
 */

import { useEffect, useRef, useState, useCallback } from 'react';

// ══════════════════════════════════════════════════════════════════════════════
// TYPES
// ══════════════════════════════════════════════════════════════════════════════

export interface HealthUpdate {
  type: 'full_state' | 'update';
  data: {
    services: ServiceHealthUpdate[];
    changes?: ServiceHealthUpdate[];
    topology?: any;
    timestamp: string;
  };
}

export interface ServiceHealthUpdate {
  name: string;
  display_name: string;
  category: string;
  status: string;
  response_time_ms: number;
  error?: string;
  url: string;
  critical: boolean;
}

type HealthMessageHandler = (update: HealthUpdate) => void;
type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

// ══════════════════════════════════════════════════════════════════════════════
// WEBSOCKET HOOK
// ══════════════════════════════════════════════════════════════════════════════

export function useHealthStream(options?: {
  autoConnect?: boolean;
  reconnectInterval?: number;
  onServiceChange?: (service: ServiceHealthUpdate) => void;
}) {
  const {
    autoConnect = true,
    reconnectInterval = 5000,
    onServiceChange,
  } = options || {};

  const wsRef = useRef<WebSocket | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const [lastUpdate, setLastUpdate] = useState<HealthUpdate | null>(null);
  const [services, setServices] = useState<ServiceHealthUpdate[]>([]);
  const handlersRef = useRef<Set<HealthMessageHandler>>(new Set());

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    setConnectionStatus('connecting');

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = 