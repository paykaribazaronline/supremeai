import React from 'react';
import { Layout, Button, Breadcrumb, Tag, Typography, Space, Tooltip, Avatar } from 'antd';
import { 
  MenuUnfoldOutlined, 
  MenuFoldOutlined, 
  HomeOutlined, 
  LoginOutlined, 
  LogoutOutlined, 
  RobotOutlined 
} from '@ant-design/icons';
import { ConnectionIndicator } from '../FeedbackSystem';

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
       padding: '0 var(--space-3)',
       background: 'rgba(0,0,0,0.4)',
       backdropFilter: 'blur(20px)',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'space-between',
       borderBottom: '1px solid rgba(255,255,255,0.05)',
       height: 'var(--header-height, clamp(64px, 8vh, 80px))',
       zIndex: 5
     }}>
       <Button
         type="text"
         icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
         onClick={() => setCollapsed(!collapsed)}
         className="mobile-menu-button"
         style={{ color: 'rgba(255,255,255,0.6)', fontSize: 'var(--text-lg)' }}
       />
       
       <div className="header-breadcrumbs" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
         <Breadcrumb 
           items={getBreadcrumbs()} 
           style={{ color: 'var(--text-dim)', fontSize: 'var(--text-sm)' }}
         />
         <div className="divider" style={{ width: '1px', height: 'var(--space-4)', background: 'rgba(255,255,255,0.1)' }} />
         <ConnectionIndicator />
       </div>
       
       <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-5)' }}>
        <div style={{ textAlign: 'right' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 12 }}>
            {!isAuthenticated && <Tag color="warning" style={{ margin: 0, borderRadius: 4, background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.3)', color: '#f59e0b' }}>সীমাবদ্ধ_অ্যাক্সেস</Tag>}
           <Text style={{ display: 'block', color: 'var(--text-main)', fontWeight: 600, fontSize: 'var(--text-sm)', letterSpacing: '1px' }}>
             {isAdmin ? 'সিস্টেম আর্কিটেক্ট' : (isAuthenticated ? 'নিউরাল অপারেটর' : 'গেস্ট এনটিটি')}
           </Text>
          </div>
           <Text style={{ fontSize: 'var(--text-xs)', color: isAdmin ? 'var(--success)' : (isAuthenticated ? 'var(--neon-purple)' : 'var(--text-dim)'), fontFamily: 'var(--font-mono)' }}>
             {isAuthenticated ? `AUTH: ${user?.email || 'Authenticated'}` : 'MODE: GUEST_BYPASS'}
           </Text>
        </div>

        <Space>
          {!isAuthenticated && (
            <Button
              type="primary"
              size="small"
              icon={<LoginOutlined />}
              onClick={() => window.location.href = '/login'}
              style={{
                background: 'var(--neon-blue)',
                border: 'none',
                fontSize: '12px'
              }}
            >
              লগইন
            </Button>
          )}

          <Tooltip title={isAuthenticated ? "লগআউট করুন" : "গেস্ট মোড থেকে সুইচ করুন"}>
            <Avatar
              size={52}
              icon={isAuthenticated ? <LogoutOutlined /> : <RobotOutlined />}
              className={isAuthenticated ? "glow-blue" : "glow-purple"}
              style={{
                background: isAuthenticated ? 'rgba(0, 243, 255, 0.1)' : 'rgba(188, 19, 254, 0.1)',
                border: `1px solid ${isAuthenticated ? 'var(--neon-blue)' : 'var(--neon-purple)'}`,
                color: isAuthenticated ? 'var(--neon-blue)' : 'var(--neon-purple)',
                cursor: 'pointer',
                transition: 'all 0.3s'
              }}
              onClick={() => {
                if (isAuthenticated) {
                  handleLogout();
                } else {
                  window.location.href = '/login';
                }
              }}
            />
          </Tooltip>
        </Space>
      </div>
    </Header>
  );
};

export default DashboardHeader;
