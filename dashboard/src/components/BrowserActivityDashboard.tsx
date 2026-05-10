import React, { useState, useEffect } from 'react';
import { authUtils } from '../lib/authUtils';
import {
    Card,
    Row,
    Col,
    Statistic,
    Table,
    Button,
    Form,
    Input,
    Modal,
    Tag,
    Alert,
    Spin,
    Space,
    Badge,
    Tooltip,
    Switch,
    Divider,
    List,
    Typography,
    message,
    Tabs,
    Popconfirm,
    Progress
} from 'antd';
import {
    GlobalOutlined,
    PauseCircleOutlined,
    PlayCircleOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    ClockCircleOutlined,
    LinkOutlined,
    UserOutlined,
    LockOutlined,
    ReloadOutlined,
    StopOutlined,
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    CheckOutlined,
    CloseOutlined,
    BulbOutlined,
    ThunderboltOutlined
} from '@ant-design/icons';

interface BrowserActivity {
    id: string;
    url: string;
    title?: string;
    status: 'navigating' | 'loading' | 'completed' | 'paused' | 'error';
    timestamp: string;
    duration: number;
    action: 'surf' | 'scrape' | 'login' | 'search';
    hasAuthRequired?: boolean;
    elementText?: string;
}

interface StoredCredential {
    id: string;
    website: string;
    username: string;
    createdAt: string;
}

interface BrowserStatus {
    isActive: boolean;
    currentUrl?: string;
    pagesVisited: number;
    startTime: string;
    pausedForAuth: boolean;
}

interface UrlPermission {
    id: string;
    url: string;
    pattern: string;
    type: 'allowed' | 'denied';
    createdAt: string;
    reason?: string;
}

interface UrlPermissionRequest {
    id: string;
    url: string;
    pattern: string;
    status: 'pending' | 'approved' | 'denied';
    requestedAt: string;
    reason?: string;
    learnedFrom?: string;
}

interface SystemLearning {
    knowledgeNodes: number;
    lastSync: string;
    autoLearnEnabled: boolean;
}

