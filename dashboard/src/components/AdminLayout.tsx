// AdminLayout.tsx - Consistent layout for all admin pages
// Provides header with navigation, logout, and responsive design

import React from 'react';
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
    const socket = new SockJS('https://supremeai-lhlwyikwlq-uc.a.run.app/ws/simulator'); // Your backend WebSocket URL
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
    <Layout style={{ minHeight: '100vh', background: '#f5f5f5' }}>
      <Header
        style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          padding: '0 24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          position: 'sticky',
          top: 0,
          zIndex: 100,
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <Link to="/admin" style={{ color: 'white', textDecoration: 'none' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Avatar
                size={36}
                style={{
                  background: 'rgba(255,255,255,0.2)',
                  color: 'white',
                  fontWeight: 700,
                }}
                icon={<RobotOutlined />}
              />
              {location.pathname === '/admin' ? (
                <span style={{ color: 'white', fontSize: '18px', fontWeight: 700 }}>
                  SupremeAI Admin
                </span>
              ) : (
                <Link to="/admin" style={{ color: 'white', textDecoration: 'none' }}>
                  <span style={{ color: 'white', fontSize: '18px', fontWeight: 700 }}>
                    SupremeAI Admin
                  </span>
                </Link>
              )}
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
                style={{
                  color: isActive ? 'white' : 'rgba(255,255,255,0.85)',
                  textDecoration: 'none',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  background: isActive ? 'rgba(255,255,255,0.2)' : 'transparent',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '14px',
                  fontWeight: isActive ? 600 : 400,
                  whiteSpace: 'nowrap',
                  transition: 'all 0.2s',
                }}
                title={item.label}
              >
                {item.icon}
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Dropdown menu={{ items: userDropdownItems }} placement="bottomRight">
            <Button
              type="text"
              style={{
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <Avatar size="small" style={{ background: 'rgba(255,255,255,0.3)' }} icon={<UserOutlined />} />
              <span>Admin</span>
            </Button>
          </Dropdown>
        </div>
      </Header>

      <Content style={{ padding: '24px', background: '#f5f5f5' }}>
        <div style={{ marginBottom: '16px' }}>
          <h1 style={{ fontSize: '24px', fontWeight: 600, color: '#333', margin: 0 }}>
            {title}
          </h1>
        </div>
        {children}
      </Content>
    </Layout>
  );
};

export default AdminLayout;
