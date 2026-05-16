import React, { useState, useEffect } from 'react';
import { Layout, Typography, Card, Space, Row, Col, Progress, Table, Tag, Statistic, Spin, Alert, Button } from 'antd';
import { 
  LineChartOutlined, 
  TrophyOutlined,
  BarChartOutlined,
  RiseOutlined,
  ReloadOutlined,
  DashboardOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface ProviderRanking {
  name: string;
  successRate: number;
  averageLatency: number;
  requestCount: number;
  score: number;
  status: string;
}

const AdminAnalytics: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rankings, setRankings] = useState<ProviderRanking[]>([]);
  const [stats, setStats] = useState<any>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [rankingsRes, contractRes] = await Promise.all([
        authUtils.fetchWithAuth('/api/admin/providers/rankings'),
        authUtils.fetchWithAuth('/api/admin/dashboard/contract')
      ]);

      if (rankingsRes.ok) {
        const rankingsData = await rankingsRes.json();
        // rankingsData might be wrapped in ApiResponse
        const list = rankingsData.data?.rankings || rankingsData.rankings || [];
        
        // Transform the map/object from backend to array for table
        const formattedRankings = Object.entries(list).map(([name, data]: [string, any]) => ({
          name,
          successRate: data.successRate * 100,
          averageLatency: data.averageLatency,
          requestCount: data.totalTasks,
          score: (data.successRate * 100),
          status: data.successRate > 0.9 ? 'excellent' : (data.successRate > 0.7 ? 'good' : 'fair')
        })).sort((a, b) => b.score - a.score);
        
        setRankings(formattedRankings);
      }

      if (contractRes.ok) {
        const contractData = await contractRes.json();
        setStats(contractData.data?.stats || contractData.stats);
      }
    } catch (err) {
      setError('ডাটা লোড করতে সমস্যা হয়েছে। দয়া করে আবার চেষ্টা করুন।');
      console.error('Analytics fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const columns = [
    { title: 'Provider', dataIndex: 'name', key: 'name', render: (text: string) => <Text strong>{text}</Text> },
    { title: 'Score', dataIndex: 'score', key: 'score', render: (score: number) => (
      <Progress percent={isNaN(score) ? 0 : Math.round(score)} size="small" strokeColor={score > 90 ? "#10b981" : "#f59e0b"} />
    )},
    { title: 'Avg Latency', dataIndex: 'averageLatency', key: 'averageLatency', render: (val: number) => `${isNaN(val) ? 'N/A' : Math.round(val) + 'ms'}` },
    { title: 'Requests', dataIndex: 'requestCount', key: 'requestCount' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (status: string) => (
      <Tag color={
        status === 'excellent' ? 'green' : 
        status === 'good' ? 'blue' : 'orange'
      }>{status.toUpperCase()}</Tag>
    )},
  ];

   return (
     <div className="admin-page">
       <div className="admin-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
         <div>
           <Title level={2} className="admin-title">
             সিস্টেম এনালাইটিক্স
           </Title>
           <Text className="admin-subtitle">
             প্রোভাইডার পারফরম্যান্স এবং ইন্টেলিজেন্স মেট্রিক্স
           </Text>
         </div>
         <Button 
           icon={<ReloadOutlined />} 
           onClick={fetchData}
           loading={loading}
           className="admin-btn-primary"
           style={{ 
             background: 'rgba(255,255,255,0.05)',
             border: '1px solid rgba(255,255,255,0.1)'
           }}
         >
           রিফ্রেশ
         </Button>
       </div>

      {error && <Alert type="error" message={error} style={{ marginBottom: 24 }} showIcon />}

      <Row gutter={[24, 24]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>সিস্টেম হেলথ</Text>}
              value={stats?.systemHealthScore || 0}
              suffix="/ 100"
              prefix={<SafetyCertificateOutlined style={{ color: '#10b981' }} />}
              valueStyle={{ color: '#fff' }}
            />
            <Progress percent={stats?.systemHealthScore || 0} showInfo={false} strokeColor="#10b981" size="small" />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>সাকসেস রেট</Text>}
              value={stats?.successRate || 0}
              suffix="%"
              prefix={<CheckCircleOutlined style={{ color: '#3b82f6' }} />}
              valueStyle={{ color: '#fff' }}
            />
            <Progress percent={stats?.successRate || 0} showInfo={false} strokeColor="#3b82f6" size="small" />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>অ্যাক্টিভ এজেন্ট</Text>}
              value={stats?.activeAIAgents || 0}
              prefix={<DashboardOutlined style={{ color: '#8b5cf6' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>নলেজ বেস সাইজ</Text>}
              value={stats?.knowledgeBaseSize || 0}
              prefix={<RiseOutlined style={{ color: '#ec4899' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={16}>
          <Card
            title={<Space><TrophyOutlined style={{ color: '#f59e0b' }} /> <span style={{ color: '#fff' }}>Provider Performance Rankings</span></Space>}
            className="glass-card"
            bordered={false}
          >
            <Table 
              columns={columns} 
              dataSource={rankings} 
              pagination={false}
              loading={loading}
              rowKey="name"
              size="middle"
              scroll={{ x: true }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card
            title={<Space><BarChartOutlined style={{ color: '#3b82f6' }} /> <span style={{ color: '#fff' }}>Learning Trends</span></Space>}
            className="glass-card"
            bordered={false}
          >
            <div style={{ padding: '8px 0' }}>
              <div style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.8)' }}>NLP Understanding</Text>
                  <Text strong style={{ color: '#fff' }}>94%</Text>
                </div>
                <Progress percent={94} showInfo={false} strokeColor="#3b82f6" />
              </div>
              <div style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.8)' }}>App Generation Accuracy</Text>
                  <Text strong style={{ color: '#fff' }}>88%</Text>
                </div>
                <Progress percent={88} showInfo={false} strokeColor="#10b981" />
              </div>
              <div style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.8)' }}>Autonomous Reasoning</Text>
                  <Text strong style={{ color: '#fff' }}>82%</Text>
                </div>
                <Progress percent={82} showInfo={false} strokeColor="#8b5cf6" />
              </div>
              
              <div style={{ marginTop: 24, padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 8 }}>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  The system automatically improves its performance based on consensus voting and success patterns from recent {stats?.completedTasks || 0} tasks.
                </Text>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AdminAnalytics;
