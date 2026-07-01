import React from 'react';
import { Home, Server, Shield, Activity, Settings, Cpu, HardDrive, X, DollarSign, Database, GitBranch, ShieldAlert } from 'lucide-react';
import ReactFlow, { Background, Controls, useNodesState, useEdgesState, Panel } from 'reactflow';
import { motion, AnimatePresence } from 'framer-motion';
import 'reactflow/dist/style.css';
import './AethelCoreStyles.css';
import { useMetrics, useHealthMap, useThreatScan, useCostReport, useCIReports } from '../../hooks/useDashboardData';
import { useDashboardStore } from '../../store/dashboardStore';
import HealthBanner from './HealthBanner';
import DeploymentModal from './DeploymentModal';
import { DynamicPanel } from './DynamicPanel';

/**
 * 🎨 SupremeAI 2.0 - Dashboard Redesign Mockup (Deep Tactical Grid Theme)
 * Real-time API Integration + Premium Sci-Fi Aesthetics
 */

const RedesignedDashboardMockup: React.FC = () => {
  const activePanel = useDashboardStore((s) => s.activePanel);
  const setActivePanel = useDashboardStore((s) => s.setActivePanel);
  const setDeploymentModal = useDashboardStore((s) => s.setDeploymentModal);
  const updateSystemStatus = useDashboardStore((s) => s.updateSystemStatus);
  const { data: metrics } = useMetrics();
  const { data: health } = useHealthMap();
  const { data: threats } = useThreatScan();
  const { data: costs } = useCostReport();
  const { data: ciReports } = useCIReports();

  React.useEffect(() => {
    if (threats?.total_findings && threats.total_findings > 0) {
      updateSystemStatus('degraded');
    } else if (ciReports?.some((r) => r.status === 'failed' || r.status === 'failure')) {
      updateSystemStatus('degraded');
    } else {
      updateSystemStatus('healthy');
    }
  }, [threats, ciReports, updateSystemStatus]);

  const [nodes, , onNodesChange] = useNodesState([
    {
      id: 'central',
      type: 'default',
      data: {
        label: (
          <div className="flex flex-col items-center justify-center relative w-64 h-64 cursor-pointer" onClick={() => setDeploymentModal(true)}>
            {/* Holographic Spinning Orbs */}
            <div className="central-orb-outer">
              <div className="central-orb-inner">
                <div className="central-orb-core">
                  <Cpu size={40} className="text-[#0a0f1e] central-orb-core-icon" />
                </div>
              </div>
            </div>
            {/* Title Badge */}
            <div className="absolute -bottom-8 sci-fi-glass px-4 py-2 text-[#00f3ff] font-mono font-bold tracking-widest text-sm border-t-2 border-[#00f3ff]">
              ORCHESTRATOR
            </div>
          </div>
        )
      },
      position: { x: 400, y: 200 },
      className: 'bg-transparent border-none',
    },
    {
      id: 'observability',
      type: 'default',
      data: {
        label: (
          <button onClick={() => setActivePanel('Observability')} className="hud-node w-full h-full flex flex-col items-center justify-center">
            <Activity size={32} className={`mb-2 drop-shadow-[0_0_10px_rgba(0,255,102,0.8)] ${health?.gcp?.status !== 'healthy' ? 'text-[#ffaa00]' : 'text-[#00ff66]'}`} />
            <span className={`font-mono text-xs font-bold tracking-widest ${health?.gcp?.status !== 'healthy' ? 'text-[#ffaa00]' : 'text-[#00ff66]'}`}>OBSERVABILITY</span>
            <div className="mt-2 flex gap-1">
              <div className={`w-2 h-2 rounded-full animate-ping ${health?.gcp?.status === 'healthy' ? 'bg-[#00ff66]' : 'bg-[#ffaa00]'}`} />
              <div className={`w-2 h-2 rounded-full ${health?.gcp?.status === 'healthy' ? 'bg-[#00ff66]' : 'bg-[#ffaa00]'}`} />
            </div>
            {metrics && <div className="mt-1 text-[10px] font-mono text-[#00ff66] opacity-80">{metrics.requests_per_second} RPS</div>}
          </button>
        )
      },
      position: { x: 50, y: 150 },
      className: 'bg-transparent border-none w-48 h-32',
    },
    {
      id: 'threats',
      type: 'default',
      data: {
        label: (
          <button onClick={() => setActivePanel('Threats')} className={`hud-node w-full h-full flex flex-col items-center justify-center ${threats && threats.total_findings > 0 ? '!border-[#ff0055]' : '!border-[#00ff66]'}`}>
            <Shield size={32} className={`mb-2 drop-shadow-[0_0_10px_rgba(255,0,85,0.8)] ${threats && threats.total_findings > 0 ? 'text-[#ff0055]' : 'text-[#00ff66]'}`} />
            <span className={`font-mono text-xs font-bold tracking-widest ${threats && threats.total_findings > 0 ? 'text-[#ff0055]' : 'text-[#00ff66]'}`}>
              THREATS {threats && threats.total_findings > 0 ? `(${threats.total_findings})` : ''}
            </span>
            <div className="absolute top-2 right-2 text-[8px] animate-pulse">
              {threats && threats.total_findings > 0 ? <span className="text-[#ff0055]">! ALERT</span> : <span className="text-[#00ff66]">OK</span>}
            </div>
          </button>
        )
      },
      position: { x: 50, y: 350 },
      className: 'bg-transparent border-none w-48 h-32',
    },
    {
      id: 'cicd',
      type: 'default',
      data: {
        label: (
          <button onClick={() => setActivePanel('GitHub')} className={`hud-node w-full h-full flex flex-col items-center justify-center ${(ciReports?.some(r => r.status === 'failed' || r.status === 'failure')) ? 'animate-pulse !border-[#ff0055]' : '!border-[#00ff66]'}`}>
            <GitBranch size={32} className={`mb-2 drop-shadow-[0_0_10px_rgba(0,243,255,0.8)] ${(ciReports?.some(r => r.status === 'failed' || r.status === 'failure')) ? 'text-[#ff0055]' : 'text-[#00ff66]'}`} />
            <span className={`font-mono text-xs font-bold tracking-widest ${(ciReports?.some(r => r.status === 'failed' || r.status === 'failure')) ? 'text-[#ff0055]' : 'text-[#00ff66]'}`}>CI/CD PIPELINES</span>
            <div className="absolute top-2 right-2 text-[8px] animate-pulse">
              {(ciReports?.some(r => r.status === 'failed' || r.status === 'failure')) ? <span className="text-[#ff0055]">! FAIL</span> : <span className="text-[#00ff66]">OK</span>}
            </div>
          </button>
        )
      },
      position: { x: 250, y: 500 },
      className: 'bg-transparent border-none w-48 h-32',
    }
  ]);

  const [edges, , onEdgesChange] = useEdgesState([
    { id: 'e1', source: 'observability', target: 'central', animated: true, className: 'edge-success' },
    { id: 'e2', source: 'threats', target: 'central', animated: true, className: 'edge-threat' },
    { id: 'e3', source: 'cicd', target: 'central', animated: true, className: ciReports?.some(r => r.status === 'failed' || r.status === 'failure') ? 'edge-threat' : 'edge-success' },
  ]);

  return (
    <>
      <HealthBanner />
      <DeploymentModal />
      <DynamicPanel />
      <div className="w-full h-screen hex-grid-bg text-slate-200 relative overflow-hidden font-sans">

      {/* --- Scanlines Overlay --- */}
      <div className="scanlines" />

      {/* --- Main Flow Canvas --- */}
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
        attributionPosition="bottom-right"
      >
        <Background color="#00f3ff" gap={40} size={1} className="opacity-10" />
        <Controls className="sci-fi-glass fill-[#00f3ff] text-[#00f3ff] border-[#00f3ff]" />
      </ReactFlow>

      {/* --- Floating Compact Java Worker Widget (Upgraded) --- */}
      <div className="absolute top-6 left-6 w-80 sci-fi-glass p-5 shadow-2xl flex flex-col gap-4 border border-[#00f3ff]/30 z-[100]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[rgba(0,243,255,0.2)] pb-3">
          <div className="flex items-center gap-3">
            <button className="p-1.5 bg-[#00f3ff]/10 hover:bg-[#00f3ff]/20 border border-[#00f3ff]/30 rounded-md transition-colors text-[#00f3ff]">
              <Home size={16} />
            </button>
            <h2 className="text-xs font-mono font-bold tracking-widest text-[#00f3ff] uppercase drop-shadow-[0_0_5px_rgba(0,243,255,0.8)]">Java Worker Node</h2>
          </div>
          {/* Glowing State Indicator */}
          <div className="flex items-center gap-2">
            <span className="text-[9px] font-mono text-[#00ff66]">ONLINE</span>
            <div className="relative flex h-3 w-3 items-center justify-center">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#00ff66] opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-[#00ff66] shadow-[0_0_8px_#00ff66]"></span>
            </div>
          </div>
        </div>

        {/* Circular Gauges for CPU/MEM */}
        <div className="flex justify-around items-center">
          {/* CPU Gauge */}
          <div className="flex flex-col items-center gap-2">
            <div className="relative w-16 h-16 flex items-center justify-center rounded-full border-4 border-[#09101f] border-t-[#00f3ff] border-l-[#00f3ff] shadow-[0_0_15px_rgba(0,243,255,0.3)] animate-[spin_4s_linear_infinite]">
              <div className="absolute w-full h-full rounded-full animate-[spin_4s_linear_infinite_reverse] flex items-center justify-center">
                <span className="text-[10px] font-mono font-bold text-[#00f3ff]">{metrics ? `${metrics.requests_per_second} RPS` : '--'}</span>
              </div>
            </div>
            <span className="text-[9px] font-mono text-cyan-500 tracking-widest uppercase">CPU Usage</span>
          </div>

          {/* MEM Gauge */}
          <div className="flex flex-col items-center gap-2">
            <div className="relative w-16 h-16 flex items-center justify-center rounded-full border-4 border-[#09101f] border-t-[#00ff66] border-r-[#00ff66] shadow-[0_0_15px_rgba(0,255,102,0.3)] animate-[spin_6s_linear_infinite_reverse]">
              <div className="absolute w-full h-full rounded-full animate-[spin_6s_linear_infinite] flex items-center justify-center">
                <span className="text-[10px] font-mono font-bold text-[#00ff66]">{metrics ? `$${metrics.cost_per_hour}/h` : '$--/h'}</span>
              </div>
            </div>
            <span className="text-[9px] font-mono text-emerald-500 tracking-widest uppercase">Memory</span>
          </div>
        </div>
      </div>

      {/* --- Floating Dock Sidebar (Command Center) --- */}
      <div className="absolute top-44 left-6 w-14 sci-fi-glass py-4 flex flex-col items-center gap-4 border-l-2 border-l-[#00f3ff] z-[100]">
        {[
          { id: 'Costs', icon: <DollarSign size={20} />, color: 'hover:text-[#00f3ff]', activeColor: 'text-[#00f3ff]' },
          { id: 'Config', icon: <Settings size={20} />, color: 'hover:text-slate-300', activeColor: 'text-slate-300' },
          { id: 'Memory', icon: <Database size={20} />, color: 'hover:text-[#00ff66]', activeColor: 'text-[#00ff66]' },
          { id: 'Threats', icon: <Shield size={20} />, color: 'hover:text-[#ff0055]', activeColor: 'text-[#ff0055]' },
          { id: 'GitHub', icon: <GitBranch size={20} />, color: 'hover:text-slate-300', activeColor: 'text-slate-300' },
        ].map((item) => (
          <button
            key={item.id}
            onClick={() => setActivePanel(item.id)}
            className={`p-2 rounded-xl transition-all ${activePanel === item.id ? `bg-[#00f3ff]/20 shadow-[inset_0_0_10px_rgba(0,243,255,0.5)] ${item.activeColor}` : `text-slate-500 ${item.color}`}`}
            title={item.id}
          >
            {item.icon}
          </button>
        ))}
      </div>

    </div>
    </>
  );
};

export default RedesignedDashboardMockup;
