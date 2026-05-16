// AdminLayout.tsx - Minimal layout wrapper for admin pages
// Provides consistent page styling and DOES NOT include navigation/header
// Navigation is handled by parent ModernAdminDashboard component

import React from 'react';
import { Layout, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Content } = Layout;
const { Title } = Typography;

interface AdminLayoutProps {
  title: string;
  children: ReactNode;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ title, children }) => {
  return (
    <Content className="admin-page">
      <div className="admin-header">
        <div className="flex flex-col">
          <h1 className="admin-title">{title}</h1>
          <div className="h-1 bg-emerald-500 mt-1" style={{ width: 'clamp(60px, 10vw, 100px)', height: 'clamp(2px, 0.3vw, 3px)' }} />
        </div>
      </div>
      {children}
    </Content>
  );
};

export default AdminLayout;
