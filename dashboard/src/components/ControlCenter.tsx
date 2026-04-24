import React, { useState, useEffect } from 'react';
import { Card, Switch, List, Typography, Space, Divider, message, Badge, Tooltip } from 'antd';
import { 
  RocketOutlined, 
  SafetyCertificateOutlined, 
  ThunderboltOutlined, 
  NodeIndexOutlined, 
  QuestionCircleOutlined,
  ApiOutlined
} from '@ant-design/icons';
import axios from 'axios';

const { Title, Text } = Typography;

interface Feature {
    id: string;
    key: string;
    label: string;
    description: string;
    enabled: boolean;
    category: 'performance' | 'intelligence' | 'security';
    icon: React.ReactNode;
}

const ControlCenter: React.FC = () => {
    const [loading, setLoading] = useState<boolean>(true);
    const [features, setFeatures] = useState<Feature[]>([
        {
            id: '1',
            key: 'redis_caching',
            label: 'Redis Caching (P15)',
            description: 'Enable in-memory caching to reduce DB load and improve response time.',
            enabled: true,
            category: 'performance',
            icon: <ThunderboltOutlined style={{ color: '#1890ff' }} />
        },
        {
            id: '2',
            key: 'connection_pooling',
            label: 'Connection Pooling (P16)',
            description: 'Optimize DB connection management using HikariCP.',
            enabled: true,
            category: 'performance',
            icon: <ApiOutlined style={{ color: '#52c41a' }} />
        },
        {
            id: '3',
            key: 'autonomous_questioning',
            label: 'Autonomous Questioning (S3)',
            description: 'AI will ask clarifying questions if user prompt is ambiguous.',
            enabled: true,
            category: 'intelligence',
            icon: <QuestionCircleOutlined style={{ color: '#722ed1' }} />
        },
        {
            id: '4',
            key: 'ai_voting_system',
            label: '10-AI Voting System (S4)',
            description: 'Multi-model consensus for high-quality code generation.',
            enabled: true,
            category: 'intelligence',
            icon: <NodeIndexOutlined style={{ color: '#eb2f96' }} />
        },
        {
            id: '5',
            key: 'auto_ranking',
            label: 'AI Auto-Ranking (S9)',
            description: 'Self-learning model selection based on historical performance.',
            enabled: false,
            category: 'intelligence',
            icon: <RocketOutlined style={{ color: '#fa8c16' }} />
        },
        {
            id: '6',
            key: 'realtime_updates',
            label: 'Real-time Updates (A9)',
            description: 'Broadcast system metrics and quota usage via WebSocket.',
            enabled: true,
            category: 'performance',
            icon: <SafetyCertificateOutlined style={{ color: '#13c2c2' }} />
        }
    ]);

    useEffect(() => {
        fetchFeatureStatus();
    }, []);

    const fetchFeatureStatus = async () => {
        try {
            const response = await axios.get('/api/admin/features');
            if (response.data && typeof response.data === 'object') {
                const updatedFeatures = features.map(f => ({
                    ...f,
                    enabled: response.data[f.key] !== undefined ? response.data[f.key] : f.enabled
                }));
                setFeatures(updatedFeatures);
            }
            setLoading(false);
        } catch (error) {
            console.error('Failed to fetch feature status', error);
            setLoading(false);
        }
    };

    const toggleFeature = async (key: string, checked: boolean) => {
        try {
            await axios.post(`/api/admin/features/toggle`, { key, enabled: checked });
            setFeatures(prev => prev.map(f => f.key === key ? { ...f, enabled: checked } : f));
            message.success(`${features.find(f => f.key === key)?.label} ${checked ? 'enabled' : 'disabled'}`);
        } catch (error) {
            message.error('Failed to update feature status');
        }
    };

    const renderFeatureList = (category: string) => {
        const filteredFeatures = features.filter(f => f.category === category);
        return (
            <List
                loading={loading}
                itemLayout="horizontal"
                dataSource={filteredFeatures}
                renderItem={item => (
                    <List.Item
                        actions={[
                            <Switch 
                                checked={item.enabled} 
                                onChange={(checked) => toggleFeature(item.key, checked)} 
                            />
                        ]}
                    >
                        <List.Item.Meta
                            avatar={
                                <div style={{ 
                                    width: 40, 
                                    height: 40, 
                                    borderRadius: 8, 
                                    background: '#f0f2f5', 
                                    display: 'flex', 
                                    alignItems: 'center', 
                                    justifyContent: 'center',
                                    fontSize: 20
                                }}>
                                    {item.icon}
                                </div>
                            }
                            title={<Text strong>{item.label}</Text>}
                            description={item.description}
                        />
                    </List.Item>
                )}
            />
        );
    };

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card title={<Space><ThunderboltOutlined /> System Performance Control</Space>}>
                {renderFeatureList('performance')}
            </Card>

            <Card title={<Space><NodeIndexOutlined /> AI Intelligence & Logic</Space>}>
                {renderFeatureList('intelligence')}
            </Card>
            
            <Card title="System Health Summary">
                <Space size="large" wrap>
                    <Badge status="processing" text="Backend: Online" />
                    <Badge status="processing" text="Redis: Connected" />
                    <Badge status="processing" text="Database: Optimized" />
                    <Badge status="success" text="WebSocket: Active" />
                    <Tooltip title="Currently processing multi-model requests">
                        <Badge status="warning" text="AI Load: Moderate" />
                    </Tooltip>
                </Space>
            </Card>
        </Space>
    );
};

export default ControlCenter;
