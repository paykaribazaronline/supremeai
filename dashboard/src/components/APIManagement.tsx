// APIManagement.tsx - API Provider Management Module

import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, Select, Space, Tag, message, Tooltip, Popconfirm, Row, Col } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, TestTubeOutlined, CheckCircleOutlined } from '@ant-design/icons';

interface APIProvider {
    id: string;
    name: string;
    type: string;
    apiKey: string;
    status: 'active' | 'inactive' | 'error';
    lastTested: string;
    models: string[];
}

const APIManagement: React.FC = () => {
    const [providers, setProviders] = useState<APIProvider[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingId, setEditingId] = useState<string | null>(null);
    const [availableProviders, setAvailableProviders] = useState<any[]>([]);

    useEffect(() => {
        fetchProviders();
        fetchAvailableProviders();
    }, []);

    const fetchProviders = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/providers/configured', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setProviders(data);
            }
        } catch (error) {
            message.error('Failed to fetch API providers');
        } finally {
            setLoading(false);
        }
    };

    const fetchAvailableProviders = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/providers/available', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setAvailableProviders(data);
            }
        } catch (error) {
            console.error('Failed to fetch available providers:', error);
        }
    };

    const handleAddProvider = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const endpoint = editingId ? `/api/providers/${editingId}` : '/api/providers/add';
            const method = editingId ? 'PUT' : 'POST';

            const response = await fetch(endpoint, {
                method,
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });

            if (response.ok) {
                message.success(editingId ? 'Provider updated!' : 'Provider added!');
                setIsModalVisible(false);
                form.resetFields();
                setEditingId(null);
                fetchProviders();
            } else {
                message.error('Failed to save provider');
            }
        } catch (error) {
            message.error('Error saving provider');
        }
    };

    const handleTestConnection = async (providerId: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/providers/test/${providerId}`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (response.ok) {
                message.success('Connection test passed!');
                fetchProviders();
            } else {
                message.error('Connection test failed');
            }
        } catch (error) {
            message.error('Error testing connection');
        }
    };

    const handleDeleteProvider = async (providerId: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/providers/remove', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ providerId }),
            });

            if (response.ok) {
                message.success('Provider removed!');
                fetchProviders();
            } else {
                message.error('Failed to remove provider');
            }
        } catch (error) {
            message.error('Error removing provider');
        }
    };

    const columns = [
        { title: 'Provider Name', dataIndex: 'name', key: 'name' },
        { title: 'Type', dataIndex: 'type', key: 'type' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Tag color={status === 'active' ? 'green' : status === 'inactive' ? 'orange' : 'red'}>
                    {status.toUpperCase()}
                </Tag>
            ),
        },
        { title: 'Models', dataIndex: 'models', key: 'models', render: (models: string[]) => models.join(', ') },
        { title: 'Last Tested', dataIndex: 'lastTested', key: 'lastTested' },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: APIProvider) => (
                <Space size="small">
                    <Tooltip title="Test Connection">
                        <Button
                            icon={<TestTubeOutlined />}
                            size="small"
                            onClick={() => handleTestConnection(record.id)}
                            type="primary"
                        />
                    </Tooltip>
                    <Tooltip title="Edit">
                        <Button icon={<EditOutlined />} size="small" onClick={() => {
                            setEditingId(record.id);
                            form.setFieldsValue(record);
                            setIsModalVisible(true);
                        }} />
                    </Tooltip>
                    <Popconfirm
                        title="Delete Provider?"
                        description="This action cannot be undone."
                        onConfirm={() => handleDeleteProvider(record.id)}
                        okText="Delete"
                        cancelText="Cancel"
                    >
                        <Button icon={<DeleteOutlined />} size="small" danger />
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <Card
                title="API Provider Management"
                extra={
                    <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={() => {
                            setEditingId(null);
                            form.resetFields();
                            setIsModalVisible(true);
                        }}
                    >
                        Add API Provider
                    </Button>
                }
            >
                <Table columns={columns} dataSource={providers} loading={loading} rowKey="id" pagination={{ pageSize: 10 }} />
            </Card>

            <Modal
                title={editingId ? 'Edit API Provider' : 'Add New API Provider'}
                open={isModalVisible}
                onOk={() => form.submit()}
                onCancel={() => {
                    setIsModalVisible(false);
                    form.resetFields();
                    setEditingId(null);
                }}
            >
                <Form form={form} layout="vertical" onFinish={handleAddProvider}>
                    <Form.Item name="name" label="Provider Name" rules={[{ required: true }]}>
                        <Select placeholder="Select provider or enter custom name">
                            {availableProviders.map((p) => (
                                <Select.Option key={p.name} value={p.name}>
                                    {p.name} (Rank: {p.rank})
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item name="type" label="Type" rules={[{ required: true }]}>
                        <Select placeholder="Select provider type">
                            <Select.Option value="llm">Large Language Model</Select.Option>
                            <Select.Option value="image">Image Generation</Select.Option>
                            <Select.Option value="voice">Voice/Audio</Select.Option>
                            <Select.Option value="embedding">Embedding</Select.Option>
                            <Select.Option value="other">Other</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="apiKey" label="API Key" rules={[{ required: true }]}>
                        <Input.Password placeholder="Enter API key" />
                    </Form.Item>

                    <Form.Item name="models" label="Supported Models" rules={[{ required: true }]}>
                        <Select mode="tags" placeholder="Enter model names" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default APIManagement;
