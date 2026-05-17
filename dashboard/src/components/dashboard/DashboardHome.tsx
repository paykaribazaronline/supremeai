import React from 'react';
import { motion } from 'framer-motion';
import { Typography, Space, Button, Progress } from 'antd';
import { RobotOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { NeuralCore } from './NeuralElements';
// import ActivityFeed from '../ActivityFeed';
import UserProfile from '../UserProfile';

const { Title, Text } = Typography;

const Waveform = () => (
  <div className="waveform-container">
    {[...Array(12)].map((_, i) => (
      <div 
        key={i} 
        className="wave-bar" 
        style={{ animationDelay: `${i * 0.1}s` }} 
      />
    ))}
  </div>
);

interface DashboardHomeProps {
  isAdmin: boolean;
  setActiveKey: (key: string) => void;
}

export const DashboardHome: React.FC<DashboardHomeProps> = ({ isAdmin, setActiveKey }) => {
  return (
    <div className="dashboard-container" style={{ padding: 'clamp(20px, 5vw, 40px)', position: 'relative', zIndex: 1 }}>
      <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 48 }}>
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.2 }}
          className="title-section"
        >
          <Title level={1} className="text-gradient text-high-vis main-title" style={{ margin: 0, fontSize: 'clamp(32px, 8vw, 48px)', fontWeight: 800, letterSpacing: '-1px' }}>
            SupremeAI অর্কেস্ট্রেটর
          </Title>
          <Space align="center" style={{ marginTop: 8 }} className="status-info">
            <div className="cyber-badge" style={{ background: 'var(--success)', color: '#000', fontWeight: 'bold' }}>সক্রিয়</div>
            <Text style={{ color: 'var(--text-main)', fontSize: 14, letterSpacing: 1, textTransform: 'uppercase', fontWeight: 500 }}>
              কার্নেল আইডি: <Text style={{ color: 'var(--neon-blue)', fontWeight: 'bold' }}>SAI-X900</Text> | ভার্সন: <Text style={{ color: 'var(--neon-purple)', fontWeight: 'bold' }}>4.2.0</Text>
            </Text>
          </Space>
        </motion.div>

        <div className="neural-core-container">
           <NeuralCore />
        </div>
      </div>

      <div 
        className="dashboard-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: 32
        }}
      >
        {/* Main Controls */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
          <motion.div whileHover={{ scale: 1.02 }} className="ai-card" style={{ flex: 1 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24, alignItems: 'center' }}>
              <RobotOutlined style={{ fontSize: 32, color: 'var(--neon-blue)' }} />
              <Waveform />
            </div>
            <Title level={3} style={{ color: 'var(--text-main)', marginBottom: 12 }}>নিউরাল নেক্সাস</Title>
            <p style={{ color: 'var(--text-dim)', lineHeight: 1.8, fontSize: 15, marginBottom: 24 }}>
              সিস্টেম ইন্টেলিজেন্সের সাথে সরাসরি যোগাযোগ করুন। মাল্টি-মোডাল ফ্লো এবং এজেন্টিক যুক্তি কার্যকর করুন।
            </p>
            <Button className="cyber-button" icon={<ThunderboltOutlined />} onClick={() => setActiveKey('ai')} style={{ width: '100%' }}>
              লিঙ্ক শুরু করুন
            </Button>
          </motion.div>

          {/* <ActivityFeed /> */}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
          {/* User Profile Summary */}
          <UserProfile />

          {/* Dynamic Metrics */}
          <motion.div className="ai-card glass-panel" style={{ background: 'rgba(188, 19, 254, 0.08)' }}>
             <Title level={4} style={{ color: 'var(--text-main)', marginBottom: 20 }}>সিস্টেম লোড ম্যাট্রিক্স</Title>
             <div style={{ marginBottom: 20 }}>
               <div style={{ display:'flex', justifyContent:'space-between', marginBottom: 8 }}><Text style={{color:'var(--text-dim)'}}>নিউরাল প্রসেসিং (NPU)</Text><Text style={{color:'var(--neon-purple)'}}>64%</Text></div>
               <Progress percent={64} status="active" strokeColor="var(--neon-purple)" trailColor="rgba(255,255,255,0.05)" showInfo={false} />
             </div>
             <div style={{ marginBottom: 20 }}>
               <div style={{ display:'flex', justifyContent:'space-between', marginBottom: 8 }}><Text style={{color:'var(--text-dim)'}}>ডেটা কগনিশন</Text><Text style={{color:'var(--neon-blue)'}}>42%</Text></div>
               <Progress percent={42} status="active" strokeColor="var(--neon-blue)" trailColor="rgba(255,255,255,0.05)" showInfo={false} />
             </div>
             <div>
               <div style={{ display:'flex', justifyContent:'space-between', marginBottom: 8 }}><Text style={{color:'var(--text-dim)'}}>সিকিউরিটি শিল্ড</Text><Text style={{color:'var(--success)'}}>99%</Text></div>
               <Progress percent={99} status="active" strokeColor="var(--success)" trailColor="rgba(255,255,255,0.05)" showInfo={false} />
             </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
};
