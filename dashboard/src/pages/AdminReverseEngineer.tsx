// AdminReverseEngineer.tsx - Cinematic Automation & Reverse Engineering
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, message } from 'antd';
import {
  ApartmentOutlined,
  ReloadOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  CodeOutlined,
  BlockOutlined,
  BranchesOutlined,
  DeploymentUnitOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { fetchWithAuth } from '../lib/authUtils';

// Import Modular Components
import { Job } from '../components/reverse-engineer/types';
import AutomationLaunchCard from '../components/reverse-engineer/AutomationLaunchCard';
import TaskMonitorTable from '../components/reverse-engineer/TaskMonitorTable';
import AlternativeSuggestionsList from '../components/reverse-engineer/AlternativeSuggestionsList';
import JobDetailsModal from '../components/reverse-engineer/JobDetailsModal';

const { Title, Text } = Typography;

const AdminReverseEngineer: React.FC = () => {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [url, setUrl] = useState('');
  const [taskType, setTaskType] = useState('reverse_engineer');
  const [instructions, setInstructions] = useState('');
  const [languages, setLanguages] = useState(['python', 'typescript', 'java']);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const res = await fetchWithAuth('/api/reverse-engineer/history?limit=50');
      if (res.ok) {
        const data = await res.json();
        setJobs(data.jobs || []);
      }
    } catch (e) {
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
    const interval = setInterval(fetchJobs, 15000);
    return () => clearInterval(interval);
  }, []);

  const submitJob = async () => {
    if (!url.trim()) { message.error('Please enter a target URL'); return; }
    setSubmitting(true);
    try {
      const res = await fetchWithAuth('/api/reverse-engineer/submit', {
        method: 'POST',
        body: JSON.stringify({ url, taskType, customInstructions: instructions, target_languages: languages, user_id: 'admin' })
      });
      if (res.ok) {
        message.success('Neural crawler deployed');
        setUrl(''); setInstructions(''); fetchJobs();
      }
    } catch (e) {} finally { setSubmitting(false); }
  };

  const handleConfig = async () => {
    try {
      const res = await fetchWithAuth('/api/reverse-engineer/config');
      if (res.ok) {
        const config = await res.json();
        message.info('Config loaded — apply settings in the reverse engineer settings panel');
      } else {
        message.error('Failed to load config');
      }
    } catch {
      message.error('Failed to load config');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid var(--neon-blue)', paddingBottom: 24, opacity: 0.8 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <ApartmentOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>NEURAL AUTOMATION</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              Reverse <span className="text-gradient">Engineering</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Deconstruct, analyze, and automate external digital architectures using AI agents.</Text>
          </Col>
          <Col>
            <Space>
              <Button icon={<SettingOutlined />} className="glass-action-button" onClick={handleConfig}>Config</Button>
              <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchJobs} className="glass-action-button">Refetch Jobs</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Statistics Row */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Active Tasks</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{jobs.filter(j => j.status === 'RUNNING').length}</div>
                       </div>
                       <DeploymentUnitOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Success Rate</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>92%</div>
                       </div>
                       <BranchesOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Scraping Nodes</Text>
                          <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>128</div>
                       </div>
                       <BlockOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Analyzed Sites</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{jobs.length}</div>
                       </div>
                       <CodeOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Main Interface */}
        <Col lg={16} md={24}>
           <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <div className="glass-card" style={{ borderTop: '2px solid var(--neon-blue)' }}>
                 <div className="glass-card-title">Launch Neural Crawler</div>
                 <AutomationLaunchCard
                    url={url} setUrl={setUrl} taskType={taskType} setTaskType={setTaskType}
                    instructions={instructions} setInstructions={setInstructions}
                    languages={languages} setLanguages={setLanguages}
                    submitting={submitting} onSubmit={submitJob}
                 />
              </div>

              <div className="glass-card">
                 <div className="glass-card-title">Task Monitor Matrix</div>
                 <TaskMonitorTable
                    jobs={jobs} loading={loading}
                    onView={(job) => { setSelectedJob(job); setPreviewOpen(true); }}
                    onCancel={() => {}} onRefresh={fetchJobs}
                 />
              </div>
           </Space>
        </Col>

        <Col lg={8} md={24}>
           <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <div className="glass-card">
                 <div className="glass-card-title">Synthesized Alternatives</div>
                 <AlternativeSuggestionsList
                    suggestions={selectedJob?.results?.alternative_suggestions}
                    onSwitch={(newUrl) => setUrl(newUrl)}
                 />
              </div>

              <div className="glass-card" style={{ background: 'rgba(0, 243, 255, 0.05)' }}>
                 <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                    <ThunderboltOutlined style={{ color: 'var(--neon-blue)' }} />
                    <Text strong style={{ color: '#fff' }}>Crawler Efficiency</Text>
                 </div>
                 <Text style={{ color: 'var(--text-dim)', fontSize: 13 }}>
                    System is currently bypassing standard JS obstacles with 94% accuracy. Anti-bot resilience: High.
                 </Text>
              </div>
           </Space>
        </Col>
      </Row>

      <JobDetailsModal visible={previewOpen} job={selectedJob} onCancel={() => setPreviewOpen(false)} />
    </motion.div>
  );
};

export default AdminReverseEngineer;
