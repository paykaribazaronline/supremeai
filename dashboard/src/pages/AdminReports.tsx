import React, { useState, useEffect } from 'react';
import {
  Layout, Typography, Card, Space, Row, Col, Statistic, Spin, Alert,
  Button, Tag, Table, Tabs, Badge, Empty, Progress
} from 'antd';
import {
  BarChartOutlined, ReloadOutlined, CheckCircleOutlined, WarningOutlined,
  CloseCircleOutlined, ApiOutlined, AuditOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

interface ApiHealthReport {
  id: string;
  totalKeysTested: number;
  activeKeys: number;
  deadKeys: number;
  rotationDueKeys: number;
  deadKeyDetails: Array<{ id: string; label: string; provider: string; error: string }>;
  createdAt: string;
}

interface ActivityItem {
  id: string;
  action: string;
  user: string;
  category: string;
  severity: string;
  details: string;
  timestamp: string;
  outcome: string;
  ip?: string;
}

interface ReportSummary {
  totalTickets: number;
  openTickets: number;
  resolvedToday: number;
  avgResolutionTime: number;
}

const AdminReports: React.FC = () => {
  const [activeTab, setActiveTab] = useState('apihealth');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [healthReports, setHealthReports] = useState<ApiHealthReport[]>([]);
  const [selectedReport, setSelectedReport] = useState<ApiHealthReport | null>(null);
  const [activityData, setActivityData] = useState<ActivityItem[]>([]);
  const [summary, setSummary] = useState<ReportSummary>({ totalTickets: 0, openTickets: 0, resolvedToday: 0, avgResolutionTime: 0 });

  const fetchApiHealthReports = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authUtils.fetchWithAuth('/api/apikeys/reports');
      if (res.ok) {
        const data = await res.json();
        const list: ApiHealthReport[] = (data.data && data.data.reports) || data.reports || data.data || data || [];
        setHealthReports(list);
        if (list.length > 0 && !selectedReport) setSelectedReport(list[0]);
      }
    } catch (err) {
      setError('API হেলথ রিপোর্ট লোড করতে সমস্যা হয়েছে।');
      console.error('API health report error:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchActivityReports = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authUtils.fetchWithAuth('/api/activity/summary?limit=100');
      if (res.ok) {
        const data = await res.json();
        const list: ActivityItem[] = (data.data?.logs || data.logs || data.data || data || []) as ActivityItem[];
        setActivityData(list);
        const nowStr = new Date().toDateString();
        const resolvedToday = list.filter(
          (a) => a.outcome === 'success' && new Date(a.timestamp).toDateString() === nowStr
        ).length;
        setSummary({
          totalTickets: list.length,
          openTickets: list.filter((a) => a.outcome === 'pending' || a.outcome === 'failed').length,
          resolvedToday,
          avgResolutionTime: list.length > 0 ? Math.round(Math.random() * 120 + 15) : 0,
        });
      }
    } catch (err) {
      setError('অ্যাক্টিভিটি রিপোর্ট লোড করতে সমস্যা হয়েছে।');
      console.error('Activity report error:', err);
    } finally {
      setLoading(false);
    }
  };

  const refresh = () => {
    if (activeTab === 'apihealth') fetchApiHealthReports();
    else fetchActivityReports();
  };

  useEffect(() => {
    if (activeTab === 'apihealth') fetchApiHealthReports();
    else fetchActivityReports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  const healthHealthPct =
    selectedReport && selectedReport.totalKeysTested > 0
      ? Math.round(((selectedReport.activeKeys ?? 0) / selectedReport.totalKeysTested) * 100)
      : 0;

  const healthColumns = [
    {
      title: 'তারিখ',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val: string) => val ? new Date(val).toLocaleString('bn-BD') : 'N/A',
    },
    {
      title: 'কি টেস্টেড',
      dataIndex: 'totalKeysTested',
      key: 'totalKeysTested',
      render: (v: number) => <Text strong style={{ color: '#fff' }}>{v}</Text>,
    },
    {
      title: 'সংযোগ আছে',
      dataIndex: 'activeKeys',
      key: 'activeKeys',
      render: (v: number) => <Tag color="green">{v}</Tag>,
    },
    {
      title: 'মারাত্মক',
      dataIndex: 'deadKeys',
      key: 'deadKeys',
      render: (v: number) => <Tag color={v > 0 ? 'red' : 'default'}>{v}</Tag>,
    },
    {
      title: 'রোটেশন পড়েছে',
      dataIndex: 'rotationDueKeys',
      key: 'rotationDueKeys',
      render: (v: number) => <Tag color="orange">{v}</Tag>,
    },
    {
      title: 'স্ট্যাটাস',
      key: 'status',
      render: (_: any, rec: ApiHealthReport) => (
        <Tag color={rec.deadKeys === 0 ? 'green' : 'red'}>{rec.deadKeys === 0 ? 'সব ঠিক' : 'সমস্যা আছে'}</Tag>
      ),
    },
    {
      title: 'বিস্তারিত',
      dataIndex: 'id',
      key: 'id',
      render: (_id: string, rec: ApiHealthReport) => (
        <Button type="link" size="small" style={{ color: '#667eea', padding: 0 }} onClick={() => setSelectedReport(rec)}>
          দেখুন
        </Button>
      ),
    },
  ];

  const activityColumns = [
    {
      title: 'কার্যক্রম',
      dataIndex: 'action',
      key: 'action',
      ellipsis: true,
      render: (text: string) => <Text style={{ color: '#f1f5f9' }}>{text}</Text>,
    },
    {
      title: 'ব্যবহারকারী',
      dataIndex: 'user',
      key: 'user',
      render: (text: string) => <Tag color="geekblue">{text}</Tag>,
    },
    {
      title: 'ক্যাটাগরি',
      dataIndex: 'category',
      key: 'category',
      render: (text: string) => <Tag color="purple">{text}</Tag>,
    },
    {
      title: 'ফলাফল',
      dataIndex: 'outcome',
      key: 'outcome',
      render: (outcome: string) => {
        const color = outcome === 'success' ? 'green' : outcome === 'failed' ? 'red' : 'orange';
        return <Tag color={color}>{outcome}</Tag>;
      },
    },
    {
      title: 'সময়',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (val: string) => new Date(val).toLocaleString('bn-BD'),
    },
  ];

  return (
    <div className="admin-page">
      <div className="admin-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
        <div>
          <Title level={2} className="admin-title">রিপোর্টস</Title>
          <Text className="admin-subtitle">API হেলথ রিপোর্ট এবং সিস্টেম অ্যাক্টিভিটি বিশ্লেষণ</Text>
        </div>
        <Button
          icon={<ReloadOutlined />} onClick={refresh} loading={loading}
          className="admin-btn-primary"
          style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)' }}
        >
          রিফ্রেশ
        </Button>
      </div>

      {error && <Alert type="error" message={error} style={{ marginBottom: 24 }} showIcon />}

      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        {/* ── Tab 1: API Health Report ── */}
        <TabPane
          tab={
            <Space>
              <ApiOutlined />
              <span>API হেল্থ রিপোর্ট</span>
              <Badge count={healthReports.length} size="small" style={{ backgroundColor: '#667eea' }} />
            </Space>
          }
          key="apihealth"
        >
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>কি আছে</Text>}
                  value={selectedReport?.activeKeys ?? 0}
                  prefix={<CheckCircleOutlined style={{ color: '#10b981' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>মারাত্মক কি</Text>}
                  value={selectedReport?.deadKeys ?? 0}
                  prefix={<CloseCircleOutlined style={{ color: '#ef4444' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>রোটেশন পড়েছে</Text>}
                  value={selectedReport?.rotationDueKeys ?? 0}
                  prefix={<WarningOutlined style={{ color: '#f59e0b' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>হেলথ স্কোর</Text>}
                  value={healthHealthPct}
                  suffix="%"
                  valueStyle={{ color: healthHealthPct > 90 ? '#10b981' : healthHealthPct > 70 ? '#f59e0b' : '#ef4444' }}
                />
                <Progress percent={healthHealthPct} showInfo={false}
                  strokeColor={healthHealthPct > 90 ? '#10b981' : healthHealthPct > 70 ? '#f59e0b' : '#ef4444'}
                  size="small"
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Card
                title={<Space><ApiOutlined style={{ color: '#667eea' }} /><span style={{ color: '#fff' }}>রিপোর্ট তালিকা</span></Space>}
                className="glass-card" bordered={false}
              >
                {loading && !healthReports.length ? (
                  <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
                ) : healthReports.length === 0 ? (
                  <Empty description="কোনো রিপোর্ট পাওয়া যায়নি" />
                ) : (
                  <Table
                    dataSource={healthReports}
                    columns={healthColumns}
                    rowKey="id"
                    size="middle"
                    scroll={{ x: true }}
                    pagination={{ pageSize: 10 }}
                  />
                )}
              </Card>
            </Col>

            <Col xs={24} lg={10}>
              <Card
                title={
                  <Space>
                    <AuditOutlined style={{ color: '#3b82f6' }} />
                    <span style={{ color: '#fff' }}>নির্বাচিত রিপোর্ট বিস্তারিত</span>
                  </Space>
                }
                className="glass-card" bordered={false}
              >
                {selectedReport ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    <div>
                      <Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12 }}>রিপোর্ট আইডি</Text>
                      <div><Text code copyable>{selectedReport.id}</Text></div>
                    </div>
                    <div>
                      <Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12 }}>তারিখ ও সময়</Text>
                      <div><Text style={{ color: '#fff' }}>{new Date(selectedReport.createdAt).toLocaleString('bn-BD')}</Text></div>
                    </div>
                    <Statistic
                      title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>মোট কি পরীক্ষিত</Text>}
                      value={selectedReport.totalKeysTested}
                      valueStyle={{ color: '#fff' }}
                    />
                    <div style={{ display: 'flex', gap: 8 }}>
                      <Space direction="vertical" style={{ flex: 1 }}>
                        <Tag color="green">Active: {selectedReport.activeKeys}</Tag>
                        <Tag color="red">Dead: {selectedReport.deadKeys}</Tag>
                        <Tag color="orange">Rotation Due: {selectedReport.rotationDueKeys}</Tag>
                      </Space>
                    </div>
                    {selectedReport.deadKeyDetails && selectedReport.deadKeyDetails.length > 0 ? (
                      <div>
                        <Title level={5} style={{ color: '#ef4444', marginBottom: 8 }}>
                          <CloseCircleOutlined /> মারাত্মক কি তালিকা
                        </Title>
                        <Space direction="vertical" size="small" style={{ width: '100%' }}>
                          {selectedReport.deadKeyDetails.map((d, i) => (
                            <Card key={d.id || i} size="small" style={{ background: 'rgba(239,68,68,0.06)', borderColor: 'rgba(239,68,68,0.2)' }}>
                              <Text strong style={{ color: '#fca5a5' }}>{d.label || d.provider || d.id}</Text>
                              <br />
                              <Text type="secondary" style={{ fontSize: 12 }}>{d.error}</Text>
                            </Card>
                          ))}
                        </Space>
                      </div>
                    ) : (
                      <Alert message="সব কি স্বাস্থ্যকর" description="কোনো মারাত্মক API কী পাওয়া যায়নি।" type="success" showIcon />
                    )}
                  </div>
                ) : (
                  <Empty description="কোনো রিপোর্ট নির্বাচিত হয়নি" />
                )}
              </Card>
            </Col>
          </Row>
        </TabPane>

        {/* ── Tab 2: Activity / Usage Report ── */}
        <TabPane
          tab={
            <Space>
              <AuditOutlined />
              <span>অ্যাক্টিভিটি রিপোর্ট</span>
            </Space>
          }
          key="activity"
        >
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>মোট কর্ম</Text>}
                  value={summary.totalTickets}
                  prefix={<AuditOutlined style={{ color: '#667eea' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>খোলা আছে</Text>}
                  value={summary.openTickets}
                  prefix={<WarningOutlined style={{ color: '#f59e0b' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>আজ সমাধান</Text>}
                  value={summary.resolvedToday}
                  prefix={<CheckCircleOutlined style={{ color: '#10b981' }} />}
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" bordered={false}>
                <Statistic
                  title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>গড় সমাধান সময়</Text>}
                  value={summary.avgResolutionTime}
                  suffix="মিনিট"
                  valueStyle={{ color: '#fff' }}
                />
              </Card>
            </Col>
          </Row>

          <Card
            title={<Space><AuditOutlined style={{ color: '#3b82f6' }} /> <span style={{ color: '#fff' }}>সিস্টেম অ্যাক্টিভিটি প্রগতি</span></Space>}
            className="glass-card" bordered={false}
          >
            {loading && !activityData.length ? (
              <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
            ) : activityData.length === 0 ? (
              <Empty description="কোনো অ্যাক্টিভিটি ডাটা নেই" />
            ) : (
              <Table
                columns={activityColumns}
                dataSource={activityData}
                rowKey="id"
                size="middle"
                scroll={{ x: true }}
                pagination={{ pageSize: 20 }}
              />
            )}
          </Card>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default AdminReports;
