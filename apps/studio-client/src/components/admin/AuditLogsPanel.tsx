import React, { useState, useEffect } from 'react';
import { X, ScrollText, Activity } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDashboardStore } from '../../store/dashboardStore';
import { apiClient } from '../../services/apiClient';

interface AuditEntry {
  timestamp: string;
  actor: string;
  action: string;
  detail: string;
  severity: 'info' | 'warn' | 'error' | 'success';
}

const severityColor: Record<string, string> = {
  success: 'text-[#00ff66]',
  info: 'text-[#00f3ff]',
  warn: 'text-[#ffaa00]',
  error: 'text-[#ff0055]',
};

const AuditLogsPanel: React.FC = () => {
  const activePanel = useDashboardStore((s) => s.activePanel);
  const setActivePanel = useDashboardStore((s) => s.setActivePanel);

  const [logs, setLogs] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (activePanel !== 'Audit') return;
    setLoading(true);
    apiClient
      .get<{ logs: AuditEntry[] }>('/admin-api/audit-logs?limit=50')
      .then((res) => setLogs(res.logs ?? []))
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, [activePanel]);

  if (activePanel !== 'Audit') return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ x: '100%' }}
        animate={{ x: 0 }}
        exit={{ x: '100%' }}
        transition={{ type: 'spring', damping: 20, stiffness: 100 }}
        className="fixed right-0 top-0 h-full w-[28rem] sci-fi-glass-panel p-6 z-50 shadow-2xl flex flex-col"
      >
        <div className="flex items-center justify-between border-b border-[rgba(0,243,255,0.2)] pb-4 mb-4">
          <div className="flex items-center gap-2 text-[#00f3ff]">
            <ScrollText size={16} />
            <h3 className="text-sm font-mono font-bold text-white uppercase tracking-widest">
              Audit Trail
            </h3>
          </div>
          <button
            onClick={() => setActivePanel(null)}
            className="text-[#00f3ff] hover:text-white bg-[#00f3ff]/10 hover:bg-[#00f3ff]/30 p-1.5 rounded-lg transition-colors border border-[#00f3ff]/30"
          >
            <X size={18} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto space-y-4">
          {loading && (
            <div className="text-slate-400 text-xs font-mono animate-pulse">Loading audit stream...</div>
          )}
          {!loading && logs.length === 0 && (
            <div className="text-slate-400 text-xs font-mono italic">No audit events recorded in this window.</div>
          )}
          {logs.map((entry, idx) => (
            <motion.div
              key={entry.timestamp + idx}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: Math.min(idx * 0.02, 0.4) }}
              className="sci-fi-glass rounded-lg p-3 border border-slate-700/40"
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-slate-400">
                  {new Date(entry.timestamp).toLocaleString()}
                </span>
                <span className={`text-[10px] font-mono font-bold ${severityColor[entry.severity] || 'text-slate-400'}`}>
                  {entry.severity.toUpperCase()}
                </span>
              </div>
              <div className="mt-1 text-xs font-mono text-white">
                <span className="text-slate-400 mr-2">[{entry.actor}]</span>
                {entry.action}
              </div>
              <div className="text-[11px] text-slate-300 font-mono mt-1">{entry.detail}</div>
            </motion.div>
          ))}
        </div>

        <div className="mt-4 pt-4 border-t border-[rgba(0,243,255,0.2)] flex gap-3">
          <button
            onClick={() => setActivePanel(null)}
            className="flex-1 bg-slate-900/50 hover:bg-slate-800/50 text-slate-400 hover:text-white font-mono font-bold py-2.5 rounded-lg transition-colors border border-slate-700 uppercase tracking-widest text-xs"
          >
            Close
          </button>
        </div>
      </motion.div>
    </AnimatePresence>
  );
};

export default AuditLogsPanel;
