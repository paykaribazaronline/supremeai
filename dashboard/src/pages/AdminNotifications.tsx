// AdminNotifications.tsx - Cinematic Alert Dispatcher
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Select, Tag, Empty, message } from 'antd';
import {
  BellOutlined,
  ReloadOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  BugOutlined,
  SecurityScanOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
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
  read: boolean;
}

const AdminNotifications: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [filter, setFilter] = useState<string>('all');

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/activity/summary?limit=50');
      if (res.ok) {
        const data = await res.json();
        const list = data.data?.logs || data.logs || [];
        setNotifications(list.map((n: any, i: number) => ({
          id: n.id || `n-${i}`,
          action: n.action,
          user: n.user || 'SYSTEM',
          category: n.category || 'GRID',
          severity: n.severity || 'info',
          details: n.details || n.action,
          timestamp: n.timestamp || new Date().toISOString(),
          read: n.outcome === 'success'
        })));
      }
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchNotifications(); }, []);

  const getSeverityColor = (s: string) => {
    switch (s.toLowerCase()) {
      case 'critical': return '#ff4d4f';
      case 'warning': return '#f59e0b';
      case 'success': return 'var(--success)';
      default: return 'var(--neon-blue)';
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: '1400px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(188, 19, 254, 0.2)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <BellOutlined style={{ color: 'var(--neon-purple)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-purple)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>NEURAL DISPATCHER</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              System <span className="text-gradient">Notifications</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Global alert stream, autonomous security pings, and operator consensus logs.</Text>
          </Col>
          <Col>
            <Space>
              <Button icon={<EyeOutlined />} className="glass-action-button">Mark All Read</Button>
              <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchNotifications} className="glass-action-button">Sync Feed</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Alerts KPI Cards */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Unread Pings</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{notifications.filter(n => !n.read).length}</div>
                       </div>
                       <BellOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid #ff4d4f' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Critical Shards</Text>
                          <div style={{ color: '#ff4d4f', fontSize: 24, fontWeight: 800 }}>{notifications.filter(n => n.severity === 'critical').length}</div>
                       </div>
                       <BugOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Defense Pings</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>ACTIVE</div>
                       </div>
                       <SecurityScanOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Sync Latency</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>12ms</div>
                       </div>
                       <GlobalOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Notification Feed */}
        <Col span={24}>
           <div className="glass-card" style={{ minHeight: 600 }}>
              <div className="glass-card-title">
                 Neural Alert Matrix
                 <Select
                   value={filter} onChange={setFilter} style={{ width: 160 }}
                   className="cyber-select" dropdownStyle={{ background: '#080810', border: '1px solid var(--neon-purple)' }}
                 >
                    <Option value="all">Global Feed</Option>
                    <Option value="unread">New Shards</Option>
                    <Option value="critical">Critical</Option>
                 </Select>
              </div>

              <div style={{ marginTop: 24 }}>
                 {loading ? <div style={{ textAlign: 'center', padding: 100 }}><Spin size="large" tip="SYNCING NEURAL DISPATCH..." /></div> :
                  notifications.length === 0 ? <Empty description="Dispatcher queue clear." /> : (
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                       <AnimatePresence>
                          {notifications.map((n) => (
                             <motion.div
                                key={n.id} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }}
                                className="glass-card" style={{ padding: '12px 20px', borderLeft: `4px solid ${getSeverityColor(n.severity)}`, background: n.read ? 'rgba(255,255,255,0.02)' : 'rgba(188, 19, 254, 0.05)' }}
                             >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                   <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                                      <div style={{ color: getSeverityColor(n.severity) }}>{n.severity === 'critical' ? <WarningOutlined /> : <InfoCircleOutlined />}</div>
                                      <div>
                                         <Text strong style={{ color: '#fff', fontSize: 14 }}>{n.action}</Text>
                                         <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, display: 'block' }}>{n.details}</Text>
                                      </div>
                                   </div>
                                   <div style={{ textAlign: 'right' }}>
                                      <Tag color="black" style={{ border: '1px solid rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.3)', fontSize: 10 }}>{n.category}</Tag>
                                      <Text style={{ color: 'rgba(255,255,255,0.2)', fontSize: 10, display: 'block', marginTop: 4 }}>{new Date(n.timestamp).toLocaleTimeString()}</Text>
                                   </div>
                                </div>
                             </motion.div>
                          ))}
                       </AnimatePresence>
                    </Space>
                  )}
              </div>
           </div>
        </Col>
      </Row>

      <style>{`
        .cyber-select .ant-select-selector { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; border-radius: 8px !important; }
      `}</style>
    </motion.div>
  );
};

export default AdminNotifications;
