import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Typography, Row, Col, Card, Statistic, List, Avatar, Tag, Empty, Spin } from 'antd';
import { UserOutlined, ClockCircleOutlined, DeploymentUnitOutlined, TeamOutlined, FileTextOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

interface ActivityItem {
  id: string;
  user: string;
  action: string;
  target: string;
  timestamp: string;
  type: 'user' | 'system' | 'ai';
}

export default function AdminActivitySummary() {
  const [loading, setLoading] = useState(true);
  const [todayStats, setTodayStats] = useState({
    totalActions: 0,
    activeUsers: 0,
    aiRequests: 0,
    projectsCreated: 0,
  });
  const [recentActivity, setRecentActivity] = useState<ActivityItem[]>([]);

  useEffect(() => {
    fetchActivityData();
  }, []);

  const fetchActivityData = async () => {
    setLoading(true);
    try {
      const [statsRes, activityRes] = await Promise.all([
        fetch('/api/activity/stats'),
        fetch('/api/activity/recent'),
      ]);
      
      if (statsRes.ok) {
        const stats = await statsRes.json();
        setTodayStats(stats);
      }
      
      if (activityRes.ok) {
        const activity = await activityRes.json();
        setRecentActivity(activity);
      }
    } catch (error) {
      console.error('Error fetching activity data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getActivityColor = (type: string) => {
    switch (type) {
      case 'user': return '#1890ff';
      case 'system': return '#52c41a';
      case 'ai': return '#722ed1';
      default: return '#1890ff';
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: '24px' }}
    >
      <Title level={4} style={{ color: '#fff', marginBottom: 24 }}>
        আজকের সারসংক্ষেপ (Today's Summary)
      </Title>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          {/* Stats Overview */}
          <Row gutter={[24, 24]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Statistic
                  title={<Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>Total Actions</Text>}
                  value={todayStats.totalActions}
                  valueStyle={{ color: 'var(--neon-blue)', fontSize: 24, fontWeight: 700 }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Statistic
                  title={<Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>Active Users</Text>}
                  value={todayStats.activeUsers}
                  valueStyle={{ color: 'var(--neon-purple)', fontSize: 24, fontWeight: 700 }}
                  prefix={<TeamOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Statistic
                  title={<Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>AI Requests</Text>}
                  value={todayStats.aiRequests}
                  valueStyle={{ color: 'var(--success)', fontSize: 24, fontWeight: 700 }}
                  prefix={<DeploymentUnitOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Statistic
                  title={<Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>Projects Created</Text>}
                  value={todayStats.projectsCreated}
                  valueStyle={{ color: '#ffb347', fontSize: 24, fontWeight: 700 }}
                  prefix={<FileTextOutlined />}
                />
              </Card>
            </Col>
          </Row>

          {/* Recent Activity */}
          <Card className="glass-card">
            <Title level={5} style={{ color: '#fff', marginBottom: 16 }}>
              Recent Activity
            </Title>
            <List
              dataSource={recentActivity}
              renderItem={(item) => (
                <List.Item style={{ padding: '12px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                  <List.Item.Meta
                    avatar={
                      <Avatar
                        style={{
                          backgroundColor: getActivityColor(item.type),
                          verticalAlign: 'middle',
                        }}
                      >
                        {item.type === 'user' && <UserOutlined />}
                        {item.type === 'system' && <ClockCircleOutlined />}
                        {item.type === 'ai' && <DeploymentUnitOutlined />}
                      </Avatar>
                    }
                    title={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Text style={{ color: '#fff', fontWeight: 500 }}>{item.action}</Text>
                        <Text style={{ color: 'var(--text-dim)' }}>{item.target}</Text>
                      </div>
                    }
                    description={
                      <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                        {new Date(item.timestamp).toLocaleString()}
                      </Text>
                    }
                  />
                  <Tag color={getActivityColor(item.type)}>
                    {item.type.toUpperCase()}
                  </Tag>
                </List.Item>
              )}
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={<Text style={{ color: 'var(--text-dim)' }}>No recent activity</Text>}
                  />
                )
              }}
            />
          </Card>
        </>
      )}
    </motion.div>
  );
}