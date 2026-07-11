# 📄 ফাইল: apps/studio-client/src/providers/MockSwarmProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,092 বাইট  
**আপডেট:** 2026-07-11T15:50:11.390852

---

## কোড

```tsx
import React, { useState, useEffect, useCallback } from 'react';
import type { SwarmContextState, SwarmMetrics, SwarmLog, CircuitState } from '../types/swarm';
import { SwarmHealthContext } from './SwarmHealthContext';

// বাংলা মন্তব্য: SwarmHealthContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
export const MockSwarmProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const [circuitState, setCircuitState] = useState<CircuitState>('CLOSED');
  
  const [metrics, setMetrics] = useState<SwarmMetrics>({
    cpuUsage: 12,
    memoryUsage: 256,
    activeAgents: 3,
    errorRate: 0,
  });
  
  const [logs, setLogs] = useState<SwarmLog[]>([]);

  // Simulate Initial Connection
  useEffect(() => {
    const timer = setTimeout(() => setConnectionStatus('connected'), 1500);
    return () => clearTimeout(timer);
  }, []);

  // Simulate Live Metrics & Logs Stream
  useEffect(() => {
    if (connectionStatus !== 'connected' || circuitState === 'OPEN') return;

    const interval = setInterval(() => {
      // Randomize metrics slightly to create a "Live" pulse effect
      setMetrics(prev => ({
        cpuUsage: Math.min(100, Math.max(5, prev.cpuUsage + (Math.random() * 10 - 5))),
        memoryUsage: Math.max(100, prev.memoryUsage + (Math.random() * 50 - 20)),
        activeAgents: Math.floor(Math.random() * 3) + 3, // 3 to 5 agents
        errorRate: Math.max(0, prev.errorRate + (Math.random() * 2 - 1)),
      }));

      // Randomly push a new log every few seconds
      if (Math.random() > 0.6) {
        const newLog: SwarmLog = {
          id: crypto.randomUUID(),
          timestamp: Date.now(),
          agentName: ['Architect', 'Coder', 'QA', 'Deployer'][Math.floor(Math.random() * 4)],
          message: ['Analyzing AST...', 'Resolving dependencies...', 'Running test suite...', 'Optimizing loop...'][Math.floor(Math.random() * 4)],
          level: Math.random() > 0.9 ? 'warn' : 'info',
        };
        setLogs(prev => [newLog, ...prev].slice(0, 50)); // Keep last 50 logs
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [connectionStatus, circuitState]);

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
    setTimeout(() => setCircuitState('CLOSED'), 2000); // Simulate recovery
  }, []);

  return (
    <SwarmHealthContext.Provider value={{ metrics, logs, circuitState, connectionStatus, triggerCircuitBreaker, resetCircuitBreaker }}>
      {children}
    </SwarmHealthContext.Provider>
  );
};
```