import React from 'react';
import { Layout, Menu, Typography } from 'antd';
import { motion } from 'framer-motion';

const { Sider } = Layout;
const { Text } = Typography;

interface DashboardSidebarProps {
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  activeKey: string;
  setActiveKey: (key: string) => void;
  menuItems: any[];
  isAdmin: boolean;
  isAuthenticated: boolean;
}

const DashboardSidebar: React.FC<DashboardSidebarProps> = ({
  collapsed,
  setCollapsed,
  activeKey,
  setActiveKey,
  menuItems,
  isAdmin,
  isAuthenticated
}) => {
  return (
     <Sider
       collapsible
       collapsed={collapsed}
       onCollapse={setCollapsed}
       theme="dark"
       className="glass-panel responsive-sidebar"
       width={260}
       collapsedWidth={0}
       style={{
         top: '8px',
         left: '8px',
         bottom: '8px',
         borderRadius: 'var(--radius-lg)',
         border: '1px solid rgba(255,255,255,0.05)',
         background: 'rgba(0,0,0,0.6)',
         zIndex: 10
       }}
     >
       <div style={{
         height: 'var(--header-height)',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         padding: '0 var(--space-3)',
         borderBottom: '1px solid rgba(255,255,255,0.05)'
       }}>
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
           style={{
             background: 'linear-gradient(135deg, var(--neon-blue), var(--neon-purple))',
             padding: 'var(--space-2) var(--space-3)',
             borderRadius: 'var(--radius-md)',
             boxShadow: '0 0 clamp(12px, 2vw, 24px) rgba(0, 243, 255, 0.4)',
             color: '#000',
             fontWeight: 900,
             fontSize: collapsed ? 'var(--text-sm)' : 'var(--text-lg)',
             letterSpacing: 'clamp(1px, 0.3vw, 3px)',
             fontFamily: 'JetBrains Mono, monospace'
           }}
        >
          {collapsed ? 'S' : 'SUPREME'}
        </motion.div>
      </div>
      
      <Menu
        mode="inline"
        selectedKeys={[activeKey]}
        onClick={(e) => setActiveKey(e.key)}
        items={menuItems}
        theme="dark"
        style={{ background: 'transparent', borderRight: 'none', marginTop: 24 }}
      />
      
       {!collapsed && (
         <div style={{ position: 'absolute', bottom: 'var(--space-5)', left: 'var(--space-3)', right: 'var(--space-3)' }}>
           <div style={{ padding: 'var(--space-3)', background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-lg)', border: '1px solid rgba(255,255,255,0.05)' }}>
             <Text style={{ fontSize: 'var(--text-xs)', color: 'rgba(255,255,255,0.4)', textTransform: 'uppercase', letterSpacing: 1 }}>অথোরাইজেশন ট্রেস</Text>
              <div style={{ display: 'flex', gap: 'var(--space-1)', marginTop: 'var(--space-3)' }}>
                {[1,2,3,4,5,6,7,8].map(i => (
                  <div key={i} style={{
                    height: 'clamp(4px, 1vw, 8px)',
                    flex: 1,
                    background: i <= (isAdmin ? 8 : (isAuthenticated ? 4 : 1)) ? 'var(--neon-blue)' : 'rgba(255,255,255,0.05)',
                    boxShadow: i <= (isAdmin ? 8 : (isAuthenticated ? 4 : 1)) ? '0 0 clamp(4px, 1vw, 8px) var(--neon-blue)' : 'none',
                    borderRadius: 'clamp(1px, 0.25vw, 3px)'
                  }} />
                ))}
             </div>
           </div>
         </div>
       )}
    </Sider>
  );
};

export default DashboardSidebar;
