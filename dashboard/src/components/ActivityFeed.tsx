import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Typography, Space, Timeline, Badge } from 'antd';
import {
  HistoryOutlined,
  ThunderboltOutlined,
  CloudUploadOutlined,
  SafetyOutlined,
  ApiOutlined
} from '@ant-design/icons';

const { Text, Title } = Typography;

interface Activity {
  id: string;
  type: 'process' | 'security' | 'network' | 'upload';
  message: string;
  time: string;
  status: 'success' | 'warning' | 'info';
}

const ActivityFeed: React.FC = () => {
  const [activities, setActivities] = useState<Activity[]>([
    { id: '1', type: 'security', message: 'নিরাপত্তা প্রোটোকল আপডেট করা হয়েছে', time: '২ মিনিট আগে', status: 'success' },
    { id: '2', type: 'network', message: 'EU-West নোডের সাথে সংযোগ স্থাপিত', time: '৫ মিনিট আগে', status: 'info' },
    { id: '3', type: 'process', message: 'নিউরাল মডেলে নতুন ডেটাসেট ইনজেকশন সফল', time: '১২ মিনিট আগে', status: 'success' },
    { id: '4', type: 'upload', message: 'ক্লাউড ব্যাকআপ সিঙ্ক্রোনাইজেশন শুরু', time: '২৫ মিনিট আগে', status: 'info' },
    { id: '5', type: 'security', message: 'অস্বাভাবিক লগইন চেষ্টা সনাক্ত (IP: 192.168.1.1)', time: '১ ঘণ্টা আগে', status: 'warning' },
  ]);

  // Simulate real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      const types: ('process' | 'security' | 'network' | 'upload')[] = ['process', 'security', 'network', 'upload'];
      const statuses: ('success' | 'warning' | 'info')[] = ['success', 'info'];
      const messages = [
        'সিস্টেম থ্রেশহোল্ড অপ্টিমাইজড',
        'এপিআই গেটওয়ে ট্রাফিক স্ট্যাবল',
        'নতুন এনক্রিপশন লেয়ার যোগ করা হয়েছে',
        'মেমরি ক্লিনিং রুটিন সম্পন্ন'
      ];

      const newActivity: Activity = {
        id: Date.now().toString(),
        type: types[Math.floor(Math.random() * types.length)],
        message: messages[Math.floor(Math.random() * messages.length)],
        time: 'এখনই',
        status: statuses[Math.floor(Math.random() * statuses.length)]
      };

      setActivities(prev => [newActivity, ...prev.slice(0, 5)]);
    }, 15000);

    return () => clearInterval(interval);
  }, []);

  const getIcon = (type: Activity['type']) => {
    switch (type) {
      case 'process': return <ThunderboltOutlined style={{ color: 'var(--neon-blue)' }} />;
      case 'security': return <SafetyOutlined style={{ color: 'var(--success)' }} />;
      case 'network': return <ApiOutlined style={{ color: 'var(--neon-purple)' }} />;
      case 'upload': return <CloudUploadOutlined style={{ color: 'var(--neon-blue)' }} />;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className="ai-card glass-panel"
      style={{
        padding: 32,
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        minHeight: 400,
        background: 'rgba(13, 13, 18, 0.4)'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Space>
          <HistoryOutlined style={{ fontSize: 24, color: 'var(--neon-blue)' }} />
          <Title level={4} style={{ color: 'var(--text-main)', margin: 0 }}>অ্যাক্টিভিটি ফিড</Title>
        </Space>
        <Badge status="processing" text={<Text style={{ fontSize: 12, color: 'var(--success)' }}>লাইভ</Text>} />
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingRight: 12 }}>
        <Timeline
          mode="left"
          items={activities.map(act => ({
            label: <Text style={{ color: 'var(--text-dim)', fontSize: 11 }}>{act.time}</Text>,
            children: (
              <AnimatePresence mode="popLayout">
                <motion.div
                  key={act.id}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  style={{ marginBottom: 16 }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <div style={{
                      padding: 8,
                      background: 'rgba(255,255,255,0.02)',
                      borderRadius: 8,
                      border: '1px solid rgba(255,255,255,0.05)',
                      display: 'flex'
                    }}>
                      {getIcon(act.type)}
                    </div>
                    <div>
                      <Text style={{ display: 'block', color: 'var(--text-main)', fontSize: 14, fontWeight: 500 }}>
                        {act.message}
                      </Text>
                      <Text style={{ fontSize: 11, color: act.status === 'warning' ? 'var(--warning)' : 'var(--text-dim)' }}>
                        প্রোটোকল: {act.type.toUpperCase()}
                      </Text>
                    </div>
                  </div>
                </motion.div>
              </AnimatePresence>
            ),
            dot: <div style={{ width: 8, height: 8, borderRadius: '50%', background: act.status === 'success' ? 'var(--success)' : act.status === 'warning' ? 'var(--warning)' : 'var(--neon-blue)', boxShadow: `0 0 10px ${act.status === 'success' ? 'var(--success)' : 'var(--neon-blue)'}` }} />
          }))}
          style={{ marginTop: 8 }}
        />
      </div>
    </motion.div>
  );
};

export default ActivityFeed;
