// AutoBrowser.tsx - Autonomous Browser Automation
import React, { useState, useEffect } from 'react';
import { Typography, Space, Row, Col, Button, notification, Card, Input, Tag, Badge, Spin, Select, Modal, Form, Slider, Divider, Progress, Statistic, Switch } from 'antd';
import {
  RocketOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  SettingOutlined,
  EyeOutlined,
  GlobalOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  BulbOutlined,
  FileTextOutlined,
  BarChartOutlined,
  SearchOutlined,
  DownloadOutlined,
  DeleteOutlined,
  PlusOutlined,
  MinusOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';
import { useRole } from '../contexts/RoleContext';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;
const { confirm } = Modal;

interface AutoTask {
  id: string;
  name: string;
  goal: string;
  status: 'idle' | 'running' | 'paused' | 'completed' | 'failed';
  progress: number;
  steps: number;
  createdAt: string;
  completedAt?: string;
  result?: string;
  error?: string;
}

const AdminAutoBrowser: React.FC = () => {
  const { isGuest } = useRole();
  const [tasks, setTasks] = useState<AutoTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [newTaskName, setNewTaskName] = useState('');
  const [newTaskGoal, setNewTaskGoal] = useState('');
  const [maxSteps, setMaxSteps] = useState(50);
  const [intervalMs, setIntervalMs] = useState(2000);
  const [autoMode, setAutoMode] = useState(false);

  const fetchTasks = async () => {
    if (isGuest) return;
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/browser/tasks');
      if (res.ok) {
        const data = await res.json();
        setTasks(data.tasks || []);
      }
    } catch (err) {
      console.error('Failed to fetch tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTasks(); }, []);

  const handleCreateTask = async () => {
    if (!newTaskName || !newTaskGoal || isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth('/api/browser/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newTaskName, goal: newTaskGoal, maxSteps, intervalMs })
      });
      if (res.ok) {
        notification.success({ message: 'Autonomous task created' });
        setNewTaskName('');
        setNewTaskGoal('');
        fetchTasks();
      }
    } catch (err) {
      notification.error({ message: 'Failed to create task' });
    }
  };

  const handleStartTask = async (taskId: string) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/browser/tasks/${taskId}/start`, { method: 'POST' });
      if (res.ok) { notification.success({ message: 'Task started' }); fetchTasks(); }
    } catch { notification.error({ message: 'Failed to start task' }); }
  };

  const handlePauseTask = async (taskId: string) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/browser/tasks/${taskId}/pause`, { method: 'POST' });
      if (res.ok) { notification.success({ message: 'Task paused' }); fetchTasks(); }
    } catch { notification.error({ message: 'Failed to pause task' }); }
  };

  const handleStopTask = async (taskId: string) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/browser/tasks/${taskId}/stop`, { method: 'POST' });
      if (res.ok) { notification.success({ message: 'Task stopped' }); fetchTasks(); }
    } catch { notification.error({ message: 'Failed to stop task' }); }
  };

  const handleDeleteTask = async (taskId: string) => {
    confirm({
      title: 'Delete this autonomous task?',
      content: 'This action cannot be undone.',
      okText: 'Delete',
      okType: 'danger',
      onOk: async () => {
        try {
          const res = await authUtils.fetchWithAuth(`/api/browser/tasks/${taskId}`, { method: 'DELETE' });
          if (res.ok) { notification.success({ message: 'Task deleted' }); fetchTasks(); }
        } catch { notification.error({ message: 'Failed to delete task' }); }
      }
    });
  };

  const handleViewFindings = async (taskId: string) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/browser/tasks/${taskId}/findings`);
      if (res.ok) {
        const data = await res.json();
        Modal.info({
          title: 'Task Findings',
          width: 700,
          content: (
            <div>
              <Text strong>Findings:</Text>
              <pre style={{ background: 'rgba(0,0,0,0.3)', padding: 12, borderRadius: 8, maxHeight: 400, overflow: 'auto', marginTop: 8 }}>
                {JSON.stringify(data.findings || [], null, 2)}
              </pre>
            </div>
          )
        });
      }
    } catch { notification.error({ message: 'Failed to load findings' }); }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'running': return 'processing';
      case 'completed': return 'success';
      case 'failed': return 'error';
      case 'paused': return 'warning';
      default: return 'default';
    }
  };

  const runningCount = tasks.filter(t => t.status === 'running').length;

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }} style={{ maxWidth: '1600px', margin: '0 auto' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: '#fff', margin: 0 }}><RocketOutlined /> Autonomous Browser</Title>
        <Text type="secondary">AI-driven autonomous web navigation and task execution</Text>
      </div>

      {/* Stats Row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic title={<Text style={{ color: 'rgba(255,255,255,0.45)' }}>Total Tasks</Text>} value={tasks.length} valueStyle={{ color: '#3b82f6' }} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic title={<Text style={{ color: 'rgba(255,255,255,0.45)' }}>Running</Text>} value={runningCount} valueStyle={{ color: '#10b981' }} prefix={<PlayCircleOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic title={<Text style={{ color: 'rgba(255,255,255,0.45)' }}>Completed</Text>} value={tasks.filter(t => t.status === 'completed').length} valueStyle={{ color: '#8b5cf6' }} prefix={<CheckCircleOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic title={<Text style={{ color: 'rgba(255,255,255,0.45)' }}>Failed</Text>} value={tasks.filter(t => t.status === 'failed').length} valueStyle={{ color: '#ef4444' }} prefix={<CloseCircleOutlined />} />
          </Card>
        </Col>
      </Row>

      {/* Create Task Card */}
      <Card className="glass-card" bordered={false} title={<><PlusOutlined /> Create Autonomous Task</>} style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={8}>
            <Input placeholder="Task name..." value={newTaskName} onChange={e => setNewTaskName(e.target.value)} prefix={<RocketOutlined style={{ color: 'rgba(255,255,255,0.2)' }} />} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', color: '#fff' }} />
          </Col>
          <Col xs={24} md={12}>
            <Input placeholder="Describe the goal..." value={newTaskGoal} onChange={e => setNewTaskGoal(e.target.value)} prefix={<BulbOutlined style={{ color: 'rgba(255,255,255,0.2)' }} />} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', color: '#fff' }} />
          </Col>
          <Col xs={24} md={4}>
            <Button type="primary" icon={<RocketOutlined />} onClick={handleCreateTask} disabled={!newTaskName || !newTaskGoal || isGuest} block style={{ background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)' }}>Create Task</Button>
          </Col>
        </Row>
        <Divider style={{ borderColor: 'rgba(255,255,255,0.05)' }} />
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={8}>
            <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12 }}>Max Steps: {maxSteps}</Text>
            <Slider min={5} max={200} value={maxSteps} onChange={setMaxSteps} style={{ marginTop: 4 }} />
          </Col>
          <Col xs={24} md={8}>
            <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12 }}>Interval: {intervalMs}ms</Text>
            <Slider min={500} max={10000} step={500} value={intervalMs} onChange={setIntervalMs} style={{ marginTop: 4 }} />
          </Col>
          <Col xs={24} md={8}>
            <Space>
              <Button icon={<SettingOutlined />} onClick={() => {}}>Advanced Config</Button>
              <Switch checked={autoMode} onChange={setAutoMode} checkedChildren="Auto Mode" unCheckedChildren="Manual" />
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Tasks Table */}
      <Card className="glass-card" bordered={false} title={<><HistoryOutlined /> Autonomous Tasks</>} extra={<Button icon={<ReloadOutlined />} onClick={fetchTasks} loading={loading}>Refresh</Button>}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                {['Name', 'Goal', 'Status', 'Progress', 'Steps', 'Created', 'Actions'].map(h => (
                  <th key={h} style={{ padding: 12, textAlign: 'left', color: 'rgba(255,255,255,0.45)', fontSize: 12, textTransform: 'uppercase', fontWeight: 600 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {tasks.length === 0 ? (
                <tr><td colSpan={7} style={{ textAlign: 'center', padding: 48, color: 'rgba(255,255,255,0.3)' }}>No autonomous tasks yet. Create one above to get started.</td></tr>
              ) : tasks.map(task => (
                <tr key={task.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'rgba(255,255,255,0.02)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
                  <td style={{ padding: 12 }}><Text strong style={{ color: '#fff' }}>{task.name}</Text></td>
                  <td style={{ padding: 12, maxWidth: 300 }}><Text ellipsis style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13 }}>{task.goal}</Text></td>
                  <td style={{ padding: 12 }}><Tag color={getStatusColor(task.status)}>{task.status.toUpperCase()}</Tag></td>
                  <td style={{ padding: 12, minWidth: 150 }}><Progress percent={task.progress} size="small" status={task.status === 'failed' ? 'exception' : task.status === 'completed' ? 'success' : 'active'} /></td>
                  <td style={{ padding: 12 }}><Text style={{ color: 'rgba(255,255,255,0.6)' }}>{task.steps}</Text></td>
                  <td style={{ padding: 12 }}><Text style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12 }}>{new Date(task.createdAt).toLocaleString()}</Text></td>
                  <td style={{ padding: 12 }}>
                    <Space size="small">
                      {task.status === 'idle' && <Button size="small" type="primary" icon={<PlayCircleOutlined />} onClick={() => handleStartTask(task.id)}>Start</Button>}
                      {task.status === 'running' && (
                        <>
                          <Button size="small" icon={<PauseCircleOutlined />} onClick={() => handlePauseTask(task.id)}>Pause</Button>
                          <Button size="small" danger icon={<StopOutlined />} onClick={() => handleStopTask(task.id)}>Stop</Button>
                        </>
                      )}
                      {task.status === 'paused' && <Button size="small" type="primary" icon={<PlayCircleOutlined />} onClick={() => handleStartTask(task.id)}>Resume</Button>}
                      {(task.status === 'completed' || task.status === 'failed') && <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewFindings(task.id)}>Findings</Button>}
                      <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDeleteTask(task.id)} />
                    </Space>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </motion.div>
  );
};

export default AdminAutoBrowser;
