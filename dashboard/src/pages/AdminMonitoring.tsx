// AdminMonitoring.tsx - Real-time System Monitoring

import React, { useState, useEffect } from 'react';
import { Layout, Card, Row, Col, Statistic, Progress, Alert, Spin, Typography, Tag, Descriptions } from 'antd';
import { ThunderboltOutlined, HddOutlined, CloudServerOutlined, DatabaseOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface ResourceMetrics {
  memoryUsed: number;
  memoryMax: number;
  cpuLoad: number;
  availableProcessors: number;
  dbActiveConnections?: number;
  dbIdleConnections?: number;
  redisStatus?: string;
  timestamp: number;
}

const AdminMonitoring: React.FC = () => {
  const [metrics, setMetrics] = useState<ResourceMetrics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchMetrics = async () => {
    setError(null);
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/metrics/resources', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to fetch metrics');
      const data: ResourceMetrics = await response.json();
      setMetrics(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load metrics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    fetchMetrics();
    const interval = setInterval(fetchMetrics, 5000); // refresh every 5s
    return () => clearInterval(interval);
  }, []);

  const getSystemStatus = (): { status: 'healthy' | 'warning' | 'critical'; text: string } => {
    if (!metrics) return { status: 'warning', text: 'Unknown' };
    const memoryPercent = (metrics.memoryUsed / metrics.memoryMax) * 100;
    if (memoryPercent > 90 || metrics.cpuLoad > 80) return { status: 'critical', text: 'Critical' };
    if (memoryPercent > 75 || metrics.cpuLoad > 60) return { status: 'warning', text: 'Warning' };
    return { status: 'healthy', text: 'Healthy' };
  };

  const systemStatus = getSystemStatus();

  return (
    <AdminLayout title="System Monitoring">
      <Row gutter={[16, 16]}>
        {/* System Status Card */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="System Status"
              value={systemStatus.text}
              prefix={
                systemStatus.status === 'healthy' ? 
                <CheckCircleOutlined style={{ color: '#52c41a' }} /> : 
                <WarningOutlined style={{ color: systemStatus.status === 'critical' ? '#f5222d' : '#faad14' }} />
              }
              valueStyle={{ color: systemStatus.status === 'healthy' ? '#52c41a' : systemStatus.status === 'critical' ? '#f5222d' : '#faad14' }}
            />
          </Card>
        </Col>

        {/* Memory */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Memory Usage"
              value={metrics ? Math.round((metrics.memoryUsed / metrics.memoryMax) * 100) : 0}
              suffix="%"
              prefix={<HddOutlined />}
            />
            {metrics && (
              <Progress 
                percent={Math.round((metrics.memoryUsed / metrics.memoryMax) * 100)} 
                status={ (metrics.memoryUsed / metrics.memoryMax) * 100 > 90 ? 'exception' : 'active' }
                style={{ marginTop: 8 }}
              />
            )}
          </Card>
        </Col>

        {/* CPU */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="CPU Load"
              value={metrics ? metrics.cpuLoad.toFixed(1) : 0}
              suffix={metrics ? `/ ${metrics.availableProcessors} cores` : ''}
              prefix={<ThunderboltOutlined />}
            />
            <Progress 
              percent={metrics ? Math.min(metrics.cpuLoad / (metrics.availableProcessors || 1) * 100, 100) : 0}
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>

        {/* Database */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="DB Connections"
              value={metrics?.dbActiveConnections ?? '--'}
              prefix={<DatabaseOutlined />}
            />
            {metrics?.dbActiveConnections !== undefined && metrics?.dbIdleConnections !== undefined && (
              <Text type="secondary" style={{ fontSize: '12px' }}>
                {metrics.dbIdleConnections} idle
              </Text>
            )}
          </Card>
        </Col>

        {/* Redis */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Redis Status"
              value={metrics?.redisStatus || 'Unknown'}
              prefix={<CloudServerOutlined />}
              valueStyle={{
                color: metrics?.redisStatus === 'PONG' ? '#52c41a' : '#f5222d',
              }}
            />
          </Card>
        </Col>

        {/* Timestamp */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Last Updated"
              value={metrics ? new Date(metrics.timestamp).toLocaleTimeString() : 'Never'}
            />
          </Card>
        </Col>
      </Row>

      {error && <Alert type="error" message={error} style={{ marginTop: 16 }} />}
      {loading && <Spin size="large" style={{ display: 'block', margin: '20px auto' }} />}

      <Card title="Resource Details" style={{ marginTop: 24 }}>
        {metrics ? (
          <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }}>
            <Descriptions.Item label="Memory Used">
              {(metrics.memoryUsed / 1024 / 1024).toFixed(2)} MB
            </Descriptions.Item>
            <Descriptions.Item label="Memory Max">
              {(metrics.memoryMax / 1024 / 1024).toFixed(2)} MB
            </Descriptions.Item>
            <Descriptions.Item label="System Load">
              {metrics.cpuLoad.toFixed(2)}
            </Descriptions.Item>
            <Descriptions.Item label="Available Processors">
              {metrics.availableProcessors}
            </Descriptions.Item>
            {metrics.dbActiveConnections !== undefined && (
              <Descriptions.Item label="DB Active Connections">
                {metrics.dbActiveConnections}
              </Descriptions.Item>
            )}
            {metrics.dbIdleConnections !== undefined && (
              <Descriptions.Item label="DB Idle Connections">
                {metrics.dbIdleConnections}
              </Descriptions.Item>
            )}
            {metrics.redisStatus !== undefined && (
              <Descriptions.Item label="Redis Ping">
                {metrics.redisStatus}
              </Descriptions.Item>
            )}
          </Descriptions>
        ) : (
          <Spin />
        )}
      </Card>
    </AdminLayout>
  );
};

export default AdminMonitoring;
