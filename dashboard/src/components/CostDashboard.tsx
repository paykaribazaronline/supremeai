import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Progress, Table, Tag, Typography, Alert, Button, Space } from 'antd';
import { DollarOutlined, LineChartOutlined, ToolOutlined, WarningOutlined, ThunderboltOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

interface CostData {
    total_monthly_spend: number;
    currency: string;
    cloud_breakdown: {
        [key: string]: {
            compute_cost: number;
            storage_cost: number;
            network_cost: number;
            total: number;
            health_score: number;
        }
    };
    forecasting: {
        next_30_days: number;
        next_90_days: number;
    };
    anomalies_detected: string[];
}

interface Recommendation {
    type: string;
    description: string;
    monthly_savings: number;
    priority: string;
}

const CostDashboard: React.FC = () => {
    const [costData, setCostData] = useState<CostData | null>(null);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCostData();
        fetchRecommendations();
    }, []);

    const fetchCostData = async () => {
        try {
            const response = await fetch('/api/v1/agents/phase9/track-costs');
            if (response.ok) {
                const data = await response.json();
                setCostData(data);
            }
        } catch (error) {
            console.error('Failed to fetch cost data');
        } finally {
            setLoading(false);
        }
    };

    const fetchRecommendations = async () => {
        try {
            const response = await fetch('/api/v1/agents/phase9/optimize-resources', { method: 'POST', body: JSON.stringify({}) });
            if (response.ok) {
                const data = await response.json();
                setRecommendations(data.recommendations || []);
            }
        } catch (error) {
            console.error('Failed to fetch recommendations');
        }
    };

    const columns = [
        {
            title: 'Type',
            dataIndex: 'type',
            key: 'type',
            render: (text: string) => <Tag color="blue">{text}</Tag>,
        },
        {
            title: 'Recommendation',
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: 'Est. Savings',
            dataIndex: 'monthly_savings',
            key: 'monthly_savings',
            render: (val: number) => <Text type="success">${val.toFixed(2)}/mo</Text>,
        },
        {
            title: 'Priority',
            dataIndex: 'priority',
            key: 'priority',
            render: (priority: string) => (
                <Tag color={priority === 'CRITICAL' ? 'red' : priority === 'HIGH' ? 'orange' : 'green'}>
                    {priority}
                </Tag>
            ),
        },
    ];

    if (!costData) return <div>Loading Phase 9 Cost Intelligence...</div>;

    return (
        <div style={{ padding: '24px' }}>
            <Title level={2}>💎 Phase 9: Cost Intelligence Dashboard</Title>

            <Row gutter={[16, 16]}>
                <Col span={8}>
                    <Card>
                        <Statistic
                            title="Total Monthly Spend"
                            value={costData.total_monthly_spend}
                            precision={2}
                            prefix={<DollarOutlined />}
                            suffix={costData.currency}
                        />
                        <Text type="secondary">30-day forecast: ${costData.forecasting.next_30_days}</Text>
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic
                            title="Optimization Potential"
                            value={32.4}
                            precision={1}
                            suffix="%"
                            prefix={<ThunderboltOutlined />}
                            valueStyle={{ color: '#3f8600' }}
                        />
                        <Text type="secondary">Est. Savings: $425.50/mo</Text>
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Title level={5}>Budget Health</Title>
                        <Progress percent={82} status="active" strokeColor={{ '0%': '#108ee9', '100%': '#87d068' }} />
                        <Text type="secondary">Annual Limit: $63,500</Text>
                    </Card>
                </Col>
            </Row>

            <Row gutter={[16, 16]} style={{ marginTop: '24px' }}>
                <Col span={12}>
                    <Card title="Cloud Provider Breakdown">
                        {Object.entries(costData.cloud_breakdown).map(([cloud, metrics]) => (
                            <div key={cloud} style={{ marginBottom: '16px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <Text strong>{cloud}</Text>
                                    <Text>${metrics.total.toFixed(2)}</Text>
                                </div>
                                <Progress percent={metrics.health_score} size="small" />
                            </div>
                        ))}
                    </Card>
                </Col>
                <Col span={12}>
                    <Card title="Cost Anomalies & Alerts">
                        {costData.anomalies_detected.map((anomaly, idx) => (
                            <Alert
                                key={idx}
                                message={anomaly}
                                type="warning"
                                showIcon
                                icon={<WarningOutlined />}
                                style={{ marginBottom: '10px' }}
                            />
                        ))}
                        {costData.total_monthly_spend > 1400 && (
                            <Alert
                                message="Critical Budget Warning"
                                description="Monthly spend is trending 15% above Q4 projections."
                                type="error"
                                showIcon
                            />
                        )}
                    </Card>
                </Col>
            </Row>

            <Card title="Optimization Recommendations" style={{ marginTop: '24px' }}>
                <Table
                    dataSource={recommendations}
                    columns={columns}
                    pagination={false}
                    rowKey="description"
                />
                <div style={{ marginTop: '20px', textAlign: 'right' }}>
                    <Space>
                        <Button type="primary" icon={<ToolOutlined />}>Apply All Optimizations</Button>
                        <Button icon={<LineChartOutlined />}>View Detailed Forecast</Button>
                    </Space>
                </div>
            </Card>
        </div>
    );
};

export default CostDashboard;
