// AdminReports.tsx - Advanced Reporting Page

import React from 'react';
import { Layout, Card, Typography, Alert } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';

const { Title, Paragraph } = Typography;

const AdminReports: React.FC = () => {
  return (
    <AdminLayout title="Advanced Reporting">
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <BarChartOutlined style={{ fontSize: 64, color: '#faad14', marginBottom: 24 }} />
          <Title level={3}>Advanced Reporting</Title>
          <Paragraph type="secondary" style={{ maxWidth: 600, margin: '0 auto 24px' }}>
            Generate detailed reports on system usage, cost analysis, user activity, and performance metrics.
            Schedule recurring reports and export to multiple formats.
          </Paragraph>
          <Alert
            message="Reporting Module In Progress"
            description="Comprehensive reporting features are being developed and will be available soon."
            type="info"
            showIcon
            style={{ maxWidth: 600, margin: '0 auto' }}
          />
        </div>
      </Card>
    </AdminLayout>
  );
};

export default AdminReports;
