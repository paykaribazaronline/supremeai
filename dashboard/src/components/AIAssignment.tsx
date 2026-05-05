// AIAssignment.tsx - Assign AI Agents to Tasks and Projects

import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Select, Input, Slider, Space, Tag, message, Row, Col, Progress } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';

interface AIAgent {
    id: string;
    name: string;
    role: string;
    assignedTasks: number;
    workload: number;
    status: 'active' | 'idle' | 'busy';
}

interface Assignment {
    id: string;
    agentId: string;
    agentName: string;
    taskName: string;
    priority: 'low' | 'medium' | 'high' | 'critical';
    progress: number;
    status: 'pending' | 'in-progress' | 'completed';
    createdAt: string;
}

const AIAssignment: React.FC = () => {
    const [agents, setAgents] = useState<AIAgent[]>([]);
    const [assignments, setAssignments] = useState<Assignment[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [selectedTab, setSelectedTab] = useState('assignments');

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const [agentsRes, assignmentsRes] = await Promise.all([
                fetch('/api/ai/agents', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('/api/assignments', { headers: { 'Authorization': `Bearer ${token}` } }),
            ]);

            if (agentsRes.ok) {
                const agentsData = await agentsRes.json();
                setAgents(agentsData);
            }
            if (assignmentsRes.ok) {
                const assignmentsData = await assignmentsRes.json();
                setAssignments(assignmentsData);
            }
        } catch (error) {
            message.error('Failed to fetch data');
        } finally {
            setLoading(false);
        }
    };

    const handleAssignTask = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/assignments/create', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });

            if (response.ok) {
                message.success('Task assigned successfully!');
                setIsModalVisible(false);
                form.resetFields();
                fetchData();
            }
        } catch (error) {
            message.error('Failed to assign task');
        }
    };

    const agentColumns = [
        { title: 'Agent Name', dataIndex: 'name', key: 'name' },
        { title: 'Role', dataIndex: 'role', key: 'role' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Tag color={status === 'active' ? 'green' : status === 'idle' ? 'blue' : 'orange'}>
                    {status.toUpperCase()}
                </Tag>
            ),
        },
        {
            title: 'Workload',
            dataIndex: 'workload',
            key: 'workload',
            render: (workload: number) => <Progress type="circle" percent={workload} width={50} />,
        },
        { title: 'Assigned Tasks', dataIndex: 'assignedTasks', key: 'assignedTasks' },
    ];

    const assignmentColumns = [
        { title: 'AI Agent', dataIndex: 'agentName', key: 'agentName' },
        { title: 'Task', dataIndex: 'taskName', key: 'taskName' },
        {
            title: 'Priority',
            dataIndex: 'priority',
            key: 'priority',
            render: (priority: string) => {
                const colorMap: any = { low: 'blue', medium: 'orange', high: 'red', critical: 'purple' };
                return <Tag color={colorMap[priority]}>{priority.toUpperCase()}</Tag>;
            },
        },
        {
            title: 'Progress',
            dataIndex: 'progress',
            key: 'progress',
            render: (progress: number) => <Progress percent={progress} />,
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const colorMap: any = { pending: 'default', 'in-progress': 'processing', completed: 'success' };
                return <Tag>{status.toUpperCase()}</Tag>;
            },
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: Assignment) => (
                <Space size="small">
                    <Button size="small" type="primary">Update</Button>
                    <Button size="small" icon={<DeleteOutlined />} danger />
                </Space>
            ),
        },
    ];

    return (
        <div>
            <Row gutter={16} style={{ marginBottom: '24px' }}>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div className="stat">
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {agents.filter((a) => a.status === 'active').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>Active Agents</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div className="stat">
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {assignments.filter((a) => a.status === 'in-progress').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>In Progress</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div className="stat">
                            <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                {assignments.filter((a) => a.status === 'completed').length}
                            </div>
                            <div style={{ fontSize: '12px', color: '#999' }}>Completed</div>
                        </div>
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card>
                        <div style={{ textAlign: 'center' }}>
                            <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalVisible(true)}>
                                Assign Task
                            </Button>
                        </div>
                    </Card>
                </Col>
            </Row>

            <Card title="Assignments" loading={loading}>
                <Table columns={assignmentColumns} dataSource={assignments} rowKey="id" pagination={{ pageSize: 10 }} />
            </Card>

            <Card title="AI Agents" style={{ marginTop: '24px' }} loading={loading}>
                <Table columns={agentColumns} dataSource={agents} rowKey="id" />
            </Card>

            <Modal
                title="Assign Task to AI Agent"
                open={isModalVisible}
                onOk={() => form.submit()}
                onCancel={() => {
                    setIsModalVisible(false);
                    form.resetFields();
                }}
                width={600}
            >
                <Form form={form} layout="vertical" onFinish={handleAssignTask}>
                    <Form.Item name="agentId" label="Select AI Agent" rules={[{ required: true }]}>
                        <Select placeholder="Choose an agent">
                            {agents.map((agent) => (
                                <Select.Option key={agent.id} value={agent.id}>
                                    {agent.name} ({agent.role}) - Workload: {agent.workload}%
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item name="taskName" label="Task Name" rules={[{ required: true }]}>
                        <Input placeholder="e.g., Analyze market trends" />
                    </Form.Item>

                    <Form.Item name="taskDescription" label="Task Description">
                        <Input.TextArea placeholder="Detailed task description..." rows={4} />
                    </Form.Item>

                    <Form.Item name="priority" label="Priority" rules={[{ required: true }]}>
                        <Select>
                            <Select.Option value="low">Low</Select.Option>
                            <Select.Option value="medium">Medium</Select.Option>
                            <Select.Option value="high">High</Select.Option>
                            <Select.Option value="critical">Critical</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="deadline" label="Deadline (hours)">
                        <Slider min={1} max={720} marks={{ 1: '1h', 24: '1d', 168: '1w', 720: '30d' }} />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default AIAssignment;
