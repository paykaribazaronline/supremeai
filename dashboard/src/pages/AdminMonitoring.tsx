// AdminMonitoring.tsx - Real-time System Monitoring

import React, { useState, useEffect, useMemo } from 'react';
import { Layout, Card, Row, Col, Statistic, Progress, Alert, Spin, Typography, Tag, Descriptions, Button, List, Badge, Space, notification } from 'antd';
import { 
  ThunderboltOutlined, 
  HddOutlined, 
  CloudServerOutlined, 
  DatabaseOutlined, 
  CheckCircleOutlined, 
  WarningOutlined,
  SyncOutlined,
  FileSearchOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';
import { useSystemWebSocket } from '../hooks/useSystemWebSocket';
import { useRole } from '../contexts/RoleContext';

const { Title, Text } = Typography;

interface ResourceMetrics {
  memoryUsed: number;
  memoryMax: number;
  cpuLoad: number;
  cpuUsagePercentage?: number;
  memoryUsagePercentage?: number;
  availableProcessors: number;
  dbActiveConnections?: number;
  dbIdleConnections?: number;
  redisStatus?: string;
  timestamp: number;
}

interface SystemLog {
  level: string;
  component: string;
  message: string;
  timestamp: number;
}

const AdminMonitoring: React.FC = () => {
  const { isGuest } = useRole();
  const [metrics, setMetrics] = useState<ResourceMetrics | null>(null);
  const [logs, setLogs] = useState<SystemLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // WebSocket Integration
  const { messages, connected } = useSystemWebSocket(['/topic/monitoring']);

  // Handle incoming WebSocket messages
  useEffect(() => {
    const monitoringData = messages['/topic/monitoring'];
    if (monitoringData) {
      if (monitoringData.type === 'SYSTEM_RESOURCES') {
        setMetrics(monitoringData as unknown as ResourceMetrics);
      } else if (monitoringData.type === 'SYSTEM_LOG') {
        const newLog = monitoringData as unknown as SystemLog;
        setLogs(prev => [newLog, ...prev].slice(0, 100));
        
        // Show notification for ALERTS
        if (newLog.level === 'ALERT' || newLog.level === 'ERROR') {
          notification[newLog.level === 'ALERT' ? 'warning' : 'error']({
            message: `System ${newLog.level}`,
            description: newLog.message,
            placement: 'topRight',
            duration: 5,
          });
        }
      }
    }
  }, [messages]);

  const fetchData = async () => {
    if (isGuest) return;
    setLoading(true);
    setError(null);
    try {
      // Fetch initial metrics
      const metricsResp = await authUtils.fetchWithAuth('/api/system/metrics/resources');
      if (metricsResp.ok) {
        const result = await metricsResp.json();
        setMetrics(result.data || null);
      }

      // Fetch log history
      const logsResp = await authUtils.fetchWithAuth('/api/admin/logs?limit=50');
      if (logsResp.ok) {
        const result = await logsResp.json();
        const logData = result.data?.logs || (Array.isArray(result.data) ? result.data : []);
        setLogs(logData);
      }
    } catch (err) {
      console.error('[AdminMonitoring] Fetch Error:', err);
      setError(err instanceof Error ? err.message : 'Failed to load system data');
    } finally {
      setLoading(false);
    }
  };

  const clearLogHistory = async () => {
    if (isGuest) return;
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/logs/clear', { method: 'DELETE' });
      if (response.ok) {
        setLogs([]);
        notification.success({ message: 'Logs Cleared', description: 'System logs history has been wiped.' });
      }
    } catch (err) {
      notification.error({ message: 'Error', description: 'Failed to clear logs.' });
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const systemStatus = useMemo(() => {
    if (!metrics) return { status: 'warning', text: 'Initializing...' };
    
    const cpuUsage = metrics.cpuUsagePercentage || (metrics.availableProcessors ? (metrics.cpuLoad / metrics.availableProcessors) * 100 : 0);
    const memoryUsage = metrics.memoryUsagePercentage || (metrics.memoryMax ? (metrics.memoryUsed / metrics.memoryMax) * 100 : 0);
    
    if (cpuUsage > 90 || memoryUsage > 90) return { status: 'critical', text: 'Critical' };
    if (cpuUsage > 75 || memoryUsage > 75) return { status: 'warning', text: 'Pressure' };
    return { status: 'healthy', text: 'Healthy' };
  }, [metrics]);

  return (
    <AdminLayout title="Real-Time System Monitoring">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={4} style={{ margin: 0, color: 'var(--text-primary)' }}>
          System Health & Resource Telemetry
        </Title>
        <Space>
          <Badge status={connected ? "processing" : "default"} text={connected ? "Live Connection" : "Disconnected"} style={{ color: 'var(--text-secondary)' }} />
          <Button icon={<SyncOutlined spin={loading} />} onClick={fetchData} size="small">Sync All</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        {/* System Status Card */}
        <Col xs={24} sm={12} md={6}>
          <Card hoverable className="glass-card">
            <Statistic
              title="Global Health"
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
          <Card hoverable className="glass-card">
            <Statistic
              title="Memory Usage"
              value={Math.round(metrics?.memoryUsagePercentage || (metrics && metrics.memoryMax > 0 ? (metrics.memoryUsed / metrics.memoryMax) * 100 : 0))}
              suffix="%"
              prefix={<HddOutlined />}
            />
            <Progress 
              percent={Math.round(metrics?.memoryUsagePercentage || (metrics && metrics.memoryMax > 0 ? (metrics.memoryUsed / metrics.memoryMax) * 100 : 0))} 
              status={metrics?.memoryUsagePercentage && metrics.memoryUsagePercentage > 90 ? 'exception' : 'active'}
              size="small"
              strokeColor={metrics?.memoryUsagePercentage && metrics.memoryUsagePercentage > 90 ? '#f5222d' : '#1890ff'}
            />
          </Card>
        </Col>

        {/* CPU */}
        <Col xs={24} sm={12} md={6}>
          <Card hoverable className="glass-card">
            <Statistic
              title="CPU Usage"
              value={Math.round(metrics?.cpuUsagePercentage || (metrics && metrics.availableProcessors > 0 ? (metrics.cpuLoad / metrics.availableProcessors) * 100 : 0))}
              suffix="%"
              prefix={<ThunderboltOutlined />}
            />
            <Progress 
              percent={Math.round(metrics?.cpuUsagePercentage || (metrics && metrics.availableProcessors > 0 ? (metrics.cpuLoad / metrics.availableProcessors) * 100 : 0))}
              size="small"
              strokeColor={metrics?.cpuUsagePercentage && metrics.cpuUsagePercentage > 80 ? '#faad14' : '#52c41a'}
            />
          </Card>
        </Col>

        {/* Database */}
        <Col xs={24} sm={12} md={6}>
          <Card hoverable className="glass-card">
            <Statistic
              title="DB Pool"
              value={metrics?.dbActiveConnections ?? '--'}
              suffix="active"
              prefix={<DatabaseOutlined />}
            />
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {metrics?.dbIdleConnections ?? 0} idle connections
            </Text>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card 
            title={<><FileSearchOutlined /> System Event Logs</>} 
            extra={<Button icon={<DeleteOutlined />} size="small" type="text" danger onClick={clearLogHistory}>Clear History</Button>}
            className="glass-card" 
            bodyStyle={{ padding: 0 }}
          >
            <div style={{ height: 450, overflowY: 'auto', padding: '12px 16px', background: 'rgba(0,0,0,0.2)', scrollBehavior: 'smooth' }}>
              {logs.length > 0 ? (
                <List
                  dataSource={logs}
                  renderItem={(item) => (
                    <List.Item style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', padding: '6px 0' }}>
                      <Space align="start">
                        <Text style={{ fontFamily: 'monospace', fontSize: '12px', color: 'rgba(255,255,255,0.45)', minWidth: 80 }}>
                          {new Date(item.timestamp).toLocaleTimeString()}
                        </Text>
                        <Tag 
                          color={
                            item.level === 'ERROR' ? 'red' : 
                            item.level === 'ALERT' ? 'magenta' : 
                            item.level === 'WARN' ? 'orange' : 
                            item.level === 'SUCCESS' ? 'green' : 'blue'
                          } 
                          style={{ fontSize: '10px', minWidth: 60, textAlign: 'center', borderRadius: 4 }}
                        >
                          {item.level}
                        </Tag>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <Text strong style={{ color: 'rgba(255,255,255,0.65)', fontSize: '11px', textTransform: 'uppercase' }}>
                            {item.component}
                          </Text>
                          <Text style={{ color: item.level === 'ALERT' ? '#ff4d4f' : 'rgba(255,255,255,0.85)', fontSize: '13px' }}>
                            {item.message}
                          </Text>
                        </div>
                      </Space>
                    </List.Item>
                  )}
                />
              ) : (
                <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'rgba(255,255,255,0.25)' }}>
                  {loading ? <Spin /> : 'No system events found.'}
                </div>
              )}
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="Infrastructure Details" className="glass-card">
            {metrics ? (
              <Descriptions column={1} size="small" bordered={false}>
                <Descriptions.Item label={<Text type="secondary">Redis Status</Text>}>
                  <Tag color={metrics.redisStatus === 'PONG' ? 'green' : 'red'} bordered={false}>
                    {metrics.redisStatus === 'PONG' ? 'ONLINE' : 'OFFLINE'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label={<Text type="secondary">Processors</Text>}>
                  <Text style={{ color: '#fff' }}>{metrics.availableProcessors ?? 'N/A'} Cores</Text>
                </Descriptions.Item>
                <Descriptions.Item label={<Text type="secondary">Heap Usage</Text>}>
                  <Text style={{ color: '#fff' }}>{((metrics.memoryUsed ?? 0) / 1024 / 1024).toFixed(1)} MB</Text>
                </Descriptions.Item>
                <Descriptions.Item label={<Text type="secondary">Total Limit</Text>}>
                  <Text style={{ color: 'rgba(255,255,255,0.45)' }}>{((metrics.memoryMax ?? 0) / 1024 / 1024).toFixed(1)} MB</Text>
                </Descriptions.Item>
                <Descriptions.Item label={<Text type="secondary">Uptime</Text>}>
                  <Badge status="success" text={<span style={{ color: '#fff' }}>Stable</span>} />
                </Descriptions.Item>
                <Descriptions.Item label={<Text type="secondary">Last Sync</Text>}>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {metrics.timestamp ? new Date(metrics.timestamp).toLocaleTimeString() : 'N/A'}
                  </Text>
                </Descriptions.Item>
              </Descriptions>
            ) : (
              <div style={{ padding: 20, textAlign: 'center' }}><Spin /></div>
            )}
          </Card>
          
          <Card style={{ marginTop: 16 }} className="glass-card bg-blue-900-opacity">
            <Title level={5} style={{ color: '#fff', fontSize: '16px' }}>Monitoring Intelligence</Title>
            <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: '13px' }}>
              The system is now tracking resource trends. Alerts are automatically triggered when CPU or Memory exceed 90% utilization. Logs are persisted to Firestore for historical analysis.
            </Text>
            <div style={{ marginTop: 12 }}>
              <Button type="primary" ghost size="small" block icon={<CloudServerOutlined />}>View Node Clusters</Button>
            </div>
          </Card>
        </Col>
      </Row>

      {error && (
        <Alert 
          type="error" 
          message="Monitoring Error" 
          description={error} 
          style={{ marginTop: 16 }} 
          showIcon
        />
      )}
    </AdminLayout>
  );
};

export default AdminMonitoring;
