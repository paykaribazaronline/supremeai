// SystemMetrics.tsx - Real-time System Performance Metrics

import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, LineChart, BarChart, Progress, Alert, Space } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, CpuOutlined, MemoryOutlined } from '@ant-design/icons';

interface SystemMetrics {
    cpuUsage: number;
    memoryUsage: number;
    apiLatency: number;
    successRate: number;
    errorRate: number;
    uptime: string;
}

const SystemMetrics: React.FC = () => {
    const [metrics, setMetrics] = useState<SystemMetrics>({
        cpuUsage: 0,
        memoryUsage: 0,
        apiLatency: 0,
        successRate: 100,
        errorRate: 0,
        uptime: '0d 0h 0m',
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchMetrics();
        const interval = setInterval(fetchMetrics, 5000); // Refresh every 5 seconds
        return () => clearInterval(interval);
    }, []);

    const fetchMetrics = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/system/metrics', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setMetrics(data);
            }
        } catch (error) {
            console.error('Failed to fetch metrics');
        }
    };

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}>
                        <Statistic
                            title="CPU Usage"
                            value={metrics.cpuUsage}
                            suffix="%"
                            prefix={<CpuOutlined />}
                            valueStyle={{ color: metrics.cpuUsage > 80 ? '#ff4d4f' : '#1890ff' }}
                        />
                        <div style={{ marginTop: '8px' }}>
                            <Progress percent={metrics.cpuUsage} showInfo={false} strokeColor={metrics.cpuUsage > 80 ? '#ff4d4f' : '#1890ff'} />
                        </div>
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}>
                        <Statistic
                            title="Memory Usage"
                            value={metrics.memoryUsage}
                            suffix="%"
                            prefix={<MemoryOutlined />}
                            valueStyle={{ color: metrics.memoryUsage > 85 ? '#ff4d4f' : '#1890ff' }}
                        />
                        <div style={{ marginTop: '8px' }}>
                            <Progress percent={metrics.memoryUsage} showInfo={false} strokeColor={metrics.memoryUsage > 85 ? '#ff4d4f' : '#1890ff'} />
                        </div>
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}>
                        <Statistic
                            title="API Latency"
                            value={metrics.apiLatency}
                            suffix=" ms"
                            valueStyle={{ color: metrics.apiLatency > 500 ? '#ff4d4f' : '#52c41a' }}
                        />
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card loading={loading}>
                        <Statistic
                            title="Uptime"
                            value={metrics.uptime}
                            prefix={<ArrowUpOutlined />}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
            </Row>

            <Row gutter={16}>
                <Col xs={24} sm={12} lg={12}>
                    <Card title="Success Rate">
                        <Statistic
                            value={metrics.successRate}
                            suffix="%"
                            valueStyle={{ color: metrics.successRate > 95 ? '#52c41a' : metrics.successRate > 90 ? '#faad14' : '#ff4d4f' }}
                        />
                        <Progress percent={metrics.successRate} showInfo={false} strokeColor={metrics.successRate > 95 ? '#52c41a' : metrics.successRate > 90 ? '#faad14' : '#ff4d4f'} />
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={12}>
                    <Card title="Error Rate">
                        <Statistic
                            value={metrics.errorRate}
                            suffix="%"
                            valueStyle={{ color: metrics.errorRate > 5 ? '#ff4d4f' : '#52c41a' }}
                        />
                        <Progress percent={metrics.errorRate} showInfo={false} strokeColor={metrics.errorRate > 5 ? '#ff4d4f' : '#52c41a'} />
                    </Card>
                </Col>
            </Row>

            {(metrics.cpuUsage > 90 || metrics.memoryUsage > 90 || metrics.apiLatency > 1000) && (
                <Alert
                    message="System Performance Warning"
                    description="One or more system metrics are above optimal thresholds. Consider optimizing resource usage."
                    type="warning"
                    showIcon
                    style={{ marginTop: '24px' }}
                />
            )}
        </div>
    );
};

export default SystemMetrics;
