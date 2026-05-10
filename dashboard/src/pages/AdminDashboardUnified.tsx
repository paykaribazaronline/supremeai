// AdminDashboardUnified.tsx - ULTRA-DENSE DARK COMMAND CENTER
// UNIFIED ADMIN DASHBOARD - Single Source of Truth Contract

import { useState, useEffect, useMemo } from 'react';
import { Layout, Menu, Alert, Badge, Space, Tabs, Button, Modal, Input, message, Avatar, Dropdown, Typography, Divider, FloatButton, Progress, Tag, Tooltip, List } from 'antd';
import { 
    RobotOutlined, 
    DashboardOutlined, 
    CheckCircleOutlined, 
    SyncOutlined, 
    DesktopOutlined, 
    BulbOutlined, 
    BugOutlined, 
    NodeIndexOutlined, 
    ApiOutlined, 
    MenuUnfoldOutlined, 
    MenuFoldOutlined, 
    SearchOutlined, 
    BellOutlined, 
    UserOutlined, 
    SettingOutlined, 
    LogoutOutlined,
    GlobalOutlined,
    DatabaseOutlined,
    RocketOutlined,
    ClockCircleOutlined,
    CloudServerOutlined,
    InfoCircleOutlined,
    HistoryOutlined,
    MessageOutlined,
    ArrowUpOutlined,
    CheckCircleFilled,
    SafetyCertificateOutlined,
    ThunderboltOutlined,
    ChromeOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import PhasesOverview from '../components/PhasesOverview';
import AIAgentsDashboard from '../components/AIAgentsDashboard';
import ExploitationDashboard from '../components/ExploitationDashboard';
import ChatWithAI from '../components/ChatWithAI';
import SystemLearningDashboard from '../components/SystemLearningDashboard';
import RequirementsDashboard from '../components/RequirementsDashboard';
import AdminOCRCard from '../components/AdminOCRCard';
import APIManagement from '../components/APIManagement';
import AdminProtocolMatrix from '../components/AdminProtocolMatrix';
import AdminHistoryMatrix from '../components/AdminHistoryMatrix';
import AdminConfigMatrix from '../components/AdminConfigMatrix';
import VPNManagement from '../components/VPNManagement';
import TelemetryBar from '../components/TelemetryBar';
import GlassKPICard from '../components/GlassKPICard';
import NeuralTerminal, { LogEntry } from '../components/NeuralTerminal';
import ResourceGauges from '../components/ResourceGauges';
import SystemHealthMatrix from '../components/SystemHealthMatrix';
import OperationalAnalytics from '../components/OperationalAnalytics';
import AuditLog from '../components/AuditLog';
import NeuralNetworkFlow from '../components/NeuralNetworkFlow';
import BrowserActivityDashboard from '../components/BrowserActivityDashboard';
import { notification } from 'antd';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  Chart as ChartJS, 
  ArcElement, 
  Tooltip as ChartTooltip, 
  Legend, 
  CategoryScale, 
  LinearScale, 
  BarElement,
  PointElement,
  LineElement,
  Filler
} from 'chart.js';
import { ApiResponse, DashboardContract } from '../types';

// Register ChartJS
ChartJS.register(
  ArcElement, 
  ChartTooltip, 
  Legend, 
  CategoryScale, 
  LinearScale, 
  BarElement,
  PointElement,
  LineElement,
  Filler
);

const { Header, Content, Sider } = Layout;

