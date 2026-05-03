import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Button, Space, message, Badge, Typography, List, Tooltip } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, InfoCircleOutlined, RocketOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text } = Typography;

interface Requirement {
    id: string;
    title: string;
    description: string;
    status: 'pending' | 'approved' | 'rejected';
    priority: 'low' | 'medium' | 'high' | 'urgent';
    source: string;
    createdAt: string;
}

const RequirementsDashboard: React.FC = () => {
    const [requirements, setRequirements] = useState<Requirement[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchRequirements();
        const interval = setInterval(fetchRequirements, 10000);
        return () => clearInterval(interval);
    }, []);

    const fetchRequirements = async () => {
        setLoading(true);
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/admin/improvements/pending', {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                const data = await response.json();
                // Map improvement proposals to requirement interface
                const mapped = (data.pending || []).map((p: any) => ({
                    id: p.id,
                    title: p.title || 'Improvement Proposal',
                    description: p.description || p.details,
                    status: 'pending',
                    priority: p.priority || 'medium',
                    source: p.source || 'AI Analyzer',
                    createdAt: p.createdAt || new Date().toISOString()
                }));
                setRequirements(mapped);
            }
        } catch (error) {
            console.error('Failed to fetch requirements');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (id: string, action: 'approve' | 'reject') => {
        try {
            const token = authUtils.getToken();
            const response = await fetch(`/api/admin/improvements/${action}/${id}`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                message.success(`Requirement ${action}d successfully`);
                setRequirements(prev =>
                    prev.filter(r => r.id !== id)
                );
            }
        } catch (error) {
            message.error(`Failed to ${action} requirement`);
        }
    };

    const getPriorityColor = (p: string) => {
        switch (p) {
            case 'urgent': return 'red';
            case 'high': return 'orange';
            case 'medium': return 'blue';
            case 'low': return 'green';
            default: return 'default';
        }
    };

    return (
        <List
            loading={loading}
            dataSource={requirements}
            renderItem={item => (
                <List.Item
                    actions={[
                        item.status === 'pending' ? (
                            <Space key="actions">
                                <Button
                                    type="primary"
                                    size="small"
                                    icon={<CheckCircleOutlined />}
                                    onClick={() => handleAction(item.id, 'approve')}
                                    style={{ background: '#52c41a', borderColor: '#52c41a' }}
                                >
                                    Approve
                                </Button>
                                <Button
                                    danger
                                    size="small"
                                    icon={<CloseCircleOutlined />}
                                    onClick={() => handleAction(item.id, 'reject')}
                                >
                                    Reject
                                </Button>
                            </Space>
                        ) : (
                            <Tag color={item.status === 'approved' ? 'success' : 'error'}>
                                {item.status.toUpperCase()}
                            </Tag>
                        )
                    ]}
                >
                    <List.Item.Meta
                        title={
                            <Space>
                                <Text strong>{item.title}</Text>
                                <Tag color={getPriorityColor(item.priority)}>{item.priority.toUpperCase()}</Tag>
                                <Tag icon={<RocketOutlined />}>{item.source}</Tag>
                            </Space>
                        }
                        description={
                            <div>
                                <Text type="secondary">{item.description}</Text>
                                <div style={{ fontSize: '12px', marginTop: '4px' }}>
                                    Created: {new Date(item.createdAt).toLocaleString()}
                                </div>
                            </div>
                        }
                    />
                </List.Item>
            )}
        />
    );
};

export default RequirementsDashboard;
