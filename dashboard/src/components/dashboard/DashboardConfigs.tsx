import React from 'react';
import {
  DashboardOutlined,
  RobotOutlined,
  CodeOutlined,
  BarChartOutlined,
  SettingOutlined,
  BulbOutlined,
  GlobalOutlined,
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
  HomeOutlined,
  RocketOutlined,
  CloudServerOutlined
} from '@ant-design/icons';

export interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  roles: string[];
  children?: MenuItem[];
}

export const allMenuItems: MenuItem[] = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: '1. 🥇 Dashboard (কমান্ড সেন্টার)', roles: ['guest', 'user', 'admin'] },

  {
    key: 'ai_center',
    icon: <RobotOutlined />,
    label: '2. 🧠 AI & Neural Hub',
    roles: ['guest', 'user', 'admin'],
    children: [
      { key: 'ai', icon: <RobotOutlined />, label: 'Neural Chat (নিউরাল চ্যাট)', roles: ['guest', 'user', 'admin'] },
      { key: 'providers', icon: <ApiOutlined />, label: 'AI Providers (AI প্রোভাইডার)', roles: ['admin'] },
      { key: 'learning', icon: <BulbOutlined />, label: 'Learning (লার্নিং)', roles: ['admin'] },
      { key: 'code-analysis', icon: <CodeOutlined />, label: 'Code Intelligence (কোড ইন্টেলিজেন্স)', roles: ['admin'] },
      { key: 'reverse', icon: <CodeOutlined />, label: 'Reverse Engineering (রিভার্স ইঞ্জিনিয়ারিং)', roles: ['admin'] },
      { key: 'superfly', icon: <RocketOutlined />, label: 'SupremeAI Offline (এজ এআই)', roles: ['guest', 'user', 'admin'] }
    ]
  },

  {
    key: 'observability',
    icon: <LineChartOutlined />,
    label: '3. 📊 System Observability',
    roles: ['admin'],
    children: [
      { key: 'monitoring', icon: <HddOutlined />, label: 'Monitoring (মনিটরিং)', roles: ['admin'] },
      { key: 'analytics', icon: <LineChartOutlined />, label: 'Analytics (এনালাইটিক্স)', roles: ['admin'] },
      { key: 'performance', icon: <ClusterOutlined />, label: 'Performance (পারফরম্যান্স)', roles: ['admin'] },
      { key: 'logs', icon: <FileTextOutlined />, label: 'System Logs (সিস্টেম লগ)', roles: ['admin'] },
      { key: 'reports', icon: <BarChartOutlined />, label: 'Reports (রিপোর্টস)', roles: ['admin'] },
      { key: 'notifications', icon: <BellOutlined />, label: 'Notifications (নোটিফিকেশন)', roles: ['admin'] }
    ]
  },

  {
    key: 'infrastructure',
    icon: <CloudServerOutlined />,
    label: '4. ☁️ Infrastructure & DB',
    roles: ['admin'],
    children: [
      { key: 'infrastructure', icon: <CloudServerOutlined />, label: 'Infrastructure (ইনফ্রাস্ট্রাকচার)', roles: ['admin'] },
      { key: 'cloud-db-hub', icon: <DatabaseOutlined />, label: 'Cloud & DB Hub (ক্লাউড হাব)', roles: ['admin'] },
      { key: 'backup', icon: <DatabaseOutlined />, label: 'Backup (ব্যাকআপ)', roles: ['admin'] },
      { key: 'vpn', icon: <GlobalOutlined />, label: 'VPN Connection (VPN কানেকশন)', roles: ['admin'] }
    ]
  },

  {
    key: 'security_governance',
    icon: <SecurityScanOutlined />,
    label: '5. 🛡️ Security & Rules',
    roles: ['admin'],
    children: [
      { key: 'security', icon: <SecurityScanOutlined />, label: 'Security (সিকিউরিটি)', roles: ['admin'] },
      { key: 'approvals', icon: <BulbOutlined />, label: 'Approvals (অ্যাপ্রুভালস)', roles: ['admin'] },
      { key: 'system-work-rules', icon: <RocketOutlined />, label: 'Work Rules (ওয়ার্ক রুলস)', roles: ['admin'] },
      { key: 'rules', icon: <AuditOutlined />, label: 'System Rules (সিস্টেম রুলস)', roles: ['admin'] }
    ]
  },

  {
    key: 'user_management',
    icon: <UserOutlined />,
    label: '6. 👥 Users & Access',
    roles: ['admin'],
    children: [
      { key: 'users', icon: <UserOutlined />, label: 'User Management (ম্যানেজমেন্ট)', roles: ['admin'] },
      { key: 'quotas', icon: <PieChartOutlined />, label: 'Quotas (কোটা)', roles: ['admin'] }
    ]
  },

  {
    key: 'tools',
    icon: <ChromeOutlined />,
    label: '7. 🛠️ Web & Tools',
    roles: ['guest', 'user', 'admin'],
    children: [
      { key: 'browser', icon: <ChromeOutlined />, label: 'Browser (ব্রাউজার)', roles: ['guest', 'user', 'admin'] },
      { key: 'auto-browser', icon: <RocketOutlined />, label: 'Auto Browser (অটো ব্রাউজার)', roles: ['guest', 'user', 'admin'] },
      { key: 'ocr', icon: <FileTextOutlined />, label: 'OCR Tool (OCR টুল)', roles: ['admin'] }
    ]
  },

  {
    key: 'deployments',
    icon: <CodeOutlined />,
    label: '8. 🚀 Workspaces',
    roles: ['user', 'admin'],
    children: [
      { key: 'projects', icon: <CodeOutlined />, label: 'Deployments (ডিপ্লয়মেন্টস)', roles: ['user', 'admin'] },
      { key: 'simulator', icon: <MobileOutlined />, label: 'Simulator (সিমুলেটর)', roles: ['guest', 'user', 'admin'] }
    ]
  },

  { key: 'settings', icon: <SettingOutlined />, label: '9. ⚙️ Config (কনফিগ)', roles: ['guest', 'user', 'admin'] }
];

export const getBreadcrumbs = (activeKey: string) => {
  let activeItem: MenuItem | undefined;
  
  for (const item of allMenuItems) {
    if (item.key === activeKey) {
      activeItem = item;
      break;
    }
    if (item.children) {
      const child = item.children.find(c => c.key === activeKey);
      if (child) {
        activeItem = child;
        break;
      }
    }
  }

  return [
    { title: <><HomeOutlined /> <span>কমান্ড</span></>, key: 'home', path: '/admin/dashboard' },
    { title: activeItem?.label || 'ড্যাশবোর্ড', key: 'active', path: `/admin/${activeKey === 'dashboard' ? '' : activeKey}` }
  ];
};

export const getMenuPath = (key: string): string => {
  return `/admin/${key === 'dashboard' ? '' : key}`;
};