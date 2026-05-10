// AdminLayout.tsx - Consistent layout for all admin pages
// Provides header with navigation, logout, and responsive design

import React, { useEffect } from 'react';
import { notification } from 'antd';
// if sockjs-client and @stomp/stompjs are not installed, run: npm install @stomp/stompjs sockjs-client
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Link, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, Space, Avatar, Dropdown } from 'antd';
import {
  LogoutOutlined,
  UserOutlined,
  DashboardOutlined,
  TeamOutlined,
  SettingOutlined,
  CloudServerOutlined,
  RobotOutlined,
  ApiOutlined,
  BellOutlined,
  FolderOutlined,
  KeyOutlined,
  FileTextOutlined,
  BarChartOutlined,
  MonitorOutlined,
  UploadOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import type { MenuProps } from 'antd';

const { Header, Content } = Layout;

interface AdminLayoutProps {
  title: string;
  children: React.ReactNode;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ title, children }) => {
  const location = useLocation();

  useEffect(() => {
    // Connect to the WebSocket server
     const socket = new SockJS(import.meta.env.VITE_WS_URL || '/ws');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("Connected to SupremeAI WebSocket!");
        
        // Subscribe to the GitHub pipeline channel
        stompClient.subscribe('/topic/notifications', (message) => {
          const data = JSON.parse(message.body);
          
          if (data.type === 'GITHUB_PIPELINE') {
            if (data.status === 'success') {
              notification.success({
                message: '🚀 Deployment Successful',
                description: data.message,
                duration: 5, // Disappears after 5 seconds
                placement: 'topRight',
              });
            } else {
              notification.error({
                message: '🚨 Deployment Failed',
                description: data.message,
                duration: 0, // Stays on screen until user dismisses
                placement: 'topRight',
              });
            }
          }
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
      }
    });

    stompClient.activate();

    // Disconnect on component unmount
    return () => {
      stompClient.deactivate();
    };
  }, []);

  const logout = () => {
    authUtils.clearAuth();
    window.location.href = '/';
  };

  const userDropdownItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: 'Profile',
      icon: <UserOutlined />,
    },
    {
      key: 'settings',
      label: 'Settings',
      icon: <SettingOutlined />,
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      label: 'Logout',
      icon: <LogoutOutlined />,
      onClick: logout,
    },
  ];

  // Define all admin navigation items
  const navItems = [
    { key: '/admin', label: 'Dashboard', icon: <DashboardOutlined /> },
    { key: '/admin/users', label: 'Users', icon: <TeamOutlined /> },
    { key: '/admin/projects', label: 'Projects', icon: <FolderOutlined /> },
    { key: '/admin/providers', label: 'Providers', icon: <RobotOutlined /> },
    { key: '/admin/apikeys', label: 'API Keys', icon: <KeyOutlined /> },
    { key: '/admin/settings', label: 'Settings', icon: <SettingOutlined /> },
    { key: '/admin/notifications', label: 'Notifications', icon: <BellOutlined /> },
    { key: '/admin/logs', label: 'Logs', icon: <FileTextOutlined /> },
    { key: '/admin/reports', label: 'Reports', icon: <BarChartOutlined /> },
    { key: '/admin/monitoring', label: 'Monitoring', icon: <MonitorOutlined /> },
    { key: '/admin/performance', label: 'Performance', icon: <BarChartOutlined /> },
    { key: '/admin/backup', label: 'Backup', icon: <UploadOutlined /> },
    { key: '/admin/ocr', label: 'OCR', icon: <EyeOutlined /> },
  ];

  return (
    <Layout style={{ minHeight: '100vh', background: '#080808' }}>
      <Header
        style={{
          background: 'rgba(10, 10, 10, 0.8)',
          backdropFilter: 'blur(10px)',
          padding: '0 24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          position: 'sticky',
          top: 0,
          zIndex: 100,
          borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
          height: '64px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <Link to="/admin" style={{ color: 'white', textDecoration: 'none' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Avatar
                size={36}
                style={{
                  background: 'rgba(16, 185, 129, 0.1)',
                  color: '#10b981',
                  fontWeight: 900,
                  border: '1px solid rgba(16, 185, 129, 0.2)'
                }}
                icon={<RobotOutlined />}
              />
              <span className="hidden sm:inline-block" style={{ color: 'white', fontSize: '18px', fontWeight: 900, letterSpacing: '0.05em' }}>
                <span className="text-white">Supreme</span>
                <span className="text-emerald-500">AI</span>
                <span className="text-[10px] ml-2 text-white/20 font-bold uppercase tracking-widest hidden lg:inline">Core</span>
              </span>
            </div>
          </Link>
        </div>

        <nav style={{ display: 'flex', gap: '8px', flexWrap: 'nowrap', overflow: 'auto' }}>
          {navItems.map((item) => {
            const isActive = location.pathname === item.key;
            return (
              <Link
                key={item.key}
                to={item.key}
                className="transition-all duration-300"
                style={{
                  color: isActive ? '#10b981' : 'rgba(255,255,255,0.4)',
                  textDecoration: 'none',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  background: isActive ? 'rgba(16, 185, 129, 0.05)' : 'transparent',
                  border: isActive ? '1px solid rgba(16, 185, 129, 0.2)' : '1px solid transparent',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '12px',
                  fontWeight: isActive ? 700 : 500,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  whiteSpace: 'nowrap',
                }}
                title={item.label}
              >
                {React.cloneElement(item.icon as React.ReactElement, { 
                  style: { fontSize: '14px', color: isActive ? '#10b981' : 'inherit' } 
                })}
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Dropdown menu={{ items: userDropdownItems }} placement="bottomRight">
            <Button
              type="text"
              className="flex items-center gap-2 px-1 sm:px-3"
              style={{ color: 'white' }}
            >
              <Avatar 
                size="small" 
                className="bg-emerald-500/20 text-emerald-500 border border-emerald-500/30"
                icon={<UserOutlined />} 
              />
              <span className="hidden md:inline font-bold text-[12px] uppercase tracking-widest text-white/80">Admin</span>
            </Button>
          </Dropdown>
        </div>
      </Header>

      <Content style={{ padding: '24px', background: '#080808' }}>
        <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div className="flex flex-col">
            <h1 style={{ fontSize: '20px', fontWeight: 900, color: '#fff', margin: 0, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              {title}
            </h1>
            <div className="h-1 w-12 bg-emerald-500 mt-1" />
          </div>
        </div>
        {children}
      </Content>
    </Layout>
  );
};

export default AdminLayout;
