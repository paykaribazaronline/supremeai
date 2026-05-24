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
  extra?: ReactNode;
  children: ReactNode;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ title, extra, children }) => {
  return (
    <Content className="admin-page">
      <div className="admin-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '24px' }}>
        <div className="flex flex-col">
          <h1 className="admin-title glow-text-cyan" style={{ margin: 0, color: 'var(--neon-blue)', letterSpacing: '1px', textTransform: 'uppercase' }}>{title}</h1>
          <div className="mt-1" style={{ width: 'clamp(60px, 10vw, 100px)', height: 'clamp(2px, 0.3vw, 3px)', background: 'var(--neon-blue)', boxShadow: '0 0 8px var(--neon-blue)' }} />
        </div>
        {extra && <div className="admin-header-extra">{extra}</div>}
      </div>
      {children}
    </Content>
  );
};

export default AdminLayout;
