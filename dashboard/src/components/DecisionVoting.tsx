// DecisionVoting.tsx - AI Decision Making and Voting System

import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, Select, Checkbox, Tag, message, Row, Col, Progress, Timeline, Avatar } from 'antd';
import { PlusOutlined, CheckCircleOutlined, CloseCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface Vote {
    agentName: string;
    vote: 'approve' | 'reject' | 'abstain';
    confidence: number;
    reasoning: string;
}

interface Decision {
    id: string;
    proposalName: string;
    description: string;
    proposedBy: string;
    createdAt: string;
    deadline: string;
    status: 'pending' | 'approved' | 'rejected' | 'abstained';
    votes: Vote[];
    approvalRate: number;
}

const DecisionVoting: React.FC = () => {
    const [decisions, setDecisions] = useState<Decision[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [selectedDecision, setSelectedDecision] = useState<Decision | null>(null);
    const [form] = Form.useForm();

    useEffect(() => {
        fetchDecisions();
        const interval = setInterval(fetchDecisions, 10000); // Refresh every 10s
        return () => clearInterval(interval);
    }, []);

    const fetchDecisions = async () => {
        setLoading(true);
        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/decisions/list', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setDecisions(data);
            }
        } catch (error) {
            message.error('Failed to fetch decisions');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateProposal = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/decisions/create', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });

            if (response.ok) {
                message.success('Proposal created! AI agents are voting...');
                setIsModalVisible(false);
                form.resetFields();
                fetchDecisions();
            }
        } catch (error) {
            message.error('Failed to create proposal');
        }
    };

    const columns = [
        { title: 'Proposal', dataIndex: 'proposalName', key: 'proposalName', width: 150 },
        { title: 'Proposed By', dataIndex: 'proposedBy', key: 'proposedBy' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const colorMap: any = {
                    pending: 'processing',
                    approved: 'success',
                    rejected: 'error',
                    abstained: 'default',
                };
                return <Tag color={colorMap[status]}>{status.toUpperCase()}</Tag>;
            },
        },
        {
            title: 'Approval Rate',
            dataIndex: 'approvalRate',
            key: 'approvalRate',
            render: (rate: number) => <Progress type="circle" percent={rate} width={50} />,
        },
        { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: Decision) => (
                <Button type="link" onClick={() => setSelectedDecision(record)}>
                    View Votes
                </Button>
            ),
        },
    ];

    return (
        <div>
            <Card
                title="AI Decision Making & Voting System"
                extra={
                    <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={() => setIsModalVisible(true)}
                    >
                        Create Proposal
                    </Button>
                }
            >
                <Table columns={columns} dataSource={decisions} loading={loading} rowKey="id" pagination={{ pageSize: 10 }} />
            </Card>

            {selectedDecision && (
                <Card title={`Voting Details: ${selectedDecision.proposalName}`} style={{ marginTop: '24px' }}>
                    <Row gutter={16} style={{ marginBottom: '24px' }}>
                        <Col xs={24} sm={12} lg={6}>
                            <div className="stat">
                                <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                                    {selectedDecision.votes.filter((v) => v.vote === 'approve').length}
                                </div>
                                <div style={{ fontSize: '12px', color: '#999' }}>Votes for</div>
                            </div>
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <div className="stat">
                                <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                                    {selectedDecision.votes.filter((v) => v.vote === 'reject').length}
                                </div>
                                <div style={{ fontSize: '12px', color: '#999' }}>Votes against</div>
                            </div>
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <div className="stat">
                                <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                                    {selectedDecision.votes.filter((v) => v.vote === 'abstain').length}
                                </div>
                                <div style={{ fontSize: '12px', color: '#999' }}>Abstained</div>
                            </div>
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <div className="stat">
                                <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                                    {selectedDecision.approvalRate}%
                                </div>
                                <div style={{ fontSize: '12px', color: '#999' }}>Approval Rate</div>
                            </div>
                        </Col>
                    </Row>

                    <div style={{ marginTop: '24px' }}>
                        <h3>Agent Votes</h3>
                        <Timeline>
                            {selectedDecision.votes.map((vote, idx) => (
                                <Timeline.Item
                                    key={idx}
                                    dot={
                                        vote.vote === 'approve' ? (
                                            <CheckCircleOutlined style={{ color: '#52c41a', fontSize: '16px' }} />
                                        ) : vote.vote === 'reject' ? (
                                            <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: '16px' }} />
                                        ) : (
                                            <QuestionCircleOutlined style={{ color: '#faad14', fontSize: '16px' }} />
                                        )
                                    }
                                >
                                    <div>
                                        <strong>{vote.agentName}</strong> - {vote.vote.toUpperCase()}
                                        <br />
                                        <span style={{ fontSize: '12px', color: '#999' }}>
                                            Confidence: {vote.confidence}% | {vote.reasoning}
                                        </span>
                                    </div>
                                </Timeline.Item>
                            ))}
                        </Timeline>
                    </div>

                    <Button
                        style={{ marginTop: '16px' }}
                        onClick={() => setSelectedDecision(null)}
                    >
                        Close
                    </Button>
                </Card>
            )}

            <Modal
                title="Create New Proposal for AI Voting"
                open={isModalVisible}
                onOk={() => form.submit()}
                onCancel={() => {
                    setIsModalVisible(false);
                    form.resetFields();
                }}
                width={600}
            >
                <Form form={form} layout="vertical" onFinish={handleCreateProposal}>
                    <Form.Item name="proposalName" label="Proposal Title" rules={[{ required: true }]}>
                        <Input placeholder="e.g., Switch to faster AI model" />
                    </Form.Item>

                    <Form.Item name="description" label="Description" rules={[{ required: true }]}>
                        <Input.TextArea placeholder="Detailed proposal description..." rows={4} />
                    </Form.Item>

                    <Form.Item name="category" label="Category">
                        <Select>
                            <Select.Option value="optimization">Optimization</Select.Option>
                            <Select.Option value="feature">Feature</Select.Option>
                            <Select.Option value="security">Security</Select.Option>
                            <Select.Option value="resource">Resource</Select.Option>
                            <Select.Option value="other">Other</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="affectedAgents" label="Affected Agents">
                        <Checkbox.Group options={[
                            { label: 'Architect Agent', value: 'architect' },
                            { label: 'Builder Agent', value: 'builder' },
                            { label: 'Review Agent', value: 'reviewer' },
                        ]} />
                    </Form.Item>

                    <Form.Item name="votingPeriodHours" label="Voting Period (hours)" initialValue={24}>
                        <Select>
                            <Select.Option value={1}>1 hour</Select.Option>
                            <Select.Option value={6}>6 hours</Select.Option>
                            <Select.Option value={12}>12 hours</Select.Option>
                            <Select.Option value={24}>24 hours</Select.Option>
                            <Select.Option value={48}>48 hours</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="requireUnanimous" valuePropName="checked">
                        <Checkbox>Require unanimous approval</Checkbox>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default DecisionVoting;
