import React, { useState, useEffect } from 'react';
import { Home, Server, Shield, Activity, Settings, Cpu, HardDrive, X, DollarSign, Database, GitBranch, ShieldAlert } from 'lucide-react';
import ReactFlow, { Background, Controls, useNodesState, useEdgesState, Panel } from 'reactflow';
import { motion, AnimatePresence } from 'framer-motion';
import 'reactflow/dist/style.css';
import './AethelCoreStyles.css'; // Importing the newly updated premium sci-fi styles

/**
 * 🎨 SupremeAI 2.0 - Dashboard Redesign Mockup (Deep Tactical Grid Theme)
 * Real-time API Integration + Premium Sci-Fi Aesthetics
 */

const RedesignedDashboardMockup: React.FC = () => {
  const [activeModal, setActiveModal] = useState<string | null>(null);

  // Real data state
  const [metrics, setMetrics] = useState<any>(null);
  const [costs, setCosts] = useState<any>(null);
  const [health, setHealth] = useState<any>(null);
  const [ciLogs, setCiLogs] = useState<any[]>([]);

  useEffect(() => {
    const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';
    const token = localStorage.getItem('supremeai_admin_token') || '';

    const fetchData = async () => {
      if (!token) return;
      const headers = {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      };

      try {
        const [metricsRes, costsRes, healthRes, ciLogsRes] = await Promise.all([
          fetch(`${API_BASE}/admin-api/metrics`, { headers }),
          fetch(`${API_BASE}/admin-api/costs`, { headers }),
          fetch(`${API_BASE}/admin-api/health-map`, { headers }),
          fetch(`${API_BASE}/admin-api/ci-logs?limit=5`, { headers })
        ]);

        if (metricsRes.ok) setMetrics(await metricsRes.json());
        if (costsRes.ok) setCosts(await costsRes.json());
        if (healthRes.ok) setHealth(await healthRes.json());
        if (ciLogsRes.ok) setCiLogs(await ciLogsRes.json());
      } catch (err) {
        console.error("Dashboard data sync failed:", err);
      }
    };

    fetchData();
    const intervalId = setInterval(fetchData, 5000);
    return () => clearInterval(intervalId);
  }, []);

  const handleTakeAction = async () => {
    if (!window.confirm(`Are you sure you want to execute action for ${activeModal}?`)) return;
    const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';
    const token = localStorage.getItem('supremeai_admin_token') || '';
    
    let endpoint = '/admin-api/deploy';
    if (activeModal === 'Threats') endpoint = '/admin-api/security-scan';
    else if (activeModal === 'Costs') endpoint = '/admin-api/deploy';
    
    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });
      if (res.ok) {
        alert("Action executed successfully.");
      } else {
        alert("Action failed.");
      }
    } catch (err) {
      alert("Action error: " + err);
    }
  };

  const renderModuleContent = () => {
    switch(activeModal) {
      case 'Threats':
        return (
          <div className="mt-6 p-4 sci-fi-glass sci-fi-glass-danger rounded-xl">
            <span className="text-[#ff0055] font-bold mb-2 block flex items-center gap-2"><ShieldAlert size={16}/> Active Threats & Logs</span>
            <ul className="list-disc pl-4 space-y-2 text-rose-300 font-mono text-xs">
              {ciLogs && ciLogs.length > 0 ? ciLogs.map((log, i) => (
                <li key={i}>[{log.status || 'INFO'}] {log.message || log.commit_message || 'Pipeline event'}</li>
              )) : (
                <>
                  <li>[CRITICAL] Unauthorized access attempt from IP 192.168.x.x</li>
                  <li>[WARN] Rate limit exceeded for endpoint /api/auth</li>
                  <li>[INFO] DDoS mitigation active on Cloudflare edge</li>
                </>
              )}
            </ul>
          </div>
        );
      case 'Observability':
        return (
          <div className="mt-6 p-4 sci-fi-glass sci-fi-glass-success rounded-xl">
            <span className="text-[#00ff66] font-bold mb-2 block flex items-center gap-2"><Activity size={16}/> System Health</span>
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
          <div className="mt-6 p-4 sci-fi-glass sci-fi-glass-panel rounded-xl">
            <span className="text-[#00f3ff] font-bold mb-2 block flex items-center gap-2"><DollarSign size={16}/> Cloud Spend</span>
            <div className="text-cyan-300 font-mono text-xs space-y-2 whitespace-pre-wrap max-h-40 overflow-y-auto">
              {costs?.report || "Loading cost data..."}
            </div>
          </div>
        );
      default:
        return (
          <div className="mt-6 p-4 sci-fi-glass rounded-xl text-slate-400 text-xs font-mono">
            Module data for {activeModal} is loading...
          </div>
        );
    }
  };

  const [nodes, setNodes, onNodesChange] = useNodesState([
    {
      id: 'central',
      type: 'default',
      data: {
        label: (
          <div className="flex flex-col items-center justify-center relative w-64 h-64">
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
      className: 'bg-transparent border-none', // Override defaults
    },
    {
      id: 'observability',
      type: 'default',
      data: {
        label: (
          <button onClick={() => setActiveModal('Observability')} className="hud-node w-full h-full flex flex-col items-center justify-center">
            <Activity size={32} className="text-[#00ff66] mb-2 drop-shadow-[0_0_10px_rgba(0,255,102,0.8)]" />
            <span className="font-mono text-xs font-bold text-[#00ff66] tracking-widest">OBSERVABILITY</span>
            <div className="mt-2 flex gap-1">
              <div className="w-2 h-2 rounded-full bg-[#00ff66] animate-ping" />
              <div className="w-2 h-2 rounded-full bg-[#00ff66]" />
            </div>
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
          <button onClick={() => setActiveModal('Threats')} className="hud-node w-full h-full flex flex-col items-center justify-center !border-[#ff0055]">
            <Shield size={32} className="text-[#ff0055] mb-2 drop-shadow-[0_0_10px_rgba(255,0,85,0.8)]" />
            <span className="font-mono text-xs font-bold text-[#ff0055] tracking-widest">THREATS</span>
            <div className="absolute top-2 right-2 text-[#ff0055] text-[8px] animate-pulse">! ALERT</div>
          </button>
        )
      },
      position: { x: 50, y: 350 },
      className: 'bg-transparent border-none w-48 h-32',
    }
  ]);

  const [edges, setEdges, onEdgesChange] = useEdgesState([
    { id: 'e1', source: 'observability', target: 'central', animated: true, className: 'edge-success' },
    { id: 'e2', source: 'threats', target: 'central', animated: true, className: 'edge-threat' },
  ]);

  return (
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
                <span className="text-[10px] font-mono font-bold text-[#00f3ff]">42%</span>
              </div>
            </div>
            <span className="text-[9px] font-mono text-cyan-500 tracking-widest uppercase">CPU Usage</span>
          </div>

          {/* MEM Gauge */}
          <div className="flex flex-col items-center gap-2">
            <div className="relative w-16 h-16 flex items-center justify-center rounded-full border-4 border-[#09101f] border-t-[#00ff66] border-r-[#00ff66] shadow-[0_0_15px_rgba(0,255,102,0.3)] animate-[spin_6s_linear_infinite_reverse]">
              <div className="absolute w-full h-full rounded-full animate-[spin_6s_linear_infinite] flex items-center justify-center">
                <span className="text-[10px] font-mono font-bold text-[#00ff66]">1.2 GB</span>
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
            onClick={() => setActiveModal(item.id)}
            className={`p-2 rounded-xl transition-all ${activeModal === item.id ? `bg-[#00f3ff]/20 shadow-[inset_0_0_10px_rgba(0,243,255,0.5)] ${item.activeColor}` : `text-slate-500 ${item.color}`}`}
            title={item.id}
          >
            {item.icon}
          </button>
        ))}
      </div>

      {/* --- Slide-over Panel / Modal (Framer Motion) --- */}
      <AnimatePresence>
        {activeModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-slate-950/70 backdrop-blur-md z-50 flex justify-end"
          >
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 20, stiffness: 100 }}
              className="w-[450px] h-full sci-fi-glass-panel p-6 flex flex-col rounded-none"
            >
              
              <div className="flex justify-between items-center border-b border-[rgba(0,243,255,0.2)] pb-4 mb-4">
                <h3 className="text-lg font-mono font-bold text-white uppercase tracking-widest flex items-center gap-2 drop-shadow-[0_0_8px_rgba(255,255,255,0.8)]">
                  {activeModal === 'Threats' ? <Shield className="text-[#ff0055]" /> : <Activity className="text-[#00ff66]" />}
                  {activeModal} Terminal
                </h3>
                <button onClick={() => setActiveModal(null)} className="text-[#00f3ff] hover:text-white bg-[#00f3ff]/10 hover:bg-[#00f3ff]/30 p-1.5 rounded-lg transition-colors border border-[#00f3ff]/30">
                  <X size={20} />
                </button>
              </div>

              <div className="flex-1 text-[#00f3ff] text-sm font-mono leading-relaxed overflow-y-auto">
                <p className="text-[10px] opacity-70 mb-4 tracking-widest uppercase">
                  &gt; INITIALIZING MODULE: {activeModal}<br/>
                  &gt; ESTABLISHING SECURE CONNECTION... OK.<br/>
                  &gt; WAITING FOR DATA STREAM...
                </p>
                
                {renderModuleContent()}
              </div>

              <div className="mt-auto pt-4 border-t border-[rgba(0,243,255,0.2)] relative">
                <div className="absolute -top-[1px] left-0 w-1/3 h-[2px] bg-[#00f3ff] shadow-[0_0_10px_#00f3ff]" />
                <button onClick={handleTakeAction} className="w-full bg-[#00f3ff]/20 hover:bg-[#00f3ff]/40 text-[#00f3ff] hover:text-white font-mono font-bold py-3 rounded-lg transition-colors border border-[#00f3ff] shadow-[0_0_15px_rgba(0,243,255,0.3)] hover:shadow-[0_0_25px_rgba(0,243,255,0.6)] uppercase tracking-widest">
                  Execute Command
                </button>
              </div>

            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
};

export default RedesignedDashboardMockup;
