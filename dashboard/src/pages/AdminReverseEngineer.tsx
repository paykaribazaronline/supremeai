// AdminReverseEngineer.tsx - Universal Automation & Reverse Engineering (Modularized)

import React, { useState, useEffect } from 'react';
import { Typography, message, Row, Col, Button, Space } from 'antd';
import { ReloadOutlined, ApartmentOutlined, SettingOutlined } from '@ant-design/icons';
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
      message.error('Failed to load jobs');
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
    if (!url.trim()) {
      message.error('Please enter a target URL');
      return;
    }
    setSubmitting(true);
    try {
      const res = await fetchWithAuth('/api/reverse-engineer/submit', {
        method: 'POST',
        body: JSON.stringify({
          url,
          taskType,
          customInstructions: instructions,
          target_languages: languages,
          user_id: 'admin'
        })
      });
      if (res.ok) {
        message.success('Automation task submitted successfully');
        setUrl('');
        setInstructions('');
        fetchJobs();
      } else {
        message.error('Failed to submit task');
      }
    } catch (e) {
      message.error('Network error');
    } finally {
      setSubmitting(false);
    }
  };

  const cancelJob = async (jobId: string) => {
    try {
      const res = await fetchWithAuth(`/api/reverse-engineer/job/${jobId}`, { method: 'DELETE' });
      if (res.ok) {
        message.success('Task cancelled');
        fetchJobs();
      }
    } catch (e) {
      message.error('Failed to cancel');
    }
  };

  return (
    <div className="admin-container" style={{ padding: '24px', background: 'transparent' }}>
      <Row gutter={[24, 24]} align="middle" style={{ marginBottom: 32 }}>
        <Col span={16}>
          <Title level={2} style={{ margin: 0, fontWeight: 800, color: '#fff', letterSpacing: '-0.5px' }}>
            <ApartmentOutlined style={{ marginRight: 12, color: '#1890ff' }} />
            Universal Automation & Reverse Engineering
          </Title>
          <Text type="secondary" style={{ fontSize: 16 }}>
            Command SupremeAI to analyze, scrape, or automate any website with AI-driven precision.
          </Text>
        </Col>
        <Col span={8} style={{ textAlign: 'right' }}>
          <Space>
            <Button icon={<SettingOutlined />} className="glass-button">Config</Button>
            <Button type="primary" icon={<ReloadOutlined />} onClick={fetchJobs} loading={loading}>Refresh Monitor</Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={[24, 24]}>
        <Col lg={16} md={24}>
          <AutomationLaunchCard 
            url={url}
            setUrl={setUrl}
            taskType={taskType}
            setTaskType={setTaskType}
            instructions={instructions}
            setInstructions={setInstructions}
            languages={languages}
            setLanguages={setLanguages}
            submitting={submitting}
            onSubmit={submitJob}
          />

          <TaskMonitorTable 
            jobs={jobs}
            loading={loading}
            onView={(job) => {
              setSelectedJob(job);
              setPreviewOpen(true);
            }}
            onCancel={cancelJob}
            onRefresh={fetchJobs}
          />
        </Col>

        <Col lg={8} md={24}>
          <AlternativeSuggestionsList 
            suggestions={selectedJob?.results?.alternative_suggestions}
            onSwitch={(newUrl) => setUrl(newUrl)}
          />
        </Col>
      </Row>

      <JobDetailsModal 
        visible={previewOpen}
        job={selectedJob}
        onCancel={() => setPreviewOpen(false)}
      />

      <style>{`
        .glass-card {
          background: rgba(255, 255, 255, 0.05) !important;
          backdrop-filter: blur(20px);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 16px;
          color: #fff !important;
        }
        .glass-card .ant-card-head {
          border-bottom: 1px solid rgba(255, 255, 255, 0.1);
          color: #fff;
        }
        .instruction-box {
          background: rgba(0, 0, 0, 0.2);
          padding: 16px;
          border-radius: 8px;
          border-left: 4px solid #1890ff;
          font-style: italic;
        }
        .code-preview {
          background: #1e1e1e;
          color: #d4d4d4;
          padding: 16px;
          border-radius: 12px;
          overflow: auto;
          font-family: 'Fira Code', monospace;
        }
        .premium-shadow {
          box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
        }
        .ant-table {
          background: transparent !important;
          color: #fff !important;
        }
        .ant-table-thead > tr > th {
          background: rgba(255, 255, 255, 0.05) !important;
          color: rgba(255, 255, 255, 0.8) !important;
        }
        .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
          color: #fff !important;
        }
        .ant-table-tbody > tr:hover > td {
          background: rgba(255, 255, 255, 0.05) !important;
        }
        .ant-typography {
          color: #fff !important;
        }
        .ant-typography-secondary {
          color: rgba(255, 255, 255, 0.6) !important;
        }
        .pulse-button:hover {
          transform: scale(1.02);
          box-shadow: 0 0 15px rgba(24, 144, 255, 0.5);
        }
      `}</style>
    </div>
  );
};

export default AdminReverseEngineer;
