import React from 'react';
import {
  DashboardOutlined,
  RobotOutlined,
  CodeOutlined,
  BarChartOutlined,
  SettingOutlined,
  BulbOutlined,
  GlobalOutlined,
  SafetyOutlined,
  ClusterOutlined,
  HddOutlined,
  ApiOutlined,
  UserOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  LineChartOutlined,
  ChromeOutlined,
  PieChartOutlined,
  MobileOutlined,
  BellOutlined,
  SecurityScanOutlined,
  AuditOutlined,
  ToolOutlined,
  HomeOutlined,
  RocketOutlined
} from '@ant-design/icons';

export interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  roles: string[];
}

export const allMenuItems: MenuItem[] = [
  // Universal tabs (all roles)
  { key: 'dashboard', icon: <DashboardOutlined />, label: 'কমান্ড সেন্টার', roles: ['guest', 'user', 'admin'] },
  { key: 'ai', icon: <RobotOutlined />, label: 'নিউরাল চ্যাট', roles: ['guest', 'user', 'admin'] },
  { key: 'projects', icon: <CodeOutlined />, label: 'ডিপ্লয়মেন্টস', roles: ['user', 'admin'] },
  { key: 'settings', icon: <SettingOutlined />, label: 'কনফিগ', roles: ['guest', 'user', 'admin'] },
  { key: 'approvals', icon: <BulbOutlined />, label: 'অ্যাপ্রুভালস', roles: ['admin'] },
  
  // Admin-only tabs
  { key: 'providers', icon: <ApiOutlined />, label: 'AI প্রোভাইডার', roles: ['admin'] },
  { key: 'users', icon: <UserOutlined />, label: 'ইউজার ম্যানেজমেন্ট', roles: ['admin'] },
  { key: 'monitoring', icon: <HddOutlined />, label: 'সিস্টেম মনিটরিং', roles: ['admin'] },
  { key: 'learning', icon: <BulbOutlined />, label: 'লার্নিং ম্যানেজমেন্ট', roles: ['admin'] },
  { key: 'security', icon: <SecurityScanOutlined />, label: 'সিকিউরিটি', roles: ['admin'] },
  { key: 'system-work-rules', icon: <RocketOutlined />, label: 'সিস্টেম ওয়ার্ক রুলস', roles: ['admin'] },
  { key: 'rules', icon: <AuditOutlined />, label: 'সিস্টেম রুলস', roles: ['admin'] },
  { key: 'analytics', icon: <LineChartOutlined />, label: 'এনালাইটিক্স', roles: ['admin'] },
  { key: 'logs', icon: <FileTextOutlined />, label: 'সিস্টেম লগ', roles: ['admin'] },
  { key: 'vpn', icon: <GlobalOutlined />, label: 'VPN কানেকশন', roles: ['admin'] },
  { key: 'browser', icon: <ChromeOutlined />, label: 'ব্রাউজার', roles: ['guest', 'user', 'admin'] },
  { key: 'auto-browser', icon: <RocketOutlined />, label: 'অটো ব্রাউজার', roles: ['guest', 'user', 'admin'] },
  { key: 'quotas', icon: <PieChartOutlined />, label: 'কোটা ম্যানেজমেন্ট', roles: ['admin'] },
  { key: 'simulator', icon: <MobileOutlined />, label: 'সিমুলেটর', roles: ['guest', 'user', 'admin'] },
  { key: 'reverse', icon: <CodeOutlined />, label: 'রিভার্স ইঞ্জিনিয়ারিং', roles: ['admin'] },
  { key: 'notifications', icon: <BellOutlined />, label: 'নোটিফিকেশন', roles: ['admin'] },
  { key: 'reports', icon: <BarChartOutlined />, label: 'রিপোর্টস', roles: ['admin'] },
  { key: 'performance', icon: <ClusterOutlined />, label: 'পারফরম্যান্স', roles: ['admin'] },
  { key: 'backup', icon: <DatabaseOutlined />, label: 'ব্যাকআপ', roles: ['admin'] },
  { key: 'ocr', icon: <FileTextOutlined />, label: 'OCR টুল', roles: ['admin'] },
];

export const getBreadcrumbs = (activeKey: string) => {
  const activeItem = allMenuItems.find(item => item.key === activeKey);
  return [
    { title: <><HomeOutlined /> <span>কমান্ড</span></>, key: 'home' },
    { title: activeItem?.label || 'ড্যাশবোর্ড', key: 'active' }
  ];
};
