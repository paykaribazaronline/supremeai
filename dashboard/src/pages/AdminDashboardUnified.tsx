// AdminDashboardUnified.tsx - Command Center with Access Control
import { useState, useEffect } from 'react';
import { Layout, Alert, Badge, Space, Tabs, Button, Modal, Avatar, Tooltip, Typography, Divider } from 'antd';
import {
    RobotOutlined,
    DashboardOutlined,
    DatabaseOutlined,
    NodeIndexOutlined,
    ApiOutlined,
    BarChartOutlined,
    SafetyCertificateOutlined,
    GlobalOutlined,
    BulbOutlined,
    BugOutlined,
    RocketOutlined,
    HistoryOutlined,
    LogoutOutlined,
    LockOutlined,
    EyeOutlined,
    CloudServerOutlined,
    UserOutlined,
    ProjectOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import AdminProviders from './AdminProviders';
import ScenarioOrchestration from '../components/ScenarioOrchestration';
import ThreeDashboard from '../components/ThreeDashboard';
import ChatWithAI from '../components/ChatWithAI';
import ConsensusMap from '../components/ConsensusMap';
import QuotaTraffic from '../components/QuotaTraffic';
import KnowledgeHub from '../components/KnowledgeHub';
import SelfHealingLogs from '../components/SelfHealingLogs';
import LearningHub from '../components/LearningHub';
import { notification } from 'antd';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useRole } from '../contexts/RoleContext';
import { useAISuggestions } from '../lib/suggestionService';
import AdminApprovals from '../components/AdminApprovals';
import { message } from 'antd';
import TelegramDrive from '../components/TelegramDrive';
import UsersTab from '../components/admin/UsersTab';
import ProjectsTab from '../components/admin/ProjectsTab';
import AdminQuotas from './AdminQuotas';
import AdminLogs from './AdminLogs';
import AdminMonitoring from './AdminMonitoring';
import AdminInfrastructure from './AdminInfrastructure';

const { Header, Content } = Layout;
const { Text } = Typography;

