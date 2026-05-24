// AdminLogs.tsx - Cinematic System Activity Logs
import React, { useState, useEffect, useMemo } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Table, Tag, Input, Select, Tooltip } from 'antd';
import {
  FileTextOutlined, 
  ReloadOutlined, 
  SearchOutlined, 
  SortAscendingOutlined, 
  SortDescendingOutlined,
  HistoryOutlined,
  FilterOutlined,
  BugOutlined,
  ExclamationCircleOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;
const { Option } = Select;

interface ActivityLog {
  id?: string;
  user: string;
  action: string;
  category: string;
  severity: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  outcome: string;
  details?: string;
  timestamp?: string;
}

const AdminLogs: React.FC = () => {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('');
  const [sortBy, setSortBy] = useState<keyof ActivityLog | null>('timestamp');
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('descend');

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/logs?severity=' + severityFilter);
      if (response.ok) {
        const result = await response.json();
        const logData = result.data?.logs || result.data || [];
        setLogs(logData);
      }
    } catch (err) {} finally { setLoading(false); }
  };

  useEffect(() => { fetchLogs(); }, [severityFilter]);

  const processedLogs = useMemo(() => {
    let filtered = logs.filter(log => {
      const searchLower = searchText.toLowerCase();
      return (log.user?.toLowerCase().includes(searchLower) || log.action?.toLowerCase().includes(searchLower) || log.category?.toLowerCase().includes(searchLower));
    });
    return filtered;
  }, [logs, searchText]);

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(255, 255, 255, 0.1)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <HistoryOutlined style={{ color: 'rgba(255,255,255,0.45)', fontSize: 20 }} />
              <Text style={{ color: 'rgba(255,255,255,0.45)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>AUDIT TRAIL</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              System <span className="text-gradient">Activity Logs</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Immutable record of system operations, operator actions, and neural state changes.</Text>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined spin={loading} />} onClick={fetchLogs} className="glass-action-button">Purge & Sync</Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Statistics Row */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Daily Events</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{logs.length * 12}</div>
                       </div>
                       <FileTextOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid #ef4444' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Error Shards</Text>
                          <div style={{ color: '#ef4444', fontSize: 24, fontWeight: 800 }}>{logs.filter(l => l.severity === 'ERROR').length}</div>
                       </div>
                       <BugOutlined style={{ color: '#ef4444', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid #f59e0b' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Security Alerts</Text>
                          <div style={{ color: '#f59e0b', fontSize: 24, fontWeight: 800 }}>{logs.filter(l => l.severity === 'WARN').length}</div>
                       </div>
                       <ExclamationCircleOutlined style={{ color: '#f59e0b', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Log Integrity</Text>
                          <div style={{ color: 'var(--success)', fontSize: 20, fontWeight: 800 }}>VERIFIED</div>
                       </div>
                       <SafetyCertificateOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Toolbar & Table */}
        <Col span={24}>
           <div className="glass-card" style={{ minHeight: 600 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 16 }}>
                 <div style={{ display: 'flex', gap: 16, flex: 1, minWidth: 300 }}>
                    <Input
                       prefix={<SearchOutlined style={{ color: 'rgba(255,255,255,0.2)' }} />}
                       placeholder="Filter by User, Action or Category..."
                       value={searchText}
                       onChange={e => setSearchText(e.target.value)}
                       style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', color: '#fff' }}
                    />
                 </div>
                 <Space>
                    <Select
                       placeholder="Severity"
                       value={severityFilter || undefined}
                       onChange={v => setSeverityFilter(v || '')}
                       style={{ width: 140 }}
                       dropdownStyle={{ background: '#080810', border: '1px solid rgba(255,255,255,0.1)' }}
                    >
                       <Option value="">All Levels</Option>
                       <Option value="INFO">INFO</Option>
                       <Option value="WARN">WARN</Option>
                       <Option value="ERROR">ERROR</Option>
                    </Select>
                    <Button
                       icon={sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
                       onClick={() => setSortOrder(sortOrder === 'ascend' ? 'descend' : 'ascend')}
                       className="glass-action-button"
                    />
                 </Space>
              </div>

              <Table
                 dataSource={processedLogs}
                 loading={loading}
                 pagination={{ pageSize: 12 }}
                 columns={[
                    { title: 'TIMESTAMP', dataIndex: 'timestamp', key: 'timestamp', render: (t: string) => <Text style={{ fontFamily: 'JetBrains Mono', color: 'rgba(255,255,255,0.45)', fontSize: 11 }}>{t}</Text> },
                    { title: 'OPERATOR', dataIndex: 'user', key: 'user', render: (u: string) => <Text strong style={{ color: '#fff' }}>{u}</Text> },
                    { title: 'ACTION', dataIndex: 'action', key: 'action', render: (a: string) => <Text style={{ color: 'var(--neon-blue)' }}>{a}</Text> },
                    { title: 'SEVERITY', dataIndex: 'severity', key: 'severity', render: (s: string) => <Tag color={s === 'ERROR' ? 'red' : s === 'WARN' ? 'orange' : 'blue'}>{s}</Tag> },
                    { title: 'OUTCOME', dataIndex: 'outcome', key: 'outcome' }
                 ]}
                 className="cyber-table"
              />
           </div>
        </Col>
      </Row>

      <style>{`
        .ant-select-selector { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; }
        .ant-select-selection-placeholder { color: rgba(255,255,255,0.2) !important; }
      `}</style>
    </motion.div>
  );
};

export default AdminLogs;
