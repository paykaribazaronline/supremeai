// AdminDashboard.tsx - Main SupremeAI Admin Control Panel

import React, { useState, useEffect } from 'react';
import { Layout, Menu, Breadcrumb, Card, Statistic, Row, Col, Alert, Badge, Space } from 'antd';
import {
    DashboardOutlined,
    CloudServerOutlined,
    ApiOutlined,
    RobotOutlined,
    TeamOutlined,
    SettingOutlined,
    CheckCircleOutlined,
    WarningOutlined,
    LikeOutlined,
    CrownOutlined,
    BarChartOutlined,
    BugOutlined,
    GithubOutlined,
    ChromeOutlined,
    ChatOutlined,
} from '@ant-design/icons';
import APIManagement from '../components/APIManagement';
import AIModelSearch from '../components/AIModelSearch';
import VPNManagement from '../components/VPNManagement';
import ChatWithAI from '../components/ChatWithAI';
import AIAssignment from '../components/AIAssignment';
import DecisionVoting from '../components/DecisionVoting';
import KingModePanel from '../components/KingModePanel';
import ProgressMo from '../components/ProgressMonitor';
import ImprovementTracking from '../components/ImprovementTracking';
import AIWorkHistory from '../components/AIWorkHistory';
import AuditLog from '../components/AuditLog';
import SystemMetrics from '../components/SystemMetrics';
import APIKeysManager from '../components/APIKeysManager';
import GitHubDashboard from '../components/GitHubDashboard';
import HeadlessBrowserDashboard from '../components/HeadlessBrowserDashboard';
import ChatHistoryDashboard from '../components/ChatHistoryDashboard';

const { Header, Content, Sider } = Layout;

interface DashboardStats {
    activeAIAgents: number;
    runningTasks: number;
    completedTasks: number;
    systemHealthStatus: 'healthy' | 'warning' | 'critical';
    systemHealthScore: number;
    successRate: number;
    lastSyncTime: string;
}

interface NavigationItem {
    key: string;
    label: string;
    enabled: boolean;
}

interface DashboardContract {
    contractVersion: string;
    title: string;
    entryPath: string;
    stats: DashboardStats;
    navigation: NavigationItem[];
}

