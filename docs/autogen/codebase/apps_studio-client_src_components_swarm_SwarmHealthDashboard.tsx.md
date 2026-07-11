# 📄 ফাইল: apps/studio-client/src/components/swarm/SwarmHealthDashboard.tsx

**প্রকার:** .tsx  
**সাইজ:** 4,382 বাইট  
**আপডেট:** 2026-07-11T11:32:07.062060

---

## কোড

```tsx
import React from 'react';
import { useSwarmStream } from '../../hooks/useSwarmStream';
import { SupremeCard } from '../../../../../packages/ui-components/src/components/SupremeCard';
import { SupremeHeader } from '../../../../../packages/ui-components/src/components/SupremeHeader';
import { HoldToKillButton } from './HoldToKillButton';
import { ShieldAlert, Activity, Server, Cpu, Database } from 'lucide-react';

export const SwarmHealthDashboard: React.FC = () => {
  const { metrics, logs, circuitState, connectionStatus, triggerCircuitBreaker, resetCircuitBreaker } = useSwarmStream();

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <SupremeHeader 
          title="Swarm Health Dashboard" 
          subtitle={`Connection: ${connectionStatus.toUpperCase()} | Circuit: ${circuitState}`}
          gradient={true}
        />
        <div className="flex items-center gap-4">
          {circuitState === 'OPEN' && (
            <button 
              onClick={resetCircuitBreaker}
              className="px-4 py-2 bg-success/20 text-success border border-success rounded-md text-sm font-bold"
            >
              RESET CIRCUIT
            </button>
          )}
          <HoldToKillButton onTrigger={triggerCircuitBreaker} />
        </div>
      </div>

      {circuitState === 'OPEN' && (
        <div className="bg-danger/20 border border-danger p-4 rounded-lg flex items-center gap-3">
          <ShieldAlert className="text-danger" size={24} />
          <div className="text-danger font-bold">
            CIRCUIT BREAKER IS OPEN. ALL AGENT EXECUTIONS ARE HALTED.
          </div>
        </div>
      )}

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <SupremeCard>
          <div className="flex items-center justify-between mb-2 text-text-secondary">
            <span>CPU Usage</span>
            <Cpu size={16} />
          </div>
          <div className="text-3xl font-bold text-neon-blue">
            {metrics.cpuUsage.toFixed(1)}%
          </div>
        </SupremeCard>

        <SupremeCard>
          <div className="flex items-center justify-between mb-2 text-text-secondary">
            <span>Memory Load</span>
            <Database size={16} />
          </div>
          <div className="text-3xl font-bold text-neon-purple">
            {metrics.memoryUsage.toFixed(0)} MB
          </div>
        </SupremeCard>

        <SupremeCard>
          <div className="flex items-center justify-between mb-2 text-text-secondary">
            <span>Active Agents</span>
            <Server size={16} />
          </div>
          <div className="text-3xl font-bold text-success">
            {metrics.activeAgents}
          </div>
        </SupremeCard>

        <SupremeCard>
          <div className="flex items-center justify-between mb-2 text-text-secondary">
            <span>Error Rate</span>
            <Activity size={16} />
          </div>
          <div className="text-3xl font-bold text-warning">
            {metrics.errorRate.toFixed(1)}%
          </div>
        </SupremeCard>
      </div>

      {/* Live Log Feed */}
      <SupremeCard className="h-96 flex flex-col">
        <SupremeHeader title="Live Execution Logs" />
        <div className="flex-1 overflow-y-auto space-y-2 mt-4 font-mono text-sm bg-input-bg p-4 rounded-lg border border-border-accent">
          {logs.length === 0 ? (
            <div className="text-text-secondary text-center py-8">No logs available.</div>
          ) : (
            logs.map((log) => (
              <div key={log.id} className="flex items-start gap-3 border-b border-border-accent pb-2">
                <span className="text-text-secondary shrink-0">
                  {new Date(log.timestamp).toLocaleTimeString()}
                </span>
                <span className={`shrink-0 font-bold ${
                  log.level === 'error' ? 'text-danger' : 
                  log.level === 'warn' ? 'text-warning' : 
                  log.level === 'success' ? 'text-success' : 'text-neon-blue'
                }`}>
                  [{log.agentName}]
                </span>
                <span className="text-foreground">{log.message}</span>
              </div>
            ))
          )}
        </div>
      </SupremeCard>
    </div>
  );
};

```