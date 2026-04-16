// AdminDashboardUnified.tsx
// UNIFIED ADMIN DASHBOARD - Single Source of Truth Contract
// 
// ⭐ ALL CLIENTS CONSUME THIS:
// - React Web (this file)
// - Flutter Mobile (fetches same /api/admin/dashboard/contract)
// - Flutter Web (fetches same /api/admin/dashboard/contract)
//
// ONE CHANGE IN BACKEND = EVERYWHERE UPDATES

import React, { useState, useEffect } from 'react';
import { Layout, Menu, Card, Statistic, Row, Col, Alert, Badge, Space, Tabs, Empty, Button, Modal, Input, message } from 'antd';
import { BulbOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import {
    DashboardOutlined,
    ApiOutlined,
    CloudServerOutlined,
    RobotOutlined,
    TeamOutlined,
    SettingOutlined,
    CheckCircleOutlined,
    WarningOutlined,
    BugOutlined,
    NodeIndexOutlined,
} from '@ant-design/icons';
import PhasesOverview from '../components/PhasesOverview';
import AIAgentsDashboard from '../components/AIAgentsDashboard';
import ExploitationDashboard from '../components/ExploitationDashboard';

const { TextArea } = Input;

const { Header, Content, Sider } = Layout;

interface DashboardStats {
    activeAIAgents: number;
    runningTasks: number;
    completedTasks: number;
    systemHealthStatus?: 'healthy' | 'warning' | 'critical';
    systemHealthScore: number;
    systemHealthReason?: string;
    successRate: number;
}

interface ComponentDef {
    key: string;
    label: string;
    icon: string;
    category: string;
    enabled: boolean;
    config: Record<string, any>;
}

interface NavigationItem {
    key: string;
    label: string;
    icon: string;
    description: string;
    enabled: boolean;
}

interface DashboardContract {
    contractVersion: string;
    title: string;
    description: string;
    entryPath: string;
    language: string;
    stats: DashboardStats;
    navigation: NavigationItem[];
    components: ComponentDef[];
    apiEndpoints: Record<string, any>;
}

const AdminDashboardUnified: React.FC = () => {
    const [collapsed, setCollapsed] = useState(false);
    const [selectedKey, setSelectedKey] = useState('overview');
    const [contract, setContract] = useState<DashboardContract | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Suggestion modal state
    const [suggestionOpen, setSuggestionOpen] = useState(false);
    const [suggestionText, setSuggestionText] = useState('');
    const [suggestionLoading, setSuggestionLoading] = useState(false);

    useEffect(() => {
        fetchContract();
        // Refresh every 30 seconds
        const interval = setInterval(fetchContract, 30000);
        return () => clearInterval(interval);
    }, []);

    const fetchContract = async () => {
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/admin/dashboard/contract', {
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (!response.ok) throw new Error('Failed to fetch contract');

            const data: DashboardContract = await response.json();
            setContract(data);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load dashboard');
        } finally {
            setLoading(false);
        }
    };

    const submitSuggestion = async (applyNow: boolean) => {
        if (!suggestionText.trim()) {
            message.warning('Please enter a suggestion before submitting.');
            return;
        }
        setSuggestionLoading(true);
        try {
            const token = authUtils.getToken();
            const selectedNav = contract?.navigation.find(n => n.key === selectedKey);
            const payload = {
                tabKey: selectedKey,
                tabLabel: selectedNav?.label || selectedKey,
                suggestion: suggestionText.trim(),
                applyNow,
            };
            const res = await fetch('/api/admin/suggestions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });
            if (!res.ok) throw new Error('Failed to submit suggestion');
            message.success(
                applyNow
                    ? '🤖 Applying your suggestion now — the system will process it shortly.'
                    : '💾 Suggestion saved. The system will review it.'
            );
            setSuggestionText('');
            setSuggestionOpen(false);
        } catch (err) {
            message.error('Failed to submit suggestion. Please try again.');
        } finally {
            setSuggestionLoading(false);
        }
    };

    if (loading) {
        return <div style={{ padding: '50px', textAlign: 'center' }}>Loading unified dashboard...</div>;
    }

    if (error || !contract) {
        return <Alert type="error" message={error || 'Failed to load dashboard'} />;
    }

    // Build menu from contract navigation
    const menuItems = contract.navigation.map((item) => ({
        key: item.key,
        icon: <span>{item.icon}</span>,
        label: item.label,
        title: item.description,
        disabled: !item.enabled,
    }));

    // Get selected component from contract
    const selectedComponent = contract.components.find((c) => c.key === selectedKey);

    return (
        <Layout style={{ minHeight: '100vh' }}>
            {/* SIDEBAR - From contract navigation */}
            <Sider
                collapsible
                collapsed={collapsed}
                onCollapse={setCollapsed}
                width={250}
                style={{ background: '#001529' }}
            >
                <div
                    style={{
                        height: '64px',
                        background: '#rgba(255,255,255,0.1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                        fontWeight: 'bold',
                        fontSize: '16px',
                    }}
                >
                    {collapsed ? 'AI' : '⭐ SupremeAI'}
                </div>

                <Menu
                    theme="dark"
                    mode="inline"
                    selectedKeys={[selectedKey]}
                    onSelect={(e) => setSelectedKey(e.key)}
                    items={menuItems}
                />
            </Sider>

            <Layout>
                {/* HEADER */}
                <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h1 style={{ margin: 0 }}>{contract.title}</h1>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}>
                                {contract.stats.systemHealthStatus && (
                                    <>
                                        <Badge
                                            status={contract.stats.systemHealthStatus === 'healthy' ? 'success' : contract.stats.systemHealthStatus === 'warning' ? 'warning' : 'error'}
                                            text={`System: ${contract.stats.systemHealthStatus}`}
                                        />
                                        {contract.stats.systemHealthReason && (
                                            <span style={{ fontSize: '12px', color: '#666', maxWidth: '300px', textAlign: 'right' }}>
                                                {contract.stats.systemHealthReason}
                                            </span>
                                        )}
                                    </>
                                )}
                            </div>
                            <Badge
                                count={`v${contract.contractVersion}`}
                                style={{ backgroundColor: '#52c41a' }}
                            />
                        </div>
                    </div>
                </Header>

                {/* CONTENT */}
                <Content style={{ margin: '24px' }}>
                    {/* Stats Row - From contract */}
                    <Row gutter={16} style={{ marginBottom: '24px' }}>
                        <Col xs={24} sm={12} md={6}>
                            <Card>
                                <Statistic
                                    title="Active AI Agents"
                                    value={contract.stats.activeAIAgents}
                                    prefix={<RobotOutlined />}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card>
                                <Statistic
                                    title="Running Tasks"
                                    value={contract.stats.runningTasks}
                                    valueStyle={{ color: '#1890ff' }}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card>
                                <Statistic
                                    title="Completed Tasks"
                                    value={contract.stats.completedTasks}
                                    valueStyle={{ color: '#52c41a' }}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card>
                                <Statistic
                                    title="Success Rate"
                                    value={contract.stats.successRate}
                                    suffix="%"
                                    valueStyle={{ color: '#faad14' }}
                                />
                            </Card>
                        </Col>
                    </Row>

                    {/* Selected Component Panel */}
                    <Card
                        title={
                            selectedComponent
                                ? `${selectedComponent.icon} ${selectedComponent.label}`
                                : 'Select a component'
                        }
                        extra={
                            <Space>
                                {selectedComponent && (
                                    <Badge
                                        status={selectedComponent.enabled ? 'success' : 'error'}
                                        text={selectedComponent.enabled ? 'Enabled' : 'Disabled'}
                                    />
                                )}
                                <Button
                                    type="primary"
                                    icon={<BulbOutlined />}
                                    onClick={() => setSuggestionOpen(true)}
                                    style={{ background: '#722ed1', borderColor: '#722ed1' }}
                                >
                                    Suggest Changes
                                </Button>
                            </Space>
                        }
                    >
                        {selectedComponent ? (
                            <div>
                                {/* Render real component for known keys */}
                                {selectedKey === 'phases' ? (
                                    <PhasesOverview />
                                ) : selectedKey === 'ai-agents' ? (
                                    <AIAgentsDashboard />
                                ) : selectedKey === 'exploitation-techniques' ? (
                                    <ExploitationDashboard />
                                ) : (
                                    <>
                                        <p>
                                            <strong>Category:</strong> {selectedComponent.category}
                                        </p>
                                        <p>
                                            <strong>Configuration:</strong>
                                        </p>
                                        <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px' }}>
                                            {JSON.stringify(selectedComponent.config, null, 2)}
                                        </pre>

                                        {/* Placeholder for actual component render */}
                                        <div
                                            style={{
                                                marginTop: '20px',
                                                padding: '20px',
                                                background: '#fafafa',
                                                textAlign: 'center',
                                                borderRadius: '4px',
                                            }}
                                        >
                                            <p style={{ color: '#999' }}>
                                                {selectedComponent.label} component placeholder
                                            </p>
                                            <p style={{ fontSize: '12px', color: '#ccc' }}>
                                                Endpoint: {selectedComponent.config?.endpoint || 'N/A'}
                                            </p>
                                        </div>
                                    </>
                                )}
                            </div>
                        ) : (
                            <Empty description="No component selected" />
                        )}
                    </Card>

                    {/* Available Endpoints - From contract */}
                    <Card title="📡 API Endpoints" style={{ marginTop: '24px' }}>
                        <Tabs
                            items={Object.entries(contract.apiEndpoints).map(([category, endpoints]) => ({
                                key: category,
                                label: category.toUpperCase(),
                                children: (
                                    <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px' }}>
                                        {JSON.stringify(endpoints, null, 2)}
                                    </pre>
                                ),
                            }))}
                        />
                    </Card>
                </Content>
            </Layout>

            {/* Suggestion Modal */}
            <Modal
                title={
                    <Space>
                        <BulbOutlined style={{ color: '#722ed1' }} />
                        <span>
                            Suggest Changes
                            {selectedComponent ? ` — ${selectedComponent.label}` : ''}
                        </span>
                    </Space>
                }
                open={suggestionOpen}
                onCancel={() => { setSuggestionOpen(false); setSuggestionText(''); }}
                footer={[
                    <Button key="cancel" onClick={() => { setSuggestionOpen(false); setSuggestionText(''); }}>
                        Cancel
                    </Button>,
                    <Button
                        key="save"
                        loading={suggestionLoading}
                        onClick={() => submitSuggestion(false)}
                    >
                        💾 Save
                    </Button>,
                    <Button
                        key="apply"
                        type="primary"
                        loading={suggestionLoading}
                        onClick={() => submitSuggestion(true)}
                        style={{ background: '#722ed1', borderColor: '#722ed1' }}
                    >
                        🤖 Do Now
                    </Button>,
                ]}
                width={600}
            >
                <p style={{ color: '#666', marginBottom: '12px' }}>
                    Describe the change you want on the <strong>{selectedComponent?.label || selectedKey}</strong> tab.
                    Click <strong>Save</strong> to store it for later, or <strong>Do Now</strong> to let the AI apply it immediately.
                </p>
                <TextArea
                    rows={6}
                    placeholder="e.g. Add a toggle to disable new user registrations from this tab..."
                    value={suggestionText}
                    onChange={e => setSuggestionText(e.target.value)}
                    maxLength={2000}
                    showCount
                    autoFocus
                />
            </Modal>
        </Layout>
    );
};

export default AdminDashboardUnified;
