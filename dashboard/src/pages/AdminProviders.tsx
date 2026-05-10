// AdminProviders.tsx - AI Provider Management Page

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message, Spin, Alert, Popconfirm } from 'antd';
import { RobotOutlined, PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;

interface Provider {
  id?: string;
  name: string;
  providerType: string;
  baseUrl: string;
  apiKey?: string;
  status: 'active' | 'inactive' | 'error';
  models?: string[];
  priority?: number;
  createdAt?: string;
}

const AdminProviders: React.FC = () => {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [form] = Form.useForm();

  const fetchProviders = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/admin/providers/configured', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to fetch providers');
      const data = await response.json();
      // Backend returns { providers: [...] }
      const provList: Provider[] = data.providers || [];
      setProviders(provList);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load providers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  const handleSubmit = async (values: any) => {
    try {
      const token = authUtils.getToken();
      const payload: Provider = {
        ...values,
        status: values.status || 'active',
        models: values.models ? values.models.split(',').map((m: string) => m.trim()) : [],
      };
      let response;
      if (editingProvider && editingProvider.id) {
        response = await fetch(`/api/admin/providers/${editingProvider.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
          body: JSON.stringify(payload),
        });
      } else {
        response = await fetch('/api/admin/providers/add', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
          body: JSON.stringify(payload),
        });
      }
      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.error || err.message || 'Failed to save provider');
      }
      message.success('Provider saved successfully');
      setModalVisible(false);
      form.resetFields();
      fetchProviders();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const token = authUtils.getToken();
      // Prefer DELETE /api/admin/providers/{id}
      const response = await fetch(`/api/admin/providers/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to delete provider');
      message.success('Provider deleted');
      fetchProviders();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <strong>{text}</strong>,
    },
    {
      title: 'Type',
      dataIndex: 'providerType',
      key: 'providerType',
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      ellipsis: true,
    },
    {
      title: 'Models',
      dataIndex: 'models',
      key: 'models',
      render: (models: string[]) => models ? models.slice(0, 3).join(', ') + (models.length > 3 ? ` +${models.length - 3} more` : '') : '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const color = status === 'active' ? 'green' : status === 'error' ? 'red' : 'default';
        return <Tag color={color}>{status.toUpperCase()}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Provider) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => {
            setEditingProvider(record);
            form.setFieldsValue({ ...record, models: record.models?.join(', ') });
            setModalVisible(true);
          }}>
            Edit
          </Button>
          <Popconfirm title="Delete provider?" onConfirm={() => handleDelete(record.id!)}>
            <Button size="small" danger icon={<DeleteOutlined />}>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <AdminLayout title="AI Provider Management">
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => {
            setEditingProvider(null);
            form.resetFields();
            setModalVisible(true);
          }}>
            Add Provider
          </Button>
          <Button icon={<ReloadOutlined />} style={{ marginLeft: 8 }} onClick={fetchProviders}>
            Refresh
          </Button>
        </div>

        {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
        {error && <Alert type="error" message={error} action={<Button onClick={fetchProviders}>Retry</Button>} />}

        {!loading && !error && (
          <Table
            columns={columns}
            dataSource={providers}
            rowKey="id"
            pagination={{ pageSize: 15 }}
          />
        )}
      </Card>

      <Modal
        title={editingProvider ? 'Edit Provider' : 'Add New Provider'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={500}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label="Provider Name" rules={[{ required: true }]}>
            <Input placeholder="e.g., OpenAI" />
          </Form.Item>
          <Form.Item name="providerType" label="Type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              <Option value="openai">OpenAI</Option>
              <Option value="anthropic">Anthropic</Option>
              <Option value="google">Google AI</Option>
              <Option value="custom">Custom</Option>
            </Select>
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL" rules={[{ required: true }]}>
            <Input placeholder="https://api.openai.com/v1" />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key">
            <Input.Password placeholder="sk-..." />
          </Form.Item>
          <Form.Item name="models" label="Models (comma-separated)">
            <Input placeholder="gpt-4, gpt-3.5-turbo" />
          </Form.Item>
          <Form.Item name="status" label="Status" initialValue="active">
            <Select>
              <Option value="active">Active</Option>
              <Option value="inactive">Inactive</Option>
              <Option value="error">Error</Option>
            </Select>
          </Form.Item>
          <Form.Item style={{ marginTop: 24 }}>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingProvider ? 'Update' : 'Add'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </AdminLayout>
  );
};

export default AdminProviders;
