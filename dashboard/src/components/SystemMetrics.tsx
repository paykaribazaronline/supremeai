// SystemMetrics.tsx - Real-time System Performance Metrics

import React, { useState, useEffect, useRef } from 'react';
import { Card, Row, Col, Statistic, Progress, Alert, Space, Table, Badge } from 'antd';
import { ThunderboltOutlined, HddOutlined, DashboardOutlined, GlobalOutlined } from '@ant-design/icons';
import { 
  Chart as ChartJS, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement, 
  BarElement,
  Title, 
  Tooltip, 
  Legend, 
  Filler 
} from 'chart.js';
import { Line, Bar } from 'react-chartjs-2';
import { authUtils } from '../lib/authUtils';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface SystemMetrics {
    cpuUsage: number;
    memoryUsage: number;
    apiLatency: number;
    successRate: number;
    errorRate: number;
    uptime: string;
}

interface UserQuota {
    userId: string;
    displayName: string;
    email: string;
    usedQuota: number;
    totalQuota: number;
    usagePercentage: number;
}

interface SystemMetricsProps {
    stompClient?: any;
}

const SystemMetrics: React.FC<SystemMetricsProps> = ({ stompClient }) => {
    const [metrics, setMetrics] = useState<SystemMetrics>({
        cpuUsage: 0,
        memoryUsage: 0,
        apiLatency: 0,
        successRate: 100,
        errorRate: 0,
        uptime: '0d 0h 0m',
    });
    
    const [userQuotas, setUserQuotas] = useState<UserQuota[]>([]);
    const [history, setHistory] = useState<{ cpu: number[], memory: number[], labels: string[] }>({
        cpu: [],
        memory: [],
        labels: []
    });

    useEffect(() => {
        if (!stompClient) return;

        // Subscribe to metrics
        const metricsSub = stompClient.subscribe('/topic/metrics', (message: any) => {
            try {
                const data = JSON.parse(message.body);
                // Map backend names to frontend names if different
                const newMetrics = {
                    cpuUsage: data.cpuLoad || 0,
                    memoryUsage: (data.memoryUsed / data.memoryMax) * 100 || 0,
                    apiLatency: data.apiLatency || Math.floor(Math.random() * 50) + 10,
                    successRate: data.successRate || 99.5,
                    errorRate: data.errorRate || 0.5,
                    uptime: data.uptime || 'Running'
                };
                
                setMetrics(newMetrics);
                
                // Update history for chart
                setHistory(prev => {
                    const newLabels = [...prev.labels, new Date().toLocaleTimeString()];
                    const newCpu = [...prev.cpu, newMetrics.cpuUsage];
                    const newMemory = [...prev.memory, newMetrics.memoryUsage];
                    
                    // Keep only last 20 points
                    if (newLabels.length > 20) {
                        newLabels.shift();
                        newCpu.shift();
                        newMemory.shift();
                    }
                    
                    return { labels: newLabels, cpu: newCpu, memory: newMemory };
                });
            } catch (e) {
                console.error('Failed to parse metrics message', e);
            }
        });

        // Subscribe to quota
        const quotaSub = stompClient.subscribe('/topic/quota', (message: any) => {
            try {
                const data = JSON.parse(message.body);
                if (data.userQuotas) {
                    setUserQuotas(data.userQuotas);
                }
            } catch (e) {
                console.error('Failed to parse quota message', e);
            }
        });

        return () => {
            metricsSub.unsubscribe();
            quotaSub.unsubscribe();
        };
    }, [stompClient]);

    const cpuChartData = {
        labels: history.labels,
        datasets: [
            {
                label: 'CPU Usage (%)',
                data: history.cpu,
                borderColor: '#1890ff',
                backgroundColor: 'rgba(24, 144, 255, 0.1)',
                fill: true,
                tension: 0.4,
            },
            {
                label: 'Memory Usage (%)',
                data: history.memory,
                borderColor: '#722ed1',
                backgroundColor: 'rgba(114, 46, 209, 0.1)',
                fill: true,
                tension: 0.4,
            }
        ],
    };

    const quotaColumns = [
        { title: 'User', dataIndex: 'displayName', key: 'name', render: (text: string, record: any) => <div>{text || record.email}</div> },
        { 
            title: 'Usage', 
            dataIndex: 'usagePercentage', 
            key: 'usage',
            render: (percent: number) => (
                <div style={{ width: 150 }}>
                    <Progress percent={Math.round(percent)} size="small" status={percent > 90 ? 'exception' : 'active'} />
                </div>
            )
        },
        { title: 'Used', dataIndex: 'usedQuota', key: 'used' },
        { title: 'Limit', dataIndex: 'totalQuota', key: 'limit' },
    ];

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Row gutter={[16, 16]}>
                <Col xs={24} lg={16}>
                    <Card title="Real-time Resource Load" extra={<Badge status="processing" text="Live" />}>
                        <div style={{ height: 300 }}>
                            <Line 
                                data={cpuChartData} 
                                options={{ 
                                    responsive: true, 
                                    maintainAspectRatio: false,
                                    scales: { y: { min: 0, max: 100 } },
                                    plugins: { legend: { position: 'top' as const } }
                                }} 
                            />
                        </div>
                    </Card>
                </Col>
                <Col xs={24} lg={8}>
                    <Row gutter={[16, 16]}>
                        <Col span={24}>
                            <Card>
                                <Statistic
                                    title="Current CPU"
                                    value={metrics.cpuUsage.toFixed(1)}
                                    suffix="%"
                                    prefix={<ThunderboltOutlined />}
                                    valueStyle={{ color: metrics.cpuUsage > 80 ? '#cf1322' : '#3f8600' }}
                                />
                            </Card>
                        </Col>
                        <Col span={24}>
                            <Card>
                                <Statistic
                                    title="Memory Utilization"
                                    value={metrics.memoryUsage.toFixed(1)}
                                    suffix="%"
                                    prefix={<HddOutlined />}
                                    valueStyle={{ color: metrics.memoryUsage > 85 ? '#cf1322' : '#3f8600' }}
                                />
                            </Card>
                        </Col>
                    </Row>
                </Col>
            </Row>

            <Card title="Live User Quota Usage" icon={<GlobalOutlined />}>
                <Table 
                    dataSource={userQuotas} 
                    columns={quotaColumns} 
                    rowKey="userId" 
                    pagination={{ pageSize: 5 }}
                />
            </Card>
        </Space>
    );
};

export default SystemMetrics;
