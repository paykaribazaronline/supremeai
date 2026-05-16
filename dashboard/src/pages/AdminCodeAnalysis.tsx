// AdminCodeAnalysis.tsx - Code Analysis Dashboard (Phase 3)
import React, { useState, useEffect } from 'react';
import { Typography, Space, message, Spin, Tag, Card, Row, Col, Button, Modal, Tabs, Badge, Tooltip } from 'antd';
import { CodeOutlined, SearchOutlined, FileZipOutlined, GithubOutlined, LoadingOutlined, ThunderboltOutlined, DeleteOutlined, AimOutlined, DatabaseOutlined, RobotOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import ProjectUploadForm from '../components/ProjectUploadForm';
import AnalysisStatsCard from '../components/AnalysisStatsCard';
import AnalysisResultsTable from '../components/AnalysisResultsTable';
import AnalysisResultsDisplay from '../components/AnalysisResultsDisplay';
import FixSuggestionCard from '../components/FixSuggestionCard';
import { authUtils } from '../lib/authUtils';
import type { AnalysisJob, AnalysisFix } from '../types';

const { Title, Text } = Typography;

interface AnalysisProgressData {
  type: 'ANALYSIS_PROGRESS' | 'ANALYSIS_COMPLETE';
  jobId: string;
  projectName?: string;
  phase?: string;        // only for progress
  filesProcessed?: number; // only for progress
  totalFiles?: number;    // only for progress
  currentAgent?: string;
  findingsSoFar?: number; // only for progress
  message?: string;       // only for progress
  totalFindings?: number; // only for complete
  severitySummary?: Record<string, number>; // only for complete
  durationMs?: number;    // only for complete
  timestamp: number;
}

interface AdminCodeAnalysisProps {
  analysisProgress: AnalysisProgressData | null;
}

const AdminCodeAnalysis: React.FC<AdminCodeAnalysisProps> = ({ analysisProgress }) => {
  const [jobs, setJobs] = useState<AnalysisJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [fixes, setFixes] = useState<AnalysisFix[]>([]);
  const [fixesLoading, setFixesLoading] = useState(false);
  const [selectedJob, setSelectedJob] = useState<AnalysisJob | null>(null);

  useEffect(() => {
    fetchJobs();
    const interval = setInterval(() => {
      if (activeJobId) {
        fetchJob(activeJobId);
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [activeJobId]);

   // React to WebSocket analysis completion events
   useEffect(() => {
     if (!analysisProgress) return;

     if (analysisProgress.type === 'ANALYSIS_COMPLETE' && analysisProgress.jobId === activeJobId) {
       setActiveJobId(null);
       const findings = analysisProgress.totalFindings ?? 0;
       message.success(`Analysis complete: ${analysisProgress.projectName} — ${findings} findings`);
       fetchJobs();
     }

     if (analysisProgress.phase === 'FAILED' && analysisProgress.jobId === activeJobId) {
       setActiveJobId(null);
       message.error(`Analysis failed: ${analysisProgress.message}`);
     }
   }, [analysisProgress, activeJobId]);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/analysis/jobs');
      if (response.ok) {
        const data = await response.json();
        setJobs(data.data || []);
      }
    } catch (error) {
      message.error('Failed to fetch analysis jobs');
    } finally {
      setLoading(false);
    }
  };

  const fetchJob = async (jobId: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/analysis/${jobId}`);
      if (response.ok) {
        const data = await response.json();
        const job = data.data;
        setJobs(prev => prev.map(j => j.id === jobId ? job : j));
        if (job.status === 'COMPLETED' || job.status === 'FAILED') {
          setActiveJobId(null);
          message.info(`Analysis ${job.status.toLowerCase()}: ${job.projectName}`);
        }
      }
    } catch (error) {
      console.error('Error fetching job:', error);
    }
  };

  const fetchFixes = async (jobId: string) => {
    setFixesLoading(true);
    try {
      const response = await authUtils.fetchWithAuth(`/api/analysis/${jobId}/fixes`);
      if (response.ok) {
        const data = await response.json();
        setFixes(data.data || []);
      }
    } catch (error) {
      message.error('Failed to fetch fix suggestions');
    } finally {
      setFixesLoading(false);
    }
  };

  const handleApplyFix = async (jobId: string, fixId: string) => {
    try {
      const response = await authUtils.fetchWithAuth(
        `/api/analysis/${jobId}/fixes/${fixId}/apply`,
        { method: 'POST' }
      );
      if (response.ok) {
        message.success('Fix marked as applied');
        setFixes(prev => prev.map(f => f.id === fixId ? { ...f, applied: true } : f));
      } else {
        message.error('Failed to apply fix');
      }
    } catch {
      message.error('Failed to apply fix');
    }
  };

  const handleClearCache = async (projectId: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/analysis/cache/${projectId}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        message.success('Cache cleared for project: ' + projectId);
      } else {
        message.error('Failed to clear cache');
      }
    } catch {
      message.error('Failed to clear cache');
    }
  };

  const handleSubmitAnalysis = async (values: any) => {
    setSubmitLoading(true);
    try {
      let response: Response;
      const params = new URLSearchParams({
        projectType: values.projectType || 'generic',
        ragEnabled: String(values.ragEnabled || false),
        incrementalEnabled: String(values.incrementalEnabled || false),
        fixesEnabled: String(values.fixesEnabled !== false),
        ...(values.gitUrl && { gitUrl: values.gitUrl }),
        ...(values.branch && { branch: values.branch })
      });

      if (values.gitUrl) {
        response = await authUtils.fetchWithAuth('/api/analysis/run?' + params.toString(), { method: 'POST' });
      } else if (values.zipFile) {
        const formData = new FormData();
        formData.append('zipFile', values.zipFile);
        formData.append('projectType', values.projectType || 'generic');
        formData.append('ragEnabled', String(values.ragEnabled || false));
        formData.append('incrementalEnabled', String(values.incrementalEnabled || false));
        formData.append('fixesEnabled', String(values.fixesEnabled !== false));
        if (values.agents) {
          Object.entries(values.agents).forEach(([agent, enabled]) => {
            formData.append(`agents[${agent}]`, String(enabled));
          });
        }
        response = await authUtils.fetchWithAuth('/api/analysis/run', { method: 'POST', body: formData });
      } else {
        message.error('Please provide a Git URL or ZIP file');
        return;
      }

      if (response.ok) {
        const data = await response.json();
        const jobId = data.data?.jobId;
        const featureCount = [values.ragEnabled, values.incrementalEnabled, values.fixesEnabled !== false].filter(Boolean).length;
        message.success(`Analysis started with ${Object.values(values.agents || {}).filter(Boolean).length} agents${featureCount > 0 ? ` + ${featureCount} Phase 3 features` : ''}`);
        setActiveJobId(jobId);
        fetchJobs();
      } else {
        const err = await response.json();
        message.error('Analysis failed: ' + (err.error || 'Unknown error'));
      }
    } catch (error) {
      message.error('Network error during analysis');
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDeleteJob = async (jobId: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/analysis/${jobId}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        message.success('Job deleted successfully');
        setJobs(prev => prev.filter(j => j.id !== jobId));
      } else {
        message.error('Failed to delete job');
      }
    } catch {
      message.error('Failed to delete job');
    }
  };

  const handleViewFixes = (job: AnalysisJob) => {
    setSelectedJob(job);
    fetchFixes(job.id);
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'red';
      case 'HIGH': return 'volcano';
      case 'MEDIUM': return 'orange';
      case 'LOW': return 'gold';
      default: return 'blue';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'RUNNING': return 'processing';
      case 'FAILED': return 'error';
      case 'PENDING': return 'default';
      default: return 'default';
    }
  };

  const activeJob = jobs.find(j => j.id === activeJobId);
  const completedJobs = jobs.filter(j => j.status === 'COMPLETED');
  const totalFixes = completedJobs.reduce((sum, j) => {
    const jobFixes = (j as any).fixes;
    return sum + (jobFixes ? jobFixes.length : 0);
  }, 0);

  return (
    <AdminLayout title="কোড অ্যানালাইসিস | Code Analysis">
      <div>
        {/* Header Stats */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card className="glass-panel" style={{ background: 'rgba(0,243,255,0.05)' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 4 }}>Total Jobs</div>
              <div style={{ color: '#fff', fontSize: 24, fontWeight: 700 }}>{jobs.length}</div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel" style={{ background: 'rgba(255,152,0,0.05)' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 4 }}>Active Analysis</div>
              <div style={{ color: 'var(--neon-blue)', fontSize: 24, fontWeight: 700 }}>
                {jobs.filter(j => j.status === 'RUNNING' || j.status === 'PENDING').length}
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel" style={{ background: 'rgba(255,0,0,0.05)' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 4 }}>Total Findings</div>
              <div style={{ color: '#ff3b30', fontSize: 24, fontWeight: 700 }}>
                {jobs.reduce((sum, j) => sum + (j.totalFindings || 0), 0)}
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card className="glass-panel" style={{ background: 'rgba(255,204,0,0.05)' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 4 }}>Fix Suggestions</div>
              <div style={{ color: '#ffcc00', fontSize: 24, fontWeight: 700 }}>{totalFixes}</div>
            </Card>
          </Col>
        </Row>

        {/* Phase 3 Feature Banner */}
        <Card className="glass-panel" style={{ marginBottom: 24, background: 'rgba(0,243,255,0.03)', border: '1px solid rgba(0,243,255,0.2)' }}>
          <Row gutter={[16, 8]} align="middle">
            <Col span={18}>
              <Space>
                <AimOutlined style={{ color: 'var(--neon-blue)', fontSize: 18 }} />
                <Text strong style={{ color: '#fff' }}>Phase 3 Features Available</Text>
              </Space>
              <div style={{ marginTop: 4 }}>
                <Space wrap>
                  <Tag color="cyan" icon={<AimOutlined />}>RAG Context</Tag>
                  <Tag color="green" icon={<DatabaseOutlined />}>Incremental</Tag>
                  <Tag color="gold" icon={<ThunderboltOutlined />}>LLM Fixes</Tag>
                </Space>
                <Text type="secondary" style={{ fontSize: 12, marginLeft: 12 }}>
                  Enable below for faster, smarter analysis
                </Text>
              </div>
            </Col>
            <Col span={6} style={{ textAlign: 'right' }}>
              <Text type="secondary" style={{ fontSize: 11 }}>Target: 1000 files in &lt;10s</Text>
            </Col>
          </Row>
        </Card>

         {/* Active Job Progress with Real-Time WebSocket Updates */}
         {activeJob && (
           <Card className="glass-panel" style={{ marginBottom: 24, background: 'rgba(0,243,255,0.05)' }}>
             <div style={{ marginBottom: 16 }}>
               <Title level={4} style={{ color: '#fff', margin: 0 }}>
                 <LoadingOutlined spin /> analyzing: {activeJob.projectName}
               </Title>
               <Text style={{ color: 'var(--text-dim)' }}>
                 {analysisProgress?.phase || activeJob.status}
                 {analysisProgress?.message && ` — ${analysisProgress.message}`}
               </Text>
             </div>

              {/* Progress bar showing file scanning completion */}
              <div style={{ marginBottom: 12 }}>
                <div style={{ background: 'rgba(255,255,255,0.1)', borderRadius: 4, height: 8, overflow: 'hidden' }}>
                  <div style={{
                    background: analysisProgress?.phase === 'FAILED' ? 'var(--error)' : 'var(--neon-blue)',
                    height: '100%',
                    width: analysisProgress && analysisProgress.filesProcessed !== undefined && analysisProgress.totalFiles !== undefined
                      ? `${Math.round((analysisProgress.filesProcessed / analysisProgress.totalFiles) * 100)}%`
                      : `${activeJob.filesAnalyzed}%`,
                    transition: 'width 0.3s ease',
                    borderRadius: 4
                  }} />
                </div>
              </div>

              <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
                <Text style={{ color: '#fff', fontSize: 12 }}>
                  {analysisProgress && analysisProgress.filesProcessed !== undefined && analysisProgress.totalFiles !== undefined
                    ? `${analysisProgress.filesProcessed} / ${analysisProgress.totalFiles} files`
                    : `${activeJob.filesAnalyzed} files scanned`}
                </Text>

                {analysisProgress?.currentAgent && (
                  <Tag color="blue" icon={<RobotOutlined />} style={{ fontSize: 11 }}>
                    {analysisProgress.currentAgent}
                  </Tag>
                )}

                <Text style={{ color: '#fff', fontSize: 12 }}>
                  Findings: {analysisProgress?.findingsSoFar ?? activeJob.totalFindings}
                </Text>

               {analysisProgress?.currentAgent && (
                 <Tag color="blue" icon={<RobotOutlined />} style={{ fontSize: 11 }}>
                   {analysisProgress.currentAgent}
                 </Tag>
               )}

               <Text style={{ color: '#fff', fontSize: 12 }}>
                 Findings: {analysisProgress?.findingsSoFar ?? activeJob.totalFindings}
               </Text>

               {activeJob.findingsBySeverity && Object.entries(activeJob.findingsBySeverity).map(([sev, count]) => (
                 count > 0 ? (
                   <Tag key={sev} color={getSeverityColor(sev)} style={{ fontSize: 11 }}>
                     {sev}: {count}
                   </Tag>
                 ) : null
               ))}

               {analysisProgress?.phase === 'COMPLETED' || activeJob.status === 'COMPLETED' ? (
                 <Tag color="success" icon={<ThunderboltOutlined />} style={{ fontSize: 11 }}>
                   Complete!
                 </Tag>
               ) : null}
             </div>
           </Card>
         )}

        {/* Upload Form */}
        <Card
          className="glass-panel"
          title={
            <Space>
              <SearchOutlined style={{ color: 'var(--neon-blue)' }} />
              <span style={{ color: '#fff', fontWeight: 700 }}>নতুন অ্যানালাইসিস শুরু করুন</span>
            </Space>
          }
          style={{ marginBottom: 24 }}
        >
          <ProjectUploadForm
            onSubmit={handleSubmitAnalysis}
            loading={submitLoading}
          />
        </Card>

        {/* Jobs Table */}
        <Card
          className="glass-panel"
          title={
            <Space>
              <CodeOutlined style={{ color: 'var(--neon-blue)' }} />
              <span style={{ color: '#fff', fontWeight: 700 }}>অ্যানালাইসিস হিসাব</span>
            </Space>
          }
        >
          <Spin spinning={loading}>
            <AnalysisResultsTable
              jobs={jobs}
              onDelete={handleDeleteJob}
              onView={fetchJob}
              onViewFixes={handleViewFixes}
              onClearCache={handleClearCache}
              getSeverityColor={getSeverityColor}
              getStatusColor={getStatusColor}
            />
          </Spin>
        </Card>
      </div>

      {/* Fixes Modal */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#ffcc00' }} />
            <span>Fix Suggestions {selectedJob ? `- ${selectedJob.projectName}` : ''}</span>
            <Badge count={fixes.length} showZero color="#ffcc00" />
          </Space>
        }
        open={!!selectedJob}
        onCancel={() => { setSelectedJob(null); setFixes([]); }}
        footer={null}
        width={900}
        bodyStyle={{ maxHeight: 600, overflow: 'auto', padding: 16 }}
      >
        <Spin spinning={fixesLoading}>
          {fixes.length === 0 && !fixesLoading && (
            <div style={{ textAlign: 'center', padding: 32 }}>
              <Text type="secondary">No fix suggestions available for this job.</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>
                Fixes are generated for HIGH and CRITICAL severity findings.
              </Text>
            </div>
          )}
          {fixes.map(fix => (
            <FixSuggestionCard
              key={fix.id}
              fix={fix}
              onApply={(fixId) => selectedJob && handleApplyFix(selectedJob.id, fixId)}
            />
          ))}
        </Spin>
      </Modal>
    </AdminLayout>
  );
};

export default AdminCodeAnalysis;
