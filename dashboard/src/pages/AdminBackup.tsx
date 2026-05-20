import React, { useState, useEffect } from 'react';
import { Layout, Card, Typography, Alert, Table, Button, Space, message, Tag, Modal, Input } from 'antd';
import { UploadOutlined, DownloadOutlined, CloudUploadOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Title, Paragraph } = Typography;

interface BackupItem {
  id: string;
  name: string;
  size: string;
  timestamp: string;
  status: string;
}

const AdminBackup: React.FC = () => {
  const [backups, setBackups] = useState<BackupItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [triggering, setTriggering] = useState(false);

  const fetchBackups = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/backup/list');
      if (!response.ok) throw new Error('Failed to fetch backups');
      const result = await response.json();
      setBackups(result.data || []);
    } catch (err) {
      message.error('Failed to load backups');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBackups();
  }, []);

  const handleTriggerBackup = async () => {
    setTriggering(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/backup/trigger', {
        method: 'POST',
        body: JSON.stringify({ name: `Manual Backup ${new Date().toLocaleString()}` })
      });
      if (!response.ok) throw new Error('Failed to trigger backup');
      message.success('Backup sequence started');
      fetchBackups();
    } catch (err) {
      message.error('Backup trigger failed');
    } finally {
      setTriggering(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/backup/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Delete failed');
      message.success('Backup deleted');
      fetchBackups();
    } catch (err) {
      message.error('Delete failed');
    }
  };

  const columns = [
    { title: 'Backup Name', dataIndex: 'name', key: 'name' },
    { title: 'Size', dataIndex: 'size', key: 'size', width: 120 },
    { 
      title: 'Status', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'COMPLETED' ? 'green' : 'blue'}>{status}</Tag>
      )
    },
    { title: 'Date', dataIndex: 'timestamp', key: 'timestamp', width: 200 },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: BackupItem) => (
        <Space>
          <Button size="small" icon={<DownloadOutlined />}>Download</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>Delete</Button>
        </Space>
      )
    }
  ];

  return (
    <AdminLayout title="Backup & Restore">
      <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
        <Card className="glass-card" title="System Snapshot Management" extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchBackups}>Refresh</Button>
            <Button type="primary" icon={<CloudUploadOutlined />} loading={triggering} onClick={handleTriggerBackup}>
              Create New Backup
            </Button>
          </Space>
        }>
          <Paragraph>
            Secure your system data by creating encrypted snapshots. You can restore your entire configuration, 
            including AI provider settings, API keys, and user data from any previous backup.
          </Paragraph>
          
          <Table 
            dataSource={backups} 
            columns={columns} 
            rowKey="id" 
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      </div>
    </AdminLayout>
  );
};

export default AdminBackup;