const AdminDashboardUnified: React.FC = () => {
    const [selectedKey, setSelectedKey] = useState('overview');
    const [contract, setContract] = useState<DashboardContract | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [chatVisible, setChatVisible] = useState(false);
    const [liveStream, setLiveStream] = useState<LogEntry[]>([]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key.toLowerCase() === 'c' && (e.target as HTMLElement).tagName !== 'INPUT' && (e.target as HTMLElement).tagName !== 'TEXTAREA') {
                setChatVisible(prev => !prev);
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, []);

    useEffect(() => {
        fetchContract();
        const interval = setInterval(fetchContract, 30000);
        
        const connectWebSocket = () => {
          try {
            const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws'}://${window.location.host}/ws`;
            const socket = new SockJS(wsUrl);
            const stompClient = new Client({
              webSocketFactory: () => socket,
              reconnectDelay: 5000,
              onConnect: () => {
                stompClient.subscribe('/topic/notifications', (message) => {
                  const data = JSON.parse(message.body);
                  const newLog: LogEntry = {
                    id: Math.random().toString(36).substr(2, 9),
                    timestamp: new Date().toLocaleTimeString(),
                    level: data.level || 'INFO',
                    source: data.source || 'SYSTEM',
                    message: data.message || JSON.stringify(data)
                  };
                  setLiveStream(prev => [newLog, ...prev].slice(0, 100));
                  
                  if (data.type === 'GITHUB_PIPELINE') {
                    if (data.status === 'success') {
                      notification.success({ message: '🚀 Deployment Successful', description: data.message });
                    } else if (data.status === 'failure') {
                      notification.error({ message: '🚨 Deployment Failed', description: data.message });
                    }
                  }
                });
              }
            });
            stompClient.activate();
            return () => stompClient.deactivate();
          } catch (err) {
            console.error("WebSocket connection error", err);
          }
        };
        
        const cleanup = connectWebSocket();
        return () => {
          clearInterval(interval);
          if (cleanup) cleanup();
        };
    }, []);

    const fetchContract = async () => {
        try {
            const resp = await authUtils.fetchWithAuth('/api/admin/dashboard/contract');
            if (!resp.ok) {
                if (resp.status === 401 || resp.status === 403) {
                    authUtils.clearAuth();
                    window.location.href = '/admin';
                    return;
                }
                throw new Error('Failed to fetch contract');
            }
            const response = await resp.json() as ApiResponse<DashboardContract>;
            if (response.success && response.data) {
                setContract(response.data);
                setError(null);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load dashboard');
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        authUtils.clearAuth();
        window.location.reload();
    };

    const menuGroups = useMemo(() => {
        if (!contract) return [];
        return [
            {
                key: 'dashboard',
                label: 'STATS',
                type: 'group' as const,
                children: contract.navigation
                    .filter(i => ['overview', 'metrics', 'analytics'].includes(i.key))
                    .map(item => ({
                        key: item.key,
                        icon: item.icon,
                        label: <span className="text-[10px] font-bold uppercase tracking-widest">{item.label}</span>,
                    }))
            },
            {
                key: 'ai',
                label: 'INTELLIGENCE',
                type: 'group' as const,
                children: contract.navigation
                    .filter(i => ['ai-agents', 'providers', 'system-learning', 'ai-systems'].includes(i.key))
                    .map(item => ({
                        key: item.key === 'ai-systems' ? 'ai-agents' : item.key,
                        icon: item.icon,
                        label: <span className="text-[9px] font-bold uppercase tracking-[0.1em]">{item.label}</span>,
                    }))
            },
            {
                key: 'ops',
                label: 'OPERATIONS',
                type: 'group' as const,
                children: [
                    ...contract.navigation
                        .filter(i => ['requirements', 'ocr', 'exploitation-techniques', 'phases', 'vpn', 'audit', 'browser-activity'].includes(i.key))
                        .map(item => ({
                            key: item.key,
                            icon: item.icon,
                            label: <span className="text-[9px] font-bold uppercase tracking-[0.1em]">{item.label}</span>,
                        })),
                    { key: 'rules', icon: <SafetyCertificateOutlined />, label: <span className="text-[9px] font-bold uppercase tracking-[0.1em]">Rules</span> },
                    { key: 'history', icon: <HistoryOutlined />, label: <span className="text-[9px] font-bold uppercase tracking-[0.1em]">History</span> },
                    { key: 'config', icon: <SettingOutlined />, label: <span className="text-[9px] font-bold uppercase tracking-[0.1em]">Config</span> }
                ]
            }
        ];
    }, [contract]);

    if (loading) return (
        <div className="h-screen bg-[#050505] flex flex-col items-center justify-center font-mono">
            <div className="w-16 h-16 border-t-2 border-emerald-500 rounded-full animate-spin mb-4"></div>
            <div className="text-[10px] text-emerald-500 uppercase tracking-[0.3em] animate-pulse">INIT_COMMAND_CENTER</div>
        </div>
    );

    if (error || !contract) return <Alert message="System Offline" description={error} type="error" showIcon />;

    const stats = contract.stats;

    return (
        <Layout className="min-h-screen bg-[#050505] text-white">
            {/* Sidebar Navigation */}
            <Sider
                breakpoint="lg"
                collapsedWidth="64"
                onBreakpoint={(broken) => {
                    console.log('Breakpoint broken:', broken);
                }}
                className="bg-[#080808] border-r border-white/5 h-screen sticky top-0"
                width={260}
            >
                <div className="h-16 flex items-center justify-center border-b border-white/5 mb-4">
                    <Avatar 
                        size={32} 
                        className="bg-emerald-500/20 text-emerald-500 border border-emerald-500/30 font-black text-[12px]"
                    >S</Avatar>
                </div>
                <Menu
                    theme="dark"
                    mode="inline"
                    selectedKeys={[selectedKey]}
                    onClick={({ key }) => setSelectedKey(key)}
                    className="bg-transparent border-none"
                    items={menuGroups}
                />
                <div className="flex flex-col items-center gap-4 mt-auto pb-4">
                    <Tooltip title="Logout" placement="right">
                        <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout} className="text-white/20 hover:text-red-500" />
                    </Tooltip>
                </div>
            </Sider>

            <Layout className="bg-transparent flex flex-col flex-1">
                <Header className="bg-black/90 backdrop-blur-2xl border-b border-white/5 h-16 px-6 flex items-center justify-between sticky top-0 z-50">
                    <div className="flex items-center gap-6">
                        <div className="hidden lg:flex flex-col gap-0.5 min-w-[200px]">
                            <span className="text-[12px] font-black uppercase tracking-[0.2em] text-white">SupremeAI Command Center</span>
                            <span className="text-[9px] font-black text-yellow-400 uppercase tracking-[0.3em]">MASTER_OPERATIONAL_AUTHORITY</span>
                        </div>
                        <div className="flex lg:hidden items-center gap-2">
                             <Avatar size={32} className="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">S</Avatar>
                             <span className="text-[10px] font-black uppercase tracking-widest text-white">Command</span>
                        </div>
                        <div className="h-8 w-[1px] bg-white/10 mx-2"></div>
                        <div className="hidden md:flex items-center gap-4">
                            <div className="flex flex-col">
                                <span className="text-[9px] text-cyan-400 uppercase font-black tracking-widest">CAPACITY</span>
                                <span className="text-[12px] text-white font-mono font-bold">{stats.activeConnections || 0} NODES_ACTIVE</span>
                            </div>
                            <div className="flex flex-col">
                                <span className="text-[10px] text-white font-black uppercase tracking-tighter">SERVER_UPTIME</span>
                                <span className="text-[14px] text-yellow-400 font-mono font-bold">{stats.serverUptime || '00:00:00'}</span>
                            </div>
                        </div>
                    </div>
                    
                    <div className="flex items-center gap-4">
                        <div className="hidden lg:flex items-center gap-4 bg-white/10 border border-white/20 rounded-lg px-4 py-2">
                            <div className="flex flex-col">
                                <div className="flex items-center gap-2">
                                    <DatabaseOutlined className={`text-[12px] ${stats.databaseConnected ? 'text-emerald-500' : 'text-red-500'}`} />
                                    <span className="text-[10px] font-black uppercase text-white">DB: {stats.databaseConnected ? 'ONLINE' : 'OFFLINE'}</span>
                                </div>
                                <span className="text-[9px] text-cyan-400 font-mono font-bold text-left">{stats.databaseConnected ? 'SYNC_OPTIMAL' : 'RECONNECTING...'}</span>
                            </div>
                            <div className="w-[1px] h-8 bg-white/20"></div>
                            <div className="flex flex-col">
                                <div className="flex items-center gap-2">
                                    <CloudServerOutlined className={`text-[12px] ${stats.backendConnected ? 'text-emerald-500' : 'text-red-500'}`} />
                                    <span className="text-[10px] font-black uppercase text-white">SRV: {stats.backendConnected ? 'ACTIVE' : 'INACTIVE'}</span>
                                </div>
                                <span className="text-[9px] text-yellow-400 font-mono font-bold text-left">UPTIME_LIVE</span>
                            </div>
                        </div>
                        <div className="h-6 w-[1px] bg-white/5 mx-1"></div>
                        <div className="flex items-center gap-2 px-2 py-1 bg-white/[0.03] border border-white/10 rounded-full hover:bg-white/10 transition-all cursor-pointer">
                            <Avatar size={28} className="bg-white text-black border-2 border-white font-bold text-[12px]">AD</Avatar>
                            <span className="hidden sm:inline-block text-[12px] font-black uppercase tracking-tighter text-white mr-1">ADMIN_USER</span>
                        </div>
                    </div>
                </Header>

                <Content className="p-6 overflow-y-auto min-h-[calc(100vh-64px)] bg-[#0c0c0c]">
                    {selectedKey === 'overview' ? (
                        <div className="flex flex-col gap-6 max-w-[1800px] mx-auto animate-fade-in h-full">
                            {/* Header Section / Sub-Navigation */}
                            <div className="flex items-center justify-between px-2">
                                <div className="flex flex-col">
                                    <h2 className="text-2xl font-black uppercase tracking-[0.1em] text-white m-0">COMMAND_CENTER</h2>
                                    <span className="text-[11px] text-yellow-400 uppercase tracking-[0.3em] font-bold">SYSTEM_STATUS: NOMINAL // ALL_SYSTEMS_FUNCTIONAL</span>
                                </div>
                                <div className="flex items-center gap-4 bg-white/10 border border-white/20 rounded-full px-6 py-2">
                                    <div className="flex items-center gap-2">
                                        <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
                                        <span className="text-[11px] font-black text-emerald-500 uppercase tracking-widest">LIVE_TELEMETRY</span>
                                    </div>
                                    <Divider type="vertical" className="bg-white/20" />
                                    <span className="text-[11px] font-black text-white">{new Date().toLocaleTimeString()}</span>
                                </div>
                            </div>

                            {/* Main Visualization Grid */}
                            <div className="grid grid-cols-1 md:grid-cols-12 gap-6 min-h-[400px]">
                                {/* Top Left: Telemetry (Analytics) */}
                                <div className="col-span-1 md:col-span-4 glass-card p-0 overflow-hidden relative group">
                                    <div className="absolute top-4 left-6 z-10">
                                        <span className="text-[12px] font-black text-white uppercase tracking-widest bg-black px-2">ANALYTICS_STREAM</span>
                                    </div>
                                    <OperationalAnalytics />
                                </div>

                                {/* Top Center: Neural Flow */}
                                <div className="col-span-1 md:col-span-5 relative">
                                    <NeuralNetworkFlow />
                                </div>

                                {/* Top Right: Neural Terminal */}
                                <div className="col-span-1 md:col-span-3 flex flex-col min-h-[300px]">
                                    <NeuralTerminal logs={liveStream} />
                                </div>
                            </div>

                            {/* KPI Matrix Row */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                                <GlassKPICard 
                                    label="Neural Flux"
                                    value={stats.activeUsers || 1240}
                                    subValue="Requests / Sec"
                                    icon={<ThunderboltOutlined />}
                                    color="#10b981"
                                    trend="up"
                                    change="+12.5%"
                                />
                                <GlassKPICard 
                                    label="Brain Capacity"
                                    value="84.2GB"
                                    subValue="Synced Data"
                                    icon={<DatabaseOutlined />}
                                    color="#a855f7"
                                    trend="neutral"
                                    change="Stable"
                                />
                                <GlassKPICard 
                                    label="System Latency"
                                    value={`${stats.latency || 24}ms`}
                                    subValue="Edge Response"
                                    icon={<GlobalOutlined />}
                                    color="#3b82f6"
                                    trend="up"
                                    change="-4ms"
                                />
                                <GlassKPICard 
                                    label="Success Rate"
                                    value={`${stats.successRate || 99.9}%`}
                                    subValue="Task Integrity"
                                    icon={<CheckCircleOutlined />}
                                    color="#f59e0b"
                                    trend="neutral"
                                    change="+0.02%"
                                />
                            </div>

                            {/* Bottom Health & Resource Section */}
                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                                <div className="col-span-1 lg:col-span-8 glass-card p-6">
                                    <div className="flex items-center justify-between mb-6">
                                        <div className="flex flex-col">
                                            <span className="text-[12px] font-black text-white uppercase tracking-widest">GLOBAL_NODE_HEALTH_MATRIX</span>
                                            <span className="text-[10px] text-yellow-400 font-bold uppercase tracking-[0.2em]">CLUSTER_HEALTH_DISTRIBUTION // ACTIVE</span>
                                        </div>
                                        <Button type="link" size="small" className="text-[10px] uppercase font-bold text-cyan-400">View Node Details</Button>
                                    </div>
                                    <SystemHealthMatrix nodes={contract?.stats.systemHealthNodes || []} />
                                </div>
                                <div className="col-span-1 lg:col-span-4 glass-card p-6">
                                    <div className="mb-6">
                                        <span className="text-[12px] font-black text-white uppercase tracking-widest">RESOURCE_ALLOCATION</span>
                                    </div>
                                    <ResourceGauges />
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-6 max-w-[1600px] mx-auto animate-fade-in">
                            {/* Module Header */}
                            <div className="glass-card px-6 py-4 flex items-center justify-between border-l-4 border-emerald-500">
                                <div className="flex items-center gap-4">
                                    <div className="w-10 h-10 rounded-xl bg-white/[0.03] border border-white/10 flex items-center justify-center text-emerald-500 shadow-xl">
                                        {selectedKey.includes('ai') ? <RobotOutlined /> : <RocketOutlined />}
                                    </div>
                                    <div className="flex flex-col">
                                        <h1 className="text-sm font-black uppercase tracking-widest text-white">{selectedKey.replace('-', ' ')}</h1>
                                        <p className="text-[9px] font-bold text-white/30 uppercase tracking-wider">Subsystem Active // Resource Allocation Optimal</p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-3">
                                    <div className="bg-emerald-500/5 border border-emerald-500/10 px-3 py-1.5 rounded-lg flex items-center gap-3">
                                        <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
                                        <span className="text-[10px] font-black text-emerald-500 uppercase tracking-tighter">Sync Verified</span>
                                    </div>
                                </div>
                            </div>

                            {/* Module Content */}
                            <div className="glass-card p-4 min-h-[600px] relative">
                                {selectedKey === 'ai-agents' && <AIAgentsDashboard />}
                                {selectedKey === 'system-learning' && <SystemLearningDashboard />}
                                {selectedKey === 'requirements' && <RequirementsDashboard />}
                                {selectedKey === 'ocr' && <AdminOCRCard />}
                                {selectedKey === 'exploitation-techniques' && <ExploitationDashboard />}
                                {selectedKey === 'phases' && <PhasesOverview />}
                                {selectedKey === 'providers' && <APIManagement />}
                                {selectedKey === 'rules' && <AdminProtocolMatrix />}
                                {selectedKey === 'history' && <AdminHistoryMatrix />}
                                {selectedKey === 'config' && <AdminConfigMatrix />}
                                {selectedKey === 'vpn' && <VPNManagement />}
                                {selectedKey === 'audit' && <AuditLog />}
                                {selectedKey === 'browser-activity' && <BrowserActivityDashboard />}
                                
                                {selectedKey === 'api-endpoints' && contract.apiEndpoints && (
                                    <div className="space-y-6 p-2">
                                        <div className="flex items-center justify-between">
                                            <span className="text-[11px] font-black uppercase tracking-widest text-white/60">System API Registry</span>
                                            <Tag color="cyan" className="m-0 text-[9px] font-black border-0 rounded-lg px-3 py-1 bg-cyan-400/10 text-cyan-400">v3.4.0-STABLE</Tag>
                                        </div>
                                        <Tabs
                                            size="small"
                                            className="dark-tabs"
                                            items={Object.entries(contract.apiEndpoints).map(([cat, end]) => ({
                                                key: cat,
                                                label: cat,
                                                children: (
                                                    <div className="bg-black/20 rounded-xl border border-white/5 overflow-hidden">
                                                        <div className="px-4 py-2 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                                                            <span className="text-[10px] font-mono text-white/30 uppercase">{cat}_SCHEMA_V1</span>
                                                            <Button type="link" size="small" className="text-[10px] p-0 font-bold uppercase h-auto">Copy Definition</Button>
                                                        </div>
                                                        <pre className="p-6 font-mono text-[11px] text-cyan-400/80 overflow-auto max-h-[500px] custom-scrollbar selection:bg-cyan-400/30">
                                                            {JSON.stringify(end, null, 2)}
                                                        </pre>
                                                    </div>
                                                )
                                            }))}
                                        />
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </Content>

                <Modal
                    title={<span className="text-white text-[14px] font-black uppercase tracking-widest">Neural Link Chat</span>}
                    open={chatVisible}
                    onCancel={() => setChatVisible(false)}
                    footer={null}
                    width={800}
                    className="dark-modal"
                    styles={{ body: { padding: 0, backgroundColor: '#050505' } }}
                    centered
                >
                    <ChatWithAI />
                </Modal>
            </Layout>
        </Layout>
    );
};

export default AdminDashboardUnified;
