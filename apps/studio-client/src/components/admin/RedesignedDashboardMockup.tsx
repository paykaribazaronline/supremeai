import { Home, Server, Shield, Activity, Settings, Cpu, HardDrive, X, DollarSign, Database, GitBranch, ShieldAlert, Sparkles, RefreshCw, Layout } from 'lucide-react';
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
 * 🎨 SupremeAI 2.0 - Dashboard Redesign Mockup (Deep Tactical Grid Theme & Executive Command Bridge Mode Toggle)
 * Real-time API Integration + Premium Sci-Fi Aesthetics & Simple Friendly Mode
 */

const RedesignedDashboardMockup: React.FC = () => {
  const activePanel = useDashboardStore((s) => s.activePanel);
  const setActivePanel = useDashboardStore((s) => s.setActivePanel);
  const setDeploymentModal = useDashboardStore((s) => s.setDeploymentModal);
  const updateSystemStatus = useDashboardStore((s) => s.updateSystemStatus);
  
  // বাংলা মন্তব্য: স্টোর থেকে ড্যাশবোর্ড মোড ও টগল ফাংশন নিয়ে আসা হলো
  const dashboardMode = useDashboardStore((s) => s.dashboardMode);
  const toggleDashboardMode = useDashboardStore((s) => s.toggleDashboardMode);

  const { data: metrics } = useMetrics();
  const { data: health } = useHealthMap();
  const { data: threats } = useThreatScan();
  const { data: costs } = useCostReport();
  const { data: ciReports } = useCIReports();

  const [isOptimizing, setIsOptimizing] = React.useState(false);
  const [optimizeStatus, setOptimizeStatus] = React.useState('');

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

  // কুইক অপ্টিমাইজেশন লজিক
  const runSmartOptimization = () => {
    setIsOptimizing(true);
    setOptimizeStatus('বিশ্লেষণ করা হচ্ছে...');
    setTimeout(() => {
      setOptimizeStatus('মেমোরি পরিষ্কার করা হচ্ছে...');
      setTimeout(() => {
        setOptimizeStatus('রিসোর্স অপ্টিমাইজেশন সফল!');
        setIsOptimizing(false);
        setTimeout(() => setOptimizeStatus(''), 2000);
      }, 1000);
    }, 1000);
  };

  const isSimple = dashboardMode === 'simple';

  return (
    <>
      <HealthBanner />
      <DeploymentModal />
      <DynamicPanel />
      
      {isSimple ? (
        // ==========================================
        // 🌟 SIMPLE MODE (User-Friendly Cockpit View)
        // ==========================================
        <div className="w-full h-screen bg-slate-50 text-slate-800 relative overflow-y-auto font-sans p-6 transition-colors duration-500">
          
          {/* Header */}
          <div className="flex justify-between items-center border-b border-slate-200 pb-5 mb-6">
            <div>
              <div className="flex items-center gap-2">
                <Sparkles className="text-indigo-600 animate-spin" size={20} />
                <h1 className="text-xl font-extrabold text-slate-900 tracking-tight">Executive Command Bridge</h1>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                সিস্টেমের গতিবিধি পর্যবেক্ষণ ও সাধারণ ইউজারদের জন্য সহজ ড্যাশবোর্ড ইন্টারফেস।
              </p>
            </div>
            
            {/* Mode Switcher */}
            <button
              onClick={toggleDashboardMode}
              className="flex items-center gap-2 px-4 py-2 text-xs font-bold text-indigo-700 bg-indigo-50 border border-indigo-200 hover:bg-indigo-100 rounded-xl transition-all shadow-sm"
            >
              <Layout size={14} />
              Switch to Developer Mode
            </button>
          </div>

          {/* KPI Cards Row */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            {/* System Health Card */}
            <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest font-mono">System Health</span>
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]"></span>
              </div>
              <p className="text-base font-bold text-slate-850">সিস্টেম গ্রিন</p>
              <p className="text-[11px] text-slate-500 mt-1">সব প্রধান সার্ভিস সচল আছে।</p>
            </div>

            {/* Threat Card */}
            <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest font-mono">Security</span>
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]"></span>
              </div>
              <p className="text-base font-bold text-slate-850">অনলাইন থ্রেট নাই</p>
              <p className="text-[11px] text-slate-500 mt-1">ফায়ারওয়াল সক্রিয় এবং নিরাপদ।</p>
            </div>

            {/* AI Skills Card */}
            <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest font-mono">AI Skills</span>
                <span className="text-xs font-mono font-bold text-indigo-600">4 Active</span>
              </div>
              <p className="text-base font-bold text-slate-850">৪টি পাইপলাইন সচল</p>
              <p className="text-[11px] text-slate-500 mt-1">এজেন্টরা নতুন টাস্ক প্রসেস করতে প্রস্তুত।</p>
            </div>

            {/* Cost Efficiency Card */}
            <div className="bg-white border border-slate-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest font-mono">Cost Per Hour</span>
                <span className="text-xs font-mono font-bold text-emerald-600">${metrics ? metrics.cost_per_hour : '0.00'}/h</span>
              </div>
              <p className="text-base font-bold text-slate-850">স্বল্প খরচে চলমান</p>
              <p className="text-[11px] text-slate-500 mt-1">ফ্রি-টিয়ার ক্লাউড অপ্টিমাইজেশন সক্রিয়।</p>
            </div>
          </div>

          {/* Split Sections */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            
            {/* Activity Feed */}
            <div className="lg:col-span-8 bg-white border border-slate-100 rounded-3xl p-6 shadow-sm">
              <h2 className="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <Sparkles size={16} className="text-indigo-500" />
                সিস্টেম আপডেট ও কার্যক্রম
              </h2>
              <div className="flex flex-col gap-4">
                <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="p-2 bg-indigo-50 rounded-lg text-indigo-600">✓</span>
                  <div>
                    <p className="text-xs font-bold text-slate-800">স্বয়ংক্রিয় সমাধান</p>
                    <p className="text-[11px] text-slate-500 mt-1">আজ সকালে সিস্টেম একটি ছোট এরর ঠিক করেছে, আপনার কোনো হস্তক্ষেপের প্রয়োজন নেই।</p>
                  </div>
                </div>
                <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="p-2 bg-emerald-50 rounded-lg text-emerald-600">⚡</span>
                  <div>
                    <p className="text-xs font-bold text-slate-800">সার্ভার হেলথ আপডেট</p>
                    <p className="text-[11px] text-slate-500 mt-1">ক্লাউড ক্লাস্টার রিকভারি সফল হয়েছে। ল্যাটেন্সি এখন স্বাভাবিক সীমার নিচে।</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Smart Actions Center */}
            <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 shadow-sm flex flex-col justify-between">
              <div>
                <h2 className="text-sm font-bold text-slate-900 mb-2">Smart Actions Center</h2>
                <p className="text-[11px] text-slate-500 mb-6">জটিল ব্যাকএন্ড কমান্ড সরাসরি ওয়ান-ক্লিকে রান করুন।</p>
                
                <div className="flex flex-col gap-3">
                  <button
                    onClick={runSmartOptimization}
                    disabled={isOptimizing}
                    className="w-full flex items-center justify-between p-3.5 rounded-xl border border-indigo-100 hover:bg-indigo-50/50 text-left transition-colors"
                  >
                    <div className="flex flex-col">
                      <span className="text-xs font-bold text-slate-800">সিস্টেম অপ্টিমাইজ করুন</span>
                      <span className="text-[9px] text-slate-400 mt-0.5">ক্লিন মেমোরি ও প্রসেস রিসেট</span>
                    </div>
                    {isOptimizing ? (
                      <RefreshCw size={14} className="text-indigo-600 animate-spin" />
                    ) : (
                      <span className="text-xs text-indigo-600 font-bold">রান করুন →</span>
                    )}
                  </button>

                  <button className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-100 hover:bg-slate-50 text-left transition-colors">
                    <div className="flex flex-col">
                      <span className="text-xs font-bold text-slate-800">রিপোর্ট ডাউনলোড করুন</span>
                      <span className="text-[9px] text-slate-400 mt-0.5">আজকের পারফরম্যান্স পিডিএফ ফাইল</span>
                    </div>
                    <span className="text-xs text-slate-400">ডাউনলোড →</span>
                  </button>
                </div>
              </div>

              {optimizeStatus && (
                <div className="mt-4 p-3 bg-indigo-50 text-indigo-700 rounded-xl text-center text-xs font-medium font-mono animate-pulse">
                  {optimizeStatus}
                </div>
              )}
            </div>

          </div>

        </div>
      ) : (
        // ==========================================
        // 🛰️ ADVANCED MODE (Sci-Fi Developer Canvas)
        // ==========================================
        <div className="w-full h-screen hex-grid-bg text-slate-200 relative overflow-hidden font-sans transition-colors duration-500">

          {/* --- Scanlines Overlay --- */}
          <div className="scanlines" />

          {/* --- Mode Switcher (Advanced View) --- */}
          <div className="absolute top-6 right-6 z-[100] flex gap-3">
            <button
              onClick={toggleDashboardMode}
              className="flex items-center gap-2 px-3 py-1.5 text-[10px] font-mono font-bold tracking-widest text-[#00f3ff] sci-fi-glass hover:bg-[#00f3ff]/20 transition-all border border-[#00f3ff]/30"
            >
              <Layout size={12} />
              SIMPLE MODE
            </button>
          </div>

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

              {/* Memory Gauge */}
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
      )}
    </>
  );
};

export default RedesignedDashboardMockup;

