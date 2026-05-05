// AdminOCR.tsx - Bengali OCR Tool Page

import React from 'react';
import { Layout, Card, Typography, Alert, Button } from 'antd';
import { EyeOutlined, UploadOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';

const { Title, Paragraph } = Typography;

const AdminOCR: React.FC = () => {
  return (
    <AdminLayout title="Bengali OCR">
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <EyeOutlined style={{ fontSize: 64, color: '#eb2f96', marginBottom: 24 }} />
          <Title level={3}>Bengali OCR Tool</Title>
          <Paragraph type="secondary" style={{ maxWidth: 600, margin: '0 auto 24px' }}>
            Extract Bengali text from images and convert to structured Excel data.
            Supports JPG, PNG up to 10MB.
          </Paragraph>
          <Alert
            message="OCR Processing Module"
            description="The Bengali OCR tool is currently being integrated with the new admin panel. It will be available shortly."
            type="info"
            showIcon
            style={{ maxWidth: 600, margin: '0 auto' }}
          />
        </div>
      </Card>
    </AdminLayout>
  );
};

export default AdminOCR;
