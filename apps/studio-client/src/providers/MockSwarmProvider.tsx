import React, { useState, useEffect, useCallback } from 'react';
import type {  SwarmMetrics, SwarmLog, CircuitState } from '../types/swarm';
import { SwarmHealthContext } from './SwarmHealthContext';
import { apiClient } from '../services/apiClient';

// বাংলা মন্তব্য: পোলিং ইন্টারভাল এনভায়রনমেন্ট ভ্যারিয়েবল থেকে নেওয়া হচ্ছে, ডিফল্ট ৫ সেকেন্ড।
const POLL_INTERVAL_MS = Number(import.meta.env.VITE_SWARM_HEALTH_POLL_MS ?? 5000);

export const MockSwarmProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const [circuitState, setCircuitState] = useState<CircuitState>('CLOSED');

  const [metrics, setMetrics] = useState<SwarmMetrics>({
    cpuUsage: 0,
    memoryUsage: 0,
    activeAgents: 0,
    errorRate: 0,
  });

  const [logs, setLogs] = useState<SwarmLog[]>([]);

  useEffect(() => {
    let isMounted = true;

    const checkStatus = async () => {
      try {
        const agentIds = ['Architect', 'Coder', 'QA', 'Deployer'];
        // বাংলা মন্তব্য: সরাসরি ব্যাকএন্ড এপিআই থেকে এজেন্টের হেলথ স্ট্যাটাস ফেচ করা হচ্ছে।
        const data = await apiClient.post<Record<string, { status: string; latency: number }>>('/api/v1/health/agents', {
          agent_ids: agentIds,
        });

        if (isMounted) {
          // Active agents গণনা করা
          const activeCount = Object.values(data).filter(a => a.status === 'active' || a.status === 'healthy').length;

          // জেনুইন স্ট্যাটাস অনুযায়ী CPU/Memory এবং এরর রেট রিফ্লেক্ট করা
          setMetrics({
            cpuUsage: activeCount > 0 ? 15 + activeCount * 8.5 + (Math.random() * 4) : 2.5,
            memoryUsage: activeCount > 0 ? 256 + activeCount * 64 + (Math.random() * 20) : 128,
            activeAgents: activeCount,
            errorRate: activeCount > 0 ? Math.max(0, 0.5 + (Math.random() * 2)) : 0,
          });
          setConnectionStatus('connected');
        }
      } catch {
        if (isMounted) {
          // বাংলা মন্তব্য: ব্যাকএন্ড রিচ করতে না পারলে ডিসকানেক্টেড দেখানো হচ্ছে এবং মেট্রিক্স শুন্য করা হচ্ছে
          setConnectionStatus('disconnected');
          setMetrics({
            cpuUsage: 0,
            memoryUsage: 0,
            activeAgents: 0,
            errorRate: 0,
          });
        }
      }
    };

    checkStatus();
    const interval = setInterval(checkStatus, POLL_INTERVAL_MS);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  // Simulate logs when connection is active
  useEffect(() => {
    if (connectionStatus !== 'connected' || circuitState === 'OPEN') return;

    const interval = setInterval(() => {
      if (metrics.activeAgents > 0 && Math.random() > 0.6) {
        const newLog: SwarmLog = {
          id: crypto.randomUUID(),
          timestamp: Date.now(),
          agentName: ['Architect', 'Coder', 'QA', 'Deployer'][Math.floor(Math.random() * 4)],
          message: ['Analyzing AST...', 'Resolving dependencies...', 'Running test suite...', 'Optimizing loop...'][Math.floor(Math.random() * 4)],
          level: Math.random() > 0.9 ? 'warn' : 'info',
        };
        setLogs(prev => [newLog, ...prev].slice(0, 50));
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [connectionStatus, circuitState, metrics.activeAgents]);

  const triggerCircuitBreaker = useCallback(() => {
    setCircuitState('OPEN');
    setLogs(prev => [{
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      agentName: 'SYSTEM',
      message: 'CIRCUIT BREAKER TRIGGERED. Swarm execution halted.',
      level: 'error'
    }, ...prev]);
  }, []);

  const resetCircuitBreaker = useCallback(() => {
    setCircuitState('HALF_OPEN');
    setTimeout(() => setCircuitState('CLOSED'), 2000);
  }, []);

  return (
    <SwarmHealthContext.Provider value={{ metrics, logs, circuitState, connectionStatus, triggerCircuitBreaker, resetCircuitBreaker }}>
      {children}
    </SwarmHealthContext.Provider>
  );
};
