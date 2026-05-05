// SimulatorDashboard.tsx - App Simulator Management Panel
// Plan 22: Simulator Controller - React frontend

import React, { useState, useEffect, useCallback } from 'react';
import {
    Card, Button, Table, Tag, Space, Modal, Select, message,
    Popconfirm, Empty, Spin, Row, Col, Typography, Progress, Badge, Alert
} from 'antd';
import {
    PlayCircleOutlined, StopOutlined, DeleteOutlined, MobileOutlined,
    CloudUploadOutlined, ReloadOutlined, DesktopOutlined, CheckCircleOutlined,
    ExclamationCircleOutlined, LinkOutlined
} from '@ant-design/icons';

const { Text, Title } = Typography;
const { Option } = Select;

// ─── Types ───────────────────────────────────────────────────────────────────

interface InstalledApp {
    appId: string;
    appName: string;
    version: string;
    previewUrl: string;
    installedAt: string;
    launchCount: number;
    lastLaunchedAt: string | null;
    status: 'INSTALLED' | 'RUNNING' | 'PAUSED' | 'ERROR';
}

interface QuotaInfo {
    used: number;
    total: number;
}

interface SessionStatus {
    hasSession: boolean;
    sessionId?: string;
    activeAppId?: string;
    state?: string;
    lastHeartbeat?: string;
}

interface DeviceProfile {
    type: string;
    name: string;
    osVersion: string;
    screenResolution: string;
    densityDpi: number;
}

// ─── Config ──────────────────────────────────────────────────────────────────

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

async function apiFetch(path: string, options?: RequestInit) {
    const token = localStorage.getItem('authToken') || '';
    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
            ...(options?.headers || {}),
        },
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ error: res.statusText }));
        throw new Error(err.error || `HTTP ${res.status}`);
    }
    return res.json();
}

// ─── Status colors ───────────────────────────────────────────────────────────

const statusColor: Record<string, string> = {
    INSTALLED: 'blue',
    RUNNING: 'green',
    PAUSED: 'orange',
    ERROR: 'red',
};

// ─── Component ───────────────────────────────────────────────────────────────

