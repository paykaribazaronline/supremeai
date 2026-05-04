 // AdminProjects.tsx - Project Management Page

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message, Spin, Alert, Steps, Progress, Typography, Row, Col, Statistic } from 'antd';
import { FolderOutlined, PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, RocketOutlined, CloudServerOutlined, CodeOutlined, CheckCircleOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;
const { Title, Paragraph } = Typography;
const { Step } = Steps;

interface Project {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface GenerationForm {
  name: string;
  description: string;
  platform: string;
  database: string;
  useAI?: boolean;
}

const AdminProjects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [form] = Form.useForm();
  
  // App Generation State
  const [generationForm, setGenerationForm] = useState<GenerationForm>({
    name: '',
    description: '',
    platform: 'fullstack',
    database: 'PostgreSQL'
  });
  const [generationStatus, setGenerationStatus] = useState<'idle' | 'generating' | 'success' | 'error'>('idle');
  const [generationStep, setGenerationStep] = useState(0);
  const [generationProgress, setGenerationProgress] = useState(0);
  const [generationResult, setGenerationResult] = useState<any>(null);

  const fetchProjects = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/projects', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to fetch projects');
      const data: Project[] = await response.json();
      setProjects(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreate = async (values: any) => {
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/projects', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(values),
      });
      if (!response.ok) throw new Error('Failed to create project');
      message.success('Project created');
      setModalVisible(false);
      form.resetFields();
      fetchProjects();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to create');
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    try {
      const token = authUtils.getToken();
      const response = await fetch(`/api/projects/${id}/status?status=${encodeURIComponent(status)}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` },
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
      const token = authUtils.getToken();
      const response = await fetch(`/api/projects/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to delete project');
      message.success('Project deleted');
      fetchProjects();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  const handleGenerateApp = async () => {
    setGenerationStatus('generating');
    setGenerationStep(0);
    
    try {
      // Step 1: Analyze requirements
      setGenerationStep(1);
      setGenerationProgress(25);
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Step 2: Design architecture
      setGenerationStep(2);
      setGenerationProgress(50);
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Step 3: Generate code
      setGenerationStep(3);
      setGenerationProgress(75);
      
      // Call generation API
      const token = authUtils.getToken();
      const response = await fetch('/api/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: generationForm.name,
          description: generationForm.description,
          platform: generationForm.platform,
          database: generationForm.database,
          type: 'project',
          useAI: generationForm.useAI || false,
        }),
      });
      
      if (!response.ok) {
        throw new Error('Generation failed');
      }
      
      const result = await response.json();
      
      // Step 4: Complete
      setGenerationStep(4);
      setGenerationProgress(100);
      setGenerationStatus('success');
      setGenerationResult(result);
      
      // Create project entry
      const projectResponse = await fetch('/api/projects', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: generationForm.name,
          description: generationForm.description,
          status: 'ACTIVE',
        }),
      });
      
      if (projectResponse.ok) {
        fetchProjects();
      }
      
      message.success('App generated successfully!');
    } catch (err) {
      setGenerationStatus('error');
      message.error(err instanceof Error ? err.message : 'Generation failed');
    }
  };

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (desc: string) => desc || '-',
    },
    {
      title: 'Owner',
      dataIndex: 'ownerId',
      key: 'ownerId',
      render: (owner: string) => <span>{owner.substring(0, 8)}...</span>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'default';
        if (status === 'ACTIVE') color = 'green';
        else if (status === 'PAUSED') color = 'orange';
        else if (status === 'COMPLETED') color = 'blue';
        else if (status === 'FAILED') color = 'red';
        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Project) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => {
            setEditingProject(record);
            form.setFieldsValue(record);
            setModalVisible(true);
          }}>
            Edit
          </Button>
          <Select
            size="small"
            value={record.status}
            style={{ width: 100 }}
            onChange={(val) => handleUpdateStatus(record.id, val)}
          >
            <Option value="ACTIVE">Active</Option>
            <Option value="PAUSED">Paused</Option>
            <Option value="COMPLETED">Completed</Option>
            <Option value="FAILED">Failed</Option>
          </Select>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <AdminLayout title="Project Management">
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => {
            setEditingProject(null);
            form.resetFields();
            setModalVisible(true);
          }}>
            New Project
          </Button>
          <Button icon={<ReloadOutlined />} style={{ marginLeft: 8 }} onClick={fetchProjects}>
            Refresh
          </Button>
        </div>

        {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
        {error && <Alert type="error" message={error} action={<Button onClick={fetchProjects}>Retry</Button>} />}

        {!loading && !error && (
          <Table
            columns={columns}
            dataSource={projects}
            rowKey="id"
            pagination={{ pageSize: 15 }}
            scroll={{ x: 1000 }}
          />
        )}
      </Card>

      {/* App Generation Card */}
      <Card title={<><RocketOutlined /> Generate New App</>} style={{ marginTop: 24 }}>
        <Row gutter={24}>
          <Col xs={24} lg={12}>
            <Title level={4}>App Requirements</Title>
            <Paragraph>Describe the app you want to generate. The AI will analyze your requirements and create a full-stack application.</Paragraph>
            
            <Form layout="vertical" onFinish={handleGenerateApp}>
              <Form.Item label="App Name" required>
                <Input 
                  placeholder="My Awesome App"
                  value={generationForm.name}
                  onChange={e => setGenerationForm({...generationForm, name: e.target.value})}
                />
              </Form.Item>
              
              <Form.Item label="Description" required>
                <Input.TextArea 
                  rows={4}
                  placeholder="Describe your app's purpose and main features..."
                  value={generationForm.description}
                  onChange={e => setGenerationForm({...generationForm, description: e.target.value})}
                />
              </Form.Item>
              
              <Form.Item label="Target Platform" required>
                <Select
                  placeholder="Select platform"
                  value={generationForm.platform}
                  onChange={val => setGenerationForm({...generationForm, platform: val})}
                >
                  <Option value="web">Web Application (React)</Option>
                  <Option value="android">Android App (Kotlin)</Option>
                  <Option value="ios">iOS App (SwiftUI)</Option>
                  <Option value="desktop">Desktop App (JavaFX)</Option>
                  <Option value="fullstack">Full-Stack Web App (Backend + Frontend)</Option>
                </Select>
              </Form.Item>
              
              <Form.Item>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input
                    type="checkbox"
                    checked={generationForm.useAI || false}
                    onChange={e => setGenerationForm({...generationForm, useAI: e.target.checked})}
                  />
                  <span>Use AI-Powered Generation (OpenAI/Gemini)</span>
                </label>
                <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                  Enable to generate custom entities and AI-optimized code structure
                </div>
              </Form.Item>
              
              <Form.Item label="Database Preference">
                <Select
                  placeholder="Select database"
                  value={generationForm.database}
                  onChange={val => setGenerationForm({...generationForm, database: val})}
                >
                  <Option value="PostgreSQL">PostgreSQL</Option>
                  <Option value="MySQL">MySQL</Option>
                  <Option value="MongoDB">MongoDB</Option>
                </Select>
              </Form.Item>
              
              <Form.Item>
                <Button 
                  type="primary" 
                  htmlType="submit"
                  icon={<RocketOutlined />}
                  loading={generationStatus === 'generating'}
                  size="large"
                  block
                  onClick={handleGenerateApp}
                >
                  {generationStatus === 'generating' ? 'Generating...' : 'Generate App'}
                </Button>
              </Form.Item>
            </Form>
          </Col>
          
          <Col xs={24} lg={12}>
            {generationStatus !== 'idle' && (
              <>
                <Title level={4}>Generation Progress</Title>
                
                <Steps
                  current={generationStep}
                  direction="vertical"
                  items={[
                    { title: 'Analyzing Requirements', icon: <CodeOutlined /> },
                    { title: 'Designing Architecture', icon: <CloudServerOutlined /> },
                    { title: 'Generating Code', icon: <RocketOutlined /> },
                    { title: 'Build Complete', icon: <CheckCircleOutlined /> },
                  ]}
                />
                
                <div style={{ marginTop: 24 }}>
                  <Progress 
                    percent={generationProgress} 
                    status={generationStatus === 'error' ? 'exception' : 'active'}
                    strokeColor={{
                      '0%': '#108ee9',
                      '100%': '#87d068',
                    }}
                  />
                </div>
                
                {generationStatus === 'success' && generationResult && (
                  <Alert
                    message="App Generated Successfully!"
                    description={`Your ${generationForm.platform} app has been generated with ${generationResult.fileCount || 0} files.`}
                    type="success"
                    showIcon
                    style={{ marginTop: 16 }}
                  />
                )}
                
                {generationStatus === 'error' && (
                  <Alert
                    message="Generation Failed"
                    description="There was an error generating your app. Please try again."
                    type="error"
                    showIcon
                    style={{ marginTop: 16 }}
                  />
                )}
              </>
            )}
          </Col>
        </Row>
      </Card>

      <Modal
        title={editingProject ? 'Edit Project' : 'Create New Project'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={500}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Project Name" rules={[{ required: true }]}>
            <Input placeholder="My AI Project" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} placeholder="Brief description..." />
          </Form.Item>
          {editingProject && (
            <Form.Item name="status" label="Status">
              <Select>
                <Option value="ACTIVE">Active</Option>
                <Option value="PAUSED">Paused</Option>
                <Option value="COMPLETED">Completed</Option>
                <Option value="FAILED">Failed</Option>
              </Select>
            </Form.Item>
          )}
          <Form.Item style={{ marginTop: 24 }}>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingProject ? 'Update' : 'Create'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </AdminLayout>
  );
};

export default AdminProjects;
