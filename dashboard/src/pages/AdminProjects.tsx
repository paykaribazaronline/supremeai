// AdminProjects.tsx - Project Management Page (Modularized)

import React, { useState, useEffect } from 'react';
import { Card, message, Spin, Alert } from 'antd';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

// Import Modular Components
import { Project, GenerationForm, GenerationStatus, ProjectSortField } from '../components/projects/types';
import ProjectTable from '../components/projects/ProjectTable';
import ProjectModal from '../components/projects/ProjectModal';
import AppGenerationCard from '../components/projects/AppGenerationCard';
import ProjectActionToolbar from '../components/projects/ProjectActionToolbar';
import InfrastructureAdviceModal from '../components/projects/InfrastructureAdviceModal';
import { useSystemWebSocket } from '../hooks/useSystemWebSocket';

const AdminProjects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  
  // Search and Sort State
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<ProjectSortField | null>('createdAt');
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('descend');

  // App Generation State
  const [generationForm, setGenerationForm] = useState<GenerationForm>({
    name: '',
    description: '',
    platform: 'fullstack',
    database: 'PostgreSQL',
    useAI: true
  });
  const [generationStatus, setGenerationStatus] = useState<GenerationStatus>('idle');
  const [generationStep, setGenerationStep] = useState(0);
  const [generationProgress, setGenerationProgress] = useState(0);
  const [generationResult, setGenerationResult] = useState<any>(null);

  // WebSocket for Real-time Pipeline Progress
  const { messages } = useSystemWebSocket(['/topic/pipeline/progress']);

  useEffect(() => {
    const pipelineMsg = messages['/topic/pipeline/progress'];
    if (pipelineMsg) {
      if (pipelineMsg.step !== undefined) setGenerationStep(pipelineMsg.step);
      if (pipelineMsg.progress !== undefined) setGenerationProgress(pipelineMsg.progress);
      if (pipelineMsg.message) {
        // Optional: show toast or log message
        console.log(`Pipeline: ${pipelineMsg.message}`);
      }
    }
  }, [messages]);

  // Infrastructure Advice State
  const [adviceVisible, setAdviceVisible] = useState(false);
  const [adviceLoading, setAdviceLoading] = useState(false);
  const [infrastructureAdvice, setInfrastructureAdvice] = useState<string | null>(null);

  const processedProjects = React.useMemo(() => {
    let result = projects.filter(p => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return p.name.toLowerCase().includes(term) || 
             (p.description && p.description.toLowerCase().includes(term)) ||
             p.id.toLowerCase().includes(term);
    });

    if (sortBy) {
      result.sort((a, b) => {
        const aVal = (a as any)[sortBy] ?? '';
        const bVal = (b as any)[sortBy] ?? '';

        if (sortBy === 'createdAt') {
          return sortOrder === 'ascend' 
            ? new Date(aVal).getTime() - new Date(bVal).getTime()
            : new Date(bVal).getTime() - new Date(aVal).getTime();
        }

        if (typeof aVal === 'string' && typeof bVal === 'string') {
          return sortOrder === 'ascend' 
            ? aVal.localeCompare(bVal) 
            : bVal.localeCompare(aVal);
        }
        return 0;
      });
    }

    return result;
  }, [projects, sortBy, sortOrder, searchTerm]);

  const fetchProjects = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await authUtils.fetchWithAuth('/api/projects');
      if (!response.ok) throw new Error('Failed to fetch projects');
      const result = await response.json();
      setProjects(result.data || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects');
      message.error('Error fetching projects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreateOrUpdate = async (values: any) => {
    try {
      const isEdit = !!editingProject;
      const url = isEdit ? `/api/projects/${editingProject.id}` : '/api/projects';
      const method = isEdit ? 'PUT' : 'POST';
      
      const response = await authUtils.fetchWithAuth(url, {
        method,
        body: JSON.stringify(values),
      });
      
      if (!response.ok) throw new Error(`Failed to ${isEdit ? 'update' : 'create'} project`);
      
      message.success(`Project ${isEdit ? 'updated' : 'created'} successfully`);
      setModalVisible(false);
      fetchProjects();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/projects/${id}/status?status=${encodeURIComponent(status)}`, {
        method: 'PUT',
      });
      if (!response.ok) throw new Error('Failed to update status');
      message.success('Status updated');
      fetchProjects();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to update');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/projects/${id}`, {
        method: 'DELETE',
      });
      if (!response.ok) throw new Error('Failed to delete project');
      message.success('Project deleted');
      fetchProjects();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  const handleGenerateApp = async () => {
    if (!generationForm.name || !generationForm.description) {
      message.warning('Please provide both name and description');
      return;
    }

    setGenerationStatus('generating');
    setGenerationStep(0);
    setGenerationProgress(5);
    
    try {
      // Direct API Call to trigger the Backend Pipeline
      const response = await authUtils.fetchWithAuth('/api/generate', {
        method: 'POST',
        body: JSON.stringify({
          ...generationForm,
          type: 'project',
        }),
      });
      
      if (!response.ok) throw new Error('Generation engine failed to start');
      
      const result = await response.json();
      
      // If the API returns immediately (ACCEPTED), the progress will be handled by WebSocket
      // If it returns the final result, we update the UI
      if (result.status === 'GENERATED' || result.status === 'COMPLETED') {
        setGenerationStep(4);
        setGenerationProgress(100);
        setGenerationStatus('success');
        setGenerationResult(result);
        fetchProjects();
        message.success('Application generation pipeline completed successfully');
      } else {
        // Pipeline is running in background, WebSocket will handle updates
        setGenerationResult(result);
        message.info('AI Agents have started the synthesis pipeline...');
      }
    } catch (err) {
      setGenerationStatus('error');
      message.error(err instanceof Error ? err.message : 'Generation pipeline failed');
    }
  };

  const handleGetInfrastructureAdvice = async () => {
    if (!generationForm.name || !generationForm.description) {
      message.warning('Please provide both name and description first');
      return;
    }

    setAdviceVisible(true);
    setAdviceLoading(true);
    setInfrastructureAdvice(null);

    try {
      const response = await authUtils.fetchWithAuth('/api/admin/infra/advice', {
        method: 'POST',
        body: JSON.stringify({
          name: generationForm.name,
          description: generationForm.description,
          techStack: generationForm.platform // platform maps to techStack in this context
        }),
      });

      if (!response.ok) throw new Error('Failed to fetch infrastructure advice');
      
      const result = await response.json();
      setInfrastructureAdvice(result.data);
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Concierge service error');
      setAdviceVisible(false);
    } finally {
      setAdviceLoading(false);
    }
  };

  return (
    <AdminLayout title="System Orchestrator: Projects">
      <Card className="glass-card">
        <ProjectActionToolbar 
          searchTerm={searchTerm}
          setSearchTerm={setSearchTerm}
          onNewProject={() => {
            setEditingProject(null);
            setModalVisible(true);
          }}
          onRefresh={fetchProjects}
          loading={loading}
          sortBy={sortBy}
          setSortBy={setSortBy}
          sortOrder={sortOrder}
          setSortOrder={setSortOrder}
        />

        {error && (
          <Alert 
            type="error" 
            message="System Sync Error" 
            description={error} 
            showIcon 
            style={{ marginBottom: 16 }}
            action={<button onClick={fetchProjects} className="retry-btn">Retry Sync</button>} 
          />
        )}

        {loading && !projects.length ? (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" tip="Synchronizing Project Vault..." />
          </div>
        ) : (
          <ProjectTable 
            projects={processedProjects}
            loading={loading}
            onEdit={(project) => {
              setEditingProject(project);
              setModalVisible(true);
            }}
            onDelete={handleDelete}
            onUpdateStatus={handleUpdateStatus}
          />
        )}
      </Card>

      <AppGenerationCard 
        generationForm={generationForm}
        setGenerationForm={setGenerationForm}
        generationStatus={generationStatus}
        generationStep={generationStep}
        generationProgress={generationProgress}
        generationResult={generationResult}
        onGenerate={handleGenerateApp}
        onGetAdvice={handleGetInfrastructureAdvice}
      />

      <InfrastructureAdviceModal 
        visible={adviceVisible}
        onCancel={() => setAdviceVisible(false)}
        advice={infrastructureAdvice}
        loading={adviceLoading}
        projectName={generationForm.name || "New Project"}
      />

      <ProjectModal 
        visible={modalVisible}
        editingProject={editingProject}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleCreateOrUpdate}
      />
    </AdminLayout>
  );
};

export default AdminProjects;
