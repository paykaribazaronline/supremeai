// AdminLogs.tsx - System Activity Logs Page

import React, { useState, useEffect } from 'react';
import { Layout, Card, Table, Tag, Button, Space, message, Spin, Alert, Input, Select } from 'antd';
import { FileTextOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

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
      const token = authUtils.getToken();
      // Use admin/control/history endpoint which returns recent activity logs (up to 100)
      const response = await fetch('/api/admin/control/history', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to fetch logs');
      const data: ActivityLog[] = await response.json();
      setLogs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const filteredLogs = logs.filter((log) => {
    const matchesSearch = !searchText || 
      log.action?.toLowerCase().includes(searchText.toLowerCase()) ||
      log.user?.toLowerCase().includes(searchText.toLowerCase()) ||
      log.details?.toLowerCase().includes(searchText.toLowerCase());
    const matchesSeverity = !severityFilter || log.severity === severityFilter;
    return matchesSearch && matchesSeverity;
  });

  const severityColors: Record<string, string> = {
    INFO: 'blue',
    WARN: 'orange',
    ERROR: 'red',
    DEBUG: 'default',
  };

  const columns = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (ts: string) => new Date(ts).toLocaleString(),
      width: 180,
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
      width: 120,
      render: (user: string) => user ? user.substring(0, 12) + (user.length > 12 ? '...' : '') : '-',
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 180,
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      render: (cat: string) => <Tag>{cat}</Tag>,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      render: (sev: string) => <Tag color={severityColors[sev] || 'default'}>{sev}</Tag>,
    },
    {
      title: 'Details',
      dataIndex: 'details',
      key: 'details',
      ellipsis: true,
    },
  ];

  return (
    <AdminLayout title="Activity Logs">
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
          <Input
            placeholder="Search logs..."
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
          />
          <Select
            placeholder="Filter by severity"
            allowClear
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
        </div>

        {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
        {error && <Alert type="error" message={error} action={<Button onClick={fetchLogs}>Retry</Button>} />}

        {!loading && !error && (
          <Table
            columns={columns}
            dataSource={filteredLogs}
            rowKey="id"
            pagination={{ pageSize: 20 }}
            scroll={{ x: 1000 }}
            size="middle"
          />
        )}
      </Card>
    </AdminLayout>
  );
};

export default AdminLogs;
