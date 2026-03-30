// AdminDashboard.tsx - Main SupremeAI Admin Control Panel

import React, { useState, useEffect } from 'react';
import { Layout, Menu, Breadcrumb, Card, Statistic, Row, Col, Alert, Badge, Tabs, Space } from 'antd';
import {
    DashboardOutlined,
    CloudServerOutlined,
    ApiOutlined,
    RobotOutlined,
    TeamOutlined,
    SettingOutlined,
    CheckCircleOutlined,
    WarningOutlined,
    VoteYeaOutlined,
    CrownOutlined,
    ProgressOutlined,
    BugOutlined,
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

const { Header, Content, Sider } = Layout;

interface DashboardStats {
    activeAIAgents: number;
    runningTasks: number;
    completedTasks: number;
    systemHealth: 'healthy' | 'warning' | 'critical';
    successRate: number;
    lastSyncTime: string;
}

const AdminDashboard: React.FC = () => {
    const [collapsed, setCollapsed] = useState(false);
    const [selectedTab, setSelectedTab] = useState('overview');
    const [stats, setStats] = useState<DashboardStats>({
        activeAIAgents: 0,
        runningTasks: 0,
        completedTasks: 0,
        systemHealth: 'healthy',
        successRate: 0,
        lastSyncTime: new Date().toLocaleString(),
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardStats();
        const interval = setInterval(fetchDashboardStats, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    const fetchDashboardStats = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/admin/dashboard/stats', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setStats(data);
            }
        } catch (error) {
            console.error('Failed to fetch dashboard stats:', error);
        } finally {
            setLoading(false);
        }
    };

    const menuItems = [
        {
            key: 'overview',
            icon: <DashboardOutlined />,
            label: 'Overview',
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
            key: 'vpn',
            icon: <ApiOutlined />,
            label: 'VPN Management',
            children: [
                { key: 'vpn-list', label: 'VPN Connections' },
                { key: 'vpn-add', label: 'Add VPN' },
                { key: 'vpn-security', label: 'Security Settings' },
            ],
        },
        {
            key: 'ai-chat',
            icon: <ChatWithAI />,
            label: 'Chat & Commands',
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
        {
            key: 'voting',
            icon: <VoteYeaOutlined />,
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
        {
            key: 'progress',
            icon: <ProgressOutlined />,
            label: 'Progress Tracking',
            children: [
                { key: 'progress-overview', label: 'Work Progress' },
                { key: 'improvements', label: 'AI Improvements' },
                { key: 'ai-decisions', label: 'AI Decisions' },
            ],
        },
        {
            key: 'improvements',
            icon: <BugOutlined />,
            label: 'Improvement Plan',
            children: [
                { key: 'planned-improvements', label: 'Planned Changes' },
                { key: 'implemented', label: 'Implemented' },
                { key: 'in-progress', label: 'In Progress' },
            ],
        },
        {
            key: 'audit',
            icon: <CheckCircleOutlined />,
            label: 'Audit & Logs',
        },
        {
            key: 'settings',
            icon: <SettingOutlined />,
            label: 'System Settings',
        },
    ];

    const handleMenuClick = (e: any) => {
        setSelectedTab(e.key);
    };

    const renderContent = () => {
        switch (selectedTab) {
            case 'overview':
                return <OverviewTab stats={stats} loading={loading} />;
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

            <Layout style={{ marginLeft: collapsed ? 80 : 250 }}>
                <Header
                    style={{
                        background: '#fff',
                        padding: '0 50px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                    }}
                >
                    <h1 style={{ margin: 0 }}>SupremeAI Admin Control Panel</h1>
                    <Space>
                        <Badge
                            status={stats.systemHealth === 'healthy' ? 'success' : stats.systemHealth === 'warning' ? 'warning' : 'error'}
                            text={`System: ${stats.systemHealth}`}
                        />
                        <span className="text-muted">Last sync: {stats.lastSyncTime}</span>
                    </Space>
                </Header>

                <Content style={{ margin: '24px 16px 0' }}>
                    <Breadcrumb style={{ marginBottom: '24px' }}>
                        <Breadcrumb.Item>Admin</Breadcrumb.Item>
                        <Breadcrumb.Item>{selectedTab}</Breadcrumb.Item>
                    </Breadcrumb>
                    {renderContent()}
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
                        prefix={<ProgressOutlined />}
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
                    <AuditLog limit={5} />
                </Card>
            </Col>
        </Row>
    </div>
);

export default AdminDashboard;
