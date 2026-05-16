import React from 'react';
import { Table, Tag, Space, Button } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { User } from './types';
import { getUserRoleColor } from '../../constants/userRoles';

interface UserTableProps {
  users: User[];
  loading: boolean;
  deletingUserId: string | null;
  onEdit: (user: User) => void;
  onDeactivate: (uid: string) => void;
  onReactivate: (uid: string) => void;
  onDelete: (uid: string) => void;
}

const UserTable: React.FC<UserTableProps> = ({
  users,
  loading,
  deletingUserId,
  onEdit,
  onDeactivate,
  onReactivate,
  onDelete
}) => {
  const columns = [
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: (email: string) => <a style={{ fontWeight: 500 }}>{email}</a>,
    },
    {
      title: 'Display Name',
      dataIndex: 'displayName',
      key: 'displayName',
      render: (name: string) => name || <span style={{ opacity: 0.5 }}>-</span>,
    },
    {
      title: 'Role',
      key: 'role',
      render: (_: any, record: User) => {
        const role = record.tier?.toUpperCase() || 'USER';
        const color = getUserRoleColor(role);
        return <Tag color={color} style={{ borderRadius: '4px' }}>{role}</Tag>;
      },
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'error'} style={{ borderRadius: '4px' }}>
          {active ? 'Active' : 'Inactive'}
        </Tag>
      ),
    },
    {
      title: 'Usage / Quota',
      key: 'usage',
      render: (_: any, record: User) => {
        const used = record.currentUsage || 0;
        const quota = record.monthlyQuota || 0;
        const percent = quota > 0 ? Math.round((used / quota) * 100) : 0;
        return (
          <div>
            <div style={{ fontSize: '12px' }}>{used.toLocaleString()} / {quota.toLocaleString()}</div>
            <div style={{ fontSize: '10px', opacity: 0.7 }}>{percent}% Consumed</div>
          </div>
        );
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
      width: 150,
      render: (_: any, record: User) => (
        <Space>
          <Button 
            size="small" 
            icon={<EditOutlined />} 
            onClick={() => onEdit(record)}
            className="glass-button"
          >
            Edit
          </Button>
          {record.isActive && (
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              loading={deletingUserId === record.uid}
              onClick={() => onDeactivate(record.uid)}
              style={{ borderRadius: '6px' }}
            >
              Deactivate
            </Button>
          )}
          {!record.isActive && (
            <Button
              size="small"
              type="primary"
              onClick={() => onReactivate(record.uid)}
              style={{ background: '#10b981', borderColor: '#10b981' }}
            >
              Reactivate
            </Button>
          )}
          <Button
            size="small"
            danger
            type="link"
            icon={<DeleteOutlined />}
            onClick={() => onDelete(record.uid)}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={users}
      rowKey="uid"
      loading={loading}
      pagination={{ pageSize: 10, showSizeChanger: true }}
      scroll={{ x: 1000 }}
      className="glass-table"
      style={{ borderRadius: '12px', overflow: 'hidden' }}
    />
  );
};

export default UserTable;
