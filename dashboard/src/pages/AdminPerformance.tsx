import React, { useState, useEffect } from 'react';
import { 
  Layout, Card, Row, Col, Statistic, Progress, Spin, Alert, Typography, 
  Table, Tag, Space, Select, DatePicker, Button, Descriptions, Timeline, message
} from 'antd';
import { 
  ClusterOutlined, ThunderboltOutlined, TrophyOutlined, 
  RobotOutlined, CheckCircleOutlined, WarningOutlined, 
  ExclamationCircleOutlined, ReloadOutlined, DatabaseOutlined,
  RiseOutlined, FallOutlined
} from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// Types
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

interface ProviderPerformance {
  name: string;
  successRate: number;
  averageLatency: number;
  requestCount: number;
  score: number;
  status: 'excellent' | 'good' | 'fair' | 'poor';
  avgResponseTime90?: number;
  errorRate?: number;
}

interface AgentStats {
  totalAgents: number;
  activeAgents: number;
  idleAgents: number;
  failedAgents: number;
  avgTaskCompletionTime: number;
  totalTasksProcessed: number;
  successRate: number;
}

interface SelfHealingStatus {
  enabled: boolean;
  lastCheck: string;
  totalHealingEvents: number;
  successfulHealingEvents: number;
  currentHealthStatus: 'healthy' | 'degraded' | 'critical';
}

interface PerformanceHistoryEntry {
  timestamp: number;
  cpuLoad: number;
  memoryUsage: number;
  activeConnections: number;
  responseTimeAvg: number;
}

