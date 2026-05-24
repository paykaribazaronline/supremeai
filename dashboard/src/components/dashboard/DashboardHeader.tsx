import React from 'react';
import { Layout, Button, Breadcrumb, Typography, Space, Badge, Tooltip } from 'antd';
import { 
  MenuUnfoldOutlined, 
  MenuFoldOutlined, 
  HomeOutlined, 
  LogoutOutlined,
  SearchOutlined,
  BellOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { ConnectionIndicator } from '../FeedbackSystem';
import { motion } from 'framer-motion';

const { Header } = Layout;
const { Text } = Typography;

interface DashboardHeaderProps {
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  getBreadcrumbs: () => any[];
  isAuthenticated: boolean;
  isAdmin: boolean;
  user: any;
  handleLogout: () => void;
}

const DashboardHeader: React.FC<DashboardHeaderProps> = ({
  collapsed,
  setCollapsed,
  getBreadcrumbs,
  isAuthenticated,
  isAdmin,
  user,
  handleLogout
}) => {
  return (
    <Header className="responsive-header" style={{
      padding: '0 24px',
      background: 'rgba(2, 2, 5, 0.7)',
      backdropFilter: 'blur(20px) saturate(180%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      borderBottom: '1px solid rgba(0, 243, 255, 0.15)',
      height: '80px',
      zIndex: 1000,
      boxShadow: '0 4px 30px rgba(0, 0, 0, 0.5)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        <motion.div whileHover={{ scale: 1.1 }} whileTap={{ scale: 0.9 }}>
          {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
            className: 'trigger',
            onClick: () => setCollapsed(!collapsed),
            style: { fontSize: '20px', color: 'var(--neon-blue)', cursor: 'pointer' }
          })}
        </motion.div>

        <Breadcrumb
          separator={<span style={{ color: 'rgba(255,255,255,0.2)' }}>/</span>}
          style={{ display: 'flex', alignItems: 'center' }}
        >
          <Breadcrumb.Item href="">
            <HomeOutlined style={{ color: 'rgba(255,255,255,0.45)' }} />
          </Breadcrumb.Item>
          {getBreadcrumbs().map((bc: any, idx: number) => (
            <Breadcrumb.Item key={idx}>
              <span style={{
                color: idx === getBreadcrumbs().length - 1 ? 'var(--neon-blue)' : 'rgba(255,255,255,0.6)',
                fontWeight: idx === getBreadcrumbs().length - 1 ? 700 : 400,
                fontSize: '13px',
                letterSpacing: '0.5px'
              }}>
                {bc.title}
              </span>
            </Breadcrumb.Item>
          ))}
        </Breadcrumb>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', borderRight: '1px solid rgba(255,255,255,0.1)', paddingRight: '24px' }}>
          <Tooltip title="Neural Link Active">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <ConnectionIndicator />
              <Text style={{ color: 'var(--success)', fontSize: 11, fontWeight: 700, letterSpacing: 1 }}>CONNECTED</Text>
            </div>
          </Tooltip>

          <Badge dot color="var(--neon-blue)">
            <Button
              type="text"
              icon={<BellOutlined style={{ color: 'rgba(255,255,255,0.65)' }} />}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            />
          </Badge>

          <Button
            type="text"
            icon={<SearchOutlined style={{ color: 'rgba(255,255,255,0.65)' }} />}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{
              color: 'var(--neon-blue)',
              fontSize: '11px',
              fontWeight: 800,
              letterSpacing: '1.5px',
              textTransform: 'uppercase',
              lineHeight: 1
            }}>
              {isAdmin ? 'System Architect' : 'Operator'}
            </div>
            <div style={{
              color: 'rgba(255,255,255,0.45)',
              fontSize: '12px',
              fontFamily: 'JetBrains Mono'
            }}>
              {user?.email || 'root@supreme.ai'}
            </div>
          </div>

          <Tooltip title="Security Clearance Alpha">
            <div style={{
              width: 42, height: 42, borderRadius: 12,
              background: 'linear-gradient(135deg, rgba(0, 243, 255, 0.2), rgba(188, 19, 254, 0.2))',
              border: '1px solid rgba(255,255,255,0.1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', position: 'relative', overflow: 'hidden'
            }}>
               <ThunderboltOutlined style={{ color: '#fff', fontSize: 18, zIndex: 1 }} />
               <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.2)' }} />
            </div>
          </Tooltip>

          <Button
            onClick={handleLogout}
            className="glass-action-button"
            icon={<LogoutOutlined />}
            style={{ height: '36px', minHeight: '36px', padding: '0 16px', borderRadius: 8 }}
          >
            DISCONNECT
          </Button>
        </div>
      </div>
    </Header>
  );
};

export default DashboardHeader;
