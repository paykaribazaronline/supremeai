// AdminLayout.tsx - Cinematic layout wrapper for admin pages
// Provides consistent, responsive page styling for ultra-wide monitors and mobile

import React from 'react';
import { Layout, Typography, Row, Col, Space } from 'antd';
import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

const { Content } = Layout;
const { Title, Text } = Typography;

interface AdminLayoutProps {
  title: string;
  titleHighlight?: string;
  subtitle?: string;
  categoryLabel?: string;
  icon?: ReactNode;
  themeColor?: string;
  extra?: ReactNode;
  children: ReactNode;
  maxWidth?: string;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({
  title,
  titleHighlight,
  subtitle,
  categoryLabel,
  icon,
  themeColor = 'var(--neon-blue)',
  extra,
  children,
  maxWidth = '1600px'
}) => {
  return (
    <Content className="admin-page" style={{ maxWidth, margin: '0 auto', width: '100%' }}>
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div style={{ marginBottom: 32, borderBottom: `1px solid ${themeColor}33`, paddingBottom: 24 }}>
          <Row justify="space-between" align="bottom" gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                {icon && <span style={{ color: themeColor, fontSize: 20 }}>{icon}</span>}
                {categoryLabel && <Text style={{ color: themeColor, letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>{categoryLabel}</Text>}
              </div>
              <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 'clamp(24px, 3vw, 32px)' }}>
                {title} {titleHighlight && <span style={{ color: themeColor, textShadow: `0 0 10px ${themeColor}66` }}>{titleHighlight}</span>}
              </Title>
              {subtitle && <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>{subtitle}</Text>}
            </Col>
            <Col xs={24} lg={8} style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-end' }}>
              <Space wrap justify="end">
                {extra}
              </Space>
            </Col>
          </Row>
        </div>
        {children}
      </motion.div>
    </Content>
  );
};

export default AdminLayout;
