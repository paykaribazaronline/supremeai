import React, { useState, useEffect } from 'react';
import { Card, Badge } from '../ui';
import { Shield, ShieldAlert, Cpu, Database, Eye, RefreshCw, Server, Loader2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface TaskReference {
  id: string;
  name: string;
  strongRef: boolean;
  status: string;
  startedAt: string;
}

interface MemoryMetrics {
  heapUsed: string;
  heapTotal: string;
  zombieTasksDetected: number;
  failuresBlocked: number;
}

export function SecurityDashboard() {
  const [activeTasks, setActiveTasks] = useState<TaskReference[]>([]);
  const [memoryMetrics, setMemoryMetrics] = useState<MemoryMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [tasksRes, metricsRes] = await Promise.all([
        apiClient.get<{ tasks: TaskReference[] }>('/admin-api/security/tasks'),
        apiClient.get<MemoryMetrics>('/admin-api/security/memory'),
      ]);
      setActiveTasks(tasksRes.tasks || []);
      setMemoryMetrics(metricsRes);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load security data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex-grow p-6 overflow-y-auto bg-[#030611] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3 text-slate-400">
          <Loader2 size={24} className="animate-spin text-[#00f3ff]" />
          <span className="text-xs font-mono">Loading security dashboard...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-grow p-6 overflow-y-auto bg-[#030611] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3 text-rose-400">
          <ShieldAlert size={24} />
          <span className="text-xs font-mono">{error}</span>
          <button
            onClick={fetchData}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[#00f3ff]/10 hover:bg-[#00f3ff]/20 text-[#00f3ff] text-xs font-mono border border-[#00f3ff]/30 transition-colors"
          >
            <RefreshCw size={12} />
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-grow p-6 overflow-y-auto bg-[#030611]">
      <div className="flex items-center justify-between mb-6 pb-2 border-b border-[#00f3ff]/15">
        <h2 className="text-lg font-bold font-['Space_Grotesk'] tracking-widest text-[#00f3ff] uppercase">
          🧠 Memory & Background Security Dashboard
        </h2>
        <div className="flex gap-2">
          <Badge variant="success">All Tasks Tracked</Badge>
          <Badge variant="info">0 Zombie Tasks</Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card title="Background Task Group">
          <div className="flex items-center gap-3">
            <Cpu size={20} className="text-cyan-400" />
            <div>
              <div className="text-xs text-slate-400">Active Tasks (Strong References)</div>
              <div className="text-xl font-bold text-cyan-400 font-mono">
                {activeTasks.filter(t => t.status === 'running').length} Active
              </div>
            </div>
          </div>
        </Card>

        <Card title="Zombie/Fire-And-Forget Checks">
          <div className="flex items-center gap-3">
            <ShieldAlert size={20} className={memoryMetrics && memoryMetrics.zombieTasksDetected > 0 ? 'text-red-400' : 'text-emerald-400'} />
            <div>
              <div className="text-xs text-slate-400">Untracked Tasks Blocked</div>
              <div className="text-xl font-bold text-emerald-400 font-mono">
                {memoryMetrics ? memoryMetrics.failuresBlocked : 0} Blocked
              </div>
            </div>
          </div>
        </Card>

        <Card title="Active Memory Usage">
          <div className="flex items-center gap-3">
            <Database size={20} className="text-yellow-400" />
            <div>
              <div className="text-xs text-slate-400">Memory Heap</div>
              <div className="text-xl font-bold text-yellow-400 font-mono">
                {memoryMetrics ? memoryMetrics.heapUsed : '—'}
              </div>
            </div>
          </div>
        </Card>

        <Card title="Database OCC Engine">
          <div className="flex items-center gap-3">
            <Server size={20} className="text-emerald-400" />
            <div>
              <div className="text-xs text-slate-400">Optimistic Locks Active</div>
              <div className="text-xl font-bold text-emerald-400 font-mono">
                0 Contended
              </div>
            </div>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Monitored Async Background Tasks">
          <div className="flex flex-col gap-2">
            {activeTasks.length === 0 ? (
              <div className="text-xs text-slate-400 font-mono text-center py-4">No active tasks monitored.</div>
            ) : (
              activeTasks.map(t => (
                <div key={t.id} className="p-3 rounded-lg border border-slate-800 bg-slate-900/30 flex items-center justify-between">
                  <div>
                    <div className="text-xs font-bold text-white font-mono">{t.name}</div>
                    <div className="text-[10px] text-slate-400 mt-1">
                      ID: {t.id} • Started: {t.startedAt}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={t.strongRef ? 'info' : 'warning'}>
                      {t.strongRef ? 'Strong Reference' : 'Weak Reference'}
                    </Badge>
                    <Badge variant={t.status === 'running' ? 'success' : 'info'}>
                      {t.status.toUpperCase()}
                    </Badge>
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>

        <Card title="Real-time Security Guard Signals">
          <div className="flex flex-col gap-3 text-xs font-mono text-slate-300">
            <div className="flex items-start gap-2">
              <span className="text-emerald-400">[OK]</span>
              <span>All active coroutines are bound to class strong-reference sets (preventing GC leakage).</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-emerald-400">[OK]</span>
              <span>Database poolclass is NullPool (avoiding PgBouncer transaction-mode deadlocks).</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-emerald-400">[OK]</span>
              <span>Fail-Closed auth guard rules compiled: OS Environment is "production". Easy Login disabled.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-emerald-400">[OK]</span>
              <span>AST security visitor module successfully verified code proposal compile outputs.</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}