// ImprovementTracking.tsx - Track AI-Generated Improvements and Changes

import React, { useState, useEffect } from 'react';
import { Card, Tabs, Table, Tag, Timeline, Button, Modal, Form, Input, Select, message, Row, Col, Empty } from 'antd';
import { CheckCircleOutlined, ClockCircleOutlined, RocketOutlined } from '@ant-design/icons';

interface Improvement {
    id: string;
    title: string;
    description: string;
    proposedBy: string;
    status: 'proposed' | 'in-progress' | 'completed' | 'rejected';
    category: string;
    estimatedImpact: number;
    completedDate?: string;
    createdDate: string;
}

const ImprovementTracking: React.FC = () => {
    const [improvements, setImprovements] = useState<Improvement[]>([]);
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();
    const [activeTab, setActiveTab] = useState('planned');

    useEffect(() => {
        fetchImprovements();
        const interval = setInterval(fetchImprovements, 30000);
        return () => clearInterval(interval);
    }, []);

    const fetchImprovements = async () => {
        setLoading(true);
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/improvements/list', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setImprovements(data);
            }
        } catch (error) {
            message.error('Failed to fetch improvements');
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        { title: 'Title', dataIndex: 'title', key: 'title', width: 150 },
        { title: 'Proposed By', dataIndex: 'proposedBy', key: 'proposedBy' },
        { title: 'Category', dataIndex: 'category', key: 'category' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const colorMap: any = {
                    proposed: 'blue',
                    'in-progress': 'orange',
                    completed: 'green',
                    rejected: 'red',
                };
                return <Tag color={colorMap[status]}>{status.toUpperCase()}</Tag>;
            },
        },
        {
            title: 'Impact',
            dataIndex: 'estimatedImpact',
            key: 'estimatedImpact',
            render: (impact: number) => `${impact}% improvement`,
        },
    ];

    const getFilteredImprovements = (status: string) => {
        return improvements.filter((i) => i.status === status);
    };

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {getFilteredImprovements('proposed').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>Proposed</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {getFilteredImprovements('in-progress').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>In Progress</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {getFilteredImprovements('completed').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>Completed</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {improvements.reduce((sum, i) => sum + i.estimatedImpact, 0) / improvements.length || 0}%
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>Avg. Impact</div>
                        </div>
                    </Card>
                </Col>
            </Row>

            <Card title="System Improvements Roadmap">
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={[
                        {
                            key: 'planned',
                            label: 'Proposed',
                            children: (
                                <div>
                                    {getFilteredImprovements('proposed').length > 0 ? (
                                        <Table
                                            columns={columns}
                                            dataSource={getFilteredImprovements('proposed')}
                                            loading={loading}
                                            rowKey="id"
                                            pagination={{ pageSize: 5 }}
                                        />
                                    ) : (
                                        <Empty description="No proposed improvements" />
                                    )}
                                </div>
                            ),
                        },
                        {
                            key: 'in-progress',
                            label: 'In Progress',
                            children: (
                                <div>
                                    {getFilteredImprovements('in-progress').length > 0 ? (
                                        <Table
                                            columns={columns}
                                            dataSource={getFilteredImprovements('in-progress')}
                                            loading={loading}
                                            rowKey="id"
                                            pagination={{ pageSize: 5 }}
                                        />
                                    ) : (
                                        <Empty description="No improvements in progress" />
                                    )}
                                </div>
                            ),
                        },
                        {
                            key: 'completed',
                            label: 'Completed',
                            children: (
                                <div>
                                    {getFilteredImprovements('completed').length > 0 ? (
                                        <Table
                                            columns={columns}
                                            dataSource={getFilteredImprovements('completed')}
                                            loading={loading}
                                            rowKey="id"
                                            pagination={{ pageSize: 5 }}
                                        />
                                    ) : (
                                        <Empty description="No completed improvements yet" />
                                    )}
                                </div>
                            ),
                        },
                    ]}
                />
            </Card>

            <Card title="Implementation Timeline" style={{ marginTop: '24px' }}>
                <Timeline>
                    {improvements
                        .filter((i) => i.status === 'completed')
                        .slice(0, 5)
                        .map((improvement) => (
                            <Timeline.Item
                                key={improvement.id}
                                dot={<CheckCircleOutlined style={{ color: '#52c41a', fontSize: '16px' }} />}
                            >
                                <div>
                                    <strong>{improvement.title}</strong>
                                    <br />
                                    <span style={{ fontSize: '12px', color: '#999' }}>
                                        Completed on {improvement.completedDate} | Impact: {improvement.estimatedImpact}%
                                    </span>
                                </div>
                            </Timeline.Item>
                        ))}
                </Timeline>
            </Card>
        </div>
    );
};

export default ImprovementTracking;
