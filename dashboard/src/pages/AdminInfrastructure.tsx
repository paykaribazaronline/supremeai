// AdminInfrastructure.tsx - Cinematic Infrastructure Concierge
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, List, Tag, Empty, Progress } from 'antd';
import { 
  CloudServerOutlined,
  BulbOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  RocketOutlined,
  ToolOutlined,
  SafetyOutlined,
  DatabaseOutlined,
  GlobalOutlined,
  ThunderboltOutlined,
  ControlOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

const { Title, Text, Paragraph } = Typography;

interface Advice {
    id: string;
    title: string;
    description: string;
    impact: 'HIGH' | 'MEDIUM' | 'LOW';
    category: 'COST' | 'PERFORMANCE' | 'SECURITY' | 'RELIABILITY';
    actionTaken: boolean;
    timestamp: string;
}

const AdminInfrastructure: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [advices, setAdvices] = useState<Advice[]>([]);
    const [generating, setGenerating] = useState(false);

    const fetchAdvice = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/infrastructure/advice');
            if (response.ok) {
                const result = await response.json();
                setAdvices(result.data || []);
            }
        } catch (err) {} finally { setLoading(false); }
    };

    useEffect(() => { fetchAdvice(); }, []);

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            style={{ maxWidth: '1400px', margin: '0 auto' }}
        >
            {/* Cinematic Header */}
            <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(16, 185, 129, 0.2)', paddingBottom: 24 }}>
                <Row justify="space-between" align="bottom">
                    <Col>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                            <CloudServerOutlined style={{ color: 'var(--success)', fontSize: 20 }} />
                            <Text style={{ color: 'var(--success)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>AUTONOMOUS CLUSTER</Text>
                        </div>
                        <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
                            Infrastructure <span style={{ color: 'var(--success)', textShadow: '0 0 10px rgba(16, 185, 129, 0.3)' }}>Concierge</span>
                        </Title>
                        <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>AI-powered node optimization, cost reduction strategies, and distributed cluster integrity.</Text>
                    </Col>
                    <Col>
                        <Button
                            type="primary"
                            icon={<SyncOutlined spin={generating} />}
                            onClick={fetchAdvice}
                            className="cyber-button"
                            style={{ background: 'var(--success)', border: 'none', color: '#000', fontWeight: 700 }}
                        >
                            RUN DIAGNOSTICS
                        </Button>
                    </Col>
                </Row>
            </div>

            <Row gutter={[24, 24]}>
                {/* Infrastructure KPIs */}
                <Col span={24}>
                   <Row gutter={[24, 24]}>
                      <Col xs={12} lg={6}>
                         <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                               <div>
                                  <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Uptime Rate</Text>
                                  <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>99.99%</div>
                               </div>
                               <CheckCircleOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                            </div>
                         </div>
                      </Col>
                      <Col xs={12} lg={6}>
                         <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                               <div>
                                  <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Global Nodes</Text>
                                  <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>48</div>
                               </div>
                               <GlobalOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                            </div>
                         </div>
                      </Col>
                      <Col xs={12} lg={6}>
                         <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                               <div>
                                  <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Daily Cost</Text>
                                  <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>$12.40</div>
                               </div>
                               <DatabaseOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                            </div>
                         </div>
                      </Col>
                      <Col xs={12} lg={6}>
                         <div className="glass-card">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                               <div>
                                  <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>System Risk</Text>
                                  <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>MINIMAL</div>
                               </div>
                               <SafetyOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                            </div>
                         </div>
                      </Col>
                   </Row>
                </Col>

                {/* Main Content */}
                <Col xs={24} lg={16}>
                    <div className="glass-card" style={{ minHeight: 600 }}>
                        <div className="glass-card-title">Optimization Roadmap <BulbOutlined style={{ color: '#f59e0b' }} /></div>

                        {loading ? <div style={{ padding: 100, textAlign: 'center' }}><Spin size="large" tip="ANALYZING CLUSTER TOPOLOGY..." /></div> :
                         advices.length > 0 ? (
                            <List
                                dataSource={advices}
                                renderItem={(item) => (
                                    <div className="glass-card" style={{ padding: 20, marginBottom: 12, background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}>
                                        <div style={{ display: 'flex', gap: 16 }}>
                                            <div style={{ width: 48, height: 48, borderRadius: 12, background: 'rgba(16, 185, 129, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--success)', fontSize: 20 }}>
                                                <RocketOutlined />
                                            </div>
                                            <div style={{ flex: 1 }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                    <Text strong style={{ color: '#fff', fontSize: 15 }}>{item.title}</Text>
                                                    <Tag color={item.impact === 'HIGH' ? 'red' : 'blue'}>{item.impact} IMPACT</Tag>
                                                </div>
                                                <Paragraph style={{ color: 'var(--text-dim)', marginTop: 4, fontSize: 13 }}>{item.description}</Paragraph>
                                                <Button type="link" className="text-gradient" style={{ padding: 0, fontWeight: 700 }}>IMPLEMENT OPTIMIZATION →</Button>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            />
                        ) : <Empty description="System running at peak efficiency." />}
                    </div>
                </Col>

                {/* Right Sidebar */}
                <Col xs={24} lg={8}>
                   <Space direction="vertical" size={24} style={{ width: '100%' }}>
                      <div className="glass-card" style={{ borderTop: '2px solid var(--success)' }}>
                         <div className="glass-card-title">Integrity Guard <SafetyOutlined /></div>
                         <div style={{ padding: '8px 0' }}>
                            {[
                               { label: 'Environment', val: 'STABLE', color: 'var(--success)' },
                               { label: 'Database Sync', val: '99.9%', color: 'var(--success)' },
                               { label: 'Cloud Latency', val: '124ms', color: 'var(--neon-blue)' }
                            ].map((row, i) => (
                               <div key={i} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                                  <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>{row.label}</Text>
                                  <Text style={{ color: row.color, fontWeight: 800, fontSize: 12 }}>{row.val}</Text>
                               </div>
                            ))}
                         </div>
                      </div>

                      <div className="glass-card" style={{ background: 'rgba(0, 243, 255, 0.05)' }}>
                         <div className="glass-card-title">Neural Thresholds <ThunderboltOutlined /></div>
                         <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                            Cluster scaling policies are currently optimized for cost-efficiency. Peak load expected in 4h.
                         </Text>
                         <Button block className="glass-action-button" style={{ marginTop: 16 }}>View Scaling Policies</Button>
                      </div>

                      <div className="glass-card" style={{ textAlign: 'center' }}>
                         <Text style={{ color: 'rgba(255,255,255,0.3)', fontSize: 10, textTransform: 'uppercase', letterSpacing: 2 }}>Cluster Signature</Text>
                         <div style={{ color: '#fff', fontFamily: 'JetBrains Mono', fontSize: 11, marginTop: 8 }}>NODE-ID: SUPREME-PRO-01</div>
                      </div>
                   </Space>
                </Col>
            </Row>
        </motion.div>
    );
};

export default AdminInfrastructure;
