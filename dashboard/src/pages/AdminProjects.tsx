// AdminProjects.tsx - Project Management Page

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message, Spin, Alert } from 'antd';
import { FolderOutlined, PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;

interface Project {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

const AdminProjects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [form] = Form.useForm();

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
