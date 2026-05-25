// AdminSystemAlerts.tsx - System Alerts Monitoring
import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Typography, Row, Col, Card, Badge, List, Tag, Empty, Spin, Button, Space, message } from 'antd';
import { ExclamationCircleOutlined, SyncOutlined, DeleteOutlined, BellOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface SystemAlert {
  id: string;
  level: 'info' | 'warning' | 'error' | 'critical';
  title: string;
  message: string;
  source: string;
  timestamp: string;
  acknowledged: boolean;
}

export default function AdminSystemAlerts() {
  const [loading, setLoading] = useState(true);
  const [alerts, setAlerts] = useState<SystemAlert[]>([]);

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/alerts');
      if (res.ok) {
        const data = await res.json();
        setAlerts(Array.isArray(data) ? data : data.alerts || []);
      }
    } catch (error) {
      message.error('Failed to fetch alerts');
    } finally {
      setLoading(false);
    }
  };

  const getAlertColor = (level: string) => {
    switch (level) {
      case 'critical': return 'red';
      case 'error': return 'orange';
      case 'warning': return 'gold';
      default: return 'blue';
    }
  };

  const getAlertIcon = (level: string) => {
    return <ExclamationCircleOutlined style={{ color: level === 'critical' || level === 'error' ? '#ff4d4f' : '#faad14' }} />;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: '24px' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          System Alerts
        </Title>
        <Space>
          <Button icon={<SyncOutlined />} onClick={fetchAlerts} className="glass-action-button">
            Refresh
          </Button>
          <Button icon={<DeleteOutlined />} onClick={() => setAlerts([])} className="glass-action-button">
            Clear All
          </Button>
        </Space>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col span={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase' }}>Critical</Text>
                <div style={{ color: '#ff4d4f', fontSize: 28, fontWeight: 700 }}>
                  {alerts.filter(a => a.level === 'critical').length}
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase' }}>Errors</Text>
                <div style={{ color: '#ff7f50', fontSize: 28, fontWeight: 700 }}>
                  {alerts.filter(a => a.level === 'error').length}
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase' }}>Warnings</Text>
                <div style={{ color: '#faad14', fontSize: 28, fontWeight: 700 }}>
                  {alerts.filter(a => a.level === 'warning').length}
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card className="glass-card" style={{ textAlign: 'center' }}>
                <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase' }}>Info</Text>
                <div style={{ color: '#1890ff', fontSize: 28, fontWeight: 700 }}>
                  {alerts.filter(a => a.level === 'info').length}
                </div>
              </Card>
            </Col>
          </Row>

          <Card className="glass-card">
            <List
              dataSource={alerts}
              renderItem={(alert) => (
                <List.Item style={{ padding: '16px', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                  <List.Item.Meta
                    avatar={getAlertIcon(alert.level)}
                    title={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Text style={{ color: '#fff', fontWeight: 600 }}>{alert.title}</Text>
                        <Tag color={getAlertColor(alert.level)}>{alert.level.toUpperCase()}</Tag>
                        {!alert.acknowledged && (
                          <Badge status="warning" text={<Text style={{ fontSize: 11, color: '#faad14' }}>NEW</Text>} />
                        )}
                      </div>
                    }
                    description={
                      <div style={{ marginTop: 8 }}>
                        <Text style={{ color: 'rgba(255,255,255,0.7)' }}>{alert.message}</Text>
                        <div style={{ marginTop: 4 }}>
                          <Text style={{ color: 'var(--text-dim)', fontSize: 11 }}>{alert.source}</Text>
                          <Text style={{ color: 'var(--text-dim)', fontSize: 11, margin: '0 8px'}>•</Text>
                          <Text style={{ color: 'var(--text-dim)', fontSize: 11 }}>
                            {new Date(alert.timestamp).toLocaleString()}
                          </Text>
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
              locale={{
                emptyText: (
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={<Text style={{ color: 'var(--text-dim)' }}>No system alerts</Text>}
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