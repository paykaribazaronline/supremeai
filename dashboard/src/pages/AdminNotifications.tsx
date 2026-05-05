// AdminNotifications.tsx - Notification Management Page

import React from 'react';
import { Layout, Card, Typography, Alert, Button } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';

const { Title, Paragraph } = Typography;

const AdminNotifications: React.FC = () => {
  return (
    <AdminLayout title="Notification Management">
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <BellOutlined style={{ fontSize: 64, color: '#667eea', marginBottom: 24 }} />
          <Title level={3}>Notification Management</Title>
          <Paragraph type="secondary" style={{ maxWidth: 600, margin: '0 auto 24px' }}>
            Configure system-wide notification channels, manage user preferences, and set up alert rules.
          </Paragraph>
          <Alert
            message="Module Under Development"
            description="This feature is being built and will be available in a future update. Stay tuned!"
            type="info"
            showIcon
            style={{ maxWidth: 600, margin: '0 auto' }}
          />
        </div>
      </Card>
    </AdminLayout>
  );
};

export default AdminNotifications;
