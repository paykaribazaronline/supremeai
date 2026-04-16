import React, { useState, useEffect } from 'react';
import { Table, Tag, Space, Card, Statistic, Row, Col, Button, message } from 'antd';
import { RobotOutlined, PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface AIAgent {
    id: number;
    name: string;
    type: string;
    status: string;
    uptime: string;
}

const AIAgentsDashboard: React.FC = () => {
    const [agents, setAgents] = useState<AIAgent[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({ totalAgents: 0, activeAgents: 0, idleAgents: 0 });

    useEffect(() => {
        fetchAgents();
        fetchStats();
    }, []);

    const fetchAgents = async () => {
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/ai-agents', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await response.json();
            setAgents(data);
        } catch (error) {
            message.error('Failed to fetch AI agents');
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/ai-agents/stats', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await response.json();
            setStats(data);
        } catch (error) {
            console.error('Failed to fetch stats');
        }
    };

    const updateStatus = async (id: number, status: string) => {
        try {
            const token = authUtils.getToken();
            await fetch(`/api/ai-agents/${id}/status?status=${status}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            message.success(`Agent status updated to ${status}`);
            fetchAgents();
            fetchStats();
        } catch (error) {
            message.error('Failed to update status');
        }
    };

    const columns = [
        { title: 'ID', dataIndex: 'id', key: 'id' },
        { title: 'Name', dataIndex: 'name', key: 'name' },
        { title: 'Type', dataIndex: 'type', key: 'type' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Tag color={status === 'ACTIVE' ? 'green' : 'gold'}>{status}</Tag>
            )
        },
        { title: 'Uptime', dataIndex: 'uptime', key: 'uptime' },
        {
            title: 'Action',
            key: 'action',
            render: (_: any, record: AIAgent) => (
                <Space size="middle">
                    {record.status === 'ACTIVE' ? (
                        <Button
                            icon={<PauseCircleOutlined />}
                            onClick={() => updateStatus(record.id, 'IDLE')}
                        >
                            Deactivate
                        </Button>
                    ) : (
                        <Button
                            type="primary"
                            icon={<PlayCircleOutlined />}
                            onClick={() => updateStatus(record.id, 'ACTIVE')}
                        >
                            Activate
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col span={8}>
                    <Card>
                        <Statistic title="Total Agents" value={stats.totalAgents} prefix={<RobotOutlined />} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="Active" value={stats.activeAgents} valueStyle={{ color: '#3f8600' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="Idle" value={stats.idleAgents} valueStyle={{ color: '#cf1322' }} />
                    </Card>
                </Col>
            </Row>
            <Table columns={columns} dataSource={agents} loading={loading} rowKey="id" />
        </div>
    );
};

export default AIAgentsDashboard;
