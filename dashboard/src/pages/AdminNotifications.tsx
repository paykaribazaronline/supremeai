import React, { useState, useEffect } from 'react';
import { Layout, Typography, Card, Space, Row, Col, Statistic, Spin, Alert, Button, Tag, Select, Badge, Empty } from 'antd';
import { BellOutlined, ReloadOutlined, CheckCircleOutlined, WarningOutlined, CloseCircleOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;
const { Option } = Select;

interface NotificationItem {
  id: string;
  action: string;
  user: string;
  category: string;
  severity: string;
  details: string;
  timestamp: string;
  ip: string;
  outcome: string;
  read: boolean;
}

interface Stats {
  total: number;
  unread: number;
  critical: number;
  warning: number;
}

const AdminNotifications: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [filter, setFilter] = useState<string>('all');
  const [stats, setStats] = useState<Stats>({ total: 0, unread: 0, critical: 0, warning: 0 });

  const fetchNotifications = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authUtils.fetchWithAuth('/api/activity/summary?limit=50');
      if (res.ok) {
        const data = await res.json();
        const list: any[] = data.data?.logs || data.logs || data.data || data || [];

        const now = Date.now();
        const items: NotificationItem[] = list.map((item: any, idx: number) => ({
          id: item.id || `act-${idx}`,
          action: item.action || '',
          user: item.user || item.action || 'Unknown',
          category: item.category || 'general',
          severity: item.severity || 'info',
          details: item.details || item.action || '',
          timestamp: item.timestamp || new Date().toISOString(),
          ip: item.ip || '',
          outcome: item.outcome || '',
          read: item.outcome === 'success',
        }));

        setNotifications(items);
        setStats({
          total: items.length,
          unread: items.filter((n) => !n.read).length,
          critical: items.filter((n) => n.severity === 'critical').length,
          warning: items.filter((n) => n.severity === 'warning').length,
        });
      } else {
        setNotifications([]);
        setStats({ total: 0, unread: 0, critical: 0, warning: 0 });
      }
    } catch (err) {
      setError('নোটিফিকেশন লোড করতে সমস্যা হয়েছে। দয়া করে আবার চেষ্টা করুন।');
      console.error('Notifications fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const toggleRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: !n.read } : n))
    );
  };

  const getSeverityTag = (severity: string) => {
    const map: Record<string, { color: string; label: string }> = {
      critical: { color: 'red', label: 'ক্রিটিক্যাল' },
      warning: { color: 'orange', label: 'ওয়ার্নিং' },
      info: { color: 'blue', label: 'ইনফো' },
      debug: { color: 'default', label: 'ডিবাগ' },
      success: { color: 'green', label: 'সাকসেস' },
    };
    const s = map[severity.toLowerCase()] || { color: 'default', label: severity };
    return <Tag color={s.color}>{s.label}</Tag>;
  };

  const getSeverityIcon = (severity: string) => {
    switch (severity.toLowerCase()) {
      case 'critical': return <WarningOutlined style={{ color: '#ef4444' }} />;
      case 'warning': return <WarningOutlined style={{ color: '#f97316' }} />;
      case 'success': return <CheckCircleOutlined style={{ color: '#10b981' }} />;
      default: return <InfoCircleOutlined style={{ color: '#3b82f6' }} />;
    }
  };

  const timeAgo = (ts: string): string => {
    const diff = Date.now() - new Date(ts).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'এইমাত্র';
    if (mins < 60) return `${mins} মিনিট আগে`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs} ঘন্টা আগে`;
    const days = Math.floor(hrs / 24);
    return `${days} দিন আগে`;
  };

  const filteredNotifications =
    filter === 'all'
      ? notifications
      : filter === 'unread'
        ? notifications.filter((n) => !n.read)
        : notifications.filter((n) => n.severity.toLowerCase() === filter);

  return (
    <div className="admin-page">
      <div className="admin-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
        <div>
          <Title level={2} className="admin-title">
            নোটিফিকেশন
          </Title>
          <Text className="admin-subtitle">
            সিস্টেম অ্যালার্ট, অ্যাক্টিভিটি আপডেট এবং ইভেন্ট লগ
          </Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={fetchNotifications}
          loading={loading}
          className="admin-btn-primary"
          style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)' }}
        >
          রিফ্রেশ
        </Button>
      </div>

      {error && <Alert type="error" message={error} style={{ marginBottom: 24 }} showIcon />}

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>সর্বমোট</Text>}
              value={stats.total}
              prefix={<BellOutlined style={{ color: '#667eea' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>অনপঠিত</Text>}
              value={stats.unread}
              prefix={<BellOutlined style={{ color: '#f59e0b' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>ক্রিটিক্যাল</Text>}
              value={stats.critical}
              prefix={<WarningOutlined style={{ color: '#ef4444' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" bordered={false}>
            <Statistic
              title={<Text style={{ color: 'rgba(255,255,255,0.6)' }}>ওয়ার্নিং</Text>}
              value={stats.warning}
              prefix={<WarningOutlined style={{ color: '#f97316' }} />}
              valueStyle={{ color: '#fff' }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        className="glass-card"
        bordered={false}
        title={
          <Space>
            <BellOutlined style={{ color: '#667eea' }} />
            <span style={{ color: '#fff', fontSize: 16, fontWeight: 600 }}>
              নোটিফিকেশন ফিড
            </span>
          </Space>
        }
        extra={
          <Select
            value={filter}
            onChange={setFilter}
            style={{ width: 140 }}
            suffixIcon={null}
          >
            <Option value="all">সবগুলো</Option>
            <Option value="unread">অনপঠিত</Option>
            <Option value="critical">ক্রিটিক্যাল</Option>
            <Option value="warning">ওয়ার্নিং</Option>
            <Option value="success">সাকসেস</Option>
            <Option value="info">ইনফো</Option>
          </Select>
        }
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: '48px 0' }}>
            <Spin size="large" />
          </div>
        ) : filteredNotifications.length === 0 ? (
          <Empty description="কোনো নোটিফিকেশন নেই" />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filteredNotifications.map((notif) => (
              <Card
                key={notif.id}
                size="small"
                style={{
                  background: notif.read ? 'rgba(255,255,255,0.02)' : 'rgba(99,102,241,0.06)',
                  border: `1px solid ${notif.read ? 'rgba(255,255,255,0.06)' : 'rgba(99,102,241,0.2)'}`,
                  borderLeft: `4px solid ${
                    notif.severity === 'critical'
                      ? '#ef4444'
                      : notif.severity === 'warning'
                        ? '#f97316'
                        : notif.severity === 'success'
                          ? '#10b981'
                          : '#3b82f6'
                  }`,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onClick={() => toggleRead(notif.id)}
                bodyStyle={{ padding: '12px 16px' }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <div style={{ marginTop: 2, flexShrink: 0 }}>{getSeverityIcon(notif.severity)}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
                      <Text strong style={{ color: '#fff', fontSize: 14 }}>
                        {notif.action}
                      </Text>
                      <Space size={4} wrap>
                        {getSeverityTag(notif.severity)}
                        <Tag color="geekblue" style={{ marginRight: 0 }}>{notif.category}</Tag>
                        {!notif.read && <Badge status="processing" text={<Text style={{ color: '#818cf8', fontSize: 11 }}>নতুন</Text>} />}
                      </Space>
                    </div>
                    <Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12, display: 'block', marginTop: 4 }}>
                      {notif.details || notif.action}
                    </Text>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginTop: 6 }}>
                      <Text style={{ color: 'rgba(255,255,255,0.35)', fontSize: 11 }}>ব্যবহারকারী: {notif.user}</Text>
                      <Text style={{ color: 'rgba(255,255,255,0.35)', fontSize: 11 }}>{timeAgo(notif.timestamp)}</Text>
                      {notif.outcome && (
                        <Text style={{ color: 'rgba(255,255,255,0.35)', fontSize: 11 }}>ফলাফল: {notif.outcome}</Text>
                      )}
                    </div>
                  </div>
                  <Button
                    type="text"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    style={{
                      color: notif.read ? '#4ade80' : 'rgba(255,255,255,0.3)',
                      flexShrink: 0,
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleRead(notif.id);
                    }}
                  />
                </div>
              </Card>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default AdminNotifications;
