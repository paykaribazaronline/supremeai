// AdminUsers.tsx - User Management Page
// Migrated from admin-users.html to React SPA

import React, { useState, useEffect, useMemo } from 'react';
import { message, Spin, Alert } from 'antd';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';
import { User, UserSortField } from '../components/users/types';
import UserTable from '../components/users/UserTable';
import UserModal from '../components/users/UserModal';
import UserActionToolbar from '../components/users/UserActionToolbar';
import { Form } from 'antd';

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [deletingUserId, setDeletingUserId] = useState<string | null>(null);
  
  // Search and sort state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<UserSortField | null>(null);
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('ascend');

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/users');
      if (!response.ok) throw new Error('Failed to fetch users');
      const result = await response.json();
      setUsers(result.data?.users || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue({
      email: user.email,
      displayName: user.displayName || '',
      tier: user.tier?.toLowerCase() || 'free',
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values: any) => {
    setSubmitLoading(true);
    try {
      if (editingUser) {
        if (values.tier !== editingUser.tier.toLowerCase()) {
          const tierUpper = values.tier.toUpperCase();
          const resp = await authUtils.fetchWithAuth(`/api/admin/users/${editingUser.uid}/tier`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tier: tierUpper }),
          });
          if (!resp.ok) throw new Error('Failed to update tier');
          message.success('User tier updated successfully');
        } else {
          message.info('No changes detected');
        }
      } else {
        const { email, password, displayName, tier } = values;
        const resp = await authUtils.fetchWithAuth('/api/admin/users/create', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
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
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDeactivate = async (uid: string) => {
    setDeletingUserId(uid);
    try {
      const resp = await authUtils.fetchWithAuth(`/api/admin/users/${uid}/deactivate`, {
        method: 'PUT',
      });
      if (!resp.ok) throw new Error('Failed to deactivate user');
      message.success('User deactivated');
      fetchUsers();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to deactivate');
    } finally {
      setDeletingUserId(null);
    }
  };

  const processedUsers = useMemo(() => {
    let result = users.filter(user => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return user.email.toLowerCase().includes(term) ||
             (user.displayName && user.displayName.toLowerCase().includes(term));
    });

    if (sortBy) {
      result.sort((a, b) => {
        let aVal: any = a[sortBy as keyof User] ?? '';
        let bVal: any = b[sortBy as keyof User] ?? '';

        if (sortBy === 'usagePercent') {
          aVal = (a.currentUsage || 0) / (a.monthlyQuota || 1);
          bVal = (b.currentUsage || 0) / (b.monthlyQuota || 1);
        } else if (sortBy === 'lastLoginAt') {
          const aTime = aVal ? new Date(aVal as string).getTime() : 0;
          const bTime = bVal ? new Date(bVal as string).getTime() : 0;
          return sortOrder === 'ascend' ? aTime - bTime : bTime - aTime;
        }

        if (typeof aVal === 'string' && typeof bVal === 'string') {
          return sortOrder === 'ascend' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
        }
        
        if (typeof aVal === 'number' && typeof bVal === 'number') {
          return sortOrder === 'ascend' ? aVal - bVal : bVal - aVal;
        }

        return 0;
      });
    }

    return result;
  }, [users, searchTerm, sortBy, sortOrder]);

  return (
    <AdminLayout title="User Management">
      <div className="admin-content-fade-in">
        <UserActionToolbar
          searchTerm={searchTerm}
          setSearchTerm={setSearchTerm}
          sortBy={sortBy}
          setSortBy={setSortBy}
          sortOrder={sortOrder}
          setSortOrder={setSortOrder}
          onAddUser={handleAdd}
          onRefresh={fetchUsers}
        />

        {error && (
          <Alert 
            type="error" 
            message="Connection Error" 
            description={error} 
            showIcon 
            style={{ marginBottom: 16, borderRadius: '8px' }}
          />
        )}

        {loading && !users.length ? (
          <div style={{ textAlign: 'center', padding: '100px' }}>
            <Spin size="large" tip="Loading system users..." />
          </div>
        ) : (
          <UserTable
            users={processedUsers}
            loading={loading}
            deletingUserId={deletingUserId}
            onEdit={handleEdit}
            onDeactivate={handleDeactivate}
          />
        )}

        <UserModal
          open={modalVisible}
          editingUser={editingUser}
          onCancel={() => setModalVisible(false)}
          onFinish={handleSubmit}
          loading={submitLoading}
          form={form}
        />
      </div>
    </AdminLayout>
  );
};

export default AdminUsers;
