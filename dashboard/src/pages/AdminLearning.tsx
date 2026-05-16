import React, { useState, useEffect } from 'react';
import { 
  Typography, 
  Space, 
  Tabs, 
  message, 
  Spin 
} from 'antd';
import { 
  BulbOutlined, 
  BookOutlined, 
  ExperimentOutlined,
  LinkOutlined
} from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
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
      if (domainsResp.ok) {
        const dData = await domainsResp.json();
        setDomains(dData.data || []);
      }
      if (recsResp.ok) {
        const rData = await recsResp.json();
        setRecommendations(rData.data || []);
      }
      if (sourcesResp.ok) {
        const sData = await sourcesResp.json();
        setSources(sData.length ? sData : []);
      }
    } catch (error) {
      console.error('Error fetching learning data:', error);
      message.error('লার্নিং ডাটা লোড করতে সমস্যা হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleModeChange = async (mode: string) => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth('/api/admin/learning/mode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode })
      });
      if (resp.ok) {
        message.success(`লার্নিং মোড পরিবর্তন হয়েছে: ${mode}`);
        fetchData();
      }
    } catch (error) {
      message.error('একটি ত্রুটি ঘটেছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleIntervalChange = async (interval: number) => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth('/api/admin/learning/interval', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ interval })
      });
      if (resp.ok) {
        message.success(`লার্নিং ইন্টারভাল সেট করা হয়েছে: ${interval} মিনিট`);
        fetchData();
      }
    } catch (error) {
      message.error('ইন্টারভাল পরিবর্তন ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleManualTrigger = async () => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth('/api/admin/learning/trigger', { method: 'POST' });
      if (resp.ok) message.success('লার্নিং সাইকেল শুরু হয়েছে');
    } catch (error) {
      message.error('সার্ভার এরর');
    } finally {
      setActionLoading(false);
    }
  };

  const handleEmergencyPause = async () => {
    setActionLoading(true);
    try {
      const endpoint = status?.emergencyPaused ? '/api/admin/learning/resume' : '/api/admin/learning/emergency-pause';
      const resp = await authUtils.fetchWithAuth(endpoint, { method: 'POST' });
      if (resp.ok) {
        message.success(status?.emergencyPaused ? 'সিস্টেম রেজুম হয়েছে' : 'সিস্টেম পজ করা হয়েছে');
        fetchData();
      }
    } catch (error) {
      message.error('অপারেশন ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCyberResearch = async (topic: string) => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth(`/api/system-learning/cyber-research?topic=${encodeURIComponent(topic)}`, {
        method: 'POST'
      });
      if (resp.ok) {
        message.success(`সিস্টেম ${topic} নিয়ে গবেষণা শুরু করেছে`);
        fetchData();
      }
    } catch (error) {
      message.error('সাইবার রিসার্চ ট্রিগার ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRunAudit = async () => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth('/api/exploitation-techniques/audit', { method: 'POST' });
      if (resp.ok) {
        const result = await resp.json();
        message.success(`অডিট সম্পন্ন: রেজিলিয়েন্স স্কোর ${Math.round(result.resilienceScore * 100)}%`);
      }
    } catch (error) {
      message.error('অডিট ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleApproveRecommendation = async (id: string) => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth(`/api/admin/knowledge/recommendations/${id}/approve`, {
        method: 'POST'
      });
      if (resp.ok) {
        message.success('রিকমেন্ডেশন অ্যাপ্রুভ করা হয়েছে এবং নতুন ডোমেইন তৈরি হয়েছে');
        fetchData();
      }
    } catch (error) {
      message.error('অপারেশন ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeclineRecommendation = async (id: string) => {
    setActionLoading(true);
    try {
      const resp = await authUtils.fetchWithAuth(`/api/admin/knowledge/recommendations/${id}/decline`, {
        method: 'POST'
      });
      if (resp.ok) {
        message.success('রিকমেন্ডেশন ডিক্লাইন করা হয়েছে');
        fetchData();
      }
    } catch (error) {
      message.error('অপারেশন ব্যর্থ হয়েছে');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <AdminLayout title="লার্নিং ম্যানেজমেন্ট">
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ fontWeight: 700, margin: 0 }}>লার্নিং ড্যাশবোর্ড</Title>
        <Text type="secondary">AI মডেল লার্নিং, নলেজ বেস এবং সিস্টেম ইমপ্রুভমেন্ট কন্ট্রোল</Text>
      </div>

      <Tabs defaultActiveKey="status" className="modern-tabs">
        <Tabs.TabPane 
          tab={<span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><ExperimentOutlined /> Neural Skill Matrix</span>} 
          key="status"
        >
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center' }}><Spin size="large" /></div>
          ) : (
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              {/* Suggestions moved to centralized Approvals tab */}
              
              <LearningStatusOverview 
                status={status} 
                onEmergencyPause={handleEmergencyPause} 
                actionLoading={actionLoading} 
              />

              <LearningModeControl 
                currentMode={status?.mode}
                onModeChange={handleModeChange}
                onManualTrigger={handleManualTrigger}
                actionLoading={actionLoading}
                interval={status?.learningIntervalMinutes}
                onIntervalChange={handleIntervalChange}
              />

              <AutonomousDefenseCard 
                status={status}
                onCyberResearch={handleCyberResearch}
                onRunAudit={handleRunAudit}
                actionLoading={actionLoading}
              />
            </Space>
          )}
        </Tabs.TabPane>

        <Tabs.TabPane 
          tab={<span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><BookOutlined /> Knowledge Domains</span>} 
          key="domains"
        >
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center' }}><Spin size="large" /></div>
          ) : (
            <KnowledgeDomainsTab 
              domains={domains} 
              onRefresh={fetchData} 
              onViewKnowledge={(id) => message.info(`Viewing knowledge for ${id}`)} 
            />
          )}
        </Tabs.TabPane>

        <Tabs.TabPane
          tab={<span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><LinkOutlined /> Learning Sources</span>}
          key="sources"
        >
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center' }}><Spin size="large" /></div>
          ) : (
            <LearningSourcesTab sources={sources} onRefresh={fetchData} />
          )}
        </Tabs.TabPane>

        <Tabs.TabPane
          tab={<span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><BulbOutlined /> Evolution Proposals</span>}
          key="recommendations"
        >
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center' }}><Spin size="large" /></div>
          ) : (
            <EvolutionProposalsTab
              recommendations={recommendations}
              onApprove={handleApproveRecommendation}
              onDecline={handleDeclineRecommendation}
            />
          )}
        </Tabs.TabPane>
      </Tabs>
    </AdminLayout>
  );
}

export default AdminLearning;