// Restricted access demo placeholder
const RestrictedAccess: React.FC = () => (
  <div style={{
    padding: '80px 40px',
    textAlign: 'center',
    background: 'rgba(255,255,255,0.02)',
    border: '1px dashed rgba(245,158,11,0.3)',
    borderRadius: '16px',
    marginTop: '20px'
  }}>
    <LockOutlined style={{ fontSize: 80, color: '#f59e0b', marginBottom: 24, opacity: 0.6 }} />
    <h2 style={{ color: '#f59e0b', marginBottom: 12, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
      ADMIN RESTRICTED
    </h2>
    <p style={{ color: 'rgba(255,255,255,0.6)', maxWidth: 600, margin: '0 auto 32px', lineHeight: 1.7 }}>
      This command center module is only accessible to administrators.
      <br />Please login with an admin account to view system metrics, provider orchestration, and advanced controls.
    </p>
    <Space size="large">
      <Button
        type="primary"
        icon={<EyeOutlined />}
        onClick={() => window.location.href = '/admin?login=true'}
        style={{ background: '#10b981', borderColor: '#10b981' }}
      >
        Login as Administrator
      </Button>
      <Button
        onClick={() => window.location.href = '/'}
      >
        Return to Home
      </Button>
    </Space>
  </div>
);

const AdminDashboardUnified: React.FC = () => {
    const { isAdmin } = useRole();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [liveStream, setLiveStream] = useState<any[]>([]);

    useEffect(() => {
        const connectWebSocket = () => {
          try {
            const wsPath = import.meta.env.VITE_WS_URL || '/ws';
            const wsUrl = wsPath.startsWith('http') || wsPath.startsWith('ws')
                ? wsPath
                : `${window.location.protocol === 'https:' ? 'https:' : 'http'}://${window.location.host}${wsPath.startsWith('/') ? '' : '/'}${wsPath}`;

            const socket = new SockJS(wsUrl);
            const stompClient = new Client({
              webSocketFactory: () => socket,
              reconnectDelay: 5000,
              onConnect: () => {
                stompClient.subscribe('/topic/notifications', (message) => {
                  const data = JSON.parse(message.body);
                  // Only show notifications for admin events
                  if (isAdmin) {
                    notification[data.status === 'success' ? 'success' : 'error']({
                      message: data.status === 'success' ? '🚀 Deployment Successful' : '🚨 System Alert',
                      description: data.message,
                      duration: 5,
                      placement: 'topRight',
                    });
                  }
                });
              }
            });
            stompClient.activate();
            return () => stompClient.deactivate();
          } catch (err) {
            console.error("WebSocket error", err);
          }
        };

        connectWebSocket();
        // Simulate minimal loading for demo
        setTimeout(() => setLoading(false), 1000);
    }, [isAdmin]);

    if (loading) return (
        <div className="h-screen bg-[#050505] flex flex-col items-center justify-center">
            <div className="w-16 h-16 border-t-2 border-emerald-500 rounded-full animate-spin mb-4"></div>
            <div className="text-[10px] text-emerald-500 uppercase tracking-[0.3em] animate-pulse">INITIALIZING COMMAND CENTER</div>
        </div>
    );

    // Admin-only dashboard content
    if (!isAdmin) {
        return (
            <Layout className="min-h-screen bg-[#050505] text-white">
                <Header className="bg-black/90 backdrop-blur-2xl border-b border-white/5 h-16 px-6 flex items-center justify-between sticky top-0 z-50">
                    <div className="flex items-center gap-6">
                        <div className="flex flex-col gap-0.5 min-w-[200px]">
                            <span className="text-[12px] font-black uppercase tracking-[0.2em] text-white">SupremeAI Command Center</span>
                            <span className="text-[9px] font-black text-yellow-400 uppercase tracking-[0.3em]">RESTRICTED ACCESS</span>
                        </div>
                    </div>
                    <div className="flex items-center gap-4">
                        <Avatar size={28} className="bg-amber-500/20 text-amber-500 border border-amber-500/30" icon={<RobotOutlined />} />
                        <span className="text-[12px] font-black uppercase tracking-tighter text-white/80">GUEST USER</span>
                        <Tooltip title="Logout">
                            <Button
                                type="text"
                                icon={<LogoutOutlined />}
                                onClick={() => { authUtils.clearAuth(); window.location.reload(); }}
                                className="text-white/40 hover:text-red-500 border border-white/5"
                            />
                        </Tooltip>
                    </div>
                </Header>
                <Content className="p-6 overflow-y-auto min-h-[calc(100vh-64px)] bg-[#0c0c0c]">
                    <div className="max-w-[1200px] mx-auto">
                        <Alert
                            message="Access Restricted"
                            description="The SupremeAI Command Center is only available to administrators. Please login with admin credentials."
                            type="warning"
                            showIcon
                            icon={<LockOutlined />}
                            style={{ marginBottom: 24, background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.3)' }}
                        />
                        <RestrictedAccess />
                    </div>
                </Content>
            </Layout>
        );
    }

    return (
        <Layout className="min-h-screen bg-[#050505] text-white">
            <Header className="bg-black/90 backdrop-blur-2xl border-b border-white/5 h-16 px-6 flex items-center justify-between sticky top-0 z-50">
                <div className="flex items-center gap-6">
                    <div className="flex flex-col gap-0.5 min-w-[200px]">
                        <span className="text-[12px] font-black uppercase tracking-[0.2em] text-white">SupremeAI Command Center</span>
                        <span className="text-[9px] font-black text-emerald-400 uppercase tracking-[0.3em]">ADMINISTRATOR ACCESS</span>
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/30 rounded-full">
                        <Avatar size={24} className="bg-emerald-500 text-white border-0" icon={<RobotOutlined />} />
                        <span className="text-[12px] font-bold uppercase tracking-tighter text-emerald-400">ADMIN</span>
                    </div>
                    <Tooltip title="Logout">
                        <Button
                            type="text"
                            icon={<LogoutOutlined />}
                            onClick={() => { authUtils.clearAuth(); window.location.reload(); }}
                            className="text-white/40 hover:text-red-500 border border-white/5"
                        />
                    </Tooltip>
                </div>
            </Header>

            <Content className="p-6 overflow-y-auto min-h-[calc(100vh-64px)] bg-[#0c0c0c]">
                <div className="max-w-[1600px] mx-auto space-y-6">
                    <div className="glass-card px-6 py-4 flex items-center justify-between border-l-4 border-emerald-500 bg-black/40">
                        <div className="flex items-center gap-4">
                            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-500 shadow-[0_0_20px_rgba(16,185,129,0.15)]">
                                <RobotOutlined className="text-lg" />
                            </div>
                            <div className="flex flex-col">
                                <h1 className="text-lg font-black uppercase tracking-[0.2em] text-white m-0">AI Model Scenario Management</h1>
                                <p className="text-[10px] font-bold text-emerald-500/60 uppercase tracking-widest m-0">Orchestrate Communication, Execution & Voting Protocols</p>
                            </div>
                        </div>
                    </div>

                    {/* Global suggestions removed from here to be moved to a separate tab as requested */}

                    <div className="glass-card p-6 border border-white/5 bg-black/20">
                        <Tabs
                            defaultActiveKey="registry"
                            className="dark-tabs"
                            items={[
                                {
                                    key: 'registry',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <DatabaseOutlined /> Provider Registry
                                        </span>
                                    ),
                                    children: <AdminProviders />
                                },
                                {
                                    key: 'users',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <UserOutlined /> Users Management
                                        </span>
                                    ),
                                    children: <UsersTab />
                                },
                                {
                                    key: 'projects',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <ProjectOutlined /> Projects & Pipeline
                                        </span>
                                    ),
                                    children: <ProjectsTab />
                                },
                                {
                                    key: 'approvals',
                                    label: (
                                        <Badge count={useAISuggestions().count} offset={[12, 0]} size="small">
                                            <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-amber-500">
                                                <BulbOutlined /> Approvals
                                            </span>
                                        </Badge>
                                    ),
                                    children: <AdminApprovals />
                                },
                                {
                                    key: 'orchestration',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <NodeIndexOutlined /> Scenario Orchestration
                                        </span>
                                    ),
                                    children: <ScenarioOrchestration />
                                },
                                {
                                    key: 'telemetry',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <BarChartOutlined /> Neural Telemetry
                                        </span>
                                    ),
                                    children: <ThreeDashboard />
                                },
                                {
                                    key: 'consensus',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <SafetyCertificateOutlined /> Consensus Map
                                        </span>
                                    ),
                                    children: <ConsensusMap />
                                },
                                {
                                    key: 'traffic',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <GlobalOutlined /> Traffic & Quotas
                                        </span>
                                    ),
                                    children: <QuotaTraffic />
                                },
                                {
                                    key: 'knowledge',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <BulbOutlined /> Knowledge Base
                                        </span>
                                    ),
                                    children: <KnowledgeHub />
                                },
                                {
                                    key: 'infrastructure',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-emerald-400">
                                            <CloudServerOutlined /> Infrastructure Concierge
                                        </span>
                                    ),
                                    children: <AdminInfrastructure />
                                },
                                {
                                    key: 'monitoring',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-blue-400">
                                            <DashboardOutlined /> Resource Monitoring
                                        </span>
                                    ),
                                    children: <AdminMonitoring />
                                },
                                {
                                    key: 'activity_logs',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-orange-400">
                                            <HistoryOutlined /> Activity Logs
                                        </span>
                                    ),
                                    children: <AdminLogs />
                                },
                                {
                                    key: 'quotas_management',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-purple-400">
                                            <LockOutlined /> Quotas Management
                                        </span>
                                    ),
                                    children: <AdminQuotas />
                                },
                                {
                                    key: 'logs',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <BugOutlined /> Self-Healing Logs
                                        </span>
                                    ),
                                    children: <SelfHealingLogs />
                                },
                                {
                                    key: 'learning',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest">
                                            <RocketOutlined /> Learning Hub
                                        </span>
                                    ),
                                    children: <LearningHub />
                                },
                                {
                                    key: 'telegram',
                                    label: (
                                        <span className="flex items-center gap-2 text-[11px] font-black uppercase tracking-widest text-emerald-500">
                                            <CloudServerOutlined /> Telegram Drive
                                        </span>
                                    ),
                                    children: <TelegramDrive />
                                }
                            ]}
                        />
                    </div>
                </div>
            </Content>
        </Layout>
    );
};

export default AdminDashboardUnified;
