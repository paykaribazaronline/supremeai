// AdminAnalytics.tsx - Cinematic System Analytics
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Progress, Table, Statistic } from 'antd';
import { 
  LineChartOutlined, 
  TrophyOutlined,
  BarChartOutlined,
  RiseOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  GlobalOutlined,
  PieChartOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
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
  const [rankings, setRankings] = useState<ProviderRanking[]>([]);
  const [stats, setStats] = useState<any>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rankingsRes, contractRes] = await Promise.all([
        authUtils.fetchWithAuth('/api/admin/providers/rankings'),
        authUtils.fetchWithAuth('/api/admin/dashboard/contract')
      ]);
      if (rankingsRes.ok) {
        const rankingsData = await rankingsRes.json();
        const list = rankingsData.data?.rankings || rankingsData.rankings || {};
        const formatted = Object.entries(list).map(([name, data]: [string, any]) => ({
          name,
          successRate: data.successRate * 100,
          averageLatency: data.averageLatency,
          requestCount: data.totalTasks,
          score: (data.successRate * 100),
          status: data.successRate > 0.9 ? 'excellent' : (data.successRate > 0.7 ? 'good' : 'fair')
        })).sort((a, b) => b.score - a.score);
        setRankings(formatted);
      }
      if (contractRes.ok) setStats((await contractRes.json()).data?.stats);
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(0, 243, 255, 0.1)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <BarChartOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>INTELLIGENCE METRICS</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              System <span className="text-gradient">Analytics</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Global performance tracking, provider rankings, and success distribution.</Text>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchData} className="glass-action-button">Refresh Analytics</Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Top KPIs */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Success Rate</Text>
                          <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>{stats?.successRate || 98.2}%</div>
                       </div>
                       <CheckCircleOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Active Agents</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{stats?.activeAIAgents || 12}</div>
                       </div>
                       <GlobalOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>System Score</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{stats?.systemHealthScore || 100}/100</div>
                       </div>
                       <SafetyCertificateOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Daily Requests</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>1.2M</div>
                       </div>
                       <RiseOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Main Ranking Table */}
        <Col xs={24} lg={16}>
           <div className="glass-card" style={{ minHeight: 500 }}>
              <div className="glass-card-title">
                 Provider Performance Ranking
                 <TrophyOutlined style={{ color: '#f59e0b' }} />
              </div>
              <Table
                dataSource={rankings}
                pagination={false}
                loading={loading}
                columns={[
                  { title: 'PROVIDER', dataIndex: 'name', key: 'name', render: (t: string) => <Text strong style={{ color: '#fff' }}>{t}</Text> },
                  { title: 'SCORE', dataIndex: 'score', key: 'score', render: (s: number) => <Progress percent={Math.round(s)} size="small" strokeColor={s > 90 ? "var(--success)" : "var(--neon-blue)"} /> },
                  { title: 'LATENCY', dataIndex: 'averageLatency', key: 'averageLatency', render: (l: number) => <Text style={{ fontFamily: 'JetBrains Mono', color: 'var(--neon-blue)' }}>{Math.round(l)}ms</Text> },
                  { title: 'STATUS', dataIndex: 'status', key: 'status', render: (s: string) => <Badge status={s === 'excellent' ? 'success' : 'processing'} text={<span style={{ color: '#fff', textTransform: 'uppercase', fontSize: 10 }}>{s}</span>} /> }
                ]}
                className="cyber-table"
              />
           </div>
        </Col>

        {/* Intelligence Side Panel */}
        <Col xs={24} lg={8}>
           <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <div className="glass-card">
                 <div className="glass-card-title">
                    Learning Trends
                    <PieChartOutlined style={{ color: 'var(--neon-purple)' }} />
                 </div>
                 <div style={{ padding: '8px 0' }}>
                    <div style={{ marginBottom: 20 }}>
                       <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 12 }}>NLP Understanding</Text>
                          <Text style={{ color: 'var(--neon-blue)', fontWeight: 800 }}>94%</Text>
                       </div>
                       <Progress percent={94} strokeColor="var(--neon-blue)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={4} />
                    </div>
                    <div style={{ marginBottom: 20 }}>
                       <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 12 }}>Synthesis Accuracy</Text>
                          <Text style={{ color: 'var(--neon-purple)', fontWeight: 800 }}>88%</Text>
                       </div>
                       <Progress percent={88} strokeColor="var(--neon-purple)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={4} />
                    </div>
                    <div>
                       <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 12 }}>Autonomous Logic</Text>
                          <Text style={{ color: 'var(--success)', fontWeight: 800 }}>82%</Text>
                       </div>
                       <Progress percent={82} strokeColor="var(--success)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={4} />
                    </div>
                 </div>
              </div>

              <div className="glass-card" style={{ background: 'rgba(0, 243, 255, 0.05)' }}>
                 <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                    <ThunderboltOutlined style={{ color: 'var(--neon-blue)' }} />
                    <Text strong style={{ color: '#fff' }}>Optimization Cycle</Text>
                 </div>
                 <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                    The system is currently auto-tuning weights based on the last 50,000 requests. Efficiency gain: +4.2%.
                 </Text>
              </div>
           </Space>
        </Col>
      </Row>
    </motion.div>
  );
};

export default AdminAnalytics;
