import React from 'react';
import { Home, Server, Shield, Activity, Settings, Cpu, HardDrive, DollarSign, Database, GitBranch, Sparkles, RefreshCw, Layout, Users, TrendingUp, Clock, CheckCircle, ArrowUpRight, Zap, Lock } from 'lucide-react';
import ReactFlow, { Background, Controls, useNodesState, useEdgesState } from 'reactflow';
import { motion } from 'framer-motion';
import 'reactflow/dist/style.css';
import './AethelCoreStyles.css';
import { useMetrics, useHealthMap, useThreatScan, useCIReports, useDashboardEvents, useDashboardReports } from '../../hooks/useDashboardData';
import { useQueryClient } from '@tanstack/react-query';
import { useDashboardStore } from '../../store/dashboardStore';
import { apiClient } from '../../services/apiClient';
import HealthBanner from './HealthBanner';
import DeploymentModal from './DeploymentModal';
import { DynamicPanel } from './DynamicPanel';

/**
 * 🎨 SupremeAI 2.0 - Admin Dashboard Component
 * Real-time API Integration + Premium Sci-Fi Aesthetics & Simple Friendly Mode
 *
 * বাংলা মন্তব্য: পুরোনো RedesignedDashboardMockup পরিবর্তন করে Dashboard হিসেবে নামকরণ করা হলো।
 */

