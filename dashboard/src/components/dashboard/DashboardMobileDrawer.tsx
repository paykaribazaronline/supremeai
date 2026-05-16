import React from 'react';
import { Drawer, Menu, Typography } from 'antd';

const { Text } = Typography;

interface DashboardMobileDrawerProps {
  open: boolean;
  onClose: () => void;
  activeKey: string;
  setActiveKey: (key: string) => void;
  menuItems: any[];
  isAdmin: boolean;
  isAuthenticated: boolean;
}

const DashboardMobileDrawer: React.FC<DashboardMobileDrawerProps> = ({
  open,
  onClose,
  activeKey,
  setActiveKey,
  menuItems,
  isAdmin,
  isAuthenticated
}) => {
  return (
    <Drawer
      title={null}
      placement="left"
      closable={false}
      open={open}
      onClose={onClose}
      className="mobile-drawer"
      width={260}
      styles={{ body: { padding: 0, background: 'rgba(0,0,0,0.8)' } }}
    >
      <div style={{
        height: 80,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '0 24px',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        background: 'linear-gradient(135deg, var(--neon-blue), var(--neon-purple))',
        color: '#000',
        fontWeight: 900,
        fontSize: 20,
        letterSpacing: 3,
        fontFamily: 'JetBrains Mono, monospace'
      }}>
        SUPREME
      </div>

      <Menu
        mode="inline"
        selectedKeys={[activeKey]}
        onClick={(e) => {
          setActiveKey(e.key);
          onClose();
        }}
        items={menuItems}
        theme="dark"
        style={{ background: 'transparent', borderRight: 'none', marginTop: 24 }}
      />

      <div style={{ position: 'absolute', bottom: 80, left: 24, right: 24 }}>
        <div style={{ padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 12, border: '1px solid rgba(255,255,255,0.05)' }}>
          <Text style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', textTransform: 'uppercase', letterSpacing: 1 }}>অথোরাইজেশন ট্রেস</Text>
          <div style={{ display: 'flex', gap: 4, marginTop: 12 }}>
            {[1,2,3,4,5,6,7,8].map(i => (
              <div key={i} style={{
                height: 6,
                flex: 1,
                background: i <= (isAdmin ? 8 : (isAuthenticated ? 4 : 1)) ? 'var(--neon-blue)' : 'rgba(255,255,255,0.05)',
                boxShadow: i <= (isAdmin ? 8 : (isAuthenticated ? 4 : 1)) ? '0 0 10px var(--neon-blue)' : 'none',
                borderRadius: 1
              }} />
            ))}
          </div>
        </div>
      </div>
    </Drawer>
  );
};

export default DashboardMobileDrawer;