const SimulatorDashboard: React.FC = () => {
    const [apps, setApps] = useState<InstalledApp[]>([]);
    const [quota, setQuota] = useState<QuotaInfo>({ used: 0, total: 5 });
    const [session, setSession] = useState<SessionStatus>({ hasSession: false });
    const [devices, setDevices] = useState<DeviceProfile[]>([]);
    const [loading, setLoading] = useState(false);
    const [sessionLoading, setSessionLoading] = useState(false);
    const [installModalOpen, setInstallModalOpen] = useState(false);
    const [installAppId, setInstallAppId] = useState('');
    const [selectedDevice, setSelectedDevice] = useState('PIXEL_6');
    const [previewOpen, setPreviewOpen] = useState(false);
    const [previewUrl, setPreviewUrl] = useState('');

    // ─── Data loaders ─────────────────────────────────────────────────────

    const loadApps = useCallback(async () => {
        setLoading(true);
        try {
            const data = await apiFetch('/api/simulator/installed');
            setApps(data.installedApps || []);
            setQuota(data.quota || { used: 0, total: 5 });
        } catch (e: any) {
            message.error('Failed to load installed apps: ' + e.message);
        } finally {
            setLoading(false);
        }
    }, []);

    const loadSession = useCallback(async () => {
        try {
            const data = await apiFetch('/api/simulator/session/status');
            setSession(data);
        } catch {
            setSession({ hasSession: false });
        }
    }, []);

    const loadDevices = useCallback(async () => {
        try {
            const data = await apiFetch('/api/simulator/devices');
            setDevices(data);
            if (data.length > 0) setSelectedDevice(data[0].type);
        } catch {
            // Use default devices if API fails
        }
    }, []);

    useEffect(() => {
        loadApps();
        loadSession();
        loadDevices();
    }, [loadApps, loadSession, loadDevices]);

    // ─── Actions ──────────────────────────────────────────────────────────

    const handleInstall = async () => {
        if (!installAppId.trim()) {
            message.warning('Please enter an App ID');
            return;
        }
        setLoading(true);
        try {
            const result = await apiFetch('/api/simulator/install', {
                method: 'POST',
                body: JSON.stringify({ appId: installAppId.trim(), deviceProfile: selectedDevice }),
            });
            message.success(`App "${result.app.appName}" installed successfully`);
            setInstallModalOpen(false);
            setInstallAppId('');
            loadApps();
        } catch (e: any) {
            message.error('Install failed: ' + e.message);
        } finally {
            setLoading(false);
        }
    };

    const handleUninstall = async (appId: string) => {
        setLoading(true);
        try {
            await apiFetch(`/api/simulator/install/${appId}`, { method: 'DELETE' });
            message.success('App uninstalled');
            loadApps();
            loadSession();
        } catch (e: any) {
            message.error('Uninstall failed: ' + e.message);
        } finally {
            setLoading(false);
        }
    };

    const handleStartSession = async (appId: string, url: string) => {
        setSessionLoading(true);
        try {
            await apiFetch(`/api/simulator/session/start?appId=${encodeURIComponent(appId)}`, {
                method: 'POST',
            });
            message.success('Simulator session started');
            setPreviewUrl(url);
            setPreviewOpen(true);
            loadSession();
            loadApps();
        } catch (e: any) {
            message.error('Failed to start session: ' + e.message);
        } finally {
            setSessionLoading(false);
        }
    };

    const handleStopSession = async () => {
        setSessionLoading(true);
        try {
            await apiFetch('/api/simulator/session/stop', { method: 'POST' });
            message.success('Session stopped');
            loadSession();
            loadApps();
        } catch (e: any) {
            message.error('Failed to stop session: ' + e.message);
        } finally {
            setSessionLoading(false);
        }
    };

    // ─── Table columns ────────────────────────────────────────────────────

    const columns = [
        {
            title: 'App',
            dataIndex: 'appName',
            key: 'appName',
            render: (name: string, record: InstalledApp) => (
                <Space>
                    <MobileOutlined />
                    <div>
                        <Text strong>{name}</Text>
                        <br />
                        <Text type="secondary" style={{ fontSize: 11 }}>
                            v{record.version} · {record.appId.substring(0, 8)}...
                        </Text>
                    </div>
                </Space>
            ),
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Tag color={statusColor[status] || 'default'}>{status}</Tag>
            ),
        },
        {
            title: 'Launches',
            dataIndex: 'launchCount',
            key: 'launchCount',
            render: (count: number) => <Text>{count}x</Text>,
        },
        {
            title: 'Installed',
            dataIndex: 'installedAt',
            key: 'installedAt',
            render: (date: string) => (
                <Text type="secondary">{new Date(date).toLocaleDateString()}</Text>
            ),
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: unknown, record: InstalledApp) => (
                <Space>
                    <Button
                        type="primary"
                        size="small"
                        icon={<PlayCircleOutlined />}
                        loading={sessionLoading}
                        disabled={session.hasSession && session.activeAppId !== record.appId}
                        onClick={() => {
                            if (session.hasSession && session.activeAppId === record.appId) {
                                setPreviewUrl(record.previewUrl);
                                setPreviewOpen(true);
                            } else {
                                handleStartSession(record.appId, record.previewUrl);
                            }
                        }}
                    >
                        {session.hasSession && session.activeAppId === record.appId ? 'View' : 'Launch'}
                    </Button>
                    <Button
                        size="small"
                        icon={<LinkOutlined />}
                        onClick={() => window.open(record.previewUrl, '_blank')}
                    >
                        Open
                    </Button>
                    <Popconfirm
                        title="Uninstall this app?"
                        onConfirm={() => handleUninstall(record.appId)}
                        okText="Yes"
                        cancelText="No"
                    >
                        <Button size="small" danger icon={<DeleteOutlined />}>
                            Remove
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    // ─── Render ───────────────────────────────────────────────────────────

    const quotaPercent = quota.total > 0 ? Math.round((quota.used / quota.total) * 100) : 0;
    const quotaStatus = quotaPercent >= 90 ? 'exception' : quotaPercent >= 70 ? 'normal' : 'success';

    return (
        <div style={{ padding: 24 }}>
            <Row gutter={[16, 16]} align="middle" style={{ marginBottom: 16 }}>
                <Col flex="auto">
                    <Title level={4} style={{ margin: 0 }}>
                        <DesktopOutlined /> App Simulator
                    </Title>
                    <Text type="secondary">
                        Preview and test your generated apps in a virtual device
                    </Text>
                </Col>
                <Col>
                    <Space>
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={() => { loadApps(); loadSession(); }}
                            loading={loading}
                        >
                            Refresh
                        </Button>
                        <Button
                            type="primary"
                            icon={<CloudUploadOutlined />}
                            onClick={() => setInstallModalOpen(true)}
                            disabled={quota.used >= quota.total}
                        >
                            Install App
                        </Button>
                    </Space>
                </Col>
            </Row>

            {/* Quota Bar */}
            <Card size="small" style={{ marginBottom: 16 }}>
                <Row gutter={16} align="middle">
                    <Col span={4}>
                        <Text strong>Install Quota</Text>
                    </Col>
                    <Col span={14}>
                        <Progress
                            percent={quotaPercent}
                            status={quotaStatus}
                            format={() => `${quota.used}/${quota.total}`}
                        />
                    </Col>
                    <Col span={6}>
                        {session.hasSession && (
                            <Space>
                                <Badge status="processing" text="Session active" />
                                <Button
                                    size="small"
                                    icon={<StopOutlined />}
                                    danger
                                    loading={sessionLoading}
                                    onClick={handleStopSession}
                                >
                                    Stop Session
                                </Button>
                            </Space>
                        )}
                    </Col>
                </Row>
            </Card>

            {/* Active Session Alert */}
            {session.hasSession && session.activeAppId && (
                <Alert
                    type="info"
                    icon={<CheckCircleOutlined />}
                    showIcon
                    message={`Simulator running: App ${session.activeAppId.substring(0, 8)}... — State: ${session.state}`}
                    style={{ marginBottom: 16 }}
                    action={
                        <Button size="small" onClick={() => { setPreviewUrl(''); setPreviewOpen(true); }}>
                            View
                        </Button>
                    }
                />
            )}

            {quota.used >= quota.total && (
                <Alert
                    type="warning"
                    icon={<ExclamationCircleOutlined />}
                    showIcon
                    message="Install quota reached. Uninstall an app to install a new one."
                    style={{ marginBottom: 16 }}
                />
            )}

            {/* Apps Table */}
            <Card>
                <Spin spinning={loading}>
                    {apps.length === 0 ? (
                        <Empty
                            image={<MobileOutlined style={{ fontSize: 48, color: '#ccc' }} />}
                            description={
                                <span>
                                    No apps installed.{' '}
                                    <Button type="link" onClick={() => setInstallModalOpen(true)}>
                                        Install your first app
                                    </Button>
                                </span>
                            }
                        />
                    ) : (
                        <Table
                            dataSource={apps}
                            columns={columns}
                            rowKey="appId"
                            pagination={{ pageSize: 10 }}
                            size="small"
                        />
                    )}
                </Spin>
            </Card>

            {/* Install Modal */}
            <Modal
                title="Install App to Simulator"
                open={installModalOpen}
                onOk={handleInstall}
                onCancel={() => { setInstallModalOpen(false); setInstallAppId(''); }}
                confirmLoading={loading}
                okText="Install"
            >
                <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                        <Text strong>App ID</Text>
                        <input
                            placeholder="Enter generated app ID (e.g. abc-123)"
                            value={installAppId}
                            onChange={e => setInstallAppId(e.target.value)}
                            style={{
                                width: '100%', padding: '8px', marginTop: 4,
                                border: '1px solid #d9d9d9', borderRadius: 6
                            }}
                        />
                    </div>
                    <div>
                        <Text strong>Device Profile</Text>
                        <Select
                            value={selectedDevice}
                            onChange={setSelectedDevice}
                            style={{ width: '100%', marginTop: 4 }}
                        >
                            {devices.length > 0 ? (
                                devices.map(d => (
                                    <Option key={d.type} value={d.type}>
                                        {d.name} ({d.osVersion}) — {d.screenResolution}
                                    </Option>
                                ))
                            ) : (
                                <>
                                    <Option value="PIXEL_6">Pixel 6 (Android 13)</Option>
                                    <Option value="PIXEL_7">Pixel 7 (Android 14)</Option>
                                    <Option value="SAMSUNG_S24">Samsung S24 (Android 14)</Option>
                                    <Option value="IPHONE_15">iPhone 15 (iOS 17)</Option>
                                    <Option value="TABLET_10">Tablet 10" (Android 13)</Option>
                                </>
                            )}
                        </Select>
                    </div>
                </Space>
            </Modal>

            {/* Preview iframe Modal */}
            <Modal
                title="App Simulator Preview"
                open={previewOpen}
                onCancel={() => setPreviewOpen(false)}
                footer={[
                    <Button key="open" icon={<LinkOutlined />} onClick={() => window.open(previewUrl, '_blank')}>
                        Open in New Tab
                    </Button>,
                    <Button key="stop" danger icon={<StopOutlined />} onClick={() => { handleStopSession(); setPreviewOpen(false); }}>
                        Stop Session
                    </Button>,
                    <Button key="close" onClick={() => setPreviewOpen(false)}>
                        Close
                    </Button>,
                ]}
                width={420}
            >
                {previewUrl ? (
                    <iframe
                        src={previewUrl}
                        style={{ width: '100%', height: 600, border: 'none', borderRadius: 8 }}
                        title="App Simulator"
                    />
                ) : (
                    <Empty description="No preview URL available" />
                )}
            </Modal>
        </div>
    );
};

export default SimulatorDashboard;