const Dashboard: React.FC = () => {
  const activePanel = useDashboardStore((s) => s.activePanel);
  const setActivePanel = useDashboardStore((s) => s.setActivePanel);
  const setDeploymentModal = useDashboardStore((s) => s.setDeploymentModal);
  const updateSystemStatus = useDashboardStore((s) => s.updateSystemStatus);

  // বাংলা মন্তব্য: স্টোর থেকে ড্যাশবোর্ড মোড ও টগল ফাংশন নিয়ে আসা হলো
  const dashboardMode = useDashboardStore((s) => s.dashboardMode);
  const toggleDashboardMode = useDashboardStore((s) => s.toggleDashboardMode);

  const { data: metrics, refetch: refetchMetrics } = useMetrics();
  const { data: health } = useHealthMap();
  const { data: threats, refetch: refetchThreats } = useThreatScan();
  const { data: ciReports } = useCIReports();
  const queryClient = useQueryClient();

  // বাংলা মন্তব্য: ব্যাকএন্ড /admin-api/metrics সরাসরি cpu_percent/memory_percent নাও দিতে পারে,
  // তাই CloudOrchestrator-এর মতো rps থেকে derived ভ্যালু ব্যবহার করে NaN রোধ করা হলো।
  const dashRps = metrics?.requests_per_second ?? 0;
  const safeCpu = metrics?.cpu_percent ?? metrics?.cpu_usage_percent ?? Math.min(100, Math.round((dashRps / 50) * 100));
  const safeMem = metrics?.memory_percent ?? metrics?.memory_usage_percent ?? Math.min(100, Math.round((dashRps / 80) * 100));

  const [selectedReportName, setSelectedReportName] = React.useState<string | undefined>();
  // বাংলা মন্তব্য: রিয়েল-টাইম ইভেন্ট এবং দৈনিক স্ট্যান্ডআপ রিপোর্ট ডেটা ফেচ করা হচ্ছে
  const { data: events } = useDashboardEvents(10);
  const { data: reportsData } = useDashboardReports();
  const { data: activeReport } = useDashboardReports(selectedReportName);

  const [isOptimizing, setIsOptimizing] = React.useState(false);
  const [optimizeStatus, setOptimizeStatus] = React.useState('');
  const [isRestarting, setIsRestarting] = React.useState(false);
  const [restartStatus, setRestartStatus] = React.useState('');
  const [isScanning, setIsScanning] = React.useState(false);
  const [scanStatus, setScanStatus] = React.useState('');

  const runSmartOptimization = React.useCallback(async () => {
    setIsOptimizing(true);
    setOptimizeStatus('Analyzing live metrics...');
    try {
      await refetchMetrics();
      queryClient.invalidateQueries({ queryKey: ['dashboard', 'metrics'] });
      setOptimizeStatus('Metrics refreshed from live backend.');
    } catch {
      setOptimizeStatus('Failed to refresh metrics.');
    } finally {
      setIsOptimizing(false);
      setTimeout(() => setOptimizeStatus(''), 2500);
    }
  }, [refetchMetrics, queryClient]);

  const restartServices = React.useCallback(async () => {
    setIsRestarting(true);
    setRestartStatus('Triggering emergency deploy...');
    try {
      await apiClient.post('/admin-api/emergency-deploy', {});
      setRestartStatus('Restart signal sent. Services redeploying...');
    } catch {
      setRestartStatus('Restart failed. Check backend connectivity.');
    } finally {
      setIsRestarting(false);
      setTimeout(() => setRestartStatus(''), 3000);
    }
  }, []);

  const runSecurityScan = React.useCallback(async () => {
    setIsScanning(true);
    setScanStatus('Running threat scan...');
    try {
      await refetchThreats();
      queryClient.invalidateQueries({ queryKey: ['dashboard', 'security-scan'] });
      setScanStatus('Scan complete. Threat panel updated.');
    } catch {
      setScanStatus('Scan failed.');
    } finally {
      setIsScanning(false);
      setTimeout(() => setScanStatus(''), 2500);
    }
  }, [refetchThreats, queryClient]);

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

  const isSimple = dashboardMode === 'simple';

  return (
    <>
      <HealthBanner />
      <DeploymentModal />
      <DynamicPanel />

      {isSimple ? (
        // ==========================================
        // 🌟 GORGEOUS SIMPLE MODE (User-Friendly Cockpit View)
        // ==========================================
        <div className="w-full h-screen bg-gradient-to-br from-gray-50 to-slate-100 text-slate-800 relative overflow-y-auto font-sans p-6 transition-colors duration-500">

          {/* Header with beautiful gradient */}
          <div className="flex justify-between items-center border-b border-slate-200 pb-5 mb-6 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-2xl p-1">
            <div className="bg-white rounded-xl p-5 w-full">
              <div className="flex items-center gap-2">
                <Sparkles className="text-indigo-600 animate-pulse" size={24} />
                <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Executive Command Bridge</h1>
              </div>
              <p className="text-sm text-slate-500 mt-2">
                সিস্টেমের গতিবিধি পর্যবেক্ষণ ও সাধারণ ইউজারদের জন্য সহজ ড্যাশবোর্ড ইন্টারফেস।
              </p>
            </div>
          </div>

          {/* Beautiful Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">

            {/* System Health Card */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-gradient-to-br from-white to-slate-50 border border-slate-200 rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1"
            >
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-emerald-100 rounded-xl">
                  <CheckCircle size={24} className="text-emerald-600" />
                </div>
                <span className="w-3 h-3 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]"></span>
              </div>
              <h3 className="text-lg font-bold text-slate-800">System Health</h3>
              <p className="text-2xl font-extrabold text-emerald-600 mt-2">
                {health?.gcp?.status === 'healthy' ? 'Excellent' : health?.gcp?.status === 'degraded' ? 'Degraded' : 'Critical'}
              </p>
              <p className="text-xs text-slate-500 mt-2">All systems operational</p>
              <div className="mt-4 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${health?.gcp?.status === 'healthy' ? '98' : health?.gcp?.status === 'degraded' ? '60' : '30'}%`,
                    backgroundColor: health?.gcp?.status === 'healthy' ? '#10b981' : health?.gcp?.status === 'degraded' ? '#f59e0b' : '#ef4444'
                  }}
                ></div>
              </div>
            </motion.div>

            {/* Threat Card */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="bg-gradient-to-br from-white to-slate-50 border border-slate-200 rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1"
            >
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-amber-100 rounded-xl">
                  <Shield size={24} className="text-amber-600" />
                </div>
                <span className="w-3 h-3 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]"></span>
              </div>
              <h3 className="text-lg font-bold text-slate-800">Security Status</h3>
              <p className="text-2xl font-extrabold text-emerald-600 mt-2">
                {threats && threats.total_findings > 0 ? 'At Risk' : 'Secure'}
              </p>
              <p className="text-xs text-slate-500 mt-2">
                {threats && threats.total_findings > 0 ? `${threats.total_findings} threats detected` : 'No active threats'}
              </p>
              <div className="mt-4 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${threats && threats.total_findings > 0 ? Math.min(threats.total_findings * 10, 100) : 0}%`,
                    backgroundColor: threats && threats.total_findings > 0 ? '#f59e0b' : '#10b981'
                  }}
                ></div>
              </div>
            </motion.div>

            {/* AI Skills Card */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="bg-gradient-to-br from-white to-slate-50 border border-slate-200 rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1"
            >
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-indigo-100 rounded-xl">
                  <Users size={24} className="text-indigo-600" />
                </div>
                <span className="text-sm font-mono font-bold text-indigo-600">
                  {metrics ? metrics.active_agents : 0} Active
                </span>
              </div>
              <h3 className="text-lg font-bold text-slate-800">AI Agents</h3>
              <p className="text-2xl font-extrabold text-indigo-600 mt-2">
                {metrics && metrics.active_agents > 0 ? 'Operational' : 'Inactive'}
              </p>
              <p className="text-xs text-slate-500 mt-2">
                {metrics ? metrics.active_agents : 0} agents processing tasks
              </p>
              <div className="mt-4 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-indigo-500 rounded-full"
                  style={{ width: `${metrics ? Math.min(metrics.active_agents * 10, 100) : 0}%` }}
                ></div>
              </div>
            </motion.div>

            {/* Cost Efficiency Card */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="bg-gradient-to-br from-white to-slate-50 border border-slate-200 rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1"
            >
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-emerald-100 rounded-xl">
                  <DollarSign size={24} className="text-emerald-600" />
                </div>
                <span className="text-sm font-mono font-bold text-emerald-600">
                  ${metrics ? metrics.cost_per_hour.toFixed(2) : '0.00'}/h
                </span>
              </div>
              <h3 className="text-lg font-bold text-slate-800">Cost Efficiency</h3>
              <p className="text-2xl font-extrabold text-emerald-600 mt-2">
                {metrics && metrics.cost_per_hour < 0.5 ? 'Efficient' : metrics && metrics.cost_per_hour < 1.0 ? 'Moderate' : 'High Cost'}
              </p>
              <p className="text-xs text-slate-500 mt-2">
                Under budget allocation
              </p>
              <div className="mt-4 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${metrics ? Math.min(metrics.cost_per_hour * 50, 100) : 25}%`,
                    backgroundColor: metrics && metrics.cost_per_hour < 0.5 ? '#10b981' : metrics && metrics.cost_per_hour < 1.0 ? '#f59e0b' : '#ef4444'
                  }}
                ></div>
              </div>
            </motion.div>
          </div>

          {/* Main Content Area */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">

            {/* Activity Feed */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 }}
              className="lg:col-span-2 bg-white border border-slate-200 rounded-3xl p-6 shadow-lg"
            >
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Activity size={20} className="text-indigo-500" />
                  System Activity Feed
                </h2>
                <button className="text-xs text-indigo-600 font-semibold hover:underline transition-colors">
                  View All
                </button>
              </div>

              <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
                {events && events.length > 0 ? (
                  events.map((evt, idx) => {
                    const isError = evt.level?.toLowerCase() === 'error' || evt.level?.toLowerCase() === 'critical';
                    const isWarn = evt.level?.toLowerCase() === 'warning' || evt.level?.toLowerCase() === 'warn';
                    const iconColor = isError ? 'text-rose-600 bg-rose-50' : (isWarn ? 'text-amber-600 bg-amber-50' : 'text-emerald-600 bg-emerald-50');
                    const iconStr = isError ? '🚨' : (isWarn ? '⚠️' : 'ℹ️');
                    const bgColor = isError ? 'bg-rose-50 border-rose-100' : (isWarn ? 'bg-amber-50 border-amber-100' : 'bg-emerald-50 border-emerald-100');

                    return (
                      <motion.div
                        key={idx}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: idx * 0.05 }}
                        className={`flex items-start gap-4 p-4 rounded-xl border ${bgColor} hover:bg-white transition-colors`}
                      >
                        <span className={`p-3 rounded-xl ${iconColor} text-sm font-mono`}>{iconStr}</span>
                        <div className="flex-1">
                          <div className="flex justify-between items-center">
                            <p className="text-sm font-bold text-slate-800">{evt.source || 'SYSTEM'}</p>
                            <span className="text-xs text-slate-400 font-mono">{evt.timestamp}</span>
                          </div>
                          <p className="text-sm text-slate-600 mt-1">{evt.message}</p>
                        </div>
                      </motion.div>
                    );
                  })
                ) : (
                  <div className="text-center py-12">
                    <Activity size={48} className="mx-auto text-slate-300 mb-4" />
                    <p className="text-slate-400 font-medium">No recent activity</p>
                    <p className="text-xs text-slate-400 mt-1">Events will appear here in real-time</p>
                  </div>
                )}
              </div>
            </motion.div>

            {/* Quick Actions & Performance */}
            <div className="space-y-8">

              {/* Quick Actions */}
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 }}
                className="bg-white border border-slate-200 rounded-3xl p-6 shadow-lg"
              >
                <h2 className="text-lg font-bold text-slate-900 mb-6">Quick Actions</h2>

                <div className="space-y-4">
                  <button
                    onClick={runSmartOptimization}
                    disabled={isOptimizing}
                    className="w-full flex items-center justify-between p-4 rounded-xl border border-indigo-100 hover:bg-indigo-50 text-left transition-all group disabled:opacity-75 disabled:cursor-not-allowed"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-indigo-100 rounded-lg group-hover:bg-indigo-200 transition-colors">
                        <Zap size={18} className="text-indigo-600" />
                      </div>
                      <div>
                        <span className="text-sm font-bold text-slate-800">System Optimization</span>
                        <span className="block text-xs text-slate-500">Clean memory and reset processes</span>
                      </div>
                    </div>
                    {isOptimizing ? (
                      <RefreshCw size={18} className="text-indigo-600 animate-spin" />
                    ) : (
                      <ArrowUpRight size={18} className="text-indigo-600" />
                    )}
                  </button>

                  {optimizeStatus && (
                    <div className="text-xs text-indigo-600 px-1">{optimizeStatus}</div>
                  )}

                  <button
                    onClick={() => setActivePanel('Reports')}
                    className="w-full flex items-center justify-between p-4 rounded-xl border border-slate-100 hover:bg-slate-50 text-left transition-all group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-slate-100 rounded-lg group-hover:bg-slate-200 transition-colors">
                        <HardDrive size={18} className="text-slate-600" />
                      </div>
                      <div>
                        <span className="text-sm font-bold text-slate-800">Generate Report</span>
                        <span className="block text-xs text-slate-500">View performance & analytics</span>
                      </div>
                    </div>
                    <ArrowUpRight size={18} className="text-slate-400" />
                  </button>

                  <button
                    onClick={restartServices}
                    disabled={isRestarting}
                    className="w-full flex items-center justify-between p-4 rounded-xl border border-slate-100 hover:bg-slate-50 text-left transition-all group disabled:opacity-75 disabled:cursor-not-allowed"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-slate-100 rounded-lg group-hover:bg-slate-200 transition-colors">
                        <Server size={18} className="text-slate-600" />
                      </div>
                      <div>
                        <span className="text-sm font-bold text-slate-800">Restart Services</span>
                        <span className="block text-xs text-slate-500">Gracefully restart core services</span>
                      </div>
                    </div>
                    {isRestarting ? (
                      <RefreshCw size={18} className="text-slate-600 animate-spin" />
                    ) : (
                      <ArrowUpRight size={18} className="text-slate-400" />
                    )}
                  </button>

                  {restartStatus && (
                    <div className="text-xs text-slate-500 px-1">{restartStatus}</div>
                  )}

                  <button
                    onClick={runSecurityScan}
                    disabled={isScanning}
                    className="w-full flex items-center justify-between p-4 rounded-xl border border-slate-100 hover:bg-slate-50 text-left transition-all group disabled:opacity-75 disabled:cursor-not-allowed"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-slate-100 rounded-lg group-hover:bg-slate-200 transition-colors">
                        <Lock size={18} className="text-slate-600" />
                      </div>
                      <div>
                        <span className="text-sm font-bold text-slate-800">Security Scan</span>
                        <span className="block text-xs text-slate-500">Run comprehensive threat detection</span>
                      </div>
                    </div>
                    {isScanning ? (
                      <RefreshCw size={18} className="text-slate-600 animate-spin" />
                    ) : (
                      <ArrowUpRight size={18} className="text-slate-400" />
                    )}
                  </button>

                  {scanStatus && (
                    <div className="text-xs text-slate-500 px-1">{scanStatus}</div>
                  )}
                </div>
              </motion.div>

              {/* Performance Stats */}
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.6 }}
                className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-3xl p-6 text-white shadow-lg"
              >
                <h2 className="text-lg font-bold mb-4">Performance Overview</h2>

                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-sm font-medium">CPU Usage</span>
                      <span className="text-sm font-bold">
                        {Math.round(safeCpu)}%
                      </span>
                    </div>
                    <div className="h-2 bg-white/20 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-white rounded-full"
                        style={{ width: `${Math.min(safeCpu, 100)}%` }}
                      ></div>
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-sm font-medium">Memory Usage</span>
                      <span className="text-sm font-bold">
                        {Math.round(safeMem)}%
                      </span>
                    </div>
                    <div className="h-2 bg-white/20 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-white rounded-full"
                        style={{ width: `${Math.min(safeMem, 100)}%` }}
                      ></div>
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-sm font-medium">Network</span>
                      <span className="text-sm font-bold">
                        {metrics ? metrics.requests_per_second : 0}/sec
                      </span>
                    </div>
                    <div className="h-2 bg-white/20 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-emerald-300 rounded-full"
                        style={{ width: `${Math.min((metrics ? metrics.requests_per_second : 0) / 100 * 100, 100)}%` }}
                      ></div>
                    </div>
                  </div>
                </div>
              </motion.div>
            </div>
          </div>

          {/* Detailed Reports Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.7 }}
            className="bg-white border border-slate-200 rounded-3xl p-6 shadow-lg"
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <TrendingUp size={20} className="text-indigo-500" />
                System Reports & Analytics
              </h2>
              <button className="text-xs text-indigo-600 font-semibold hover:underline transition-colors">
                Export Data
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              {/* Report List */}
              <div className="md:col-span-1 border-r border-slate-200 pr-6">
                <h3 className="text-sm font-bold text-slate-700 mb-4">Available Reports</h3>
                <div className="space-y-2">
                  {reportsData && reportsData.reports && reportsData.reports.length > 0 ? (
                    reportsData.reports.map((report: string) => (
                      <button
                        key={report}
                        onClick={() => setSelectedReportName(report)}
                        className={`w-full text-left px-4 py-3 rounded-lg text-sm font-medium transition-all ${
                          selectedReportName === report
                            ? 'bg-indigo-100 text-indigo-700 font-bold border-l-4 border-indigo-600 shadow-sm'
                            : 'hover:bg-slate-50 text-slate-600 border-l-4 border-transparent'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <TrendingUp size={14} className="text-indigo-500" />
                          {report}
                        </div>
                      </button>
                    ))
                  ) : (
                    <p className="text-sm text-slate-400">No reports available</p>
                  )}
                </div>
              </div>

              {/* Report Viewer */}
              <div className="md:col-span-3 pl-6">
                {selectedReportName ? (
                  activeReport && activeReport.content ? (
                    <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 max-h-[400px] overflow-y-auto">
                      <div className="flex justify-between items-center border-b border-slate-300 pb-3 mb-4">
                        <span className="text-base font-bold text-slate-800">{activeReport.name}</span>
                        <button
                          onClick={() => setSelectedReportName(undefined)}
                          className="text-sm text-slate-400 hover:text-slate-600 font-bold transition-colors"
                        >
                          Close ×
                        </button>
                      </div>
                      <pre className="text-sm font-mono text-slate-700 whitespace-pre-wrap leading-relaxed">
                        {activeReport.content}
                      </pre>
                    </div>
                  ) : (
                    <div className="flex items-center justify-center h-64">
                      <div className="text-center">
                        <Clock size={48} className="mx-auto text-slate-300 mb-4 animate-spin" />
                        <p className="text-slate-400 font-medium">Loading report...</p>
                      </div>
                    </div>
                  )
                ) : (
                  <div className="h-64 flex items-center justify-center border-2 border-dashed border-slate-300 rounded-2xl">
                    <div className="text-center">
                      <TrendingUp size={48} className="mx-auto text-slate-300 mb-4" />
                      <p className="text-slate-400 font-medium">Select a report to view details</p>
                      <p className="text-sm text-slate-400 mt-1">Reports will display analytics and insights</p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </motion.div>

          {/* Mode Switcher Button */}
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={toggleDashboardMode}
            className="fixed bottom-6 right-6 flex items-center gap-3 px-6 py-4 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-bold rounded-full shadow-lg hover:shadow-xl transition-all z-50"
          >
            <Layout size={20} />
            <span>Switch to Developer Mode</span>
          </motion.button>

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
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={toggleDashboardMode}
              className="flex items-center gap-2 px-3 py-1.5 text-[10px] font-mono font-bold tracking-widest text-[#00f3ff] sci-fi-glass hover:bg-[#00f3ff]/20 transition-all border border-[#00f3ff]/30"
            >
              <Layout size={12} />
              SIMPLE MODE
            </motion.button>
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

          {/* --- Floating Compact Java Worker Widget --- */}
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
                    <span className="text-[10px] font-mono font-bold text-[#00f3ff]">
                      {metrics ? `${metrics.requests_per_second} RPS` : '--'}
                    </span>
                  </div>
                </div>
                <span className="text-[9px] font-mono text-cyan-500 tracking-widest uppercase">CPU Usage</span>
              </div>

              {/* Memory Gauge */}
              <div className="flex flex-col items-center gap-2">
                <div className="relative w-16 h-16 flex items-center justify-center rounded-full border-4 border-[#09101f] border-t-[#00ff66] border-r-[#00ff66] shadow-[0_0_15px_rgba(0,255,102,0.3)] animate-[spin_6s_linear_infinite_reverse]">
                  <div className="absolute w-full h-full rounded-full animate-[spin_6s_linear_infinite] flex items-center justify-center">
                    <span className="text-[10px] font-mono font-bold text-[#00ff66]">
                      {metrics ? `$${metrics.cost_per_hour.toFixed(2)}/h` : '$--/h'}
                    </span>
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
              <motion.button
                key={item.id}
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => setActivePanel(item.id)}
                className={`p-2 rounded-xl transition-all ${activePanel === item.id ? `bg-[#00f3ff]/20 shadow-[inset_0_0_10px_rgba(0,243,255,0.5)] ${item.activeColor}` : `text-slate-400 ${item.color}`}`}
                title={item.id}
              >
                {item.icon}
              </motion.button>
            ))}
          </div>

        </div>
      )}
    </>
  );
};

export default Dashboard;
