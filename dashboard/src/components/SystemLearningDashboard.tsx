import React, { useState, useEffect } from 'react';
import {
  Card, Table, Button, Space, Tag, Statistic, Row, Col, Tabs,
  Form, Input, Select, Modal, Spin, message, Badge, Progress, Drawer
} from 'antd';
import {
  RobotOutlined as BrainOutlined, PlayCircleOutlined, HistoryOutlined,
  RocketOutlined, CheckCircleOutlined, LoadingOutlined
} from '@ant-design/icons';

/**
 * SystemLearningDashboard
 * 
 * Visualizes:
 * 1. Patterns SupremeAI has learned from your actions
 * 2. Ability to execute patterns autonomously
 * 3. Learning statistics and success rates
 */
export default function SystemLearningDashboard() {
  const [patterns, setPatterns] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [selectedPattern, setSelectedPattern] = useState(null);
  const [executeDrawer, setExecuteDrawer] = useState(false);
  const [form] = Form.useForm();

  // Load learned patterns and stats
  useEffect(() => {
    fetchPatterns();
    fetchStats();
    const interval = setInterval(() => {
      fetchPatterns();
      fetchStats();
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchPatterns = async () => {
    try {
      const response = await fetch('/api/teach/patterns?sortBy=frequency');
      const data = await response.json();
      if (data.success && data.patterns) {
        setPatterns(data.patterns);
      }
    } catch (error) {
      console.error('Failed to fetch patterns:', error);
    }
  };

  const fetchStats = async () => {
    try {
      const response = await fetch('/api/teach/stats');
      const data = await response.json();
      if (data.success) {
        setStats(data);
      }
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    }
  };

  const handleExecutePattern = (pattern) => {
    setSelectedPattern(pattern);
    setExecuteDrawer(true);
    form.resetFields();
  };

  const onExecute = async (values) => {
    setExecuting(true);
    try {
      const response = await fetch('/api/teach/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          pattern: selectedPattern.name,
          inputs: values
        })
      });

      const data = await response.json();

      if (data.success) {
        message.success(`✓ Pattern executed: ${selectedPattern.name}`);
        setExecuteDrawer(false);
        setTimeout(() => {
          fetchPatterns();
          fetchStats();
        }, 1000);
      } else {
        message.error(data.error || 'Execution failed');
      }
    } catch (error) {
      message.error('Failed to execute pattern: ' + error.message);
    } finally {
      setExecuting(false);
    }
  };

  const patternColumns = [
    {
      title: 'Pattern',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <strong>{text}</strong>,
      width: 200
    },
    {
      title: 'Sequence',
      dataIndex: 'actions',
      key: 'actions',
      render: (actions) => (
        <Space size="small" wrap>
          {actions.map((a, i) => (
            <Tag key={i} color="blue" style={{ fontSize: '11px' }}>
              {a}
            </Tag>
          ))}
        </Space>
      )
    },
    {
      title: 'Used',
      dataIndex: 'frequency',
      key: 'frequency',
      render: (freq) => <Badge count={freq} color="blue" />,
      width: 80
    },
    {
      title: 'Confidence',
      dataIndex: 'confidence',
      key: 'confidence',
      render: (conf) => {
        const value = parseFloat(conf);
        const color = value > 80 ? 'green' : value > 60 ? 'orange' : 'red';
        return (
          <Tag color={color}>
            {conf}
          </Tag>
        );
      },
      width: 120
    },
    {
      title: 'Action',
      key: 'action',
      render: (_, record) => (
        <Button
          type="primary"
          size="small"
          icon={<RocketOutlined />}
          onClick={() => handleExecutePattern(record)}
          disabled={parseFloat(record.confidence) < 60}
        >
          Execute
        </Button>
      ),
      width: 120
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1>
          <BrainOutlined style={{ marginRight: '8px', color: '#1890ff' }} />
          System Learning Dashboard
        </h1>
        <p style={{ color: '#666' }}>
          SupremeAI learns from your actions and can now execute complex work patterns autonomously
        </p>
      </div>

      {/* Statistics */}
      {stats && (
        <Row gutter={16} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Patterns Learned"
                value={stats.patternsLearned || 0}
                prefix={<BrainOutlined style={{ color: '#1890ff' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Autonomy Level"
                value={Math.min(100, (stats.patternsLearned || 0) * 20)}
                suffix="%"
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="System Status"
                value="Active"
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Auto-Learning"
                value="Enabled"
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* Learned Patterns Table */}
      <Card
        title={
          <Space>
            <HistoryOutlined />
            Learned Work Patterns
          </Space>
        }
        loading={loading}
        style={{ marginBottom: '24px' }}
      >
        <Table
          dataSource={patterns}
          columns={patternColumns}
          rowKey="name"
          pagination={{
            pageSize: 5,
            showTotal: (total) => `${total} patterns`
          }}
          locale={{
            emptyText: 'No patterns learned yet. Keep working, I\'m watching and learning! 🧠'
          }}
        />
      </Card>

      {/* Pattern Details */}
      <Card
        title={
          <Space>
            <CheckCircleOutlined />
            How It Works
          </Space>
        }
      >
        <div style={{ lineHeight: '1.8' }}>
          <h3>🚀 SupremeAI's Autonomous Execution System</h3>
          <ol>
            <li><strong>Recording</strong>: Every action you take is recorded (code generation, commits, tests, deploys, etc.)</li>
            <li><strong>Pattern Recognition</strong>: System analyzes action sequences to extract reusable patterns</li>
            <li><strong>Learning</strong>: Patterns stored with confidence scores based on success rates</li>
            <li><strong>Autonomous Execution</strong>: Click "Execute" to run the entire pattern without manual steps</li>
          </ol>

          <h3>📊 Default Patterns Available</h3>
          <ul>
            <li>
              <strong>feature_development</strong>: 
              Code Generation → Tests → Commit → Push
            </li>
            <li>
              <strong>bug_fix_cycle</strong>: 
              Fix Errors → Tests → Commit → Deploy
            </li>
          </ul>

          <h3>🎯 Example: Execute Feature Development</h3>
          <pre style={{
            background: '#f5f5f5',
            padding: '12px',
            borderRadius: '4px',
            overflow: 'auto'
          }}>
{`{
  "pattern": "feature_development",
  "inputs": {
    "requirement": "Add chat history to admin dashboard",
    "framework": "React",
    "targetBranch": "main"
  }
}

Response: System auto-generates code, runs tests,
commits, and pushes to your target branch! ✨`}
          </pre>
        </div>
      </Card>

      {/* Execute Pattern Drawer */}
      <Drawer
        title={`Execute Pattern: ${selectedPattern?.name}`}
        placement="right"
        width={500}
        onClose={() => setExecuteDrawer(false)}
        open={executeDrawer}
      >
        {selectedPattern && (
          <div>
            <Card
              size="small"
              style={{ marginBottom: '16px' }}
              title="Action Sequence"
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                {selectedPattern.actions && selectedPattern.actions.map((action, idx) => (
                  <div key={idx}>
                    <Tag color="blue">{idx + 1}</Tag>
                    <strong>{action}</strong>
                  </div>
                ))}
              </Space>
            </Card>

            <Card
              size="small"
              style={{ marginBottom: '16px' }}
              title="Confidence Quality"
            >
              <Progress
                percent={parseFloat(selectedPattern.confidence)}
                status={parseFloat(selectedPattern.confidence) > 80 ? 'success' : 'normal'}
              />
              <p style={{ fontSize: '12px', color: '#666', marginTop: '8px' }}>
                Used {selectedPattern.frequency} times • Success rate: {selectedPattern.confidence}
              </p>
            </Card>

            <Form
              form={form}
              layout="vertical"
              onFinish={onExecute}
            >
              <Form.Item
                label="Requirement / Description"
                name="requirement"
              >
                <Input.TextArea
                  placeholder="What do you want the system to do?"
                  rows={3}
                />
              </Form.Item>

              <Form.Item
                label="Framework / Technology"
                name="framework"
              >
                <Select
                  placeholder="Select framework"
                  options={[
                    { label: 'React', value: 'React' },
                    { label: 'Flutter', value: 'Flutter' },
                    { label: 'Spring Boot', value: 'Spring Boot' },
                    { label: 'Generic', value: 'Generic' }
                  ]}
                />
              </Form.Item>

              <Form.Item
                label="Target Branch"
                name="targetBranch"
              >
                <Input placeholder="main" />
              </Form.Item>

              <Space style={{ width: '100%' }} size="large">
                <Button
                  type="primary"
                  icon={executing ? <LoadingOutlined /> : <RocketOutlined />}
                  loading={executing}
                  htmlType="submit"
                  block
                >
                  {executing ? 'Executing...' : 'Execute Pattern'}
                </Button>
                <Button onClick={() => setExecuteDrawer(false)}>
                  Cancel
                </Button>
              </Space>
            </Form>
          </div>
        )}
      </Drawer>
    </div>
  );
}
