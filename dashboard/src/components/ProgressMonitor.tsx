// ProgressMonitor.tsx — Real-time work progress tracking
import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Progress, Table, Tag, Badge, Button, Space, Empty } from 'antd';
import { ReloadOutlined, RocketOutlined, CheckCircleOutlined, ClockCircleOutlined, SyncOutlined } from '@ant-design/icons';

interface TaskProgress {
    id: string;
    title: string;
    status: 'running' | 'completed' | 'failed' | 'queued';
    progress: number;
    agent: string;
    startedAt: string;
    completedAt?: string;
    duration?: string;
}

interface ProgressStats {
    runningTasks: number;
    completedToday: number;
    queuedTasks: number;
    overallProgress: number;
    successRate: number;
    avgDuration: string;
}

const ProgressMonitor: React.FC = () => {
    const [tasks, setTasks] = useState<TaskProgress[]>([]);
    const [stats, setStats] = useState<ProgressStats>({ runningTasks: 0, completedToday: 0, queuedTasks: 0, overallProgress: 0, successRate: 0, avgDuration: '0s' });
    const [loading, setLoading] = useState(false);

    const fetchProgress = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const headers = { 'Authorization': `Bearer ${token}` };
            const [metricsRes, statusRes, healthRes] = await Promise.all([
                fetch('/api/system/metrics', { headers }),
                fetch('/api/status/summary', { headers }),
                fetch('/api/status/performance', { headers }),
            ]);

            if (metricsRes.ok) {
                const metrics = await metricsRes.json();
                setStats(prev => ({
                    ...prev,
                    runningTasks: metrics.runningTasks ?? metrics.activeTasks ?? 0,
                    completedToday: metrics.completedTasks ?? metrics.completedToday ?? 0,
                    overallProgress: metrics.overallProgress ?? 0,
                    successRate: metrics.successRate ?? 0,
                }));
            }

            if (statusRes.ok) {
                const summary = await statusRes.json();
                const taskList = (summary.tasks || summary.recentTasks || summary.activeOperations || []).map((t: any, i: number) => ({
                    id: t.id || `t-${i}`,
                    title: t.title || t.name || t.description || 'Task',
                    status: t.status || 'running',
                    progress: t.progress ?? (t.status === 'completed' ? 100 : t.status === 'running' ? 50 : 0),
                    agent: t.agent || t.assignedTo || 'system',
                    startedAt: t.startedAt || t.timestamp || new Date().toISOString(),
                    completedAt: t.completedAt,
                    duration: t.duration,
                }));
                setTasks(taskList);
                setStats(prev => ({ ...prev, queuedTasks: taskList.filter((t: TaskProgress) => t.status === 'queued').length }));
            }

            if (healthRes.ok) {
                const perf = await healthRes.json();
                setStats(prev => ({ ...prev, avgDuration: perf.avgResponseTime || perf.avgDuration || '0ms' }));
            }
        } catch (error) {
            console.error('Failed to fetch progress:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchProgress(); const interval = setInterval(fetchProgress, 15000); return () => clearInterval(interval); }, []);

    const statusColors: Record<string, string> = { running: 'processing', completed: 'success', failed: 'error', queued: 'default' };
    const statusIcons: Record<string, React.ReactNode> = {
        running: <SyncOutlined spin />, completed: <CheckCircleOutlined />, failed: <ClockCircleOutlined />, queued: <ClockCircleOutlined />,
    };

    const columns = [
        { title: 'Task', dataIndex: 'title', key: 'title', render: (t: string) => <strong>{t}</strong> },
        {
            title: 'Status', dataIndex: 'status', key: 'status',
            render: (s: string) => <Badge status={statusColors[s] as any} text={<Tag color={s === 'completed' ? 'green' : s === 'running' ? 'blue' : s === 'failed' ? 'red' : 'default'}>{statusIcons[s]} {s.toUpperCase()}</Tag>} />,
        },
        {
            title: 'Progress', dataIndex: 'progress', key: 'progress', width: 200,
            render: (p: number, r: TaskProgress) => <Progress percent={p} size="small" status={r.status === 'failed' ? 'exception' : r.status === 'completed' ? 'success' : 'active'} />,
        },
        { title: 'Agent', dataIndex: 'agent', key: 'agent', render: (a: string) => <Tag>{a}</Tag> },
        { title: 'Started', dataIndex: 'startedAt', key: 'startedAt', render: (t: string) => new Date(t).toLocaleString(), width: 180 },
        { title: 'Duration', dataIndex: 'duration', key: 'duration', render: (d: string) => d || '—' },
    ];

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={4}><Card><Statistic title="Running" value={stats.runningTasks} prefix={<SyncOutlined spin />} valueStyle={{ color: '#1890ff' }} /></Card></Col>
                <Col span={4}><Card><Statistic title="Completed Today" value={stats.completedToday} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card></Col>
                <Col span={4}><Card><Statistic title="Queued" value={stats.queuedTasks} prefix={<ClockCircleOutlined />} /></Card></Col>
                <Col span={4}><Card><Statistic title="Success Rate" value={stats.successRate} suffix="%" valueStyle={{ color: stats.successRate >= 90 ? '#52c41a' : '#faad14' }} /></Card></Col>
                <Col span={4}><Card><div style={{ textAlign: 'center' }}><div style={{ fontSize: 12, color: '#8c8c8c' }}>Overall Progress</div><Progress type="circle" percent={stats.overallProgress} size={64} style={{ marginTop: 4 }} /></div></Card></Col>
                <Col span={4}><Card><Statistic title="Avg Duration" value={stats.avgDuration} /></Card></Col>
            </Row>

            <Card title={<><RocketOutlined /> Active Tasks</>} extra={<Button icon={<ReloadOutlined />} onClick={fetchProgress} loading={loading}>Refresh</Button>}>
                <Table columns={columns} dataSource={tasks} rowKey="id" loading={loading} pagination={{ pageSize: 15 }} size="small"
                    locale={{ emptyText: <Empty description="No active tasks. System is idle." /> }} />
            </Card>
        </div>
    );
};

export default ProgressMonitor;
