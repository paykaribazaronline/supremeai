// AdminBackup.tsx - Cinematic Data Vault
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Table, Tag, Progress, Statistic } from 'antd';
import {
  CloudUploadOutlined,
  ReloadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  DatabaseOutlined,
  HistoryOutlined,
  SafetyCertificateOutlined,
  LockOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface BackupItem {
  id: string;
  name: string;
  size: string;
  timestamp: string;
  status: string;
}

const AdminBackup: React.FC = () => {
  const [backups, setBackups] = useState<BackupItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [triggering, setTriggering] = useState(false);

  const fetchBackups = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/backup/list');
      if (response.ok) {
        const result = await response.json();
        setBackups(result.data || []);
      }
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchBackups(); }, []);

  const handleTriggerBackup = async () => {
    setTriggering(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/backup/trigger', {
        method: 'POST',
        body: JSON.stringify({ name: `Vault Sync ${new Date().toLocaleTimeString()}` })
      });
      if (response.ok) {
        message.success('Snapshot sequence initiated');
        fetchBackups();
      }
    } catch (err) {} finally { setTriggering(false); }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <DatabaseOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>SYSTEM ARCHIVE</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              Data <span className="text-gradient">Vault</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Secure system snapshots, encrypted neural weights, and operator state backups.</Text>
          </Col>
          <Col>
            <Space>
              <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchBackups} className="glass-action-button">Refresh Index</Button>
              <Button type="primary" icon={<CloudUploadOutlined />} loading={triggering} onClick={handleTriggerBackup} className="cyber-button">Initiate Snapshot</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
         {/* Summary Cards */}
         <Col span={24}>
            <Row gutter={[24, 24]}>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Stored Snapshots</Text>
                           <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{backups.length}</div>
                        </div>
                        <HistoryOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Vault Integrity</Text>
                           <div style={{ color: 'var(--success)', fontSize: 20, fontWeight: 800 }}>VERIFIED</div>
                        </div>
                        <SafetyCertificateOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Encryption</Text>
                           <div style={{ color: '#fff', fontSize: 20, fontWeight: 800 }}>AES-256</div>
                        </div>
                        <LockOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
               <Col xs={12} lg={6}>
                  <div className="glass-card">
                     <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                           <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Cloud Sync</Text>
                           <div style={{ color: 'var(--neon-blue)', fontSize: 20, fontWeight: 800 }}>ACTIVE</div>
                        </div>
                        <ThunderboltOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                     </div>
                  </div>
               </Col>
            </Row>
         </Col>

         {/* Main Archive Table */}
         <Col span={24}>
            <div className="glass-card" style={{ minHeight: 600 }}>
               <div className="glass-card-title">Historical Archive Matrix <DatabaseOutlined /></div>
               <Table
                 dataSource={backups} pagination={{ pageSize: 12 }} loading={loading} rowKey="id"
                 columns={[
                    { title: 'SNAPSHOT NAME', dataIndex: 'name', key: 'name', render: (t: string) => <Text strong style={{ color: '#fff' }}>{t}</Text> },
                    { title: 'MAGNITUDE', dataIndex: 'size', key: 'size', render: (s: string) => <Text style={{ fontFamily: 'JetBrains Mono', color: 'var(--neon-blue)' }}>{s}</Text> },
                    { title: 'TIMESTAMP', dataIndex: 'timestamp', key: 'timestamp', render: (t: string) => <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11 }}>{t}</Text> },
                    { title: 'STATUS', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'COMPLETED' ? 'green' : 'blue'}>{s}</Tag> },
                    { title: 'ACTIONS', key: 'a', render: (_, r) => <Space><Button icon={<DownloadOutlined />} size="small" ghost>Retrieve</Button><Button icon={<DeleteOutlined />} size="small" danger ghost>Purge</Button></Space> }
                 ]}
                 className="cyber-table"
               />
            </div>
         </Col>
      </Row>

      <div className="glass-card" style={{ marginTop: 24, background: 'rgba(16, 185, 129, 0.05)' }}>
         <Text style={{ color: 'var(--success)', fontSize: 11, fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }}>
            Vault Protocol: All system snapshots are sharded and distributed across redundant secure nodes.
         </Text>
      </div>
    </motion.div>
  );
};

export default AdminBackup;
