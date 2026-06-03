// AdminLearning.tsx - Cinematic Learning Management
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Tabs, message } from 'antd';
import { 
  BulbOutlined, 
  BookOutlined, 
  ExperimentOutlined,
  LinkOutlined,
  ReloadOutlined,
  CloudDownloadOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

// Modular Components
import LearningStatusOverview from '../components/learning/LearningStatusOverview';
import LearningModeControl from '../components/learning/LearningModeControl';
import AutonomousDefenseCard from '../components/learning/AutonomousDefenseCard';
import KnowledgeDomainsTab from '../components/learning/KnowledgeDomainsTab';
import LearningSourcesTab from '../components/learning/LearningSourcesTab';
import EvolutionProposalsTab from '../components/learning/EvolutionProposalsTab';

const { Title, Text } = Typography;

const AdminLearning: React.FC = () => {
  const [status, setStatus] = useState<any | null>(null);
  const [domains, setDomains] = useState<any[]>([]);
  const [recommendations, setRecommendations] = useState<any[]>([]);
  const [sources, setSources] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [statusResp, domainsResp, recsResp, sourcesResp] = await Promise.all([
        authUtils.fetchWithAuth('/api/admin/learning/status'),
        authUtils.fetchWithAuth('/api/admin/knowledge/domains'),
        authUtils.fetchWithAuth('/api/admin/knowledge/recommendations'),
        authUtils.fetchWithAuth('/api/admin/learning/sources'),
      ]);
      if (statusResp.ok) setStatus(await statusResp.json());
      if (domainsResp.ok) setDomains((await domainsResp.json()).data || []);
      if (recsResp.ok) setRecommendations((await recsResp.json()).data || []);
      if (sourcesResp.ok) setSources(await sourcesResp.json());
    } catch (error) {
      message.error('Failed to sync neural learning database');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleEmergencyPause = async () => {
    setActionLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/emergency-pause', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) { message.success(status?.emergencyPaused ? 'Learning resumed' : 'Learning paused'); fetchData(); }
      else message.error('Failed to toggle learning pause');
    } catch { message.error('Failed to toggle learning pause'); }
    finally { setActionLoading(false); }
  };

  const handleModeChange = async (mode: string) => {
    setActionLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/mode', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode })
      });
      if (res.ok) { message.success(`Mode changed to ${mode}`); fetchData(); }
      else message.error('Failed to change learning mode');
    } catch { message.error('Failed to change learning mode'); }
    finally { setActionLoading(false); }
  };

  const handleManualTrigger = async () => {
    setActionLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/manual-trigger', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) message.success('Neural training triggered');
      else message.error('Failed to trigger training');
    } catch { message.error('Failed to trigger training'); }
    finally { setActionLoading(false); }
  };

  const handleIntervalChange = async (interval: number) => {
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/interval', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ interval })
      });
      if (res.ok) { message.success(`Interval set to ${interval} minutes`); fetchData(); }
    } catch { message.error('Failed to update interval'); }
  };

  const handleCyberResearch = async (topic: string) => {
    setActionLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/cyber-research', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topic })
      });
      if (res.ok) message.success(`Cyber research started: ${topic}`);
      else message.error('Failed to start cyber research');
    } catch { message.error('Failed to start cyber research'); }
    finally { setActionLoading(false); }
  };

  const handleRunAudit = async () => {
    setActionLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/learning/audit', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) message.success('Self-audit completed');
      else message.error('Self-audit failed');
    } catch { message.error('Self-audit failed'); }
    finally { setActionLoading(false); }
  };

  const handleViewKnowledge = (id: string) => {
    // Navigate to knowledge detail or open modal
    window.open(`/admin/learning/knowledge/${id}`, '_blank');
  };

  const handleApprove = async (id: string) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/learning/proposals/${id}/approve`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) { message.success('Proposal approved'); fetchData(); }
      else message.error('Failed to approve proposal');
    } catch { message.error('Failed to approve proposal'); }
  };

  const handleDecline = async (id: string) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/learning/proposals/${id}/decline`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) { message.success('Proposal declined'); fetchData(); }
      else message.error('Failed to decline proposal');
    } catch { message.error('Failed to decline proposal'); }
  };

  const tabItems = [
    {
      key: 'status',
      label: <span className="tab-label"><ExperimentOutlined /> NEURAL MATRIX</span>,
      children: (
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <LearningStatusOverview status={status} onEmergencyPause={handleEmergencyPause} actionLoading={actionLoading} />
          <LearningModeControl currentMode={status?.mode} onModeChange={handleModeChange} onManualTrigger={handleManualTrigger} actionLoading={actionLoading} interval={status?.learningIntervalMinutes} onIntervalChange={handleIntervalChange} />
          <AutonomousDefenseCard status={status} onCyberResearch={handleCyberResearch} onRunAudit={handleRunAudit} actionLoading={actionLoading} />
        </Space>
      ),
    },
    {
      key: 'domains',
      label: <span className="tab-label"><BookOutlined /> KNOWLEDGE DOMAINS</span>,
      children: <KnowledgeDomainsTab domains={domains} onRefresh={fetchData} onViewKnowledge={handleViewKnowledge} />,
    },
    {
      key: 'sources',
      label: <span className="tab-label"><LinkOutlined /> SYNERGY SOURCES</span>,
      children: <LearningSourcesTab sources={sources} onRefresh={fetchData} />,
    },
    {
      key: 'evolution',
      label: <span className="tab-label"><BulbOutlined /> EVOLUTION PROPOSALS</span>,
      children: <EvolutionProposalsTab recommendations={recommendations} onApprove={handleApprove} onDecline={handleDecline} />,
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(188, 19, 254, 0.2)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <BulbOutlined style={{ color: 'var(--neon-purple)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-purple)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>AUTONOMOUS COGNITION</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              Learning <span className="text-gradient">Management</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Monitor AI neural training, expand knowledge domains, and authorize evolution paths.</Text>
          </Col>
          <Col>
            <Space>
              <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchData} className="glass-action-button">Refetch State</Button>
              <Button type="primary" icon={<CloudDownloadOutlined />} className="cyber-button">Manual Ingest</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Stats Summary */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Neural Stability</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>96.4%</div>
                       </div>
                       <ThunderboltOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Knowledge Nodes</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{domains.length * 120}</div>
                       </div>
                       <GlobalOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Active Agents</Text>
                          <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>24/24</div>
                       </div>
                       <SafetyCertificateOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Evolution Proposals</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{recommendations.length}</div>
                       </div>
                       <BulbOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Tabs Content */}
        <Col span={24}>
           <div className="glass-card" style={{ minHeight: 600 }}>
              {loading && !status ? (
                 <div style={{ height: 400, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Spin size="large" tip="SYNCING NEURAL KNOWLEDGE..." />
                 </div>
              ) : (
                 <Tabs
                   defaultActiveKey="status"
                   items={tabItems}
                   className="cyber-tabs"
                 />
              )}
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
          color: var(--neon-purple) !important;
          text-shadow: 0 0 10px rgba(188, 19, 254, 0.3);
        }
        .cyber-tabs .ant-tabs-ink-bar {
          background: var(--neon-purple) !important;
          box-shadow: 0 0 10px var(--neon-purple);
        }
        .tab-label { display: flex; align-items: center; gap: 8px; }
      `}</style>
    </motion.div>
  );
};

export default AdminLearning;