const AdminPerformance: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Data states
  const [metrics, setMetrics] = useState<ResourceMetrics | null>(null);
  const [providers, setProviders] = useState<ProviderPerformance[]>([]);
  const [agentStats, setAgentStats] = useState<AgentStats | null>(null);
  const [selfHealing, setSelfHealing] = useState<SelfHealingStatus | null>(null);
  const [history, setHistory] = useState<PerformanceHistoryEntry[]>([]);
  
  // Filters
  const [timeRange, setTimeRange] = useState<'1h' | '6h' | '24h' | '7d'>('24h');
  const [providerFilter, setProviderFilter] = useState<string>('all');

  const fetchAllData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [
        metricsRes,
        rankingsRes,
        agentsRes,
        healingRes,
        historyRes
      ] = await Promise.all([
        authUtils.fetchWithAuth('/api/system/metrics/resources'),
        authUtils.fetchWithAuth('/api/admin/providers/rankings'),
        authUtils.fetchWithAuth('/api/ai-agents/stats'),
        authUtils.fetchWithAuth('/api/self-healing/status'),
        authUtils.fetchWithAuth('/api/self-healing/history?limit=50')
      ]);

      // Handle metrics
      if (metricsRes.ok) {
        const data = await metricsRes.json();
        setMetrics(data.data || null);
      }

      // Handle provider rankings
      if (rankingsRes.ok) {
        const data = await rankingsRes.json();
        const list = data.data?.rankings || data.rankings || {};
        const formatted = Object.entries(list).map(([name, d]: [string, any]) => ({
          name,
          successRate: (d.successRate || 0) * 100,
          averageLatency: d.averageLatency || 0,
          requestCount: d.totalTasks || 0,
          score: Math.round((d.successRate || 0) * 100),
 status: ((d.successRate || 0) > 0.9 ? 'excellent' :
                    (d.successRate || 0) > 0.7 ? 'good' :
                    (d.successRate || 0) > 0.5 ? 'fair' : 'poor') as 'excellent' | 'good' | 'fair' | 'poor',
          avgResponseTime90: d.avgResponseTime90 || 0,
          errorRate: (1 - (d.successRate || 0)) * 100
        })).sort((a, b) => b.score - a.score);
        setProviders(formatted);
      }

      // Handle agent stats
      if (agentsRes.ok) {
        const data = await agentsRes.json();
        setAgentStats(data.data || data);
      }

      // Handle self-healing
      if (healingRes.ok) {
        const data = await healingRes.json();
        setSelfHealing(data.data || data);
      }

      // Handle history
      if (historyRes.ok) {
        const data = await historyRes.json();
        const historyList = data.data?.history || data.history || [];
        // Filter by time range
        const cutoff = Date.now() - getTimeOffset(timeRange);
        const filtered = historyList.filter((h: any) => h.timestamp > cutoff);
        setHistory(filtered);
      }

      message.success('পারফরম্যান্স মেট্রিক্স রিফ্রেশ হয়েছে');

    } catch (err) {
      console.error('Performance fetch error:', err);
      setError('পারফরম্যান্স ডাটা লোড করতে সমস্যা হয়েছে।');
      message.error('পারফরম্যান্স ডাটা লোড ব্যর্থ হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  const getTimeOffset = (range: string): number => {
    switch (range) {
      case '1h': return 60 * 60 * 1000;
      case '6h': return 6 * 60 * 60 * 1000;
      case '24h': return 24 * 60 * 60 * 1000;
      case '7d': return 7 * 24 * 60 * 60 * 1000;
      default: return 24 * 60 * 60 * 1000;
    }
  };

  useEffect(() => {
    fetchAllData();
    const interval = setInterval(fetchAllData, 10000); // Refresh every 10s
    return () => clearInterval(interval);
  }, [timeRange]);

  const getSystemHealthColor = (): string => {
    if (!metrics) return '#8c8c8c';
    const memoryPercent = (metrics.memoryUsed / metrics.memoryMax) * 100;
    const cpuLoad = metrics.cpuLoad || 0;
    if (memoryPercent > 90 || cpuLoad > 80) return '#f5222d';
    if (memoryPercent > 75 || cpuLoad > 60) return '#faad14';
    return '#52c41a';
  };

  const calculateAvgLatency = (): number => {
    if (!providers.length) return 0;
    const total = providers.reduce((sum, p) => sum + p.averageLatency, 0);
    return Math.round(total / providers.length);
  };

  const calculateOverallSuccessRate = (): number => {
    if (!providers.length) return 0;
    const totalSuccess = providers.reduce((sum, p) => sum + (p.successRate * p.requestCount / 100), 0);
    const totalRequests = providers.reduce((sum, p) => sum + p.requestCount, 0);
    return totalRequests > 0 ? Math.round((totalSuccess / totalRequests) * 100) : 0;
  };

  // Provider table columns
  const providerColumns = [
    {
      title: 'Provider',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <Text strong style={{ color: '#fff' }}>{text}</Text>
    },
    {
      title: 'Score',
      dataIndex: 'score',
      key: 'score',
      render: (score: number) => (
        <Progress 
          percent={score} 
          size="small" 
          strokeColor={score > 90 ? '#10b981' : score > 70 ? '#3b82f6' : '#f59e0b'}
        />
      )
    },
    {
      title: 'Latency',
      dataIndex: 'averageLatency',
      key: 'averageLatency',
      render: (val: number) => `${val}ms`
    },
    {
      title: 'P95 Latency',
      dataIndex: 'avgResponseTime90',
      key: 'avgResponseTime90',
      render: (val: number) => val ? `${val}ms` : 'N/A'
    },
    {
      title: 'Error Rate',
      dataIndex: 'errorRate',
      key: 'errorRate',
      render: (val: number) => (
        <Text style={{ color: val > 5 ? '#f5222d' : val > 2 ? '#faad14' : '#52c41a' }}>
          {val.toFixed(1)}%
        </Text>
      )
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colors: Record<string, string> = {
          excellent: 'green',
          good: 'blue',
          fair: 'orange',
          poor: 'red'
        };
        return <Tag color={colors[status]}>{status.toUpperCase()}</Tag>;
      }
    }
  ];

  // Performance trend columns
  const historyColumns = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (ts: number) => dayjs(ts).format('HH:mm:ss')
    },
    {
      title: 'CPU Load',
      dataIndex: 'cpuLoad',
      key: 'cpuLoad',
      render: (val: number) => `${val.toFixed(1)}`
    },
    {
      title: 'Memory',
      dataIndex: 'memoryUsage',
      key: 'memoryUsage',
      render: (val: number) => `${(val / 1024 / 1024).toFixed(0)} MB`
    },
    {
      title: 'DB Conn',
      dataIndex: 'activeConnections',
      key: 'activeConnections',
      render: (val: number) => val
    },
    {
      title: 'Avg Response',
      dataIndex: 'responseTimeAvg',
      key: 'responseTimeAvg',
      render: (val: number) => `${val}ms`
    }
  ];

  if (loading && !metrics) {
    return (
      <AdminLayout title="Performance Dashboard">
        <div style={{ textAlign: 'center', padding: '100px 0' }}>
          <Spin size="large" tip="পারফরম্যান্স ডাটা লোড হচ্ছে..." />
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout title="Performance Dashboard">
      <div className="admin-page">
        {/* Header with filters */}
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'flex-end', 
          marginBottom: 24,
          flexWrap: 'wrap',
          gap: 12
        }}>
          <div>
            <Title level={2} style={{ color: '#fff', margin: 0 }}>
              <ClusterOutlined /> পারফরম্যান্স ড্যাশবোর্ড
            </Title>
            <Text type="secondary">
              রিয়েল-টাইম সিস্টেম পারফরম্যান্স, প্রোভাইডার স্ট্যাটস, ও এজেন্ট হেলথ
            </Text>
          </div>
          <Space>
            <Select
              value={timeRange}
              onChange={setTimeRange}
              style={{ width: 120 }}
              options={[
                { value: '1h', label: '১ ঘন্টা' },
                { value: '6h', label: '৬ ঘন্টা' },
                { value: '24h', label: '২৪ ঘন্টা' },
                { value: '7d', label: '৭ দিন' }
              ]}
            />
            <Select
              value={providerFilter}
              onChange={setProviderFilter}
              style={{ width: 150 }}
              allowClear
              placeholder="ফিল্টার প্রোভাইডার"
              options={providers.map(p => ({ value: p.name, label: p.name }))}
            />
            <Button 
              icon={<ReloadOutlined />} 
              onClick={fetchAllData}
              loading={loading}
            >
              রিফ্রেশ
            </Button>
          </Space>
        </div>

        {error && (
          <Alert 
            message="Error" 
            description={error} 
            type="error" 
            showIcon 
            style={{ marginBottom: 24 }}
            action={
              <Button size="small" onClick={fetchAllData}>
                Retry
              </Button>
            }
          />
        )}

        {/* Quick Stats Row */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={6}>
            <Card className="glass-card" bordered={false}>
              <Statistic
                title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>সিস্টেম হেলথ</Text>}
                value={selfHealing?.currentHealthStatus === 'healthy' ? 100 : 
                       selfHealing?.currentHealthStatus === 'degraded' ? 60 : 30}
                suffix="/ 100"
                prefix={
                  selfHealing?.currentHealthStatus === 'healthy' ? 
                  <CheckCircleOutlined style={{ color: '#10b981' }} /> : 
                  <WarningOutlined style={{ color: '#faad14' }} />
                }
                valueStyle={{ color: selfHealing?.currentHealthStatus === 'healthy' ? '#10b981' : '#faad14' }}
              />
              <Text type="secondary" style={{ fontSize: 12 }}>
                Status: {selfHealing?.currentHealthStatus || 'Unknown'}
              </Text>
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card className="glass-card" bordered={false}>
              <Statistic
                title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>সরাসরি সাকসেস রেট</Text>}
                value={calculateOverallSuccessRate()}
                suffix="%"
                prefix={<CheckCircleOutlined style={{ color: '#3b82f6' }} />}
              />
              <Progress 
                percent={calculateOverallSuccessRate()} 
                showInfo={false} 
                strokeColor="#3b82f6" 
                size="small" 
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card className="glass-card" bordered={false}>
              <Statistic
                title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>oucAvg Latency</Text>}
                value={calculateAvgLatency()}
                suffix="ms"
                prefix={<ThunderboltOutlined style={{ color: '#8b5cf6' }} />}
              />
              <Text type="secondary" style={{ fontSize: 12 }}>
                Across {providers.length} providers
              </Text>
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card className="glass-card" bordered={false}>
              <Statistic
                title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>অ্যাক্টিভ এজেন্ট</Text>}
                value={agentStats?.activeAgents || 0}
                suffix={`/ ${agentStats?.totalAgents || 0}`}
                prefix={<RobotOutlined style={{ color: '#ec4899' }} />}
              />
              <Progress 
                percent={agentStats?.totalAgents ? Math.round((agentStats.activeAgents / agentStats.totalAgents) * 100) : 0}
                showInfo={false}
                strokeColor="#ec4899"
                size="small"
              />
            </Card>
          </Col>
        </Row>

        {/* System Resources & Self-Healing */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} lg={12}>
            <Card 
              title={<Space><DatabaseOutlined /> সিস্টেম রিসোর্স</Space>} 
              className="glass-card cyan-bordered-panel"
              bordered={false}
            >
              {metrics ? (
                <Row gutter={[16, 16]}>
                  <Col span={12}>
                    <div style={{ marginBottom: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text style={{ color: 'rgba(255,255,255,0.7)' }}>Memory</Text>
                        <Text strong>{Math.round((metrics.memoryUsed / metrics.memoryMax) * 100)}%</Text>
                      </div>
                      <Progress 
                        percent={Math.round((metrics.memoryUsed / metrics.memoryMax) * 100)}
                        strokeColor={getSystemHealthColor()}
                      />
                    </div>
                    <div style={{ marginBottom: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text style={{ color: 'rgba(255,255,255,0.7)' }}>CPU</Text>
                        <Text strong>{metrics.cpuLoad.toFixed(1)} / {metrics.availableProcessors} cores</Text>
                      </div>
                      <Progress 
                        percent={Math.min((metrics.cpuLoad / metrics.availableProcessors) * 100, 100)}
                        strokeColor={metrics.cpuLoad > metrics.availableProcessors * 0.8 ? '#f5222d' : '#1890ff'}
                      />
                    </div>
                  </Col>
                  <Col span={12}>
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="DB Active">
                        {metrics.dbActiveConnections || 'N/A'}
                      </Descriptions.Item>
                      <Descriptions.Item label="DB Idle">
                        {metrics.dbIdleConnections || 'N/A'}
                      </Descriptions.Item>
                      <Descriptions.Item label="Redis">
                        <Tag color={metrics.redisStatus === 'PONG' ? 'green' : 'red'}>
                          {metrics.redisStatus === 'PONG' ? 'Online' : 'Offline'}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="Updated">
                        {dayjs(metrics.timestamp).fromNow()}
                      </Descriptions.Item>
                    </Descriptions>
                  </Col>
                </Row>
              ) : (
                <Spin tip="Loading metrics..." />
              )}
            </Card>
          </Col>

          <Col xs={24} lg={12}>
            <Card 
              title={<Space><ExclamationCircleOutlined />Self-Healing System</Space>} 
              className="glass-card cyan-bordered-panel"
              bordered={false}
            >
              {selfHealing ? (
                <Row gutter={[16, 16]}>
                  <Col span={12}>
                    <Statistic
                      title="Total Healing Events"
                      value={selfHealing.totalHealingEvents}
                      prefix={<CheckCircleOutlined />}
                    />
                  </Col>
                  <Col span={12}>
                    <Statistic
                      title="Success Rate"
                      value={selfHealing.totalHealingEvents > 0 ? 
                        Math.round((selfHealing.successfulHealingEvents / selfHealing.totalHealingEvents) * 100) : 0}
                      suffix="%"
                    />
                  </Col>
                  <Col span={24}>
                    <Text type="secondary">
                      Last check: {selfHealing.lastCheck ? dayjs(selfHealing.lastCheck).fromNow() : 'Never'}
                    </Text>
                    <br />
                    <Tag color={selfHealing.enabled ? 'green' : 'red'} style={{ marginTop: 8 }}>
                      {selfHealing.enabled ? 'Enabled' : 'Disabled'}
                    </Tag>
                  </Col>
                </Row>
              ) : (
                <Spin tip="Loading self-healing status..." />
              )}
            </Card>
          </Col>
        </Row>

        {/* Provider Performance Table */}
        <Card 
          title={
            <Space>
              <TrophyOutlined style={{ color: '#f59e0b' }} />
              <span style={{ color: '#fff' }}>Provider Performance Rankings</span>
            </Space>
          }
          className="glass-card cyan-bordered-panel"
          bordered={false}
          style={{ marginBottom: 24 }}
        >
          <Table 
            columns={providerColumns}
            dataSource={providers}
            rowKey="name"
            pagination={{ pageSize: 10 }}
            size="middle"
            scroll={{ x: true }}
            loading={loading}
          />
        </Card>

        {/* Agent Stats */}
        {agentStats && (
          <Card 
            title={<Space><RobotOutlined /> AI Agent Statistics</Space>}
            className="glass-card"
            bordered={false}
            style={{ marginBottom: 24 }}
          >
            <Row gutter={[24, 24]}>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Total Agents"
                  value={agentStats.totalAgents}
                  prefix={<RobotOutlined />}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Total Tasks Processed"
                  value={agentStats.totalTasksProcessed}
                  prefix={<CheckCircleOutlined />}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Avg Completion Time"
                  value={agentStats.avgTaskCompletionTime}
                  suffix="ms"
                  prefix={<ThunderboltOutlined />}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Failed Agents"
                  value={agentStats.failedAgents}
                  valueStyle={{ color: agentStats.failedAgents > 0 ? '#f5222d' : '#52c41a' }}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Idle Agents"
                  value={agentStats.idleAgents}
                  valueStyle={{ color: '#faad14' }}
                />
              </Col>
              <Col xs={24} sm={8}>
                <Statistic
                  title="Success Rate"
                  value={agentStats.successRate}
                  suffix="%"
                />
              </Col>
            </Row>
          </Card>
        )}

        {/* Performance History Timeline */}
        {history.length > 0 && (
          <Card 
            title={<Space><RiseOutlined /> Performance History</Space>}
            className="glass-card"
            bordered={false}
          >
            <Table 
              columns={historyColumns}
              dataSource={history.map((h, i) => ({ ...h, key: i }))}
              pagination={{ pageSize: 10 }}
              size="small"
              loading={loading}
            />
          </Card>
        )}

      </div>
    </AdminLayout>
  );
};

export default AdminPerformance;
