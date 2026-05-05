// AuditLog.tsx — Audit trail, activity log, and security events
import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Space, Input, Select, DatePicker, Button, Empty, Row, Col, Statistic, Badge } from 'antd';
import { ReloadOutlined, SearchOutlined, FileProtectOutlined, WarningOutlined, CheckCircleOutlined } from '@ant-design/icons';

const { RangePicker } = DatePicker;

interface AuditEntry {
    id: string;
    action: string;
    user: string;
    category: string;
    severity: 'info' | 'warning' | 'critical';
    details: string;
    timestamp: string;
    ip?: string;
    outcome: 'success' | 'failure' | 'pending';
}

interface AuditStats {
    totalActions: number;
    criticalEvents: number;
    failedAttempts: number;
    activeUsers: number;
}

const AuditLog: React.FC = () => {
    const [entries, setEntries] = useState<AuditEntry[]>([]);
    const [stats, setStats] = useState<AuditStats>({ totalActions: 0, criticalEvents: 0, failedAttempts: 0, activeUsers: 0 });
    const [loading, setLoading] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [categoryFilter, setCategoryFilter] = useState<string>('');

    const fetchAuditLog = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const headers = { 'Authorization': `Bearer ${token}` };
            const [activityRes, summaryRes] = await Promise.all([
                fetch('/api/activity/summary', { headers }),
                fetch('/api/admin/control/history', { headers }),
            ]);

            if (activityRes.ok) {
                const data = await activityRes.json();
                const activityEntries = (data.recentActions || data.entries || data || []).map((item: any, idx: number) => ({
                    id: item.id || `act-${idx}`,
                    action: item.action || item.type || 'Unknown',
                    user: item.user || item.actor || 'system',
                    category: item.category || 'general',
                    severity: item.severity || 'info',
                    details: item.details || item.description || item.message || '',
                    timestamp: item.timestamp || new Date().toISOString(),
                    ip: item.ip,
                    outcome: item.outcome || item.status || 'success',
                }));
                setEntries(activityEntries);
            }

            if (summaryRes.ok) {
                const historyData = await summaryRes.json();
                const history = Array.isArray(historyData) ? historyData : [];
                setStats({
                    totalActions: entries.length + history.length,
                    criticalEvents: history.filter((h: any) => h.severity === 'critical').length,
                    failedAttempts: history.filter((h: any) => h.outcome === 'failure' || h.status === 'rejected').length,
                    activeUsers: new Set(history.map((h: any) => h.user || h.actor)).size || 1,
                });
            }
        } catch (error) {
            console.error('Failed to fetch audit log:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchAuditLog(); }, []);

    const filteredEntries = entries.filter((e) => {
        const matchesSearch = !searchText || e.action.toLowerCase().includes(searchText.toLowerCase()) || e.details.toLowerCase().includes(searchText.toLowerCase());
        const matchesCategory = !categoryFilter || e.category === categoryFilter;
        return matchesSearch && matchesCategory;
    });

    const columns = [
        {
            title: 'Time', dataIndex: 'timestamp', key: 'timestamp', width: 180,
            render: (t: string) => new Date(t).toLocaleString(),
            sorter: (a: AuditEntry, b: AuditEntry) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
            defaultSortOrder: 'descend' as const,
        },
        { title: 'User', dataIndex: 'user', key: 'user', width: 120 },
        { title: 'Action', dataIndex: 'action', key: 'action' },
        {
            title: 'Severity', dataIndex: 'severity', key: 'severity', width: 100,
            render: (s: string) => <Tag color={s === 'critical' ? 'red' : s === 'warning' ? 'orange' : 'blue'}>{s.toUpperCase()}</Tag>,
        },
        {
            title: 'Outcome', dataIndex: 'outcome', key: 'outcome', width: 100,
            render: (o: string) => <Tag color={o === 'success' ? 'green' : o === 'failure' ? 'red' : 'gold'}>{o}</Tag>,
        },
        { title: 'Details', dataIndex: 'details', key: 'details', ellipsis: true },
    ];

    const categories = [...new Set(entries.map((e) => e.category))];

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}><Card><Statistic title="Total Actions" value={stats.totalActions} prefix={<FileProtectOutlined />} /></Card></Col>
                <Col span={6}><Card><Statistic title="Critical Events" value={stats.criticalEvents} prefix={<WarningOutlined />} valueStyle={{ color: stats.criticalEvents > 0 ? '#ff4d4f' : '#52c41a' }} /></Card></Col>
                <Col span={6}><Card><Statistic title="Failed Attempts" value={stats.failedAttempts} valueStyle={{ color: stats.failedAttempts > 0 ? '#faad14' : '#52c41a' }} /></Card></Col>
                <Col span={6}><Card><Statistic title="Active Users" value={stats.activeUsers} prefix={<CheckCircleOutlined />} /></Card></Col>
            </Row>

            <Card title="Audit Trail" extra={<Button icon={<ReloadOutlined />} onClick={fetchAuditLog} loading={loading}>Refresh</Button>}>
                <Space style={{ marginBottom: 16 }}>
                    <Input prefix={<SearchOutlined />} placeholder="Search actions..." value={searchText} onChange={(e) => setSearchText(e.target.value)} style={{ width: 250 }} />
                    <Select placeholder="Category" value={categoryFilter || undefined} onChange={setCategoryFilter} allowClear style={{ width: 160 }}>
                        {categories.map((c) => <Select.Option key={c} value={c}>{c}</Select.Option>)}
                    </Select>
                </Space>
                <Table columns={columns} dataSource={filteredEntries} rowKey="id" loading={loading} pagination={{ pageSize: 20 }} size="small"
                    locale={{ emptyText: <Empty description="No audit entries yet" /> }} />
            </Card>
        </div>
    );
};

export default AuditLog;
