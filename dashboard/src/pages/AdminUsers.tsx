// AdminUsers.tsx - User Management Page
// Migrated from admin-users.html to React SPA

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message, Alert, Spin } from 'antd';
import { UserOutlined, PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;

interface User {
  uid: string;
  email: string;
  displayName: string;
  tier: string;
  isActive: boolean;
  currentUsage: number;
  monthlyQuota: number;
  createdAt: string | null;
  lastLoginAt: string | null;
}

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/accounts', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to fetch users');
      const data: User[] = await response.json();
      setUsers(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue({
      email: user.email,
      displayName: user.displayName || '',
      tier: user.tier?.toLowerCase() || 'free',
    });
    setModalVisible(true);
  };

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleSubmit = async (values: any) => {
    try {
      const token = authUtils.getToken();
      // For now, only tier update is supported via backend.
      // Creation would require POST /api/accounts/create - exists.
      if (editingUser) {
        // Update tier if changed
        if (values.tier !== editingUser.tier.toLowerCase()) {
          const tierUpper = values.tier.toUpperCase();
          const resp = await fetch(`/api/accounts/${editingUser.uid}/tier`, {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`,
            },
            body: JSON.stringify({ tier: tierUpper }),
          });
          if (!resp.ok) throw new Error('Failed to update tier');
          message.success('User updated successfully');
        } else {
          message.success('No changes');
        }
      } else {
        // Create new user
        const { email, password, displayName, tier } = values;
        if (!password) {
          message.error('Password is required for new user');
          return;
        }
        const resp = await fetch('/api/accounts/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
          body: JSON.stringify({ email, password, displayName, tier: tier.toUpperCase() }),
        });
        if (!resp.ok) {
          const err = await resp.json();
          throw new Error(err.error || 'Failed to create user');
        }
        message.success('User created successfully');
      }
      setModalVisible(false);
      form.resetFields();
      fetchUsers();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const handleDeactivate = async (uid: string) => {
    try {
      const token = authUtils.getToken();
      const resp = await fetch(`/api/accounts/${uid}/deactivate`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!resp.ok) throw new Error('Failed to deactivate user');
      message.success('User deactivated');
      fetchUsers();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to deactivate');
    }
  };

  const columns = [
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: (email: string) => <a>{email}</a>,
    },
    {
      title: 'Display Name',
      dataIndex: 'displayName',
      key: 'displayName',
      render: (name: string) => name || '-',
    },
    {
      title: 'Role',
      key: 'role',
      render: (_: any, record: User) => {
        const role = record.tier?.toUpperCase() || 'USER';
        const color =
          role === 'ADMIN' ? 'red' :
          role === 'PRO' ? 'blue' :
          role === 'ENTERPRISE' ? 'purple' : 'default';
        return <Tag color={color}>{role}</Tag>;
      },
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'error'}>{active ? 'Active' : 'Inactive'}</Tag>
      ),
    },
    {
      title: 'Current Usage',
      key: 'usage',
      render: (_: any, record: User) => {
        const used = record.currentUsage || 0;
        const quota = record.monthlyQuota || 0;
        const percent = quota > 0 ? Math.round((used / quota) * 100) : 0;
        return `${used.toLocaleString()} / ${quota.toLocaleString()} (${percent}%)`;
      },
    },
    {
      title: 'Last Login',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      render: (date: string | null) => (date ? new Date(date).toLocaleString() : 'Never'),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: User) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            Edit
          </Button>
          {record.isActive && (
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDeactivate(record.uid)}
            >
              Deactivate
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <AdminLayout title="User Management">
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <div>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              Add User
            </Button>
            <Button icon={<ReloadOutlined />} style={{ marginLeft: 8 }} onClick={fetchUsers}>
              Refresh
            </Button>
          </div>
        </div>

        {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
        {error && <Alert type="error" message={error} action={<Button onClick={fetchUsers}>Retry</Button>} />}
        
        {!loading && !error && (
          <Table
            columns={columns}
            dataSource={users}
            rowKey="uid"
            pagination={{ pageSize: 15 }}
            scroll={{ x: 1200 }}
          />
        )}
      </Card>

      <Modal
        title={editingUser ? 'Edit User' : 'Add New User'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={500}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, type: 'email' }]}
          >
            <Input placeholder="user@example.com" disabled={!!editingUser} />
          </Form.Item>

          <Form.Item
            name="displayName"
            label="Display Name"
          >
            <Input placeholder="John Doe" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              name="password"
              label="Password"
              rules={[{ required: true, min: 6 }]}
            >
              <Input.Password placeholder="At least 6 characters" />
            </Form.Item>
          )}

          <Form.Item
            name="tier"
            label="Tier / Role"
            rules={[{ required: true }]}
          >
            <Select placeholder="Select tier">
              <Option value="free">Free</Option>
              <Option value="pro">Pro</Option>
              <Option value="enterprise">Enterprise</Option>
              <Option value="admin">Admin</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginTop: 24 }}>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingUser ? 'Update' : 'Create'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </AdminLayout>
  );
};

export default AdminUsers;
