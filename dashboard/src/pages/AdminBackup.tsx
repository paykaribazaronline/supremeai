// AdminBackup.tsx - Backup & Restore Page

import React from 'react';
import { Layout, Card, Typography, Alert } from 'antd';
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';

const { Title, Paragraph } = Typography;

const AdminBackup: React.FC = () => {
  return (
    <AdminLayout title="Backup & Restore">
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <UploadOutlined style={{ fontSize: 64, color: '#52c41a', marginBottom: 24 }} />
          <Title level={3}>Backup & Restore</Title>
          <Paragraph type="secondary" style={{ maxWidth: 600, margin: '0 auto 24px' }}>
            Create full system backups, schedule automated backups, and restore from previous snapshots.
          </Paragraph>
          <Alert
            message="Coming Soon"
            description="Backup and restore functionality is under development and will be released soon."
            type="info"
            showIcon
            style={{ maxWidth: 600, margin: '0 auto' }}
          />
        </div>
      </Card>
    </AdminLayout>
  );
};

export default AdminBackup;
