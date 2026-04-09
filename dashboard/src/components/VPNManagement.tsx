// VPNManagement.tsx - VPN Configuration and Management

import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, Select, Switch, Space, Tag, message, Popconfirm, Row, Col, Alert } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface VPNConnection {
    id: string;
    name: string;
    protocol: string;
    server: string;
    port: number;
    status: 'connected' | 'disconnected' | 'error';
    encryption: string;
    autoConnect: boolean;
    lastConnected: string;
}

const VPNManagement: React.FC = () => {
    const [vpnConnections, setVpnConnections] = useState<VPNConnection[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingId, setEditingId] = useState<string | null>(null);

    useEffect(() => {
        fetchVPNConnections();
    }, []);

    const fetchVPNConnections = async () => {
        setLoading(true);
        try {
            const response = await fetch('/api/vpn/list', {
                headers: authUtils.getAuthHeaders(),
            });
            if (response.ok) {
                const data = await response.json();
                setVpnConnections(data);
            }
        } catch (error) {
            message.error('Failed to fetch VPN connections');
        } finally {
            setLoading(false);
        }
    };

    const handleSaveVPN = async (values: any) => {
        try {
            const token = authUtils.getToken();
            const response = await fetch(editingId ? `/api/vpn/${editingId}` : '/api/vpn/add', {
                method: editingId ? 'PUT' : 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });

            if (response.ok) {
                message.success(editingId ? 'VPN updated!' : 'VPN added!');
                setIsModalVisible(false);
                form.resetFields();
                setEditingId(null);
                fetchVPNConnections();
            }
        } catch (error) {
            message.error('Failed to save VPN');
        }
    };

    const handleConnectVPN = async (vpnId: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/vpn/${vpnId}/connect`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (response.ok) {
                message.success('VPN Connected!');
                fetchVPNConnections();
            }
        } catch (error) {
            message.error('Failed to connect VPN');
        }
    };

    const handleDisconnectVPN = async (vpnId: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/vpn/${vpnId}/disconnect`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (response.ok) {
                message.success('VPN Disconnected!');
                fetchVPNConnections();
            }
        } catch (error) {
            message.error('Failed to disconnect VPN');
        }
    };

    const columns = [
        { title: 'VPN Name', dataIndex: 'name', key: 'name' },
        { title: 'Protocol', dataIndex: 'protocol', key: 'protocol' },
        { title: 'Server', dataIndex: 'server', key: 'server' },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <>
                    {status === 'connected' ? (
                        <>
                            <CheckCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
                            <Tag color="green">Connected</Tag>
                        </>
                    ) : (
                        <>
                            <CloseCircleOutlined style={{ color: '#ff4d4f', marginRight: '8px' }} />
                            <Tag color="red">Disconnected</Tag>
                        </>
                    )}
                </>
            ),
        },
        { title: 'Encryption', dataIndex: 'encryption', key: 'encryption' },
        { title: 'Auto Connect', dataIndex: 'autoConnect', key: 'autoConnect', render: (v: boolean) => v ? '✓' : '✗' },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: VPNConnection) => (
                <Space size="small">
                    {record.status === 'connected' ? (
                        <Button size="small" onClick={() => handleDisconnectVPN(record.id)} danger>
                            Disconnect
                        </Button>
                    ) : (
                        <Button size="small" type="primary" onClick={() => handleConnectVPN(record.id)}>
                            Connect
                        </Button>
                    )}
                    <Button icon={<EditOutlined />} size="small" onClick={() => {
                        setEditingId(record.id);
                        form.setFieldsValue(record);
                        setIsModalVisible(true);
                    }} />
                    <Popconfirm title="Delete?" onConfirm={() => {
                        setVpnConnections(vpnConnections.filter((v) => v.id !== record.id));
                        message.success('VPN deleted!');
                    }}>
                        <Button icon={<DeleteOutlined />} size="small" danger />
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <Alert
                message="VPN Management"
                description="Manage secure VPN connections for encrypted API communication and privacy"
                type="info"
                showIcon
                style={{ marginBottom: '24px' }}
            />

            <Card
                title="VPN Connections"
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
                        Add VPN
                    </Button>
                }
            >
                <Table columns={columns} dataSource={vpnConnections} loading={loading} rowKey="id" />
            </Card>

            <Modal
                title={editingId ? 'Edit VPN' : 'Add VPN Connection'}
                open={isModalVisible}
                onOk={() => form.submit()}
                onCancel={() => {
                    setIsModalVisible(false);
                    form.resetFields();
                    setEditingId(null);
                }}
            >
                <Form form={form} layout="vertical" onFinish={handleSaveVPN}>
                    <Form.Item name="name" label="VPN Label" rules={[{ required: true }]}>
                        <Input placeholder="e.g., Private US Gateway" />
                    </Form.Item>

                    <Form.Item name="protocol" label="Protocol" rules={[{ required: true }]}>
                        <Select>
                            <Select.Option value="OpenVPN">OpenVPN</Select.Option>
                            <Select.Option value="WireGuard">WireGuard</Select.Option>
                            <Select.Option value="L2TP">L2TP/IPSec</Select.Option>
                            <Select.Option value="PPTP">PPTP</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="server" label="VPN Server" rules={[{ required: true }]}>
                        <Input placeholder="vpn.example.com" />
                    </Form.Item>

                    <Form.Item name="port" label="Port" rules={[{ required: true }]}>
                        <Input type="number" placeholder="1194" />
                    </Form.Item>

                    <Form.Item name="encryption" label="Encryption">
                        <Select>
                            <Select.Option value="AES-256">AES-256</Select.Option>
                            <Select.Option value="AES-128">AES-128</Select.Option>
                            <Select.Option value="ChaCha20">ChaCha20</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item name="credentials" label="Credentials">
                        <Input.TextArea placeholder="Paste VPN config/credentials here" rows={4} />
                    </Form.Item>

                    <Form.Item name="autoConnect" label="Auto Connect" valuePropName="checked">
                        <Switch />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default VPNManagement;
