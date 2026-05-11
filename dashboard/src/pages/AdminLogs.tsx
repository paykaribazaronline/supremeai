// AdminLogs.tsx - System Activity Logs Page

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Tag, Button, Space, message, Spin, Alert, Input, Select, Typography } from 'antd';
import { FileTextOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;

interface ActivityLog {
  id?: string;
  user: string;
  action: string;
  category: string;
  severity: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  outcome: string;
  details?: string;
  timestamp?: string;
}

const AdminLogs: React.FC = () => {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchText, setSearchText] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('');

  const fetchLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await authUtils.fetchWithAuth('/api/logs?severity=' + severityFilter);
      if (!response.ok) throw new Error('Failed to fetch logs');
      const data = await response.json();
      setLogs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [severityFilter]);

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (severity: string) => {
        const color = severity === 'ERROR' ? 'red' : severity === 'WARN' ? 'orange' : 'blue';
        return <Tag color={color}>{severity}</Tag>;
      },
    },
    {
      title: 'Outcome',
      dataIndex: 'outcome',
      key: 'outcome',
    },
  ];

  return (
    <AdminLayout title="System Activity Logs">
      <div style={{ padding: '24px', maxWidth: '1400px', margin: '0 auto' }}>
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
            <Typography.Title level={2} style={{ margin: 0 }}>System Activity Logs</Typography.Title>
            <Space>
              <Input.Search
                placeholder="Search logs..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: 300 }}
              />
              <Select
                value={severityFilter || undefined}
                onChange={(val) => setSeverityFilter(val || '')}
                style={{ width: 180 }}
              >
                <Option value="INFO">Info</Option>
                <Option value="WARN">Warning</Option>
                <Option value="ERROR">Error</Option>
                <Option value="DEBUG">Debug</Option>
              </Select>
              <Button icon={<ReloadOutlined />} onClick={fetchLogs}>Refresh</Button>
            </Space>
          </div>

          {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
          {error && <Alert type="error" message={error} action={<Button onClick={fetchLogs}>Retry</Button>} />}
          
          <Table
            columns={columns}
            dataSource={logs}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 20 }}
          />
        </Card>
      </div>
    </AdminLayout>
  );
};

export default AdminLogs;
