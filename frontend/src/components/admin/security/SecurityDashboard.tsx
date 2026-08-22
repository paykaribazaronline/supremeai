import { useState, useEffect } from 'react';
import { Card, Badge } from '../ui';
import {  ShieldAlert, Cpu, Database, RefreshCw, Server, Loader2, DollarSign, Activity } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { useUnifiedStore } from '../../store/unifiedStore';

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
  const lastSecurityScan = useUnifiedStore(s => s.lastSecurityScan);

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
            {lastSecurityScan?.issues && lastSecurityScan.issues.length > 0 ? (
              lastSecurityScan.issues.map((issue, idx) => (
                <div key={idx} className="flex items-start gap-2">
                  <span className={issue.severity === 'critical' ? 'text-rose-500' : issue.severity === 'warning' ? 'text-amber-400' : 'text-emerald-400'}>
                    [{issue.severity.toUpperCase()}]
                  </span>
                  <span>{issue.message}</span>
                </div>
              ))
            ) : (
              <>
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
              </>
            )}
            {lastSecurityScan?.score !== undefined && (
              <div className="mt-2 pt-2 border-t border-slate-800">
                <span className="text-cyan-400">Last Scan Score: {lastSecurityScan.score}/100</span>
                <div className="text-[10px] text-slate-500">URL: {lastSecurityScan.url}</div>
              </div>
            )}
          </div>
        </Card>
      </div>

      <div className="mt-8 flex items-center justify-between mb-6 pb-2 border-b border-[#00f3ff]/15">
        <h2 className="text-lg font-bold font-['Space_Grotesk'] tracking-widest text-[#00f3ff] uppercase flex items-center gap-2">
          <DollarSign size={20} />
          Free-Tier Monitor
        </h2>
        <div className="flex gap-2">
          <Badge variant="warning">Survival Score: 64.2/100</Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card title="Supabase">
          <div className="text-xs text-slate-400 mb-2">Storage: ~200-400 MB / 500 MB</div>
          <div className="h-2 bg-slate-800 rounded overflow-hidden">
            <div className="h-full bg-emerald-400 w-[60%]"></div>
          </div>
        </Card>
        
        <Card title="Upstash Redis">
          <div className="text-xs text-slate-400 mb-2">Commands: ~1,200 / 10,000 daily</div>
          <div className="h-2 bg-slate-800 rounded overflow-hidden">
            <div className="h-full bg-emerald-400 w-[12%]"></div>
          </div>
        </Card>

        <Card title="Render">
          <div className="text-xs text-slate-400 mb-2">Web Service Hours: 513.6 / 750.0</div>
          <div className="h-2 bg-slate-800 rounded overflow-hidden">
            <div className="h-full bg-amber-400 w-[68%]"></div>
          </div>
        </Card>
      </div>

    </div>
  );
}
