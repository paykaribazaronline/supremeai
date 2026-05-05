// KingModePanel.tsx — Admin Override (King Mode) with WAIT/AUTO/FORCE_STOP
import React, { useState, useEffect } from 'react';
import { Card, Button, Tag, Space, Alert, List, Modal, Typography, Row, Col, Statistic, Switch, Badge, message, Popconfirm } from 'antd';
import { CrownOutlined, StopOutlined, PlayCircleOutlined, PauseCircleOutlined, CheckOutlined, CloseOutlined, ExclamationCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text, Paragraph } = Typography;

interface PendingAction {
    id: string;
    type: string;
    description: string;
    requestedBy: string;
    requestedAt: string;
    severity: string;
}

interface SystemStatus {
    mode: 'AUTO' | 'WAIT' | 'FORCE_STOP';
    isRunning: boolean;
    pendingCount: number;
    lastModeChange: string;
    uptime: string;
}

interface ErrorState {
    message: string;
    recoverable: boolean;
    action?: string;
}

const KingModePanel: React.FC = () => {
    const [status, setStatus] = useState<SystemStatus>({ mode: 'AUTO', isRunning: true, pendingCount: 0, lastModeChange: '', uptime: '' });
    const [pendingActions, setPendingActions] = useState<PendingAction[]>([]);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [error, setError] = useState<ErrorState | null>(null);

    const getHeaders = () => {
        const token = authUtils.getToken();
        return { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };
    };

    const handleError = (err: unknown, context: string): ErrorState => {
        if (err instanceof TypeError && err.message.includes('fetch')) {
            return {
                message: `Unable to connect to the server while ${context.toLowerCase()}. Please check your internet connection and try again.`,
                recoverable: true,
                action: 'Retry'
            };
        }
        if (err instanceof Error) {
            return {
                message: `${context} failed: ${err.message}`,
                recoverable: true,
                action: 'Retry'
            };
        }
        return {
            message: `An unexpected error occurred during ${context.toLowerCase()}. Please refresh the page and try again.`,
            recoverable: true,
            action: 'Refresh Page'
        };
    };

    const fetchStatus = async () => {
        setLoading(true);
        setError(null);
        try {
            const [statusRes, pendingRes] = await Promise.all([
                fetch('/api/admin/control', { headers: getHeaders() }),
                fetch('/api/admin/control/pending', { headers: getHeaders() }),
            ]);

            if (statusRes.ok) {
                const data = await statusRes.json();
                setStatus({
                    mode: data.mode || data.currentMode || 'AUTO',
                    isRunning: data.isRunning !== false,
                    pendingCount: data.pendingCount || 0,
                    lastModeChange: data.lastModeChange || data.lastChanged || '',
                    uptime: data.uptime || '',
                });
            } else if (statusRes.status === 401) {
                setError({ message: 'Your session has expired. Please log in again.', recoverable: false });
            } else if (statusRes.status === 403) {
                setError({ message: 'You do not have permission to access King Mode controls.', recoverable: false });
            }
            if (pendingRes.ok) {
                const data = await pendingRes.json();
                setPendingActions(Array.isArray(data) ? data : data.actions || []);
            }
        } catch (err) {
            const errorState = handleError(err, 'Fetching system status');
            setError(errorState);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchStatus(); const interval = setInterval(fetchStatus, 10000); return () => clearInterval(interval); }, []);

    const changeMode = async (newMode: string) => {
        setError(null);
        try {
            const res = await fetch('/api/admin/control/mode', { method: 'POST', headers: getHeaders(), body: JSON.stringify({ mode: newMode }) });
            if (res.ok) {
                message.success(`Mode changed to ${newMode}`);
                fetchStatus();
            } else if (res.status === 401) {
                setError({ message: 'Session expired. Please log in again to change modes.', recoverable: false });
            } else if (res.status === 503) {
                setError({ message: 'System is currently unavailable. Please try again in a few moments.', recoverable: true, action: 'Retry' });
            } else {
                setError({ message: `Failed to change mode to ${newMode}. The server returned an error.`, recoverable: true, action: 'Retry' });
            }
        } catch (err) {
            setError(handleError(err, 'Changing mode'));
        }
    };

    const forceStop = async () => {
        setError(null);
        try {
            const res = await fetch('/api/admin/control/stop', { method: 'POST', headers: getHeaders() });
            if (res.ok) {
                message.warning('FORCE STOP executed!');
                fetchStatus();
            } else if (res.status === 503) {
                setError({ message: 'System is unresponsive. Try refreshing the page or contacting support.', recoverable: true, action: 'Refresh' });
            } else {
                setError({ message: 'Failed to stop operations. Please try again.', recoverable: true, action: 'Retry' });
            }
        } catch (err) {
            setError(handleError(err, 'stopping operations'));
        }
    };

    const resumeOperations = async () => {
        setError(null);
        try {
            const res = await fetch('/api/admin/control/resume', { method: 'POST', headers: getHeaders() });
            if (res.ok) {
                message.success('Operations resumed!');
                fetchStatus();
            } else {
                setError({ message: 'Failed to resume operations. Please try again.', recoverable: true, action: 'Retry' });
            }
        } catch (err) {
            setError(handleError(err, 'resuming operations'));
        }
    };

    const handlePendingAction = async (actionId: string, decision: 'approve' | 'reject') => {
        setActionLoading(actionId);
        setError(null);
        try {
            const res = await fetch(`/api/admin/control/pending/${actionId}/${decision}`, { method: 'POST', headers: getHeaders() });
            if (res.ok) {
                message.success(`Action ${decision}d!`);
                fetchStatus();
            } else if (res.status === 404) {
                setError({ message: `This action no longer exists. It may have been handled by another admin.`, recoverable: true, action: 'Refresh' });
            } else {
                setError({ message: `Failed to ${decision} the action. Please try again.`, recoverable: true, action: 'Retry' });
            }
        } catch (err) {
            setError(handleError(err, `${decision}ing action`));
        }
        finally { setActionLoading(null); }
    };

    const modeColor = status.mode === 'AUTO' ? '#52c41a' : status.mode === 'WAIT' ? '#faad14' : '#ff4d4f';
    const modeIcon = status.mode === 'AUTO' ? <PlayCircleOutlined /> : status.mode === 'WAIT' ? <PauseCircleOutlined /> : <StopOutlined />;

    return (
        <div>
            <Alert message={<><CrownOutlined /> King Mode — Full Admin Override Control</>}
                description="You have complete authority over the AI system. Change modes, approve/reject actions, and force stop operations."
                type="info" showIcon style={{ marginBottom: 24 }} />

            {error && (
                <Alert
                    message="Error"
                    description={error.message}
                    type="error"
                    showIcon
                    closable
                    onClose={() => setError(null)}
                    action={
                        error.recoverable && error.action ? (
                            <Button size="small" onClick={error.action === 'Retry' ? fetchStatus : () => window.location.reload()}>
                                {error.action}
                            </Button>
                        ) : null
                    }
                    style={{ marginBottom: 24 }}
                />
            )}

            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}><Card><div style={{ textAlign: 'center' }}><div style={{ fontSize: 12, color: '#8c8c8c' }}>Current Mode</div><Tag color={modeColor} style={{ fontSize: 20, padding: '4px 16px', marginTop: 8 }}>{modeIcon} {status.mode}</Tag></div></Card></Col>
                <Col span={6}><Card><Statistic title="Pending Actions" value={status.pendingCount} valueStyle={{ color: status.pendingCount > 0 ? '#faad14' : '#52c41a' }} /></Card></Col>
                <Col span={6}><Card><Statistic title="System Status" value={status.isRunning ? 'RUNNING' : 'STOPPED'} valueStyle={{ color: status.isRunning ? '#52c41a' : '#ff4d4f' }} /></Card></Col>
                <Col span={6}><Card><div style={{ textAlign: 'center' }}><div style={{ fontSize: 12, color: '#8c8c8c' }}>Last Change</div><div style={{ fontSize: 14, marginTop: 8 }}>{status.lastModeChange ? new Date(status.lastModeChange).toLocaleString() : 'N/A'}</div></div></Card></Col>
            </Row>

            <Card title="Mode Control" style={{ marginBottom: 24 }} extra={<Button icon={<ReloadOutlined />} onClick={fetchStatus} loading={loading}>Refresh</Button>}>
                <Space size="large">
                    <Button type={status.mode === 'AUTO' ? 'primary' : 'default'} icon={<PlayCircleOutlined />} size="large"
                        onClick={() => changeMode('AUTO')} style={status.mode === 'AUTO' ? { background: '#52c41a', borderColor: '#52c41a' } : {}}>
                        AUTO — Run Freely
                    </Button>
                    <Button type={status.mode === 'WAIT' ? 'primary' : 'default'} icon={<PauseCircleOutlined />} size="large"
                        onClick={() => changeMode('WAIT')} style={status.mode === 'WAIT' ? { background: '#faad14', borderColor: '#faad14' } : {}}>
                        WAIT — Approve First
                    </Button>
                    <Popconfirm title="Force stop ALL operations?" onConfirm={forceStop} okText="STOP" okType="danger">
                        <Button danger icon={<StopOutlined />} size="large" type={status.mode === 'FORCE_STOP' ? 'primary' : 'default'}>
                            FORCE STOP
                        </Button>
                    </Popconfirm>
                    {!status.isRunning && (
                        <Button type="primary" icon={<PlayCircleOutlined />} size="large" onClick={resumeOperations} style={{ background: '#52c41a', borderColor: '#52c41a' }}>
                            Resume Operations
                        </Button>
                    )}
                </Space>
            </Card>

            <Card title={<>Pending Actions <Badge count={pendingActions.length} /></>}>
                {pendingActions.length === 0 ? (
                    <Alert message="No pending actions" type="success" showIcon />
                ) : (
                    <List dataSource={pendingActions} renderItem={(action) => (
                        <List.Item key={action.id} actions={[
                            <Button type="primary" icon={<CheckOutlined />} loading={actionLoading === action.id} onClick={() => handlePendingAction(action.id, 'approve')}>Approve</Button>,
                            <Button danger icon={<CloseOutlined />} loading={actionLoading === action.id} onClick={() => handlePendingAction(action.id, 'reject')}>Reject</Button>,
                        ]}>
                            <List.Item.Meta title={<><Tag color={action.severity === 'critical' ? 'red' : 'orange'}>{action.severity}</Tag> {action.type}</>}
                                description={<>{action.description}<br /><Text type="secondary">{action.requestedBy} — {new Date(action.requestedAt).toLocaleString()}</Text></>} />
                        </List.Item>
                    )} />
                )}
            </Card>
        </div>
    );
};

export default KingModePanel;
