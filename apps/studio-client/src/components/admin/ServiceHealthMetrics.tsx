import React, { useEffect, useState } from 'react';
import { Activity, Server, Database, Cpu, CheckCircle, Terminal, DollarSign, Users, Settings, Zap, Cloud, Eye, ShieldAlert, FileText, GitMerge, GitBranch, Save, Clock, Network } from 'lucide-react';
import { fetchJavaWorkerHealth, JavaWorkerHealth } from '../../services/api/microserviceMonitor';
import { useAdminStore } from '../../store/adminStore';
import type { AdminSubTab } from '../../types';

export const ServiceHealthMetrics: React.FC = () => {
  const [metrics, setMetrics] = useState<JavaWorkerHealth | null>(null);
  const setAdminSubTab = useAdminStore(state => state.setAdminSubTab);

  useEffect(() => {
    const loadMetrics = async () => {
      const data = await fetchJavaWorkerHealth();
      setMetrics(data);
    };
    
    loadMetrics();
    const interval = setInterval(loadMetrics, 5000);
    return () => clearInterval(interval);
  }, []);

  const navButtons: { id: AdminSubTab; label: string; icon: React.ReactNode; color: string }[] = [
    { id: 'command-center', label: 'Central ORC', icon: <Server size={14} />, color: 'text-[var(--text-main)]' },
    { id: 'sandbox', label: 'Sandbox', icon: <Terminal size={14} />, color: 'text-gray-500' },
    { id: 'logs', label: 'Logs', icon: <Activity size={14} />, color: 'text-blue-500' },
    { id: 'costs', label: 'Costs', icon: <DollarSign size={14} />, color: 'text-green-500' },
    { id: 'health', label: 'Health', icon: <CheckCircle size={14} />, color: 'text-emerald-500' },
    { id: 'users', label: 'Users', icon: <Users size={14} />, color: 'text-purple-500' },
    { id: 'config', label: 'Config', icon: <Settings size={14} />, color: 'text-gray-500' },
    { id: 'model-router', label: 'Router', icon: <Network size={14} />, color: 'text-indigo-500' },
    { id: 'skills', label: 'Skills', icon: <Zap size={14} />, color: 'text-yellow-500' },
    { id: 'memory', label: 'Memory', icon: <Database size={14} />, color: 'text-cyan-500' },
    { id: 'cloud', label: 'Cloud', icon: <Cloud size={14} />, color: 'text-sky-500' },
    { id: 'observability', label: 'Observe', icon: <Eye size={14} />, color: 'text-teal-500' },
    { id: 'threats', label: 'Threats', icon: <ShieldAlert size={14} />, color: 'text-red-500' },
    { id: 'rules', label: 'Rules', icon: <FileText size={14} />, color: 'text-orange-500' },
    { id: 'cicd', label: 'CI/CD', icon: <GitMerge size={14} />, color: 'text-pink-500' },
    { id: 'github', label: 'GitHub', icon: <GitBranch size={14} />, color: 'text-slate-500' },
    { id: 'backups', label: 'Backups', icon: <Save size={14} />, color: 'text-lime-500' },
    { id: 'rate-limits', label: 'Limits', icon: <Clock size={14} />, color: 'text-rose-500' }
  ];

  if (!metrics) return <div className="text-gray-500 p-2 text-xs font-mono">Loading Worker Metrics...</div>;

  return (
    <div className="bg-[var(--bg-panel)] border border-[var(--border-accent)] rounded-xl p-3 shadow-lg relative overflow-hidden group backdrop-blur-xl w-80 transition-colors duration-500">
      
      {/* Java Metrics Section (Compact) */}
      <div className="flex items-center justify-between mb-2 relative z-10 border-b border-[var(--border-accent)] pb-2 transition-colors duration-500">
        <h3 className="text-xs font-bold text-[var(--accent-primary)] flex items-center gap-1.5 uppercase tracking-wider font-mono">
          <Server size={12} className="animate-pulse" />
          Java Background Worker
        </h3>
        <span className={`px-1.5 py-0.5 rounded text-[8px] font-bold tracking-widest uppercase ${metrics.status === 'HEALTHY' ? 'bg-green-500/20 text-green-600 dark:text-[#00ff66] border border-green-500/50' : 'bg-red-500/20 text-red-600 dark:text-red-400 border border-red-500/50'}`}>
          {metrics.status}
        </span>
      </div>

      <div className="grid grid-cols-4 gap-1.5 relative z-10 mb-3">
        <div className="bg-black/5 dark:bg-[#0c1222] rounded p-1.5 border border-[var(--border-accent)] flex flex-col items-center justify-center transition-colors duration-500">
          <div className="text-[8px] text-gray-500 uppercase font-bold tracking-wider mb-0.5">CPU</div>
          <div className="text-xs font-mono text-[var(--text-main)]">{metrics.cpuLoadPercentage}%</div>
        </div>
        <div className="bg-black/5 dark:bg-[#0c1222] rounded p-1.5 border border-[var(--border-accent)] flex flex-col items-center justify-center transition-colors duration-500">
          <div className="text-[8px] text-gray-500 uppercase font-bold tracking-wider mb-0.5">Mem</div>
          <div className="text-xs font-mono text-[var(--text-main)]">{metrics.memoryUsageMb}M</div>
        </div>
        <div className="bg-black/5 dark:bg-[#0c1222] rounded p-1.5 border border-[var(--border-accent)] flex flex-col items-center justify-center transition-colors duration-500">
          <div className="text-[8px] text-gray-500 uppercase font-bold tracking-wider mb-0.5">Tasks</div>
          <div className="text-xs font-mono text-[var(--text-main)]">{metrics.activeTasks}</div>
        </div>
        <div className="bg-black/5 dark:bg-[#0c1222] rounded p-1.5 border border-[var(--border-accent)] flex flex-col items-center justify-center transition-colors duration-500">
          <div className="text-[8px] text-gray-500 uppercase font-bold tracking-wider mb-0.5">Done</div>
          <div className="text-xs font-mono text-[var(--text-main)]">{metrics.totalTasksProcessed}</div>
        </div>
      </div>

      {/* Quick Navigation Section */}
      <div className="relative z-10 border-t border-[var(--border-accent)] pt-2 transition-colors duration-500">
        <div className="text-[9px] text-[var(--accent-primary)] uppercase font-bold tracking-widest mb-2 px-1">Quick Navigate</div>
        <div className="grid grid-cols-3 gap-1.5">
          {navButtons.map((btn) => (
            <button
              key={btn.id}
              onClick={() => setAdminSubTab(btn.id)}
              className="flex items-center gap-1.5 bg-black/5 dark:bg-[#0c1222]/80 hover:bg-[var(--accent-primary)]/10 border border-transparent hover:border-[var(--border-accent)] rounded p-1.5 transition-all active:scale-95 group/btn"
              title={btn.label}
            >
              <div className={`${btn.color} group-hover/btn:scale-110 transition-transform`}>{btn.icon}</div>
              <span className="text-[9px] font-medium text-gray-600 dark:text-gray-300 group-hover/btn:text-[var(--text-main)] truncate">{btn.label}</span>
            </button>
          ))}
        </div>
      </div>

    </div>
  );
};
