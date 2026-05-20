// ModernAdminDashboard.tsx - Cinematic AI Command Center
import React, { useState, useEffect, Suspense, lazy } from 'react';
import { Layout, theme, Typography, ConfigProvider, message, Modal, Spin } from 'antd';
import {
  ExclamationCircleOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { useRole } from '../contexts/RoleContext';
import { authUtils } from '../lib/authUtils';

// Modular Components
import { DataStream } from '../components/dashboard/DashboardDecorations';
import { DashboardHome } from '../components/dashboard/DashboardHome';
import DashboardHeader from '../components/dashboard/DashboardHeader';
import DashboardSidebar from '../components/dashboard/DashboardSidebar';
import DashboardMobileDrawer from '../components/dashboard/DashboardMobileDrawer';
import { RestrictedDemo } from '../components/dashboard/RestrictedDemo';
import { allMenuItems, getBreadcrumbs } from '../components/dashboard/DashboardConfigs';

// Sub-components
import ChatWithAI from '../components/ChatWithAI';
import UserSettings from '../components/UserSettings';
import UserProfile from '../components/UserProfile';

// Lazy Loaded Admin Pages
const AdminProjects = lazy(() => import('./AdminProjects'));
const AdminSettings = lazy(() => import('./AdminSettings'));
const AdminUsers = lazy(() => import('./AdminUsers'));
const AdminProviders = lazy(() => import('./AdminProviders'));
const AdminLogs = lazy(() => import('./AdminLogs'));
const AdminMonitoring = lazy(() => import('./AdminMonitoring'));
const AdminLearning = lazy(() => import('./AdminLearning'));
const AdminSecurity = lazy(() => import('./AdminSecurity'));
const AdminRules = lazy(() => import('./AdminRules'));
const AdminSystemWorkRules = lazy(() => import('../components/AdminSystemWorkRules'));
const AdminAnalytics = lazy(() => import('./AdminAnalytics'));
const AdminVPN = lazy(() => import('./AdminVPN'));
const AdminBrowser = lazy(() => import('./AdminBrowser'));
const AdminAutoBrowser = lazy(() => import('./AutoBrowser'));
const AdminQuotas = lazy(() => import('./AdminQuotas'));
const AdminNotifications = lazy(() => import('./AdminNotifications'));
const AdminPerformance = lazy(() => import('./AdminPerformance'));
const AdminBackup = lazy(() => import('./AdminBackup'));
const AdminOCR = lazy(() => import('./AdminOCR'));
const AdminSimulator = lazy(() => import('./AdminSimulator'));
const AdminReverseEngineer = lazy(() => import('./AdminReverseEngineer'));
const AdminReports = lazy(() => import('./AdminReports'));
const AdminApprovals = lazy(() => import('../components/AdminApprovals'));


const { Content } = Layout;
const { Text } = Typography;

export default function ModernAdminDashboard() {
  const { isAdmin, isAuthenticated, user, refreshUser } = useRole();
  const [collapsed, setCollapsed] = useState(false);
  const [autoHide, setAutoHide] = useState(() => {
    return localStorage.getItem('sidebar_autohide') === 'true';
  });
  const [activeKey, setActiveKey] = useState('dashboard');
  const [darkMode, setDarkMode] = useState(true);
  const [chatFont, setChatFont] = useState(localStorage.getItem('chatFont') || 'font-mono');
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handleLogout = () => {
    Modal.confirm({
      title: <Text style={{ color: 'var(--text-main)', fontSize: 18, fontWeight: 700 }}>লগআউট নিশ্চিত করুন</Text>,
      icon: <ExclamationCircleOutlined style={{ color: 'var(--warning)', fontSize: 24 }} />,
      content: (
        <div style={{ marginTop: 12 }}>
          <Text style={{ color: 'var(--text-dim)' }}>আপনি কি নিশ্চিতভাবে আপনার বর্তমান সেশনটি শেষ করতে চান? সকল সেভ না করা পরিবর্তন হারিয়ে যেতে পারে।</Text>
        </div>
      ),
      okText: 'লগআউট',
      cancelText: 'ফিরে যান',
      centered: true,
      okButtonProps: { 
        className: 'cyber-button', 
        style: { background: 'var(--warning)', border: 'none', color: '#000' } 
      },
      cancelButtonProps: { 
        style: { background: 'rgba(255,255,255,0.05)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)' } 
      },
      onOk: async () => {
        try {
          await authUtils.logout();
          refreshUser();
          message.success('নিরাপদে লগআউট করা হয়েছে।');
        } catch (error) {
          message.error('লগআউট করতে সমস্যা হয়েছে।');
        }
      },
    });
  };

  const currentRole = isAdmin ? 'admin' : (isAuthenticated ? 'user' : 'guest');
  const menuItems = allMenuItems.filter(item => Array.isArray(item.roles) && item.roles.includes(currentRole));

  const renderContent = () => {
    const activeItem = allMenuItems.find(item => item.key === activeKey);
    const hasAccess = activeItem?.roles.includes(currentRole);

    if (!hasAccess && activeKey !== 'dashboard') {
      return <RestrictedDemo title={activeItem?.label || "Unknown"} description="Security clearance insufficient." icon={<LockOutlined />} />;
    }

    return (
      <div style={{ height: 'calc(100vh - 100px)', padding: '20px', display: 'flex', flexDirection: 'column' }}>
        <Suspense fallback={
          <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Spin size="large" tip="লোড হচ্ছে..." />
          </div>
        }>
          {(() => {
            switch (activeKey) {
              case 'dashboard': return <DashboardHome isAdmin={isAdmin} setActiveKey={setActiveKey} />;
              case 'ai': return <ChatWithAI chatFont={chatFont} />;
              case 'projects': return <AdminProjects />;
              
              // Admin tabs
              case 'providers': return <AdminProviders />;
              case 'users': return <AdminUsers />;
              case 'monitoring': return <AdminMonitoring />;
              case 'learning': return <AdminLearning />;
              case 'security': return <AdminSecurity />;
              case 'system-work-rules': return <AdminSystemWorkRules />;
              case 'rules': return <AdminRules />;
              case 'analytics': return <AdminAnalytics />;
              case 'logs': return <AdminLogs />;
              case 'vpn': return <AdminVPN />;
              case 'browser': return <AdminBrowser />;
              case 'auto-browser': return <AdminAutoBrowser />;
              case 'quotas': return <AdminQuotas />;
              case 'simulator': return <AdminSimulator />;
              case 'reverse': return <AdminReverseEngineer />;
              case 'notifications': return <AdminNotifications />;
              case 'reports': return <AdminReports />;
              case 'performance': return <AdminPerformance />;
              case 'backup': return <AdminBackup />;
              case 'ocr': return <AdminOCR />;
              case 'approvals': return <AdminApprovals />;
              
              case 'settings': return isAdmin ? 
                <AdminSettings darkMode={darkMode} setDarkMode={setDarkMode} chatFont={chatFont} setChatFont={setChatFont} /> : 
                <UserSettings darkMode={darkMode} setDarkMode={setDarkMode} chatFont={chatFont} setChatFont={setChatFont} />;
              
              default: return <DashboardHome isAdmin={isAdmin} setActiveKey={setActiveKey} />;
            }
          })()}
        </Suspense>
      </div>
    );
  };

  if (!mounted) return null;

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#00f3ff',
          colorBgBase: '#020205',
          colorTextBase: '#ffffff',
          borderRadius: 8,
          colorLink: '#00f3ff',
        },
        components: {
          Layout: {
            // Old tokens (for backward compatibility)
            colorBgHeader: 'rgba(0,0,0,0.6)',
            colorBgBody: 'transparent',
            colorBgTrigger: '#00f3ff',
            // New tokens (suppress deprecation warnings)
            headerBg: 'rgba(0,0,0,0.6)',
            bodyBg: 'transparent',
            triggerBg: '#00f3ff',
          },
          Menu: {
            // Old tokens
            colorItemText: '#ffffff',
            colorItemTextSelected: '#00f3ff',
            colorItemBg: 'transparent',
            colorItemBgSelected: 'rgba(0, 243, 255, 0.1)',
            // New tokens
            itemColor: '#ffffff',
            itemSelectedColor: '#00f3ff',
            itemBg: 'transparent',
            itemSelectedBg: 'rgba(0, 243, 255, 0.1)',
          },
          Progress: {
            colorSuccess: '#00f3ff',
            colorInfo: '#00f3ff',
            // New token
            size: 8, // replaces strokeWidth
          },
          Table: {
            colorBgContainer: 'rgba(13, 13, 18, 0.5)',
            colorTextHeading: '#00f3ff',
          }
        }
      }}
    >
      <Layout className="animated-bg" style={{ minHeight: '100vh', position: 'relative' }}>
        <div className="bg-grid" />
        <div className="hex-grid" />
        <DataStream />
        <div className="scanline" />
        
        <DashboardSidebar 
          collapsed={collapsed}
          setCollapsed={setCollapsed}
          activeKey={activeKey}
          setActiveKey={setActiveKey}
          menuItems={menuItems}
          isAdmin={isAdmin}
          isAuthenticated={isAuthenticated}
          autoHide={autoHide}
          setAutoHide={setAutoHide}
        />

        {autoHide && collapsed && (
          <div 
            className="sidebar-hover-trigger"
            onMouseEnter={() => setCollapsed(false)}
          />
        )}

        <DashboardMobileDrawer 
          open={mobileDrawerOpen}
          onClose={() => setMobileDrawerOpen(false)}
          activeKey={activeKey}
          setActiveKey={setActiveKey}
          menuItems={menuItems}
          isAdmin={isAdmin}
          isAuthenticated={isAuthenticated}
        />

        <Layout 
          className="responsive-layout" 
          style={{ 
            background: 'transparent',
            ['--sidebar-margin' as any]: collapsed ? '0px' : '276px'
          }}
        >
          <DashboardHeader 
            collapsed={collapsed}
            setCollapsed={setCollapsed}
            getBreadcrumbs={() => getBreadcrumbs(activeKey)}
            isAuthenticated={isAuthenticated}
            isAdmin={isAdmin}
            user={user}
            handleLogout={handleLogout}
          />
          
          <Content style={{ overflow: 'auto', position: 'relative' }}>
            {renderContent()}
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
}