const BrowserActivityDashboard: React.FC = () => {
    const [activities, setActivities] = useState<BrowserActivity[]>([]);
    const [status, setStatus] = useState<BrowserStatus>({
        isActive: false,
        pagesVisited: 0,
        startTime: new Date().toISOString(),
        pausedForAuth: false
    });
    const [loading, setLoading] = useState(true);
    const [autoSurfEnabled, setAutoSurfEnabled] = useState(false);
    const [credentials, setCredentials] = useState<StoredCredential[]>([]);
    const [pauseModalVisible, setPauseModalVisible] = useState(false);
    const [pauseActivity, setPauseActivity] = useState<BrowserActivity | null>(null);
    const [credentialsForm] = Form.useForm();
    const [simulateForm] = Form.useForm();
    const [simulateModalVisible, setSimulateModalVisible] = useState(false);
    
    // URL Permission lists
    const [allowedUrls, setAllowedUrls] = useState<UrlPermission[]>([]);
    const [deniedUrls, setDeniedUrls] = useState<UrlPermission[]>([]);
    const [urlPermissionRequests, setUrlPermissionRequests] = useState<UrlPermissionRequest[]>([]);
    const [urlForm] = Form.useForm();
    const [urlModalVisible, setUrlModalVisible] = useState(false);
    const [editingUrl, setEditingUrl] = useState<UrlPermission | null>(null);
    const [activeUrlTab, setActiveUrlTab] = useState<'allowed' | 'denied' | 'requests'>('allowed');

    // System Learning state
    const [systemLearning, setSystemLearning] = useState<SystemLearning>({
        knowledgeNodes: 0,
        lastSync: new Date().toISOString(),
        autoLearnEnabled: true
    });

    const [tasks, setTasks] = useState<any[]>([]);
    const [taskModalVisible, setTaskModalVisible] = useState(false);
    const [findingsMap, setFindingsMap] = useState<Record<string, any[]>>({});
    const [taskForm] = Form.useForm();

    useEffect(() => {
        fetchBrowserData();
        const interval = setInterval(fetchBrowserData, 5000);
        return () => clearInterval(interval);
    }, []);

    const getToken = () => authUtils.getToken();

    const fetchBrowserData = async () => {
        setLoading(true);
        try {
            const token = getToken();
            const headers = { 'Authorization': `Bearer ${token}` };

            const statusRes = await fetch('/api/browser/surf/status', { headers });
            if (statusRes.ok) {
                const data = await statusRes.json();
                setStatus(data.status);
            }

            const activityRes = await fetch('/api/browser/activity/recent', { headers });
            if (activityRes.ok) {
                const data = await activityRes.json();
                setActivities(data.activities || []);
            }

            const credRes = await fetch('/api/browser/credentials', { headers });
            if (credRes.ok) {
                const data = await credRes.json();
                setCredentials(data.credentials || []);
            }

            // Fetch URL permissions
            const allowedRes = await fetch('/api/browser/urls/allowed', { headers });
            if (allowedRes.ok) {
                const data = await allowedRes.json();
                setAllowedUrls(data.urls || []);
            }

            const deniedRes = await fetch('/api/browser/urls/denied', { headers });
            if (deniedRes.ok) {
                const data = await deniedRes.json();
                setDeniedUrls(data.urls || []);
            }

            const requestsRes = await fetch('/api/browser/urls/requests', { headers });
            if (requestsRes.ok) {
                const data = await requestsRes.json();
                setUrlPermissionRequests(data.requests || []);
            }

            // Fetch system learning status
            const learnRes = await fetch('/api/browser/system-learning', { headers });
            if (learnRes.ok) {
                const data = await learnRes.json();
                setSystemLearning(data);
            }

            const tasksRes = await fetch('/api/browser/tasks', { headers });
            if (tasksRes.ok) {
                const data = await tasksRes.json();
                setTasks(data.tasks || []);
                
                // Fetch findings for each task
                data.tasks?.forEach(async (task: any) => {
                    const findRes = await fetch(`/api/browser/tasks/${task.id}/findings`, { headers });
                    if (findRes.ok) {
                        const findData = await findRes.json();
                        setFindingsMap(prev => ({ ...prev, [task.id]: findData.findings }));
                    }
                });
            }
        } catch (error) {
            console.error('Failed to fetch browser data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleStartStop = async () => {
        try {
            const token = getToken();
            const endpoint = status.isActive ? '/api/browser/surf/stop' : '/api/browser/surf/start';
            
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                setStatus(prev => ({ ...prev, isActive: !prev.isActive }));
                message.success(status.isActive ? 'Browser paused' : 'Browser started');
            } else {
                message.error('Failed to update browser status');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handlePauseForAuth = (activity: BrowserActivity) => {
        setPauseActivity(activity);
        setPauseModalVisible(true);
    };

    const handleSaveCredentials = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/credentials', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    website: pauseActivity?.url,
                    username: values.username,
                    password: values.password,
                    selectorUsername: values.selectorUsername,
                    selectorPassword: values.selectorPassword,
                    selectorSubmit: values.selectorSubmit
                })
            });

            if (response.ok) {
                message.success('Credentials saved successfully');
                credentialsForm.resetFields();
                setPauseModalVisible(false);
                fetchBrowserData();

                if (pauseActivity) {
                    const resumeResponse = await fetch('/api/browser/surf/resume', {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ activityId: pauseActivity.id })
                    });
                    if (resumeResponse.ok) {
                        setStatus(prev => ({ ...prev, pausedForAuth: false }));
                        message.success('Resuming browser activity');
                    }
                }
            } else {
                message.error('Failed to save credentials');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleSkipAuth = async () => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/surf/skip-auth', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ activityId: pauseActivity?.id })
            });

            if (response.ok) {
                message.info('Skipping authentication step');
                setPauseModalVisible(false);
                setStatus(prev => ({ ...prev, pausedForAuth: false }));
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    // URL Permission handlers
    const handleAddUrl = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch(`/api/browser/urls/${activeUrlTab}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    url: values.url,
                    pattern: values.pattern,
                    reason: values.reason
                })
            });

            if (response.ok) {
                message.success('URL added successfully');
                urlForm.resetFields();
                setUrlModalVisible(false);
                fetchBrowserData();
            } else {
                message.error('Failed to add URL');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleUpdateUrl = async (values: any) => {
        if (!editingUrl) return;
        try {
            const token = getToken();
            const response = await fetch(`/api/browser/urls/${editingUrl.id}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    url: values.url,
                    pattern: values.pattern,
                    reason: values.reason
                })
            });

            if (response.ok) {
                message.success('URL updated successfully');
                urlForm.resetFields();
                setUrlModalVisible(false);
                setEditingUrl(null);
                fetchBrowserData();
            } else {
                message.error('Failed to update URL');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleDeleteUrl = async (id: string) => {
        try {
            const token = getToken();
            const response = await fetch(`/api/browser/urls/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                message.success('URL deleted');
                fetchBrowserData();
            } else {
                message.error('Failed to delete URL');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleUrlPermissionDecision = async (requestId: string, approved: boolean) => {
        try {
            const token = getToken();
            const response = await fetch(`/api/browser/urls/requests/${requestId}/decision`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ approved })
            });

            if (response.ok) {
                message.success(`URL ${approved ? 'approved' : 'denied'}`);
                fetchBrowserData();
            } else {
                message.error('Failed to process decision');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleCreateTask = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/tasks', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(values)
            });

            if (response.ok) {
                message.success('New mission started');
                setTaskModalVisible(false);
                taskForm.resetFields();
                fetchBrowserData();
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const handleSimulateActivity = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/simulate-activity', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(values)
            });

            if (response.ok) {
                message.success('Activity simulated and learned');
                setSimulateModalVisible(false);
                simulateForm.resetFields();
                fetchBrowserData();
            } else {
                message.error('Failed to simulate activity');
            }
        } catch (error) {
            message.error(String(error));
        }
    };

    const columns = [
        {
            title: 'URL',
            dataIndex: 'url',
            key: 'url',
            render: (text: string, record: any) => (
                <div>
                    <a href={text} target="_blank" rel="noopener noreferrer" style={{ marginRight: 8 }}>
                        <LinkOutlined />
                    </a>
                    <Tooltip title={record.reasoning || text}>
                        <span>{text.length > 30 ? `${text.substring(0, 30)}...` : text}</span>
                    </Tooltip>
                    {record.reasoning && (
                        <div style={{ fontSize: '11px', color: '#8c8c8c', marginTop: '4px', fontStyle: 'italic' }}>
                            <ThunderboltOutlined style={{ color: '#faad14', marginRight: 4 }} />
                            {record.reasoning}
                        </div>
                    )}
                </div>
            ),
        },
        {
            title: 'Action',
            dataIndex: 'action',
            key: 'action',
            render: (action: string) => {
                const colors: Record<string, string> = {
                    surf: 'blue',
                    scrape: 'cyan',
                    login: 'orange',
                    search: 'green'
                };
                return <Tag color={colors[action] || 'default'}>{action.toUpperCase()}</Tag>;
            },
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const statusConfig: Record<string, { color: string; icon: React.ReactNode }> = {
                    navigating: { color: 'processing', icon: <GlobalOutlined spin /> },
                    loading: { color: 'processing', icon: <GlobalOutlined spin /> },
                    completed: { color: 'success', icon: <CheckCircleOutlined /> },
                    paused: { color: 'warning', icon: <PauseCircleOutlined /> },
                    error: { color: 'error', icon: <CloseCircleOutlined /> }
                };
                const config = statusConfig[status];
                return (
                    <Badge
                        status={config.color as any}
                        text={status.toUpperCase()}
                    />
                );
            },
        },
        {
            title: 'Auth Required',
            dataIndex: 'hasAuthRequired',
            key: 'hasAuthRequired',
            render: (hasAuth: boolean) => hasAuth ? <Tag color="orange">YES</Tag> : '-',
        },
        {
            title: 'Duration',
            dataIndex: 'duration',
            key: 'duration',
            render: (duration: number) => duration ? `${duration}ms` : '-',
        },
        {
            title: 'Timestamp',
            dataIndex: 'timestamp',
            key: 'timestamp',
            render: (time: string) => new Date(time).toLocaleTimeString(),
        },
        {
            title: 'Action',
            key: 'actionBtn',
            render: (_: any, record: BrowserActivity) => {
                if (record.hasAuthRequired && record.status === 'paused') {
                    return (
                        <Button
                            type="primary"
                            size="small"
                            onClick={() => handlePauseForAuth(record)}
                        >
                            Enter Credentials
                        </Button>
                    );
                }
                return null;
            },
        },
    ];

    const sessionDuration = status.startTime ? 
        Math.floor((Date.now() - new Date(status.startTime).getTime()) / 1000 / 60) : 0;

    const urlColumns = [
        {
            title: 'URL',
            dataIndex: 'url',
            key: 'url',
            render: (text: string) => (
                <Tooltip title={text}>
                    <span>{text.length > 40 ? `${text.substring(0, 40)}...` : text}</span>
                </Tooltip>
            ),
        },
        {
            title: 'Pattern',
            dataIndex: 'pattern',
            key: 'pattern',
        },
        {
            title: 'Reason',
            dataIndex: 'reason',
            key: 'reason',
        },
        {
            title: 'Added',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date: string) => new Date(date).toLocaleDateString(),
        },
        {
            title: 'Action',
            key: 'action',
            render: (_: any, record: UrlPermission) => (
                <Space>
                    <Button
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => {
                            setEditingUrl(record);
                            urlForm.setFieldsValue(record);
                            setUrlModalVisible(true);
                        }}
                    >
                        Edit
                    </Button>
                    <Popconfirm
                        title="Delete URL?"
                        description="This action cannot be undone."
                        onConfirm={() => handleDeleteUrl(record.id)}
                        okText="Yes"
                        cancelText="No"
                    >
                        <Button size="small" danger icon={<DeleteOutlined />}>
                            Delete
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '20px' }}>
            <Card
                title={
                    <Space>
                        <GlobalOutlined />
                        <span>Browser Activity Monitor</span>
                        {status.pausedForAuth && <Tag color="warning">PAUSED FOR AUTH</Tag>}
                    </Space>
                }
                bordered={false}
                extra={
                    <Space>
                        <span>Auto-Surf:</span>
                        <Switch checked={autoSurfEnabled} onChange={setAutoSurfEnabled} />
                        <span style={{ marginLeft: '20px' }}>Learning:</span>
                        <Switch 
                            checkedChildren={<BulbOutlined />} 
                            unCheckedChildren={<BulbOutlined />} 
                            checked={systemLearning.autoLearnEnabled} 
                            onChange={async (checked) => {
                                const token = getToken();
                                await fetch('/api/browser/system-learning/toggle', {
                                    method: 'POST',
                                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ enabled: checked })
                                });
                                setSystemLearning(prev => ({ ...prev, autoLearnEnabled: checked }));
                            }}
                        />
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={fetchBrowserData}
                            loading={loading}
                        >
                            Refresh
                        </Button>
                    </Space>
                }
            >
                <Alert
                    message="Browser Surfing Monitor - System Learning Enabled"
                    description="Monitor system internet browsing. URL permissions control where the system can surf. System learns from browsing patterns and updates knowledge base."
                    type="info"
                    showIcon
                    style={{ marginBottom: '20px' }}
                />

                <Row gutter={16} style={{ marginBottom: '20px' }}>
                    <Col xs={24} sm={12} md={6}>
                        <Statistic
                            title="Status"
                            value={status.isActive ? 'ACTIVE' : 'STOPPED'}
                            valueStyle={{ color: status.isActive ? '#52c41a' : '#f5222d' }}
                            prefix={<GlobalOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={12} md={6}>
                        <Statistic
                            title="Pages Visited"
                            value={status.pagesVisited}
                            prefix={<LinkOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={12} md={6}>
                        <Statistic
                            title="Session Duration"
                            value={`${sessionDuration} min`}
                            prefix={<ClockCircleOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={12} md={6}>
                        <Statistic
                            title="Knowledge Nodes"
                            value={systemLearning.knowledgeNodes}
                            prefix={<BulbOutlined />}
                            suffix={<Tag color="cyan" style={{ marginLeft: 8 }}>LEARNING</Tag>}
                        />
                        <Progress 
                            percent={Math.min(100, (systemLearning.knowledgeNodes / 500) * 100)} 
                            size="small" 
                            status="active"
                            strokeColor={{
                                '0%': '#108ee9',
                                '100%': '#87d068',
                            }}
                            showInfo={false}
                        />
                        <div style={{ fontSize: '10px', color: '#8c8c8c' }}>Intelligence Level: {Math.floor(systemLearning.knowledgeNodes / 100) + 1}</div>
                    </Col>
                </Row>

                <div style={{ background: '#f0f2f5', padding: '16px', borderRadius: '8px', marginBottom: '20px' }}>
                    <Typography.Title level={5}>
                        <BulbOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                        Live Intelligence Feed
                    </Typography.Title>
                    <Typography.Paragraph type="secondary">
                        The system is currently {systemLearning.autoLearnEnabled ? 'actively' : 'not'} learning from browsing patterns. 
                        Last knowledge sync: {new Date(systemLearning.lastSync).toLocaleTimeString()}.
                    </Typography.Paragraph>
                    <Space>
                        <Button type="primary" size="small" onClick={() => setTaskModalVisible(true)}>
                            New Autonomous Mission
                        </Button>
                        <Badge count={tasks.filter(t => t.status === 'active').length} overflowCount={9}>
                            <Tag color="blue">Active Missions</Tag>
                        </Badge>
                    </Space>
                </div>

                <Row gutter={16} style={{ marginBottom: '20px' }}>
                    <Col span={24}>
                        <Space>
                            <Button
                                type={status.isActive ? 'default' : 'primary'}
                                icon={status.isActive ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                                onClick={handleStartStop}
                                size="large"
                            >
                                {status.isActive ? 'Pause Browser' : 'Start Browsing'}
                            </Button>
                            {status.isActive && (
                                <Button
                                    danger
                                    icon={<StopOutlined />}
                                    onClick={async () => {
                                        const token = getToken();
                                        await fetch('/api/browser/surf/stop', {
                                            method: 'POST',
                                            headers: { 'Authorization': `Bearer ${token}` }
                                        });
                                        setStatus(prev => ({ ...prev, isActive: false }));
                                    }}
                                >
                                    Stop All Activity
                                </Button>
                            )}
                            <Button
                                icon={<BulbOutlined />}
                                onClick={() => setSimulateModalVisible(true)}
                                type="dashed"
                            >
                                Simulate AI Activity
                            </Button>
                        </Space>
                    </Col>
                </Row>

                <Divider>URL Permissions</Divider>
                
                <Tabs
                    activeKey={activeUrlTab}
                    onChange={(key) => setActiveUrlTab(key as any)}
                    items={[
                        {
                            key: 'allowed',
                            label: `Allowed URLs (${allowedUrls.length})`,
                            children: (
                                <>
                                    <Button
                                        type="primary"
                                        icon={<PlusOutlined />}
                                        onClick={() => {
                                            setEditingUrl(null);
                                            urlForm.resetFields();
                                            setUrlModalVisible(true);
                                        }}
                                        style={{ marginBottom: 16 }}
                                    >
                                        Add Allowed URL
                                    </Button>
                                    <Table
                                        columns={urlColumns}
                                        dataSource={allowedUrls}
                                        rowKey="id"
                                        size="small"
                                        pagination={{ pageSize: 10 }}
                                    />
                                </>
                            )
                        },
                        {
                            key: 'denied',
                            label: `Denied URLs (${deniedUrls.length})`,
                            children: (
                                <>
                                    <Button
                                        type="primary"
                                        icon={<PlusOutlined />}
                                        onClick={() => {
                                            setEditingUrl(null);
                                            urlForm.resetFields();
                                            setUrlModalVisible(true);
                                        }}
                                        style={{ marginBottom: 16 }}
                                    >
                                        Add Denied URL
                                    </Button>
                                    <Table
                                        columns={urlColumns}
                                        dataSource={deniedUrls}
                                        rowKey="id"
                                        size="small"
                                        pagination={{ pageSize: 10 }}
                                    />
                                </>
                            )
                        },
                        {
                            key: 'requests',
                            label: `Permission Requests (${urlPermissionRequests.filter(r => r.status === 'pending').length})`,
                            children: (
                                <Table
                                    columns={[
                                        {
                                            title: 'URL',
                                            dataIndex: 'url',
                                            key: 'url',
                                            render: (text: string) => (
                                                <Tooltip title={text}>
                                                    <span>{text.length > 40 ? `${text.substring(0, 40)}...` : text}</span>
                                                </Tooltip>
                                            ),
                                        },
                                        {
                                            title: 'Pattern',
                                            dataIndex: 'pattern',
                                            key: 'pattern',
                                        },
                                        {
                                            title: 'Status',
                                            dataIndex: 'status',
                                            key: 'status',
                                            render: (status: string) => (
                                                <Tag color={
                                                    status === 'pending' ? 'processing' :
                                                    status === 'approved' ? 'success' : 'error'
                                                }>
                                                    {status.toUpperCase()}
                                                </Tag>
                                            ),
                                        },
                                        {
                                            title: 'Requested',
                                            dataIndex: 'requestedAt',
                                            key: 'requestedAt',
                                            render: (date: string) => new Date(date).toLocaleString(),
                                        },
                                        {
                                            title: 'Action',
                                            key: 'action',
                                            render: (_: any, record: UrlPermissionRequest) => {
                                                if (record.status === 'pending') {
                                                    return (
                                                        <Space>
                                                            <Button
                                                                size="small"
                                                                type="primary"
                                                                icon={<CheckOutlined />}
                                                                onClick={() => handleUrlPermissionDecision(record.id, true)}
                                                            >
                                                                Approve
                                                            </Button>
                                                            <Tooltip title={record.reason || "No reasoning provided"}>
                                                                <Tag color={record.reason?.includes('Trust Score: 0.9') ? 'green' : 'orange'}>
                                                                    {record.reason?.includes('Trust Score: ') ? record.reason.split('Trust Score: ')[1] : 'Unverified'}
                                                                </Tag>
                                                            </Tooltip>
                                                            <Button
                                                                size="small"
                                                                danger
                                                                icon={<CloseOutlined />}
                                                                onClick={() => handleUrlPermissionDecision(record.id, false)}
                                                            >
                                                                Deny
                                                            </Button>
                                                        </Space>
                                                    );
                                                }
                                                return null;
                                            },
                                        },
                                    ]}
                                    dataSource={urlPermissionRequests}
                                    rowKey="id"
                                    size="small"
                                    pagination={{ pageSize: 10 }}
                                />
                            )
                        }
                    ]}
                />

                <Divider>Recent Activity</Divider>

                <Spin spinning={loading}>
                    <Table
                        columns={columns}
                        dataSource={activities}
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 10 }}
                    />
                </Spin>

                <Divider>Autonomous Missions</Divider>
                <Table
                    dataSource={tasks}
                    rowKey="id"
                    size="small"
                    expandable={{
                        expandedRowRender: (record) => (
                            <div style={{ padding: '8px 24px', background: '#fafafa' }}>
                                <Typography.Title level={5}>Findings for this Mission</Typography.Title>
                                <List
                                    dataSource={findingsMap[record.id] || []}
                                    renderItem={(item) => (
                                        <List.Item>
                                            <List.Item.Meta
                                                avatar={<Tag color="blue">{item.type}</Tag>}
                                                title={item.title}
                                                description={item.content}
                                            />
                                            <div style={{ textAlign: 'right' }}>
                                                <Badge status="success" text={`${(item.confidence * 100).toFixed(0)}% Match`} />
                                                <div style={{ fontSize: '10px' }}>{new Date(item.foundAt).toLocaleTimeString()}</div>
                                            </div>
                                        </List.Item>
                                    )}
                                    locale={{ emptyText: 'No findings extracted yet' }}
                                />
                            </div>
                        ),
                    }}
                    columns={[
                        {
                            title: 'Mission Goal',
                            dataIndex: 'goal',
                            key: 'goal',
                            render: (text: string) => <Typography.Text strong>{text}</Typography.Text>
                        },
                        {
                            title: 'Status',
                            dataIndex: 'status',
                            key: 'status',
                            render: (status: string) => (
                                <Tag color={status === 'active' ? 'processing' : 'success'}>
                                    {status.toUpperCase()}
                                </Tag>
                            )
                        },
                        {
                            title: 'Started',
                            dataIndex: 'startedAt',
                            key: 'startedAt',
                            render: (date: string) => new Date(date).toLocaleString()
                        },
                        {
                            title: 'Action',
                            key: 'action',
                            render: (_: any, record: any) => (
                                <Button size="small" onClick={async () => {
                                    const token = getToken();
                                    await fetch(`/api/browser/tasks/${record.id}`, {
                                        method: 'DELETE',
                                        headers: { 'Authorization': `Bearer ${token}` }
                                    });
                                    fetchBrowserData();
                                }}>Archive</Button>
                            )
                        }
                    ]}
                />

                <Divider>Stored Credentials</Divider>
                <Card type="inner" title={`Saved Credentials (${credentials.length})`}>
                    {credentials.length > 0 ? (
                        <List
                            dataSource={credentials}
                            renderItem={(cred) => (
                                <List.Item
                                    actions={[
                                        <Button size="small" danger onClick={async () => {
                                            const token = getToken();
                                            await fetch(`/api/browser/credentials/${cred.id}`, {
                                                method: 'DELETE',
                                                headers: { 'Authorization': `Bearer ${token}` }
                                            });
                                            fetchBrowserData();
                                        }}>
                                            Delete
                                        </Button>
                                    ]}
                                >
                                    <Typography.Text>{cred.website}</Typography.Text>
                                    <Typography.Text type="secondary">User: {cred.username}</Typography.Text>
                                </List.Item>
                            )}
                        />
                    ) : (
                        <Alert message="No stored credentials" type="info" showIcon />
                    )}
                </Card>
            </Card>

            <Modal
                title={
                    <Space>
                        <LockOutlined />
                        <span>Authentication Required</span>
                    </Space>
                }
                open={pauseModalVisible}
                onCancel={() => setPauseModalVisible(false)}
                footer={null}
                width={600}
            >
                {pauseActivity && (
                    <>
                        <Alert
                            message="Browser Paused"
                            description={`Authentication required at: ${pauseActivity.url}`}
                            type="warning"
                            showIcon
                            style={{ marginBottom: 16 }}
                        />
                        <Form
                            form={credentialsForm}
                            layout="vertical"
                            onFinish={handleSaveCredentials}
                        >
                            <Form.Item
                                label="Website"
                                name="website"
                                initialValue={pauseActivity.url}
                            >
                                <Input disabled />
                            </Form.Item>

                            <Form.Item
                                label="Username / Email"
                                name="username"
                                rules={[{ required: true, message: 'Please enter username' }]}
                            >
                                <Input 
                                    prefix={<UserOutlined />} 
                                    placeholder="Enter username or email" 
                                />
                            </Form.Item>

                            <Form.Item
                                label="Password"
                                name="password"
                                rules={[{ required: true, message: 'Please enter password' }]}
                            >
                                <Input.Password 
                                    prefix={<LockOutlined />} 
                                    placeholder="Enter password" 
                                />
                            </Form.Item>

                            <Divider>Login Form Selectors (Optional)</Divider>

                            <Form.Item
                                label="Username Input Selector"
                                name="selectorUsername"
                                tooltip="CSS selector for username field (e.g., #email, input[name='username'])"
                            >
                                <Input placeholder="e.g., #email" />
                            </Form.Item>

                            <Form.Item
                                label="Password Input Selector"
                                name="selectorPassword"
                                tooltip="CSS selector for password field (e.g., #password, input[type='password'])"
                            >
                                <Input placeholder="e.g., #password" />
                            </Form.Item>

                            <Form.Item
                                label="Submit Button Selector"
                                name="selectorSubmit"
                                tooltip="CSS selector for submit button (e.g., button[type='submit'])"
                            >
                                <Input placeholder="e.g., button[type='submit']" />
                            </Form.Item>

                            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
                                <Space>
                                    <Button onClick={handleSkipAuth}>
                                        Skip Authentication
                                    </Button>
                                    <Button onClick={() => setPauseModalVisible(false)}>
                                        Cancel
                                    </Button>
                                    <Button type="primary" htmlType="submit">
                                        Save & Resume
                                    </Button>
                                </Space>
                            </Form.Item>
                        </Form>
                    </>
                )}
            </Modal>

            <Modal
                title={editingUrl ? 'Edit URL' : `Add ${activeUrlTab === 'allowed' ? 'Allowed' : 'Denied'} URL`}
                open={urlModalVisible}
                onCancel={() => {
                    setUrlModalVisible(false);
                    setEditingUrl(null);
                    urlForm.resetFields();
                }}
                footer={null}
            >
                <Form
                    form={urlForm}
                    layout="vertical"
                    onFinish={editingUrl ? handleUpdateUrl : handleAddUrl}
                >
                    <Form.Item
                        label="URL"
                        name="url"
                        rules={[
                            { required: true, message: 'Please enter URL' },
                            { pattern: /^https?:\/\/.+/, message: 'Must start with http:// or https://' }
                        ]}
                    >
                        <Input placeholder="https://example.com" />
                    </Form.Item>

                    <Form.Item
                        label="Pattern (Optional)"
                        name="pattern"
                        tooltip="Regex pattern for matching similar URLs"
                    >
                        <Input placeholder="e.g., .*example\.com.*" />
                    </Form.Item>

                    <Form.Item
                        label="Reason"
                        name="reason"
                        rules={[{ required: true, message: 'Please enter reason' }]}
                    >
                        <Input.TextArea rows={2} placeholder="Why is this URL allowed/denied?" />
                    </Form.Item>

                    <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
                        <Space>
                            <Button onClick={() => {
                                setUrlModalVisible(false);
                                setEditingUrl(null);
                                urlForm.resetFields();
                            }}>
                                Cancel
                            </Button>
                            <Button type="primary" htmlType="submit">
                                {editingUrl ? 'Update' : 'Add'}
                            </Button>
                        </Space>
                    </Form.Item>
                    </Form>
                </Modal>

            <Modal
                title="Simulate AI Browser Activity"
                open={simulateModalVisible}
                onCancel={() => setSimulateModalVisible(false)}
                onOk={() => simulateForm.submit()}
                width={500}
            >
                <Alert 
                    message="Intelligence Simulation"
                    description="This simulates the AI agent browsing the web. It will generate a permission request if the URL is new and a system learning node if auto-learn is enabled."
                    type="info"
                    showIcon
                    style={{ marginBottom: 16 }}
                />
                <Form form={simulateForm} onFinish={handleSimulateActivity} layout="vertical">
                    <Form.Item name="url" label="URL" rules={[{ required: true }]} initialValue="https://en.wikipedia.org/wiki/Artificial_intelligence">
                        <Input placeholder="https://example.com" />
                    </Form.Item>
                    <Form.Item name="title" label="Page Title" rules={[{ required: true }]} initialValue="Artificial intelligence - Wikipedia">
                        <Input placeholder="Example Page Title" />
                    </Form.Item>
                    <Form.Item name="action" label="Action" initialValue="surf">
                        <Input placeholder="surf, scrape, login, or search" />
                    </Form.Item>
                    <Form.Item name="reasoning" label="AI Reasoning" initialValue="Confirming latest version requirements for project dependencies.">
                        <Input.TextArea rows={2} />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="New Autonomous Mission"
                open={taskModalVisible}
                onCancel={() => setTaskModalVisible(false)}
                onOk={() => taskForm.submit()}
            >
                <Form form={taskForm} onFinish={handleCreateTask} layout="vertical">
                    <Form.Item name="goal" label="Mission Goal" rules={[{ required: true }]}>
                        <Input.TextArea rows={3} placeholder="e.g., Research all competitors for the new payment gateway feature." />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default BrowserActivityDashboard;