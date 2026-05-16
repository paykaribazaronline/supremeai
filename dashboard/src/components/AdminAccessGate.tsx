// AdminAccessGate.tsx - Gatekeeper for admin panel features
import React from 'react';
import { Alert, Card, Badge, Button, Space, Typography } from 'antd';
import { 
  LockOutlined, 
  EyeOutlined, 
  SafetyCertificateOutlined,
  ExperimentOutlined,
  RobotOutlined,
  CodeOutlined,
  BarChartOutlined,
  BulbOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { useRole } from '../contexts/RoleContext';

const { Title, Paragraph, Text } = Typography;

interface AdminAccessGateProps {
  children: React.ReactNode;
  requireAdmin?: boolean; // If true, only admins can see content. If false, all can see (but with demo notice)
  feature?: string; // Feature name for banner
  demoComponent?: React.ReactNode; // Custom demo component
}

const AdminAccessGate: React.FC<AdminAccessGateProps> = ({
  children,
  requireAdmin = true,
  feature = 'Feature',
  demoComponent
}) => {
  const { isAdmin } = useRole();

  // If admin required and user is not admin, show locked message
  if (requireAdmin && !isAdmin) {
    return (
      <Card
        className="demo-mode-card"
        style={{
          textAlign: 'center',
          padding: '60px 40px',
          background: 'linear-gradient(135deg, rgba(139,92,246,0.1) 0%, rgba(0,0,0,0.2) 100%)',
          border: '1px solid rgba(139,92,246,0.3)',
          borderRadius: '16px',
          margin: '20px 0'
        }}
      >
        <LockOutlined style={{ fontSize: 64, color: '#8b5cf6', marginBottom: 24, opacity: 0.8 }} />
        <Title level={3} style={{ color: '#8b5cf6', marginBottom: 12, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
          ADMIN-ONLY MODULE
        </Title>
        <Paragraph type="secondary" style={{ marginBottom: 32, fontSize: 15, lineHeight: 1.7 }}>
          <Text strong style={{ color: '#f59e0b' }}>{feature}</Text> requires administrator privileges.<br/>
          Please sign in with an admin account to access this functionality.
        </Paragraph>
        <Space size="large">
          <Button 
            type="primary" 
            size="large"
            icon={<SafetyCertificateOutlined />}
            onClick={() => window.location.href = '/admin?login=true'}
            style={{ background: '#10b981', borderColor: '#10b981' }}
          >
            Login as Admin
          </Button>
          <Button 
            size="large"
            icon={<EyeOutlined />}
            onClick={() => window.location.href = '/'}
          >
            Back to Home
          </Button>
        </Space>
      </Card>
    );
  }

  // Non-admin sees demo version if provided, else full content
  if (!isAdmin && !requireAdmin) {
    return demoComponent ? (
      <>
        <Badge 
          count="DEMO MODE" 
          style={{ 
            '--antd-badge-bg': '#f59e0b',
            '--antd-badge-color': '#000' 
          } as any} 
        />
        {demoComponent}
        <div style={{
          marginTop: 16,
          padding: '12px 16px',
          background: 'rgba(245,158,11,0.1)',
          border: '1px solid rgba(245,158,11,0.3)',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          fontSize: 13,
          color: '#f59e0b'
        }}>
          <ExperimentOutlined />
          <span>
            This is a <Text strong>demo preview</Text>. Sign in as admin for full capabilities.
          </span>
          <Button 
            type="link" 
            size="small" 
            onClick={() => window.location.href = '/admin?login=true'}
            style={{ color: '#f59e0b', padding: 0 }}
          >
            Upgrade →
          </Button>
        </div>
      </>
    ) : (
      <div style={{ opacity: 0.5, pointerEvents: 'none' }}>
        {children}
        <div style={{
          marginTop: 16,
          padding: 12,
          background: 'rgba(245,158,11,0.1)',
          border: '1px dashed rgba(245,158,11,0.5)',
          borderRadius: 8,
          textAlign: 'center',
          color: '#f59e0b',
          fontSize: 13
        }}>
          <ExperimentOutlined style={{ marginRight: 8 }} />
          Demo Mode - Feature requires admin access
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

export default AdminAccessGate;
