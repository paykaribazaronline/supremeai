import { motion, AnimatePresence } from 'framer-motion';
import { X, Activity, ShieldAlert, DollarSign } from 'lucide-react';
import { useDashboardStore } from '../../store/dashboardStore';
import { useMetrics, useHealthMap, useThreatScan, useCostReport, useCIReports } from '../../hooks/useDashboardData';
import RulesEnginePanel from './RulesEnginePanel';
import AuditLogsPanel from './AuditLogsPanel';

export const DynamicPanel = () => {
  const { activePanel, setActivePanel } = useDashboardStore();
  const { data: metrics } = useMetrics();
  const { data: health } = useHealthMap();
  const { data: threats } = useThreatScan();
  const { data: costs } = useCostReport();
  const { data: ciReports } = useCIReports();

  const renderContent = () => {
    switch (activePanel) {
      case 'Threats':
        return (
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-[#ff0055]">
              <ShieldAlert size={16} />
              <span className="text-xs font-mono font-bold uppercase tracking-widest">Active Threats & Logs</span>
            </div>
            <ul className="list-disc pl-4 space-y-2 text-rose-300 font-mono text-xs">
              {ciReports && ciReports.length > 0 ? ciReports.map((log, i) => (
                <li key={i}>[{log.status || 'INFO'}] {log.message || log.commit_message || 'Pipeline event'}</li>
              )) : (
                <>
                  <li>[CRITICAL] Unauthorized access attempt from IP 192.168.x.x</li>
                  <li>[WARN] Rate limit exceeded for endpoint /api/auth</li>
                  <li>[INFO] DDoS mitigation active on Cloudflare edge</li>
                </>
              )}
            </ul>
            {threats && threats.findings && threats.findings.length > 0 && (
              <div className="p-2 bg-[#ff0055]/10 border border-[#ff0055]/30 rounded">
                <span className="text-[10px] font-mono text-[#ff0055] font-bold">SCAN RESULT: {threats.total_findings} findings</span>
              </div>
            )}
          </div>
        );
      case 'Observability':
        return (
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-[#00ff66]">
              <Activity size={16} />
              <span className="text-xs font-mono font-bold uppercase tracking-widest">System Health</span>
            </div>
            <ul className="list-disc pl-4 space-y-2 text-emerald-300 font-mono text-xs">
              <li>API Latency: {metrics?.latency_p50_ms || 42}ms (p50)</li>
              <li>Error Rate: {metrics?.error_rate || 0}%</li>
              <li>RPS: {metrics?.requests_per_second || 12}</li>
              <li>Active Providers: {metrics?.active_providers?.join(', ') || 'ollama'}</li>
              {health && health.gcp && <li>GCP Region: {health.gcp.region} - {health.gcp.status}</li>}
            </ul>
          </div>
        );
      case 'Costs':
        return (
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-[#00f3ff]">
              <DollarSign size={16} />
              <span className="text-xs font-mono font-bold uppercase tracking-widest">Cloud Spend</span>
            </div>
            <div className="text-cyan-300 font-mono text-xs space-y-2 whitespace-pre-wrap max-h-96 overflow-y-auto">
              {costs?.report || "Loading cost data..."}
            </div>
          </div>
        );
      case 'Audit':
        return (
          <div className="text-slate-400 text-xs font-mono">
            Audit module loaded via dedicated panel.
          </div>
        );
      default:
        return (
          <div className="text-slate-400 text-xs font-mono">
            Module data for {activePanel} is loading...
          </div>
        );
    }
  };

  return (
    <>
      <AnimatePresence>
        {activePanel && activePanel !== 'Rules' && activePanel !== 'Audit' && (
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 20, stiffness: 100 }}
            className="fixed right-0 top-0 h-full w-96 sci-fi-glass-panel p-6 z-50 shadow-2xl flex flex-col"
          >
            <div className="flex items-center justify-between border-b border-[rgba(0,243,255,0.2)] pb-4 mb-4">
              <h3 className="text-base font-mono font-bold text-white uppercase tracking-widest">
                {activePanel} Terminal
              </h3>
              <button
                onClick={() => setActivePanel(null)}
                className="text-[#00f3ff] hover:text-white bg-[#00f3ff]/10 hover:bg-[#00f3ff]/30 p-1.5 rounded-lg transition-colors border border-[#00f3ff]/30"
              >
                <X size={18} />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              {renderContent()}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      <RulesEnginePanel />
      <AuditLogsPanel />
    </>
  );
};
