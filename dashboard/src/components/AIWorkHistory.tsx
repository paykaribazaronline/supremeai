// AIWorkHistory.tsx - Track AI Decisions and Work History

import React, { useState, useEffect } from 'react';
import { Card, Table, Tabs, Tag, Timeline, Button, Space, message, Row, Col, Statistic } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';

interface Decision {
    id: string;
    agent: string;
    type: string;
    description: string;
    confidence: number;
    outcome: 'success' | 'failure' | 'pending';
    timestamp: string;
    impact: string;
}

interface WorkHistory {
    id: string;
    agent: string;
    taskName: string;
    startTime: string;
    endTime: string;
    duration: number;
    status: 'completed' | 'failed' | 'in-progress';
    outcome: string;
}

const AIWorkHistory: React.FC = () => {
    const [decisions, setDecisions] = useState<Decision[]>([]);
    const [workHistory, setWorkHistory] = useState<WorkHistory[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 60000); // Refresh every minute
        return () => clearInterval(interval);
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const [decisionsRes, historyRes] = await Promise.all([
                fetch('/api/decisions/history', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('/api/work-history', { headers: { 'Authorization': `Bearer ${token}` } }),
            ]);

            if (decisionsRes.ok) {
                const data = await decisionsRes.json();
                setDecisions(data);
            }
            if (historyRes.ok) {
                const data = await historyRes.json();
                setWorkHistory(data);
            }
        } catch (error) {
            message.error('Failed to fetch data');
        } finally {
            setLoading(false);
        }
    };

    const decisionColumns = [
        { title: 'AI Agent', dataIndex: 'agent', key: 'agent' },
        { title: 'Decision Type', dataIndex: 'type', key: 'type' },
        { title: 'Description', dataIndex: 'description', key: 'description', width: 200 },
        {
            title: 'Confidence',
            dataIndex: 'confidence',
            key: 'confidence',
            render: (conf: number) => `${conf}%`,
        },
        {
            title: 'Outcome',
            dataIndex: 'outcome',
            key: 'outcome',
            render: (outcome: string) => {
                const colorMap: any = { success: 'green', failure: 'red', pending: 'orange' };
                return <Tag color={colorMap[outcome]}>{outcome.toUpperCase()}</Tag>;
            },
        },
        { title: 'Timestamp', dataIndex: 'timestamp', key: 'timestamp' },
    ];

    const workColumns = [
        { title: 'AI Agent', dataIndex: 'agent', key: 'agent' },
        { title: 'Task', dataIndex: 'taskName', key: 'taskName' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const colorMap: any = { completed: 'green', failed: 'red', 'in-progress': 'orange' };
                return <Tag color={colorMap[status]}>{status.toUpperCase()}</Tag>;
            },
        },
        { title: 'Start Time', dataIndex: 'startTime', key: 'startTime' },
        { title: 'Duration', dataIndex: 'duration', key: 'duration', render: (d: number) => `${d}min` },
        { title: 'Outcome', dataIndex: 'outcome', key: 'outcome' },
    ];

    const successRate = workHistory.length > 0
        ? ((workHistory.filter((w) => w.status === 'completed').length / workHistory.length) * 100).toFixed(2)
        : 0;

    const avgConfidence = decisions.length > 0
        ? (decisions.reduce((sum, d) => sum + d.confidence, 0) / decisions.length).toFixed(2)
        : 0;

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <Statistic
                            title="Success Rate"
                            value={parseFloat(successRate as string)}
                            suffix="%"
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <Statistic
                            title="Avg. Confidence"
                            value={parseFloat(avgConfidence as string)}
                            suffix="%"
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <Statistic
                            title="Total Decisions"
                            value={decisions.length}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <Statistic
                            title="Tasks Completed"
                            value={workHistory.filter((w) => w.status === 'completed').length}
                        />
                    </Card>
                </Col>
            </Row>

            <Card title="AI Work and Decisions History">
                <Tabs
                    items={[
                        {
                            key: 'work',
                            label: 'Work History',
                            children: (
                                <Table
                                    columns={workColumns}
                                    dataSource={workHistory}
                                    loading={loading}
                                    rowKey="id"
                                    pagination={{ pageSize: 10 }}
                                />
                            ),
                        },
                        {
                            key: 'decisions',
                            label: 'Decision History',
                            children: (
                                <Table
                                    columns={decisionColumns}
                                    dataSource={decisions}
                                    loading={loading}
                                    rowKey="id"
                                    pagination={{ pageSize: 10 }}
                                />
                            ),
                        },
                        {
                            key: 'timeline',
                            label: 'Timeline View',
                            children: (
                                <Timeline>
                                    {decisions
                                        .slice(0, 10)
                                        .map((decision) => (
                                            <Timeline.Item
                                                key={decision.id}
                                                dot={
                                                    decision.outcome === 'success' ? (
                                                        <CheckCircleOutlined style={{ color: '#52c41a', fontSize: '16px' }} />
                                                    ) : decision.outcome === 'failure' ? (
                                                        <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: '16px' }} />
                                                    ) : null
                                                }
                                            >
                                                <div>
                                                    <strong>{decision.agent}</strong> - {decision.description}
                                                    <br />
                                                    <span style={{ fontSize: '12px', color: '#999' }}>
                                                        {decision.timestamp} | Confidence: {decision.confidence}% | {decision.impact}
                                                    </span>
                                                </div>
                                            </Timeline.Item>
                                        ))}
                                </Timeline>
                            ),
                        },
                    ]}
                />
            </Card>
        </div>
    );
};

export default AIWorkHistory;
