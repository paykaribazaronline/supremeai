import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Typography, Row, Col, Card, Select, Button, Space, message, Input, Progress } from 'antd';
import { CloudOutlined, SyncOutlined, DeploymentUnitOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;
const { Option } = Select;

interface Deployment {
  id: string;
  projectName: string;
  target: string;
  status: 'pending' | 'running' | 'success' | 'failed';
  progress: number;
  createdAt: string;
}

export default function AdminDeployment() {
  const [loading, setLoading] = useState(false);
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [selectedProject, setSelectedProject] = useState('');
  const [selectedTarget, setSelectedTarget] = useState('vercel');
  const [envVars, setEnvVars] = useState('');

  const targets = ['vercel', 'netlify', 'aws', 'gcp', 'azure'];

  useEffect(() => {
    fetchDeployments();
  }, []);

  const fetchDeployments = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/deploy');
      if (res.ok) {
        const data = await res.json();
        setDeployments(Array.isArray(data) ? data : data.deployments || []);
      }
    } catch (error) {
      message.error('Failed to fetch deployments');
    } finally {
      setLoading(false);
    }
  };

  const handleDeploy = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/deploy', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          project: selectedProject,
          target: selectedTarget,
          env: envVars
        })
      });
      if (res.ok) {
        message.success('Deployment started');
        fetchDeployments();
      }
    } catch (error) {
      message.error('Failed to start deployment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: '24px' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          Deployment
        </Title>
        <Button icon={<SyncOutlined />} onClick={fetchDeployments} className="glass-action-button">
          Refresh
        </Button>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={12}>
          <Card className="glass-card" title="New Deployment">
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text style={{ color: 'var(--text-dim)', display: 'block', marginBottom: 8 }}>Project</Text>
                <Input
                  placeholder="Project name or path"
                  value={selectedProject}
                  onChange={(e) => setSelectedProject(e.target.value)}
                />
              </div>

              <div>
                <Text style={{ color: 'var(--text-dim)', display: 'block', marginBottom: 8 }}>Target Platform</Text>
                <Select
                  style={{ width: '100%' }}
                  value={selectedTarget}
                  onChange={setSelectedTarget}
                >
                  {targets.map(t => (
                    <Option key={t} value={t}>{t.toUpperCase()}</Option>
                  ))}
                </Select>
              </div>

              <div>
                <Text style={{ color: 'var(--text-dim)', display: 'block', marginBottom: 8 }}>Environment Variables (JSON)</Text>
                <Input.TextArea
                  placeholder='{"KEY": "value"}'
                  value={envVars}
                  onChange={(e) => setEnvVars(e.target.value)}
                  rows={4}
                />
              </div>

              <Button 
                type="primary" 
                icon={<DeploymentUnitOutlined />}
                onClick={handleDeploy}
                loading={loading}
                style={{ marginTop: 8 }}
              >
                Deploy
              </Button>
            </Space>
          </Card>
        </Col>

        <Col span={12}>
          <Card className="glass-card" title="Recent Deployments">
            {deployments.map(d => (
              <div key={d.id} style={{ marginBottom: 16, padding: 12, backgroundColor: 'rgba(255,255,255,0.02)', borderRadius: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={{ color: '#fff' }}>{d.projectName}</Text>
                  <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>{d.target}</Text>
                </div>
                <Progress 
                  percent={d.progress} 
                  status={d.status === 'failed' ? 'exception' : d.status === 'success' ? 'success' : 'active'}
                  size="small"
                  style={{ marginTop: 8 }}
                />
              </div>
            ))}
          </Card>
        </Col>
      </Row>
    </motion.div>
  );
}