const AdminDashboard: React.FC = () => {
    const [collapsed, setCollapsed] = useState(false);
    const [selectedTab, setSelectedTab] = useState('overview');
    const [dashboardContract, setDashboardContract] = useState<DashboardContract | null>(null);
    const [stats, setStats] = useState<DashboardStats>({
        activeAIAgents: 0,
        runningTasks: 0,
        completedTasks: 0,
        systemHealthStatus: 'healthy',
        systemHealthScore: 0,
        successRate: 0,
        lastSyncTime: new Date().toLocaleString(),
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardStats();
        const interval = setInterval(fetchDashboardStats, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    const fetchDashboardContract = async () => {
        try {
            const token = localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');
            const response = await fetch('/api/admin/dashboard/contract', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data: DashboardContract = await response.json();
                setDashboardContract(data);
                setStats(data.stats);
            }
        } catch (error) {
            console.error('Failed to fetch dashboard contract:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchDashboardStats = fetchDashboardContract;

    const defaultMenuItems = [
        // ╔═══════════════════════════════════╗
        // ║ 1️⃣  DASHBOARD & OVERVIEW          ║
        // ╚═══════════════════════════════════╝
        {
            key: 'overview',
            icon: <DashboardOutlined />,
            label: 'Dashboard',
        },
        
        // ╔═══════════════════════════════════╗
        // ║ 2️⃣  🤖 AI & MODELS MANAGEMENT     ║
        // ╚═══════════════════════════════════╝
        {
            key: 'api-keys',
            icon: <ApiOutlined />,
            label: 'API Keys & Models',
            children: [
                { key: 'api-keys-manage', label: 'Manage API Keys' },
                { key: 'api-keys-discover', label: 'Discover Models' },
                { key: 'api-keys-usage', label: 'Usage Stats' },
            ],
        },
        {
            key: 'api-management',
            icon: <CloudServerOutlined />,
            label: 'API Management',
            children: [
                { key: 'api-add', label: 'Add API Provider' },
                { key: 'api-list', label: 'Manage APIs' },
                { key: 'api-test', label: 'Test Connections' },
            ],
        },
        {
            key: 'ai-models',
            icon: <RobotOutlined />,
            label: 'AI Models & Search',
            children: [
                { key: 'search-models', label: 'Search New Models' },
                { key: 'manage-models', label: 'Manage Models' },
                { key: 'model-performance', label: 'Performance Analytics' },
            ],
        },
        {
            key: 'ai-assignment',
            icon: <TeamOutlined />,
            label: 'AI Assignment',
            children: [
                { key: 'assign-new', label: 'Assign AI to Tasks' },
                { key: 'view-assignments', label: 'View Assignments' },
                { key: 'workload-balance', label: 'Workload Balance' },
            ],
        },

        // ╔═══════════════════════════════════╗
        // ║ 3️⃣  💬 AI COMMUNICATION           ║
        // ╚═══════════════════════════════════╝
        {
            key: 'ai-chat',
            icon: <ChatWithAI />,
            label: 'Chat & Commands',
        },

        // ╔═══════════════════════════════════╗
        // ║ 4️⃣  🎯 AI CONTROL & DECISIONS     ║
        // ╚═══════════════════════════════════╝
        {
            key: 'voting',
            icon: <LikeOutlined />,
            label: 'Decision & Voting',
            children: [
                { key: 'voting-active', label: 'Active Votes' },
                { key: 'voting-history', label: 'Vote History' },
                { key: 'decision-rules', label: 'Decision Rules' },
            ],
        },
        {
            key: 'kingmode',
            icon: <CrownOutlined />,
            label: 'King Mode (Override)',
        },

        // ╔═══════════════════════════════════╗
        // ║ 5️⃣  🔧 INFRASTRUCTURE & TOOLS     ║
        // ╚═══════════════════════════════════╝
        {
            key: 'browser',
            icon: <ChromeOutlined />,
            label: 'Headless Browser',
            children: [
                { key: 'browser-scrape', label: 'Scraping & Automation' },
                { key: 'browser-screenshots', label: 'Screenshots' },
                { key: 'browser-logs', label: 'Activity Logs' },
            ],
        },
        {
            key: 'vpn',
            icon: <ApiOutlined />,
            label: 'VPN Management',
            children: [
                { key: 'vpn-list', label: 'VPN Connections' },
                { key: 'vpn-add', label: 'Add VPN' },
                { key: 'vpn-security', label: 'Security Settings' },
            ],
        },

        // ╔═══════════════════════════════════╗
        // ║ 6️⃣  🐙 DEVELOPMENT & DEPLOYMENT   ║
        // ╚═══════════════════════════════════╝
        {
            key: 'github',
            icon: <GithubOutlined />,
            label: 'GitHub Integration',
            children: [
                { key: 'github-workflows', label: 'Workflows & Runs' },
                { key: 'github-commits', label: 'Commits & History' },
                { key: 'github-issues', label: 'Issues & PRs' },
            ],
        },

        // ╔═══════════════════════════════════╗
        // ║ 7️⃣  📊 MONITORING & ANALYTICS     ║
        // ╚═══════════════════════════════════╝
        {
            key: 'progress',
            icon: <BarChartOutlined />,
            label: 'Progress Tracking',
            children: [
                { key: 'progress-overview', label: 'Work Progress' },
                { key: 'improvements', label: 'AI Improvements' },
                { key: 'ai-decisions', label: 'AI Decisions' },
            ],
        },

        // ╔═══════════════════════════════════╗
        // ║ 8️⃣  🛡️  SYSTEM & SECURITY        ║
        // ╚═══════════════════════════════════╝
        {
            key: 'audit',
            icon: <CheckCircleOutlined />,
            label: 'Audit & Logs',
        },
        {
            key: 'chat-history',
            icon: <ChatOutlined />,
            label: 'Chat History & Process',
        },

        // ╔═══════════════════════════════════╗
        // ║ 9️⃣  ⚙️  SETTINGS                  ║
        // ╚═══════════════════════════════════╝
        {
            key: 'settings',
            icon: <SettingOutlined />,
            label: 'System Settings',
        },
    ];

    const labelOverrides = Object.fromEntries(
        (dashboardContract?.navigation || []).map((item) => [item.key, item.label])
    );

    const menuItems = defaultMenuItems.map((item) => ({
        ...item,
        label: labelOverrides[item.key] || item.label,
    }));

    const handleMenuClick = (e: any) => {
        setSelectedTab(e.key);
    };

    const renderContent = () => {
        switch (selectedTab) {
            case 'overview':
                return <OverviewTab stats={stats} loading={loading} />;
            case 'api-keys':
            case 'api-keys-manage':
            case 'api-keys-discover':
            case 'api-keys-usage':
                return <APIKeysManager />;
            case 'api-management':
            case 'api-add':
            case 'api-list':
            case 'api-test':
                return <APIManagement />;
            case 'search-models':
            case 'manage-models':
            case 'model-performance':
                return <AIModelSearch />;
            case 'vpn':
            case 'vpn-list':
            case 'vpn-add':
            case 'vpn-security':
                return <VPNManagement />;
            case 'ai-chat':
                return <ChatWithAI />;
            case 'ai-assignment':
            case 'assign-new':
            case 'view-assignments':
            case 'workload-balance':
                return <AIAssignment />;
            case 'voting':
            case 'voting-active':
            case 'voting-history':
            case 'decision-rules':
                return <DecisionVoting />;
            case 'kingmode':
                return <KingModePanel />;
            case 'progress':
            case 'progress-overview':
                return <ProgressMo />;
            case 'improvements':
            case 'planned-improvements':
            case 'implemented':
            case 'in-progress':
                return <ImprovementTracking />;
            case 'audit':
                return <AuditLog />;
            case 'chat-history':
                return <ChatHistoryDashboard />;
            case 'github':
            case 'github-workflows':
            case 'github-commits':
            case 'github-issues':
                return <GitHubDashboard />;
            case 'browser':
            case 'browser-scrape':
            case 'browser-screenshots':
            case 'browser-logs':
                return <HeadlessBrowserDashboard />;
            case 'ai-decisions':
                return <AIWorkHistory />;
            case 'settings':
                return <SystemMetrics />;
            default:
                return <OverviewTab stats={stats} loading={loading} />;
        }
    };

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Sider
                collapsible
                collapsed={collapsed}
                onCollapse={setCollapsed}
                style={{ background: '#001529', position: 'fixed', left: 0, top: 0, bottom: 0 }}
                width={250}
            >
                <div style={{ padding: '16px', color: '#fff', textAlign: 'center', fontSize: '18px', fontWeight: 'bold' }}>
                    SupremeAI
                </div>
                <Menu
                    items={menuItems}
                    onClick={handleMenuClick}
                    selectedKeys={[selectedTab]}
                    mode="inline"
                    theme="dark"
                />
            </Sider>

            <Layout style={{ marginLeft: collapsed ? 80 : 250, display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
                <Header
                    style={{
                        background: '#fff',
                        padding: '0 50px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        flexShrink: 0,
                    }}
                >
                    <h1 style={{ margin: 0 }}>{dashboardContract?.title || 'SupremeAI Admin Control Panel'}</h1>
                    <Space>
                        <Badge
                            status={stats.systemHealthStatus === 'healthy' ? 'success' : stats.systemHealthStatus === 'warning' ? 'warning' : 'error'}
                            text={`System: ${stats.systemHealthStatus}`}
                        />
                        <span className="text-muted">Last sync: {stats.lastSyncTime}</span>
                    </Space>
                </Header>

                <Content style={{ margin: '24px 16px', flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
                    <Breadcrumb style={{ marginBottom: '24px', flexShrink: 0 }}>
                        <Breadcrumb.Item>Admin</Breadcrumb.Item>
                        <Breadcrumb.Item>{selectedTab}</Breadcrumb.Item>
                    </Breadcrumb>
                    <div style={{ flex: 1, overflow: 'auto' }}>
                        {renderContent()}
                    </div>
                </Content>
            </Layout>
        </Layout>
    );
};

const OverviewTab: React.FC<{ stats: DashboardStats; loading: boolean }> = ({ stats, loading }) => (
    <div>
        <Alert
            message="Welcome to SupremeAI Admin Control Panel"
            description="Monitor and manage all aspects of your AI system - from API integrations to AI model assignments and decision voting."
            type="info"
            showIcon
            style={{ marginBottom: '24px' }}
        />

        <Row gutter={16} style={{ marginBottom: '24px' }}>
            <Col xs={24} sm={12} lg={6}>
                <Card loading={loading}>
                    <Statistic
                        title="Active AI Agents"
                        value={stats.activeAIAgents}
                        prefix={<RobotOutlined />}
                        suffix="agents"
                    />
                </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
                <Card loading={loading}>
                    <Statistic
                        title="Running Tasks"
                        value={stats.runningTasks}
                        prefix={<BarChartOutlined />}
                    />
                </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
                <Card loading={loading}>
                    <Statistic
                        title="Completed Tasks"
                        value={stats.completedTasks}
                        prefix={<CheckCircleOutlined />}
                        valueStyle={{ color: '#52c41a' }}
                    />
                </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
                <Card loading={loading}>
                    <Statistic
                        title="Success Rate"
                        value={stats.successRate}
                        suffix="%"
                        prefix={<CheckCircleOutlined />}
                        valueStyle={{ color: '#1890ff' }}
                    />
                </Card>
            </Col>
        </Row>

        <Row gutter={16}>
            <Col xs={24} lg={12}>
                <Card title="System Health" loading={loading}>
                    <SystemMetrics />
                </Card>
            </Col>
            <Col xs={24} lg={12}>
                <Card title="Recent Activity" loading={loading}>
                    <AuditLog />
                </Card>
            </Col>
        </Row>
    </div>
);

export default AdminDashboard;
