// SystemLearningDashboard.tsx - Recursive Knowledge Acquisition System
import { useState, useEffect } from 'react';
import { Table, Button, Space, Form, Input, Select, message, Progress, Modal, Card, Row, Col, List, Tag, Popconfirm, Badge } from 'antd';
import { PlusOutlined, CheckOutlined, CloseOutlined, SyncOutlined, TagOutlined, ThunderboltOutlined, DeleteOutlined, DatabaseOutlined, BulbOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import { motion, AnimatePresence } from 'framer-motion';
import { useSystemWebSocket } from '../hooks/useSystemWebSocket';

interface KnowledgeDomain {
  id: string;
  name: string;
  keywords: string[];
  status: 'IDLE' | 'LEARNING' | 'COMPLETE' | 'PAUSED';
  lastUpdated: string;
  depth: number;
  nodesDiscovered: number;
  averageConfidence: number;
}

interface KnowledgeRecommendation {
  id: string;
  topic: string;
  reasoning: string;
  confidence: number;
  keywords: string[];
  status: 'PENDING' | 'APPROVED' | 'DECLINED';
  createdAt: string;
}

interface KnowledgeSnapshot {
  totalKnowledgeNodes: number;
  topLearningDomains: string[];
  lastDiscoveryTime: string | null;
  discoveryEfficiency: string;
}

export default function SystemLearningDashboard() {
  const [domains, setDomains] = useState<KnowledgeDomain[]>([]);
  const [recommendations, setRecommendations] = useState<KnowledgeRecommendation[]>([]);
  const [snapshot, setSnapshot] = useState<KnowledgeSnapshot | null>(null);
  const [recentKnowledge, setRecentKnowledge] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [learningProgress, setLearningProgress] = useState<Record<string, number>>({});
  const [newDomainModal, setNewDomainModal] = useState(false);
  const [newDomainForm] = Form.useForm();
  const [crawlLogs, setCrawlLogs] = useState<string[]>([]);
  
  const { lastMessage } = useSystemWebSocket();

  useEffect(() => {
    fetchSnapshot();
    fetchDomains();
    fetchRecommendations();
    fetchRecentKnowledge();
    const interval = setInterval(() => {
      fetchSnapshot();
      fetchDomains();
      fetchRecommendations();
      fetchRecentKnowledge();
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  const fetchRecentKnowledge = async () => {
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/knowledge/recent?limit=5');
      const data = await response.json();
      if (data.success) setRecentKnowledge(data.data || []);
    } catch (e) { console.error('Knowledge sync failed'); }
  };

  useEffect(() => {
    if (lastMessage?.type === 'KNOWLEDGE_CRAWL') {
      const domainId = lastMessage.domainId as string;
      const progress = lastMessage.progress as number;
      setLearningProgress(prev => ({ ...prev, [domainId]: progress }));
      setCrawlLogs(prev => [`[${new Date().toLocaleTimeString()}] NODE_LINKED: ${lastMessage.fact}`, ...prev.slice(0, 49)]);
      if (progress >= 100) fetchSnapshot();
    }
  }, [lastMessage]);

  const fetchSnapshot = async () => {
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/knowledge/snapshot');
      const data = await response.json() as any;
      if (data.success && data.data) {
        setSnapshot(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch snapshot:', error);
    }
  };

  const fetchDomains = async () => {
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/knowledge/domains');
      const data = await response.json() as any;
      if (data.success && data.data) {
        setDomains(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch domains:', error);
    }
  };

  const fetchRecommendations = async () => {
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/knowledge/recommendations');
      const data = await response.json() as any;
      if (data.success && data.data) {
        setRecommendations(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch recommendations:', error);
    }
  };

  const handleStartLearning = async (domainId: string) => {
    setLoading(true);
    message.loading({ content: 'INITIATING_AUTONOMOUS_CRAWL...', key: 'learning' });
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/knowledge/domains/${domainId}/process`, {
        method: 'POST'
      });
      const data = await response.json() as any;
      if (data.success) {
        message.success({ content: `SUCCESS: Processed ${data.data.factsDiscovered} Nodes`, key: 'learning' });
        fetchDomains();
        fetchSnapshot();
      }
    } catch (error) {
      message.error({ content: 'LEARNING_PROTOCOL_FAILED', key: 'learning' });
    } finally {
      setLoading(false);
    }
  };

  const handleAddDomain = async (values: any) => {
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/knowledge/domains?name=' + encodeURIComponent(values.name), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values.keywords || [])
      });
      const data = await response.json() as any;
      if (data.success) {
        message.success('DOMAIN_ACQUIRED');
        setNewDomainModal(false);
        newDomainForm.resetFields();
        fetchDomains();
      }
    } catch (error) {
      message.error('DOMAIN_REGISTRATION_ERROR');
    }
  };

  const handleApproveRecommendation = async (rec: KnowledgeRecommendation) => {
    try {
      const response = await authUtils.fetchWithAuth(
        `/api/admin/knowledge/recommendations/${rec.id}/approve?domainName=${encodeURIComponent(rec.topic)}`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(rec.keywords || []) }
      );
      const data = await response.json() as any;
      if (data.success) {
        message.success('PROPOSAL_ACCEPTED: ADDED_TO_MATRIX');
        fetchRecommendations();
        fetchDomains();
      }
    } catch (error) {
      message.error('PROPOSAL_PROCESSING_ERROR');
    }
  };

  const handleDeclineRecommendation = async (recId: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/knowledge/recommendations/${recId}/decline`, {
        method: 'POST'
      });
      const data = await response.json() as any;
      if (data.success) {
        message.info('PROPOSAL_DECLINED');
        fetchRecommendations();
      }
    } catch (error) {
      message.error('DECLINE_PROTOCOL_ERROR');
    }
  };

  const domainColumns = [
    {
      title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Domain</span>,
      key: 'domain',
      render: (_: any, r: KnowledgeDomain) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded bg-white/[0.03] border border-white/5 flex items-center justify-center relative">
            <TagOutlined className="text-amber-500 text-[12px]" />
            {r.status === 'LEARNING' && <div className="absolute -top-1 -right-1 w-2 h-2 bg-amber-500 rounded-full animate-ping" />}
          </div>
          <div className="flex flex-col leading-tight">
            <span className="text-[11px] font-bold text-white/90 uppercase">{r.name}</span>
            <div className="flex gap-1">
              {r.keywords?.slice(0, 2).map(k => <span key={k} className="text-[7px] text-white/20 font-mono uppercase">#{k}</span>)}
            </div>
          </div>
        </div>
      )
    },
    {
      title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Discovery Status</span>,
      dataIndex: 'status',
      key: 'status',
      render: (status: string, r: KnowledgeDomain) => (
        <div className="flex flex-col gap-1">
          <span className={`text-[8px] px-1.5 py-0.5 rounded-sm font-black uppercase tracking-widest border inline-block w-fit ${
            status === 'COMPLETE' ? 'text-emerald-500 border-emerald-500/20 bg-emerald-500/5' :
            status === 'LEARNING' ? 'text-amber-500 border-amber-500/20 bg-amber-500/5' :
            'text-white/20 border-white/5'
          }`}>
            {status}
          </span>
          {status === 'LEARNING' && (
            <div className="w-16 h-[2px] bg-white/5 rounded-full overflow-hidden">
              <div className="h-full bg-amber-500 animate-shimmer" style={{ width: `${learningProgress[r.id] || 30}%` }} />
            </div>
          )}
        </div>
      )
    },
    {
      title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Intel Nodes</span>,
      key: 'nodes',
      render: (_: any, r: KnowledgeDomain) => (
        <div className="flex flex-col">
          <span className="text-[11px] font-mono text-emerald-500/80">{r.nodesDiscovered || 0}</span>
          <span className="text-[7px] text-white/10 uppercase font-black">Captured Nodes</span>
        </div>
      )
    },
    {
      title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">Operation</span>,
      key: 'action',
      align: 'right' as const,
      render: (_: any, record: KnowledgeDomain) => (
        <Space size={4}>
          <Button
            type="text"
            size="small"
            className="h-6 px-3 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-500 border border-emerald-500/20 text-[9px] font-black uppercase"
            onClick={() => handleStartLearning(record.id)}
            disabled={loading || record.status === 'LEARNING'}
            icon={<ThunderboltOutlined />}
          >
            {record.status === 'LEARNING' ? 'Syncing' : 'Activate'}
          </Button>
          <Popconfirm title="Delete Domain?" onConfirm={() => {}}>
            <Button 
                type="text" 
                size="small" 
                className="h-6 w-6 flex items-center justify-center text-red-500/30 hover:text-red-500"
                icon={<DeleteOutlined style={{ fontSize: '10px' }} />} 
            />
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div className="space-y-6">
      {/* Knowledge Core Stats */}
      <div className="grid grid-cols-4 gap-4">
          {[
            { label: 'Neural Base Facts', value: snapshot?.totalKnowledgeNodes || 0, sub: 'Verified Data Points', color: 'emerald', icon: <DatabaseOutlined /> },
            { label: 'Active Domains', value: domains.length, sub: 'Subject Areas', color: 'amber', icon: <BulbOutlined /> },
            { label: 'Crawl Velocity', value: snapshot?.discoveryEfficiency || '0%', sub: 'Nodes/Minute', color: 'blue', icon: <SyncOutlined /> },
            { label: 'Registry Sync', value: 'OPTIMAL', sub: 'Last: ' + (snapshot?.lastDiscoveryTime ? 'Active' : 'Idle'), color: 'purple', icon: <SafetyCertificateOutlined /> }
          ].map((s, i) => (
            <div key={i} className={`bg-white/[0.02] border-l-2 border-${s.color}-500/50 p-4 rounded-xl relative group hover:bg-white/[0.04] transition-all`}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[8px] font-black uppercase tracking-widest text-white/30">{s.label}</span>
                <span className={`text-[12px] text-${s.color}-500/50 group-hover:text-${s.color}-500`}>{s.icon}</span>
              </div>
              <div className="text-xl font-mono font-black text-white">{s.value}</div>
              <div className="text-[8px] text-white/10 uppercase font-bold mt-1 tracking-tighter">{s.sub}</div>
            </div>
          ))}
      </div>

      <div className="grid grid-cols-3 gap-6">
          {/* Main Domain Matrix */}
          <div className="col-span-2 space-y-6">
              <div className="bg-white/[0.02] border border-white/5 rounded-xl overflow-hidden">
                <div className="px-5 py-3 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                  <div className="flex flex-col">
                    <span className="text-[10px] font-black uppercase tracking-[0.2em] text-white/80">Knowledge Acquisition Matrix</span>
                    <span className="text-[8px] text-white/20 uppercase font-bold">Autonomous Domain Crawler v6.0</span>
                  </div>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="small"
                    className="bg-emerald-500 text-black font-black uppercase tracking-widest border-none h-8 px-4"
                    onClick={() => setNewDomainModal(true)}
                  >
                    Add Target
                  </Button>
                </div>
                <Table
                  dataSource={domains}
                  columns={domainColumns}
                  rowKey="id"
                  size="small"
                  pagination={{ pageSize: 5 }}
                  className="dense-table"
                />
              </div>

              {/* Suggestions Panel */}
              <div className="bg-white/[0.02] border border-white/5 rounded-xl overflow-hidden">
                <div className="px-5 py-3 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-white/80">Neural Proposals</span>
                  <Badge count={recommendations.length} style={{ backgroundColor: '#722ed1', fontSize: '8px' }} />
                </div>
                <div className="p-4 grid grid-cols-2 gap-4">
                  {recommendations.length === 0 ? (
                    <div className="col-span-2 py-8 text-center opacity-10 italic uppercase text-[10px]">Awaiting next intelligence cycle...</div>
                  ) : (
                    recommendations.slice(0, 4).map((rec) => (
                      <div key={rec.id} className="p-4 bg-white/[0.02] border border-white/5 rounded-xl group hover:border-purple-500/30 transition-all relative">
                        <div className="flex justify-between items-start mb-2">
                           <span className="text-[11px] font-black text-white/80 uppercase">{rec.topic}</span>
                           <span className="text-[8px] font-mono text-purple-400">{Math.round(rec.confidence * 100)}% Match</span>
                        </div>
                        <p className="text-[9px] text-white/30 line-clamp-2 leading-relaxed mb-4">{rec.reasoning}</p>
                        <div className="flex gap-2">
                          <button onClick={() => handleApproveRecommendation(rec)} className="flex-1 h-6 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-500 border border-emerald-500/20 rounded text-[8px] font-black uppercase">Approve</button>
                          <button onClick={() => handleDeclineRecommendation(rec.id)} className="w-12 h-6 bg-white/5 hover:bg-red-500/10 hover:text-red-500 border border-white/10 rounded text-[8px] font-black uppercase transition-all">Reject</button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
          </div>

          {/* Sidebar: Knowledge Stream & Snapshot */}
          <div className="space-y-6">
              {/* Actual Knowledge Snapshot */}
              <div className="bg-white/[0.02] border border-white/5 rounded-xl p-5">
                <div className="flex items-center gap-2 mb-4 border-b border-white/5 pb-2">
                   <SafetyCertificateOutlined className="text-emerald-500 text-[14px]" />
                   <span className="text-[10px] font-black uppercase tracking-widest text-white/80">Stored Intel Snapshot</span>
                </div>
                <div className="space-y-4">
                   {recentKnowledge.length === 0 ? (
                      <div className="py-4 text-center text-[9px] text-white/10 italic">Registry synchronization in progress...</div>
                   ) : (
                      recentKnowledge.map((k, i) => (
                        <div key={i} className="flex flex-col gap-1 border-b border-white/[0.02] pb-2 last:border-0">
                           <span className="text-[10px] text-white/70 font-mono leading-tight">{k.content || k.fact}</span>
                           <div className="flex justify-between items-center">
                              <span className="text-[7px] text-white/20 uppercase font-black tracking-tighter">Source: {k.source || 'Neural_Crawler'}</span>
                              <span className="text-[7px] font-mono text-emerald-500/40">{new Date(k.timestamp || Date.now()).toLocaleTimeString()}</span>
                           </div>
                        </div>
                      ))
                   )}
                </div>
              </div>

              {/* Crawl Terminal */}
              <div className="bg-[#050505] border border-white/5 rounded-xl overflow-hidden h-[300px] flex flex-col">
                <div className="px-4 py-2 bg-white/[0.03] border-b border-white/5 flex items-center justify-between">
                   <span className="text-[9px] font-black uppercase tracking-widest text-amber-500/80">Live Crawl Stream</span>
                   <div className="flex gap-1">
                      <div className="w-1 h-1 rounded-full bg-red-500 animate-pulse"></div>
                      <div className="w-1 h-1 rounded-full bg-amber-500"></div>
                      <div className="w-1 h-1 rounded-full bg-emerald-500"></div>
                   </div>
                </div>
                <div className="flex-1 p-4 font-mono text-[9px] text-emerald-500/60 overflow-y-auto custom-scrollbar bg-[radial-gradient(circle_at_top,_rgba(16,185,129,0.05)_0%,_transparent_100%)]">
                   {crawlLogs.length === 0 ? (
                      <div className="opacity-20 uppercase py-20 text-center">System Idle - Awaiting Trigger</div>
                   ) : (
                      crawlLogs.map((log, i) => (
                        <div key={i} className="mb-1.5 opacity-80 hover:opacity-100 transition-opacity border-l border-white/5 pl-2">{log}</div>
                      ))
                   )}
                </div>
              </div>
          </div>
      </div>

      {/* Add Domain Modal */}
      <Modal
        title={<span className="text-white text-[12px] font-black uppercase tracking-widest">Register New Learning Domain</span>}
        open={newDomainModal}
        onCancel={() => setNewDomainModal(false)}
        footer={null}
        className="dark-modal"
        styles={{ body: { background: '#050505', padding: '16px' } }}
      >
        <Form form={newDomainForm} layout="vertical" onFinish={handleAddDomain}>
          <Form.Item
            name="name"
            label={<span className="text-white/40 text-[9px] font-black uppercase tracking-widest">Domain Name</span>}
            rules={[{ required: true, message: 'Domain name required' }]}
          >
            <Input
              className="dark-input font-mono"
              placeholder="e.g., React 19 Best Practices"
            />
          </Form.Item>
          <Form.Item
            name="keywords"
            label={<span className="text-white/40 text-[9px] font-black uppercase tracking-widest">Keywords (comma-separated)</span>}
          >
            <Select
              mode="tags"
              className="dark-select"
              placeholder="Add relevant keywords"
            />
          </Form.Item>
          <Button
            type="primary"
            block
            icon={<PlusOutlined />}
            htmlType="submit"
            className="bg-emerald-500 text-black h-12 font-black uppercase tracking-widest"
          >
            Register Domain
          </Button>
        </Form>
      </Modal>
    </div>
  );
}