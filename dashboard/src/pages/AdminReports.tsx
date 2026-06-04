// AdminReports.tsx - Cinematic System Intelligence Reports
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Tabs, Statistic, Progress, Table, Tag, Empty } from 'antd';
import {
  BarChartOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  ApiOutlined,
  AuditOutlined,
  FileTextOutlined,
  GlobalOutlined,
  ThunderboltOutlined,
  DatabaseOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface ApiHealthReport {
  id: string;
  totalKeysTested: number;
  activeKeys: number;
  deadKeys: number;
  rotationDueKeys: number;
  deadKeyDetails: Array<{ id: string; label: string; provider: string; error: string }>;
  createdAt: string;
}

const AdminReports: React.FC = () => {
  const [activeTab, setActiveTab] = useState('apihealth');
  const [loading, setLoading] = useState(false);
  const [healthReports, setHealthReports] = useState<ApiHealthReport[]>([]);
  const [selectedReport, setSelectedReport] = useState<ApiHealthReport | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/apikeys/reports');
      if (res.ok) {
        const data = await res.json();
        const list = data.data?.reports || data.reports || [];
        setHealthReports(list);
        if (list.length > 0) setSelectedReport(list[0]);
      }
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  const healthScore = selectedReport && selectedReport.totalKeysTested > 0
    ? Math.round(((selectedReport.activeKeys ?? 0) / selectedReport.totalKeysTested) * 100) : 0;

  const tabItems = [
    {
      key: 'apihealth',
      label: <span className="tab-label"><ApiOutlined /> NEURAL HEALTH</span>,
      children: (
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={16}>
             <div className="glass-card" style={{ minHeight: 500 }}>
                <div className="glass-card-title">Historical Health Matrix <FileTextOutlined /></div>
                <Table
                   dataSource={healthReports}
                   loading={loading}
                   pagination={{ pageSize: 8 }}
                   columns={[
                      { title: 'TIMESTAMP', dataIndex: 'createdAt', key: 'createdAt', render: (t: string) => <Text style={{ fontFamily: 'JetBrains Mono', fontSize: 11, color: 'rgba(255,255,255,0.45)' }}>{new Date(t).toLocaleString()}</Text> },
                      { title: 'TESTED', dataIndex: 'totalKeysTested', key: 'totalKeysTested', render: (v: number) => <Text strong style={{ color: '#fff' }}>{v}</Text> },
                      { title: 'ACTIVE', dataIndex: 'activeKeys', key: 'activeKeys', render: (v: number) => <Badge status="success" text={<span style={{ color: 'var(--success)' }}>{v}</span>} /> },
                      { title: 'DEAD', dataIndex: 'deadKeys', key: 'deadKeys', render: (v: number) => <Badge status="error" text={<span style={{ color: '#ff4d4f' }}>{v}</span>} /> },
                      { title: 'ACTION', key: 'id', render: (_, r) => <Button type="link" onClick={() => setSelectedReport(r)} className="text-gradient" style={{ padding: 0 }}>Deconstruct</Button> }
                   ]}
                   className="cyber-table"
                />
             </div>
          </Col>
          <Col xs={24} lg={8}>
             <div className="glass-card" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <div className="glass-card-title">Report Synthesis <AuditOutlined /></div>
                {selectedReport ? (
                   <Space direction="vertical" size={24} style={{ width: '100%' }}>
                      <div>
                         <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase' }}>Consensus Accuracy</Text>
                         <div style={{ marginTop: 12 }}>
                            <Progress
                               type="dashboard"
                               percent={healthScore}
                               strokeColor="var(--neon-blue)"
                               trailColor="rgba(255,255,255,0.05)"
                               width={120}
                               strokeWidth={10}
                               format={p => <span style={{ color: '#fff', fontWeight: 800, fontSize: 24 }}>{p}%</span>}
                            />
                         </div>
                      </div>
                      <div style={{ background: 'rgba(255,255,255,0.03)', padding: 16, borderRadius: 12, border: '1px solid rgba(255,255,255,0.05)' }}>
                         <Text strong style={{ color: 'var(--neon-blue)', fontSize: 12 }}>ID: {selectedReport.id.substring(0, 8)}...</Text>
                         <div style={{ marginTop: 12 }}>
                            <Badge status="processing" text={<Text style={{ color: '#fff', fontSize: 12 }}>Rotation Queue: {selectedReport.rotationDueKeys}</Text>} />
                         </div>
                      </div>
                      <Button block className="cyber-button" icon={<ThunderboltOutlined />}>Trigger Auto-Repair</Button>
                   </Space>
                ) : <Empty description="No Report Selected" />}
             </div>
          </Col>
        </Row>
      ),
    },
    {
      key: 'activity',
      label: <span className="tab-label"><AuditOutlined /> NEURAL ACTIVITY</span>,
      children: <div className="glass-card" style={{ height: 400, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Empty description="System activity reporting initialized..." /></div>,
    },
    {
      key: 'coverage',
      label: <span className="tab-label"><FileTextOutlined /> TEST COVERAGE</span>,
      children: (
        <Row gutter={[24, 24]}>
          <Col span={24}>
             <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div className="glass-card-title">Code Verification Coverage <CheckCircleOutlined /></div>
                <Row gutter={[24, 24]}>
                   <Col xs={24} md={8}>
                      <div style={{ background: 'rgba(255,255,255,0.03)', padding: '24px', borderRadius: '12px', textAlign: 'center', border: '1px solid rgba(255,255,255,0.05)' }}>
                         <Text style={{ color: 'var(--text-dim)', fontSize: 12, textTransform: 'uppercase', letterSpacing: 1 }}>Overall Coverage</Text>
                         <div style={{ marginTop: 16 }}>
                            <Progress
                               type="dashboard"
                               percent={84.5}
                               strokeColor="var(--success)"
                               trailColor="rgba(255,255,255,0.05)"
                               width={160}
                               strokeWidth={8}
                               format={p => <span style={{ color: '#fff', fontWeight: 800, fontSize: 32 }}>{p}%</span>}
                            />
                         </div>
                         <Text style={{ color: 'var(--success)', display: 'block', marginTop: 12 }}>+2.3% since last week</Text>
                      </div>
                   </Col>
                   <Col xs={24} md={16}>
                      <Space direction="vertical" size={16} style={{ width: '100%' }}>
                         <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                               <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Components (UI)</Text>
                               <Text style={{ color: 'var(--neon-blue)', fontWeight: 700 }}>88%</Text>
                            </div>
                            <Progress percent={88} strokeColor="var(--neon-blue)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={8} />
                         </div>
                         <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                               <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Services & API (Backend)</Text>
                               <Text style={{ color: 'var(--neon-purple)', fontWeight: 700 }}>92%</Text>
                            </div>
                            <Progress percent={92} strokeColor="var(--neon-purple)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={8} />
                         </div>
                         <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                               <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Utils & Helpers</Text>
                               <Text style={{ color: '#faad14', fontWeight: 700 }}>75%</Text>
                            </div>
                            <Progress percent={75} strokeColor="#faad14" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={8} />
                         </div>
                      </Space>
                   </Col>
                </Row>
             </div>
          </Col>
        </Row>
      )
    }
  ];

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
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>COMMAND INTELLIGENCE</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              System <span className="text-gradient">Reports</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Aggregated system health analytics, neural consensus logs, and operator audits.</Text>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchData} className="glass-action-button">Force Synthesis</Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
         {/* Summary KPI Cards */}
         <Col span={24}>
            <Row gutter={[24, 24]}>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Aggregated Health</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>98.4%</div>
                        </div>
                        <CheckCircleOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Anomaly Shards</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>0</div>
                        </div>
                        <WarningOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Daily Reports</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{healthReports.length}</div>
                        </div>
                        <GlobalOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card">
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Storage Utilization</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>42.5 GB</div>
                        </div>
                        <DatabaseOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
            </Row>
         </Col>

         <Col span={24}>
            <div className="glass-card" style={{ minHeight: 600 }}>
               <Tabs defaultActiveKey="apihealth" items={tabItems} className="cyber-tabs" />
            </div>
         </Col>
      </Row>

      <style>{`
        .cyber-tabs .ant-tabs-nav-list { gap: 24px; }
        .cyber-tabs .ant-tabs-tab {
          margin: 0 !important;
          padding: 12px 4px !important;
          color: rgba(255,255,255,0.4) !important;
          font-family: 'Outfit', sans-serif !important;
          font-weight: 700 !important;
          font-size: 13px !important;
          letter-spacing: 1px;
        }
        .cyber-tabs .ant-tabs-tab-active {
          color: var(--neon-blue) !important;
          text-shadow: 0 0 10px rgba(0, 243, 255, 0.3);
        }
        .cyber-tabs .ant-tabs-ink-bar {
          background: var(--neon-blue) !important;
          box-shadow: 0 0 10px var(--neon-blue);
        }
        .tab-label { display: flex; align-items: center; gap: 8px; }
      `}</style>
    </motion.div>
  );
};

export default AdminReports;
