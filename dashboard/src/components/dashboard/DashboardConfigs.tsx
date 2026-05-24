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
  RocketOutlined,
  CloudServerOutlined
} from '@ant-design/icons';

export interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  roles: string[];
}

export const allMenuItems: MenuItem[] = [
  // Universal tabs (all roles)
  { key: 'dashboard', icon: <DashboardOutlined />, label: '1. 🥇 Dashboard (কমান্ড সেন্টার)', roles: ['guest', 'user', 'admin'] },
  { key: 'ai', icon: <RobotOutlined />, label: '2. 🤖 Neural Chat (নিউরাল চ্যাট)', roles: ['guest', 'user', 'admin'] },
  { key: 'projects', icon: <CodeOutlined />, label: '3. 🚀 Deployments (ডিপ্লয়মেন্টস)', roles: ['user', 'admin'] },
  { key: 'settings', icon: <SettingOutlined />, label: '4. ⚙️ Config (কনফিগ)', roles: ['guest', 'user', 'admin'] },
  { key: 'approvals', icon: <BulbOutlined />, label: '5. ✅ Approvals (অ্যাপ্রুভালস)', roles: ['admin'] },
  
  // Admin-only tabs
  { key: 'providers', icon: <ApiOutlined />, label: '6. 🌐 AI Providers (AI প্রোভাইডার)', roles: ['admin'] },
  { key: 'users', icon: <UserOutlined />, label: '7. 👥 User Management (ইউজার ম্যানেজমেন্ট)', roles: ['admin'] },
  { key: 'monitoring', icon: <HddOutlined />, label: '8. 🖥️ Monitoring (মনিটরিং)', roles: ['admin'] },
  { key: 'learning', icon: <BulbOutlined />, label: '9. 🧠 Learning (লার্নিং)', roles: ['admin'] },
  { key: 'security', icon: <SecurityScanOutlined />, label: '10. 🛡️ Security (সিকিউরিটি)', roles: ['admin'] },
  { key: 'system-work-rules', icon: <RocketOutlined />, label: '11. 📋 Work Rules (ওয়ার্ক রুলস)', roles: ['admin'] },
  { key: 'rules', icon: <AuditOutlined />, label: '12. 📜 System Rules (সিস্টেম রুলস)', roles: ['admin'] },
  { key: 'analytics', icon: <LineChartOutlined />, label: '13. 📊 Analytics (এনালাইটিক্স)', roles: ['admin'] },
  { key: 'logs', icon: <FileTextOutlined />, label: '14. 📂 System Logs (সিস্টেম লগ)', roles: ['admin'] },
  { key: 'vpn', icon: <GlobalOutlined />, label: '15. 🌐 VPN Connection (VPN কানেকশন)', roles: ['admin'] },
  { key: 'browser', icon: <ChromeOutlined />, label: '16. 🌐 Browser (ব্রাউজার)', roles: ['guest', 'user', 'admin'] },
  { key: 'auto-browser', icon: <RocketOutlined />, label: '17. 🤖 Auto Browser (অটো ব্রাউজার)', roles: ['guest', 'user', 'admin'] },
  { key: 'quotas', icon: <PieChartOutlined />, label: '18. 📈 Quotas (কোটা ম্যানেজমেন্ট)', roles: ['admin'] },
  { key: 'simulator', icon: <MobileOutlined />, label: '19. 📱 Simulator (সিমুলেটর)', roles: ['guest', 'user', 'admin'] },
  { key: 'reverse', icon: <CodeOutlined />, label: '20. 🔄 Reverse Engineering (রিভার্স ইঞ্জিনিয়ারিং)', roles: ['admin'] },
  { key: 'notifications', icon: <BellOutlined />, label: '21. 🔔 Notifications (নোটিফিকেশন)', roles: ['admin'] },
  { key: 'reports', icon: <BarChartOutlined />, label: '22. 📄 Reports (রিপোর্টস)', roles: ['admin'] },
  { key: 'performance', icon: <ClusterOutlined />, label: '23. ⚡ Performance (পারফরম্যান্স)', roles: ['admin'] },
  { key: 'backup', icon: <DatabaseOutlined />, label: '24. 💾 Backup (ব্যাকআপ)', roles: ['admin'] },
  { key: 'ocr', icon: <FileTextOutlined />, label: '25. 🔍 OCR Tool (OCR টুল)', roles: ['admin'] },
  { key: 'infrastructure', icon: <CloudServerOutlined />, label: '26. ☁️ Infrastructure (ইনফ্রাস্ট্রাকচার)', roles: ['admin'] },
  { key: 'code-analysis', icon: <CodeOutlined />, label: '27. 🔍 Code Intelligence (কোড ইন্টেলিজেন্স)', roles: ['admin'] },
];

export const getBreadcrumbs = (activeKey: string) => {
  const activeItem = allMenuItems.find(item => item.key === activeKey);
  return [
    { title: <><HomeOutlined /> <span>কমান্ড</span></>, key: 'home' },
    { title: activeItem?.label || 'ড্যাশবোর্ড', key: 'active' }
  ];
};
