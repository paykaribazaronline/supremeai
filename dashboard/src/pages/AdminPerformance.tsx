// AdminPerformance.tsx - Performance Metrics Page

import React from 'react';
import { Layout, Card, Typography, Alert } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';

const { Title, Paragraph } = Typography;

const AdminPerformance: React.FC = () => {
  return (
    <AdminLayout title="Performance Metrics">
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <LineChartOutlined style={{ fontSize: 64, color: '#722ed1', marginBottom: 24 }} />
          <Title level={3}>Performance Metrics</Title>
          <Paragraph type="secondary" style={{ maxWidth: 600, margin: '0 auto 24px' }}>
            Detailed API response times, throughput, error rates, and provider-specific performance statistics.
          </Paragraph>
          <Alert
            message="Performance Dashboard Coming Soon"
            description="Advanced performance analytics are under development."
            type="info"
            showIcon
            style={{ maxWidth: 600, margin: '0 auto' }}
          />
        </div>
      </Card>
    </AdminLayout>
  );
};

export default AdminPerformance;
