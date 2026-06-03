import React from 'react';
import { motion } from 'framer-motion';
import { Avatar, Typography, Space, Tag, Progress } from 'antd';
import { UserOutlined, CrownOutlined, SafetyCertificateOutlined, MailOutlined } from '@ant-design/icons';
import { useRole } from '../contexts/RoleContext';

const { Text, Title } = Typography;

const UserProfile: React.FC = () => {
  const { user, isAdmin, isAuthenticated } = useRole();

  if (!isAuthenticated) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ai-card glass-panel"
        style={{ padding: 32, textAlign: 'center' }}
      >
        <Avatar size={80} icon={<UserOutlined />} style={{ background: 'rgba(255,255,255,0.05)', marginBottom: 16 }} />
        <Title level={4} style={{ color: 'var(--text-main)', marginBottom: 8 }}>গেস্ট এনটিটি</Title>
        <Text style={{ color: 'var(--text-dim)' }}>সিস্টেমের পূর্ণ সুবিধা পেতে লগইন করুন।</Text>
      </motion.div>
    );
  }

  const userTier = isAdmin ? 'ADMIN' : 'PRO OPERATOR';
  const tierColor = isAdmin ? 'var(--success)' : 'var(--neon-blue)';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="ai-card glass-panel profile-widget"
      style={{
        padding: 32,
        position: 'relative',
        overflow: 'hidden',
        border: `1px solid rgba(255,255,255,0.05)`,
        background: 'rgba(13, 13, 18, 0.4)'
      }}
    >
      <div className="profile-bg-glow" style={{
        position: 'absolute',
        top: -50,
        right: -50,
        width: 150,
        height: 150,
        background: tierColor,
        filter: 'blur(80px)',
        opacity: 0.1,
        pointerEvents: 'none'
      }} />

      <Space align="start" size="large" style={{ width: '100%' }}>
        <div style={{ position: 'relative' }}>
          <Avatar
            size={84}
            src={user?.photoURL}
            icon={<UserOutlined />}
            style={{
              border: `2px solid ${tierColor}`,
              boxShadow: `0 0 20px ${tierColor}44`,
              background: '#1a1a2e'
            }}
          />
          {isAdmin && (
            <div style={{
              position: 'absolute',
              bottom: 0,
              right: 0,
              background: 'var(--success)',
              borderRadius: '50%',
              width: 24,
              height: 24,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px solid #000'
            }}>
              <CrownOutlined style={{ fontSize: 12, color: '#000' }} />
            </div>
          )}
        </div>

        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <Title level={3} style={{ color: 'var(--text-main)', margin: 0, fontSize: 20 }}>
              {user?.displayName || 'অজ্ঞাত ব্যবহারকারী'}
            </Title>
            <Tag color={isAdmin ? 'success' : 'blue'} style={{ borderRadius: 4, margin: 0 }}>
              {userTier}
            </Tag>
          </div>
          <Space direction="vertical" size={2}>
            <Text style={{ color: 'var(--text-dim)', fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
              <MailOutlined style={{ fontSize: 12 }} /> {user?.email}
            </Text>
            <Text style={{ color: 'var(--neon-purple)', fontSize: 11, letterSpacing: 1, textTransform: 'uppercase', fontWeight: 700 }}>
              <SafetyCertificateOutlined /> ভেরিফাইড কার্নেল অ্যাক্সেস
            </Text>
          </Space>
        </div>
      </Space>

      <div style={{ marginTop: 32 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>ব্যক্তিগত কোটা ব্যবহার</Text>
          <Text style={{ color: tierColor, fontSize: 12, fontWeight: 'bold' }}>78%</Text>
        </div>
        <Progress
          percent={78}
          showInfo={false}
          strokeColor={{
            '0%': 'var(--neon-blue)',
            '100%': 'var(--neon-purple)',
          }}
          trailColor="rgba(255,255,255,0.05)"
          strokeWidth={6}
        />
      </div>

      <div style={{
        marginTop: 24,
        padding: '12px 16px',
        background: 'rgba(255,255,255,0.02)',
        borderRadius: 8,
        border: '1px solid rgba(255,255,255,0.05)'
      }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div>
            <Text style={{ display: 'block', color: 'var(--text-dim)', fontSize: 10, textTransform: 'uppercase' }}>অবদান</Text>
            <Text style={{ color: 'var(--text-main)', fontSize: 16, fontWeight: 700 }}>২৪২</Text>
          </div>
          <div>
            <Text style={{ display: 'block', color: 'var(--text-dim)', fontSize: 10, textTransform: 'uppercase' }}>র‍্যাঙ্ক</Text>
            <Text style={{ color: 'var(--text-main)', fontSize: 16, fontWeight: 700 }}>#১২</Text>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export default UserProfile;
