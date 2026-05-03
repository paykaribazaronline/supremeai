// AdminDashboardUnified.tsx - MODERN PREMIUM REDESIGN
// UNIFIED ADMIN DASHBOARD - Single Source of Truth Contract

import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Layout, Menu, Card, Statistic, Row, Col, Alert, Badge, Space, Tabs, Empty, Button, Modal, Input, message, Avatar, Dropdown, Typography, Divider, Collapse, FloatButton } from 'antd';
import { SearchOutlined, MessageOutlined, CloseOutlined } from '@ant-design/icons';
import { 
    BulbOutlined, LogoutOutlined, UserOutlined, BellOutlined,
    DashboardOutlined, ApiOutlined, CloudServerOutlined, RobotOutlined,
    TeamOutlined, SettingOutlined, CheckCircleOutlined, WarningOutlined,
    BugOutlined, NodeIndexOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
    RocketOutlined, SafetyOutlined, DollarOutlined, AuditOutlined,
    SyncOutlined, GlobalOutlined, DesktopOutlined, HistoryOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import PhasesOverview from '../components/PhasesOverview';
import AIAgentsDashboard from '../components/AIAgentsDashboard';
import ExploitationDashboard from '../components/ExploitationDashboard';
import ChatWithAI from '../components/ChatWithAI';
import SystemLearningDashboard from '../components/SystemLearningDashboard';
import RequirementsDashboard from '../components/RequirementsDashboard';
import AdminOCRCard from '../components/AdminOCRCard';
import { notification } from 'antd';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const { TextArea, Title } = Typography;
const { Header, Content, Sider } = Layout;
const { Panel } = Collapse;

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
    const [collapsed, setCollapsed] = useState(true);
    const [selectedKey, setSelectedKey] = useState('overview');
    const [contract, setContract] = useState<DashboardContract | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [chatVisible, setChatVisible] = useState(false);
    const [notifications, setNotifications] = useState<Record<string, number>>({
        'ai-agents': 3,
        'exploitation-techniques': 1,
        'phases': 0,
        'git-ops': 2
    });

    // Suggestion modal state
    const [suggestionOpen, setSuggestionOpen] = useState(false);
    const [suggestionText, setSuggestionText] = useState('');
    const [suggestionLoading, setSuggestionLoading] = useState(false);

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
        // Refresh every 30 seconds
        const interval = setInterval(fetchContract, 30000);
        
        // WebSocket connection for real-time notifications
        const connectWebSocket = () => {
          try {
            // Use relative URL to work with both dev and prod (same origin)
            const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws'}://${window.location.host}/ws`;
            const socket = new SockJS(wsUrl);
            const stompClient = new Client({
              webSocketFactory: () => socket,
              reconnectDelay: 5000,
              onConnect: () => {
                console.log("[SupremeAI] WebSocket connected for notifications");
                
                // Subscribe to notifications topic
                stompClient.subscribe('/topic/notifications', (message) => {
                  const data = JSON.parse(message.body);
                  console.log("[SupremeAI] Notification received:", data);
                  
                  if (data.type === 'GITHUB_PIPELINE') {
                    if (data.status === 'success') {
                      notification.success({
                        message: '🚀 Deployment Successful',
                        description: data.message || 'Pipeline completed successfully',
                        duration: 5,
                        placement: 'topRight',
                      });
                    } else if (data.status === 'failure' || data.status === 'error') {
                      notification.error({
                        message: '🚨 Deployment Failed',
                        description: data.message || 'Pipeline failed',
                        duration: 0, // Persistent until user dismisses
                        placement: 'topRight',
                      });
                    } else {
                      notification.info({
                        message: '📋 Pipeline Update',
                        description: data.message,
                        duration: 4,
                        placement: 'topRight',
                      });
                    }
                  } else if (data.type === 'SYSTEM_ALERT') {
                    const level = data.status;
                    if (level === 'error' || level === 'critical') {
                      notification.error({
                        message: '⚠️ System Alert',
                        description: data.message,
                        duration: 0,
                        placement: 'topRight',
                      });
                    } else if (level === 'warning') {
                      notification.warning({
                        message: '⚡ System Warning',
                        description: data.message,
                        duration: 6,
                        placement: 'topRight',
                      });
                    } else {
                      notification.info({
                        message: 'ℹ️ System Info',
                        description: data.message,
                        duration: 4,
                        placement: 'topRight',
                      });
                    }
                  } else if (data.type === 'LEARNING_UPDATE') {
                    notification.success({
                      message: '🧠 SupremeAI Learning Update',
                      description: data.message || 'New patterns learned from your edits',
                      duration: 4,
                      placement: 'topRight',
                    });
                  }
                });
              },
              onStompError: (frame) => {
                console.error('WebSocket error:', frame);
                // Silent reconnect will happen automatically
              },
              onWebSocketError: (event) => {
                console.error('WebSocket connection error:', event);
              }
            });
            
            stompClient.activate();
            return () => stompClient.deactivate();
          } catch (err) {
            console.error("Failed to connect WebSocket:", err);
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

    // Build grouped menu from contract navigation
    const menuGroups = [
        {
            key: 'dashboard',
            label: '📊 Dashboard & Analytics',
            type: 'group',
            children: contract.navigation
                .filter(i => ['overview', 'metrics', 'analytics', 'quota', 'cost'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
        {
            key: 'ai',
            label: '🤖 AI Systems',
            type: 'group',
            children: contract.navigation
                .filter(i => ['ai-agents', 'providers', 'ml-intelligence', 'ai-models'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
        {
            key: 'learning',
            label: '📚 Knowledge & Learning',
            type: 'group',
            children: contract.navigation
                .filter(i => ['learning', 'system-learning', 'teaching', 'research'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
        {
            key: 'operations',
            label: '⚙️ Operations',
            type: 'group',
            children: contract.navigation
                .filter(i => ['git-ops', 'deployment', 'vpn', 'headless-browser', 'exploitation-techniques'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
        {
            key: 'health',
            label: '🛡️ System Health',
            type: 'group',
            children: contract.navigation
                .filter(i => ['resilience', 'autofix', 'self-healing', 'audit'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
        {
            key: 'admin',
            label: '👥 Administration',
            type: 'group',
            children: contract.navigation
                .filter(i => ['user-management', 'api-keys', 'notifications', 'settings', 'phases'].includes(i.key))
                .map(item => ({
                    key: item.key,
                    icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                    label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                    title: item.description,
                    disabled: !item.enabled,
                }))
        },
    ];

    // Filter out empty groups
    const menuItems = menuGroups.filter(group => group.children.length > 0);

    // Get selected component from contract
    const selectedComponent = contract.components.find((c) => c.key === selectedKey);

    const handleLogout = () => {
        authUtils.clearToken();
        window.location.reload();
    };

    const userDropdownItems = [
        { key: 'profile', label: 'Profile', icon: <UserOutlined /> },
        { key: 'settings', label: 'Settings', icon: <SettingOutlined /> },
        { type: 'divider' },
        { key: 'logout', label: 'Logout', icon: <LogoutOutlined />, onClick: handleLogout },
    ];

    return (
        <Layout style={{ minHeight: '100vh', background: '#F8FAFC' }}>
            {/* SIDEBAR - Modern premium design */}
            <Sider
                collapsible
                collapsed={collapsed}
                onCollapse={setCollapsed}
                width={280}
                collapsedWidth={80}
                style={{ 
                    background: '#0F172A',
                    borderRight: '1px solid rgba(255,255,255,0.05)',
                    boxShadow: '2px 0 10px rgba(0,0,0,0.1)',
                }}
            >
                <div
                    style={{
                        height: '80px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        padding: '0 20px',
                        borderBottom: '1px solid rgba(255,255,255,0.05)',
                    }}
                >
                    {collapsed ? (
                        <Avatar 
                            size={44} 
                            style={{ 
                                background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                                fontWeight: 700,
                                fontSize: '18px',
                            }}
                        >
                            AI
                        </Avatar>
                    ) : (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <Avatar 
                                size={44} 
                                style={{ 
                                    background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                                    fontWeight: 700,
                                }}
                                icon={<RobotOutlined style={{ fontSize: '22px' }} />}
                            />
                            <div>
                                <div style={{ color: '#fff', fontWeight: 700, fontSize: '18px' }}>SupremeAI</div>
                                <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px' }}>Admin Console</div>
                            </div>
                        </div>
                    )}
                </div>

                <div style={{ padding: '12px 12px' }}>
                    {!collapsed && (
                        <Input
                            prefix={<SearchOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
                            placeholder="Search tabs..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            style={{
                                background: 'rgba(255,255,255,0.05)',
                                border: '1px solid rgba(255,255,255,0.1)',
                                borderRadius: '10px',
                                color: 'white',
                                marginBottom: '8px',
                            }}
                            placeholderStyle={{ color: 'rgba(255,255,255,0.4)' }}
                            allowClear
                        />
                    )}
                    
                    <Menu
                        theme="dark"
                        mode="inline"
                        selectedKeys={[selectedKey]}
                        onSelect={(e) => {
                            // Scroll to section instead of selecting tab
                            const element = document.getElementById(e.key);
                            if (element) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                            }
                            setSelectedKey(e.key);
                            setSearchQuery('');
                        }}
                        items={useMemo(() => {
                            if (!searchQuery.trim()) return menuItems;
                            
                            // Filter menu when searching
                            const query = searchQuery.toLowerCase();
                            const flatItems = contract.navigation.filter(i => 
                                i.label.toLowerCase().includes(query) ||
                                i.key.toLowerCase().includes(query) ||
                                i.description?.toLowerCase().includes(query)
                            ).map(item => ({
                                key: item.key,
                                icon: <span style={{ fontSize: '16px' }}>{item.icon}</span>,
                                label: <span style={{ fontWeight: 500 }}>{item.label}</span>,
                                title: item.description,
                                disabled: !item.enabled,
                            }));
                            
                            return flatItems.length > 0 ? flatItems : [{
                                key: 'no-results',
                                label: <span style={{ color: 'rgba(255,255,255,0.4)' }}>No results found</span>,
                                disabled: true
                            }];
                        }, [searchQuery, menuItems, contract])}
                        style={{ 
                            background: 'transparent',
                            borderRight: 'none',
                        }}
                    />
                </div>
            </Sider>

            <Layout>
                {/* HEADER - Modern premium design */}
                <Header style={{ 
                    background: '#FFFFFF', 
                    padding: '0 32px', 
                    borderBottom: '1px solid #E5E7EB',
                    height: '72px',
                    lineHeight: '72px',
                    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
                }}>
                    <div style={{ 
                        display: 'flex', 
                        justifyContent: 'space-between', 
                        alignItems: 'center',
                        height: '100%',
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                            <Button 
                                type="text" 
                                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                                onClick={() => setCollapsed(!collapsed)}
                                style={{ fontSize: '18px', width: '44px', height: '44px', borderRadius: '10px' }}
                            />
                            <div>
                                <Title level={4} style={{ margin: 0, fontWeight: 700 }}>{contract.title}</Title>
                                <div style={{ color: '#6B7280', fontSize: '13px', marginTop: '-4px' }}>
                                    Intelligent Platform Management
                                </div>
                            </div>
                        </div>
                        
                        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                            {contract.stats.systemHealthStatus && (
                                <Badge
                                    status={contract.stats.systemHealthStatus === 'healthy' ? 'success' : contract.stats.systemHealthStatus === 'warning' ? 'warning' : 'error'}
                                    text={<span style={{ fontWeight: 500 }}>System {contract.stats.systemHealthStatus}</span>}
                                />
                            )}
                            
                            <Button 
                                type="text" 
                                icon={<BellOutlined style={{ fontSize: '20px', color: '#6B7280' }} />}
                                style={{ width: '44px', height: '44px', borderRadius: '10px' }}
                            />
                            
                            <Badge
                                count={`v${contract.contractVersion}`}
                                style={{ 
                                    backgroundColor: '#7C3AED',
                                    fontWeight: 600,
                                    padding: '0 12px',
                                    borderRadius: '20px',
                                    height: '28px',
                                    lineHeight: '28px',
                                }}
                            />
                            
                            <Divider type="vertical" style={{ height: '32px' }} />
                            
                            <Dropdown menu={{ items: userDropdownItems }} placement="bottomRight">
                                <div style={{ 
                                    display: 'flex', 
                                    alignItems: 'center', 
                                    gap: '12px', 
                                    cursor: 'pointer',
                                    padding: '6px 12px',
                                    borderRadius: '12px',
                                    transition: 'all 0.2s',
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.background = '#F3F4F6'}
                                onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                                >
                                    <Avatar 
                                        size={36} 
                                        style={{ 
                                            background: 'linear-gradient(135deg, #10B981 0%, #34D399 100%)',
                                            fontWeight: 600,
                                        }}
                                        icon={<UserOutlined />}
                                    />
                                    <div style={{ textAlign: 'right' }}>
                                        <div style={{ fontWeight: 600, color: '#111827' }}>Admin</div>
                                        <div style={{ fontSize: '12px', color: '#6B7280' }}>Administrator</div>
                                    </div>
                                </div>
                            </Dropdown>
                        </div>
                    </div>
                </Header>

                {/* CONTENT */}
                <Content style={{ margin: '28px 32px', paddingBottom: '40px', overflowY: 'auto' }}>
                    {/* Stats Row - Modern gradient cards */}
                    <Row gutter={[20, 20]} style={{ marginBottom: '28px' }}>
                        <Col xs={24} sm={12} md={6}>
                            <Card 
                                style={{ 
                                    borderRadius: '16px',
                                    border: 'none',
                                    background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                                    boxShadow: '0 10px 40px -10px rgba(124, 58, 237, 0.3)',
                                    transition: 'all 0.3s ease',
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-4px)';
                                    e.currentTarget.style.boxShadow = '0 20px 60px -15px rgba(124, 58, 237, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 10px 40px -10px rgba(124, 58, 237, 0.3)';
                                }}
                            >
                                <Statistic
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)', fontWeight: 500, fontSize: '13px' }}>Active AI Agents</span>}
                                    value={contract.stats.activeAIAgents}
                                    prefix={<RobotOutlined style={{ color: 'rgba(255,255,255,0.9)', fontSize: '22px' }} />}
                                    valueStyle={{ color: '#FFFFFF', fontWeight: 700, fontSize: '32px' }}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card 
                                style={{ 
                                    borderRadius: '16px',
                                    border: 'none',
                                    background: 'linear-gradient(135deg, #3B82F6 0%, #60A5FA 100%)',
                                    boxShadow: '0 10px 40px -10px rgba(59, 130, 246, 0.3)',
                                    transition: 'all 0.3s ease',
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-4px)';
                                    e.currentTarget.style.boxShadow = '0 20px 60px -15px rgba(59, 130, 246, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 10px 40px -10px rgba(59, 130, 246, 0.3)';
                                }}
                            >
                                <Statistic
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)', fontWeight: 500, fontSize: '13px' }}>Running Tasks</span>}
                                    value={contract.stats.runningTasks}
                                    valueStyle={{ color: '#FFFFFF', fontWeight: 700, fontSize: '32px' }}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card 
                                style={{ 
                                    borderRadius: '16px',
                                    border: 'none',
                                    background: 'linear-gradient(135deg, #10B981 0%, #34D399 100%)',
                                    boxShadow: '0 10px 40px -10px rgba(16, 185, 129, 0.3)',
                                    transition: 'all 0.3s ease',
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-4px)';
                                    e.currentTarget.style.boxShadow = '0 20px 60px -15px rgba(16, 185, 129, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 10px 40px -10px rgba(16, 185, 129, 0.3)';
                                }}
                            >
                                <Statistic
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)', fontWeight: 500, fontSize: '13px' }}>Completed Tasks</span>}
                                    value={contract.stats.completedTasks}
                                    valueStyle={{ color: '#FFFFFF', fontWeight: 700, fontSize: '32px' }}
                                />
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Card 
                                style={{ 
                                    borderRadius: '16px',
                                    border: 'none',
                                    background: 'linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%)',
                                    boxShadow: '0 10px 40px -10px rgba(245, 158, 11, 0.3)',
                                    transition: 'all 0.3s ease',
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-4px)';
                                    e.currentTarget.style.boxShadow = '0 20px 60px -15px rgba(245, 158, 11, 0.4)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 10px 40px -10px rgba(245, 158, 11, 0.3)';
                                }}
                            >
                                <Statistic
                                    title={<span style={{ color: 'rgba(255,255,255,0.8)', fontWeight: 500, fontSize: '13px' }}>Success Rate</span>}
                                    value={contract.stats.successRate}
                                    suffix={<span style={{ fontSize: '20px' }}>%</span>}
                                    valueStyle={{ color: '#FFFFFF', fontWeight: 700, fontSize: '32px' }}
                                />
                            </Card>
                        </Col>
                    </Row>

                    <Row gutter={[24, 24]}>
                        <Col lg={chatVisible ? 16 : 24} md={24} style={{ transition: 'all 0.3s' }}>
                            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                                {/* Unified Cards - All in one scrollable page */}

                                <Card
                                    id="requirements"
                                    title={
                                        <Space>
                                            <Badge count={notifications['requirements'] || 2} offset={[10, 0]} color="#f5222d">
                                                <CheckCircleOutlined style={{ color: '#10B981' }} />
                                            </Badge>
                                            <span>Requirements Approval</span>
                                        </Space>
                                    }
                                    extra={<Button size="small" type="primary" ghost>Approve All</Button>}
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <RequirementsDashboard />
                                </Card>

                                <Card
                                    id="ocr"
                                    title={
                                        <Space>
                                            <DesktopOutlined style={{ color: '#eb2f96' }} />
                                            <span>Bengali OCR Processing</span>
                                        </Space>
                                    }
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <AdminOCRCard />
                                </Card>

                                <Card
                                    id="ai-agents"
                                    title={
                                        <Space>
                                            <Badge count={notifications['ai-agents']} offset={[10, 0]} color="#ff4d4f">
                                                <RobotOutlined style={{ color: '#7C3AED' }} />
                                            </Badge>
                                            <span>AI Agents Management</span>
                                        </Space>
                                    }
                                    extra={<Button size="small" type="primary" ghost icon={<SyncOutlined />}>Rotate All</Button>}
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <AIAgentsDashboard />
                                </Card>

                                <Card
                                    id="phases"
                                    title={
                                        <Space>
                                            <Badge count={notifications['phases']} offset={[10, 0]}>
                                                <NodeIndexOutlined style={{ color: '#3B82F6' }} />
                                            </Badge>
                                            <span>Project Phases Overview</span>
                                        </Space>
                                    }
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <PhasesOverview />
                                </Card>

                                <Card
                                    id="exploitation-techniques"
                                    title={
                                        <Space>
                                            <Badge count={notifications['exploitation-techniques']} offset={[10, 0]}>
                                                <BugOutlined style={{ color: '#EF4444' }} />
                                            </Badge>
                                            <span>Exploitation & Security</span>
                                        </Space>
                                    }
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <ExploitationDashboard />
                                </Card>

                                <Card
                                    id="system-learning"
                                    title={
                                        <Space>
                                            <BulbOutlined style={{ color: '#F59E0B' }} />
                                            <span>System Learning Intelligence</span>
                                        </Space>
                                    }
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <SystemLearningDashboard />
                                </Card>

                                {/* API Endpoints Section */}
                                <Card
                                    id="api-endpoints"
                                    title={<Space><ApiOutlined style={{ color: '#10B981' }} /><span>API Endpoints Console</span></Space>}
                                    style={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}
                                >
                                    <Tabs
                                        size="large"
                                        items={Object.entries(contract.apiEndpoints).map(([category, endpoints]) => ({
                                            key: category,
                                            label: <span style={{ fontWeight: 600, textTransform: 'uppercase' }}>{category}</span>,
                                            children: (
                                                <pre style={{
                                                    background: '#F8FAFC',
                                                    padding: '20px',
                                                    borderRadius: '12px',
                                                    border: '1px solid #E5E7EB',
                                                    overflow: 'auto',
                                                    maxHeight: '400px',
                                                    fontSize: '13px',
                                                    fontFamily: 'JetBrains Mono, monospace',
                                                }}>
                                                    {JSON.stringify(endpoints, null, 2)}
                                                </pre>
                                            ),
                                        }))}
                                    />
                                </Card>
                            </Space>
                        </Col>

                        {/* Inline Chat Panel */}
                        {chatVisible && (
                            <Col lg={8} md={24}>
                                <div style={{
                                    position: 'sticky',
                                    top: '20px',
                                    height: 'calc(100vh - 150px)',
                                    display: 'flex',
                                    flexDirection: 'column'
                                }}>
                                    <Card
                                        title={
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <Space><MessageOutlined /><span>ChatAI Assistant</span></Space>
                                                <Button type="text" icon={<CloseOutlined />} onClick={() => setChatVisible(false)} />
                                            </div>
                                        }
                                        bodyStyle={{ flex: 1, padding: 0, overflow: 'hidden' }}
                                        style={{ height: '100%', borderRadius: '16px', display: 'flex', flexDirection: 'column' }}
                                    >
                                        <ChatWithAI />
                                    </Card>
                                </div>
                            </Col>
                        )}
                    </Row>
                </Content>

                {/* Floating Action Button for Chat */}
                {!chatVisible && (
                    <FloatButton
                        icon={<MessageOutlined />}
                        type="primary"
                        onClick={() => setChatVisible(true)}
                        style={{ right: 24, bottom: 24, width: 60, height: 60 }}
                        tooltip={<div>Open ChatAI (C)</div>}
                    />
                )}
            </Layout>

            {/* Suggestion Modal */}
            <Modal
                title={
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '12px',
                            background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                        }}>
                            <BulbOutlined style={{ color: 'white', fontSize: '20px' }} />
                        </div>
                        <div>
                            <div style={{ fontWeight: 700, fontSize: '18px' }}>Suggest Changes</div>
                            {selectedComponent && (
                                <div style={{ fontSize: '13px', color: '#6B7280' }}>
                                    for {selectedComponent.label}
                                </div>
                            )}
                        </div>
                    </div>
                }
                open={suggestionOpen}
                onCancel={() => { setSuggestionOpen(false); setSuggestionText(''); }}
                footer={[
                    <Button 
                        key="cancel" 
                        onClick={() => { setSuggestionOpen(false); setSuggestionText(''); }}
                        style={{ borderRadius: '10px', height: '44px', fontWeight: 500 }}
                    >
                        Cancel
                    </Button>,
                    <Button
                        key="save"
                        loading={suggestionLoading}
                        onClick={() => submitSuggestion(false)}
                        style={{ borderRadius: '10px', height: '44px', fontWeight: 500 }}
                    >
                        💾 Save Suggestion
                    </Button>,
                    <Button
                        key="apply"
                        type="primary"
                        loading={suggestionLoading}
                        onClick={() => submitSuggestion(true)}
                        style={{ 
                            background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                            border: 'none',
                            borderRadius: '10px',
                            height: '44px',
                            fontWeight: 600,
                            boxShadow: '0 4px 15px -5px rgba(124, 58, 237, 0.4)',
                        }}
                    >
                        🤖 Apply Immediately
                    </Button>,
                ]}
                width={650}
                centered
                style={{ top: 20 }}
                bodyStyle={{ padding: '24px 32px' }}
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
