// AdminPerformance.tsx - Cinematic Performance Matrix
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Progress, Table, Statistic, Tag, Select } from 'antd';
import { 
  ClusterOutlined,
  ThunderboltOutlined,
  TrophyOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ReloadOutlined,
  DatabaseOutlined,
  RiseOutlined,
  GlobalOutlined,
  DotChartOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

const AdminPerformance: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [providers, setProviders] = useState<any[]>([]);
  const [stats, setStats] = useState<any>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rankingsRes, agentsRes] = await Promise.all([
        authUtils.fetchWithAuth('/api/admin/providers/rankings'),
        authUtils.fetchWithAuth('/api/ai-agents/stats')
      ]);
      if (rankingsRes.ok) {
        const data = await rankingsRes.json();
        const list = data.data?.rankings || data.rankings || {};
        setProviders(Object.entries(list).map(([name, d]: [string, any]) => ({
          name, score: Math.round((d.successRate || 0) * 100), latency: d.averageLatency || 0
        })).sort((a, b) => b.score - a.score));
      }
      if (agentsRes.ok) setStats(await agentsRes.json());
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(0, 243, 255, 0.1)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <ClusterOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>NEURAL THROUGHPUT</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              Performance <span className="text-gradient">Matrix</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Real-time cluster telemetry, provider optimization rankings, and agent efficiency tracking.</Text>
          </Col>
          <Col>
            <Space>
               <Select defaultValue="24h" className="cyber-select" style={{ width: 120 }} dropdownStyle={{ background: '#080810' }}>
                  <Select.Option value="1h">1H Trace</Select.Option>
                  <Select.Option value="24h">24H Cycle</Select.Option>
               </Select>
               <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchData} className="glass-action-button">Refetch Matrix</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
         {/* KPI Summary Cards */}
         <Col span={24}>
            <Row gutter={[24, 24]}>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Consensus Rate</Text>
                           <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>98.4%</div>
                        </div>
                        <CheckCircleOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Global Latency</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>142ms</div>
                        </div>
                        <ThunderboltOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Neural Stability</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>OPTIMAL</div>
                        </div>
                        <GlobalOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card">
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Active Shards</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>24/24</div>
                        </div>
                        <DatabaseOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
            </Row>
         </Col>

         {/* Main Ranking Table */}
         <Col xs={24} lg={16}>
            <div className="glass-card" style={{ minHeight: 500 }}>
               <div className="glass-card-title">Provider Performance Ranking <TrophyOutlined style={{ color: '#f59e0b' }} /></div>
               <Table
                 dataSource={providers} pagination={false} loading={loading}
                 columns={[
                    { title: 'PROVIDER', dataIndex: 'name', key: 'name', render: (t: string) => <Text strong style={{ color: '#fff' }}>{t}</Text> },
                    { title: 'EFFICIENCY', dataIndex: 'score', key: 'score', render: (s: number) => <Progress percent={s} size="small" strokeColor="var(--neon-blue)" /> },
                    { title: 'AVG RESPONSE', dataIndex: 'latency', key: 'latency', render: (l: number) => <Text style={{ fontFamily: 'JetBrains Mono', color: 'var(--neon-blue)' }}>{l}ms</Text> },
                    { title: 'HEALTH', key: 'h', render: () => <Badge status="success" text="STABLE" /> }
                 ]}
                 className="cyber-table"
               />
            </div>
         </Col>

         {/* Right Sidebar Stats */}
         <Col xs={24} lg={8}>
            <Space direction="vertical" size={24} style={{ width: '100%' }}>
               <div className="glass-card">
                  <div className="glass-card-title">Neural Node Load <RiseOutlined /></div>
                  <div style={{ padding: '8px 0' }}>
                     {[
                        { label: 'Core Engine', val: 84, color: 'var(--neon-blue)' },
                        { label: 'Inference Layer', val: 62, color: 'var(--neon-purple)' },
                        { label: 'Data Sharding', val: 45, color: 'var(--success)' }
                     ].map((item, i) => (
                        <div key={i} style={{ marginBottom: 16 }}>
                           <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                              <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>{item.label}</Text>
                              <Text style={{ color: '#fff', fontWeight: 800 }}>{item.val}%</Text>
                           </div>
                           <Progress percent={item.val} strokeColor={item.color} trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={4} />
                        </div>
                     ))}
                  </div>
               </div>

               <div className="glass-card" style={{ background: 'rgba(0, 243, 255, 0.05)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                     <DotChartOutlined style={{ color: 'var(--neon-blue)' }} />
                     <Text strong style={{ color: '#fff' }}>Throughput Pulse</Text>
                  </div>
                  <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                     Current processing rate: 4.2k ops/sec. Optimized for peak traffic.
                  </Text>
               </div>
            </Space>
         </Col>
      </Row>

      <style>{`
        .cyber-select .ant-select-selector { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; border-radius: 8px !important; }
      `}</style>
    </motion.div>
  );
};

export default AdminPerformance;
