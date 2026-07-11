# 📄 ফাইল: apps/studio-client/src/types/swarm.ts

**প্রকার:** .ts  
**সাইজ:** 693 বাইট  
**আপডেট:** 2026-07-11T10:59:17.922062

---

## কোড

```ts
export type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';
export type LogLevel = 'info' | 'warn' | 'error' | 'success';

export interface SwarmMetrics {
  cpuUsage: number;      // Percentage (0-100)
  memoryUsage: number;   // MB
  activeAgents: number;  // Count
  errorRate: number;     // Percentage (0-100)
}

export interface SwarmLog {
  id: string;
  timestamp: number;
  agentName: string;
  message: string;
  level: LogLevel;
}

export interface SwarmContextState {
  metrics: SwarmMetrics;
  logs: SwarmLog[];
  circuitState: CircuitState;
  connectionStatus: 'connecting' | 'connected' | 'disconnected';
  triggerCircuitBreaker: () => void;
  resetCircuitBreaker: () => void;
}

```