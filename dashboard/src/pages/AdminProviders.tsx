// AdminProviders.tsx - Cinematic AI Orchestration Center
import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Spin, Alert, message, Input, Select, Tag, Statistic } from 'antd';
import {
  ApiOutlined,
  ReloadOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  AppstoreOutlined,
  DatabaseOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';
import { useRole } from '../contexts/RoleContext';

// Modular Components
import { Provider, ProviderHealthStats as StatsType } from '../components/providers/types';
import ProvidersTable from '../components/providers/ProvidersTable';
import ProviderModal from '../components/providers/ProviderModal';

const { Title, Text } = Typography;

const AdminProviders: React.FC = () => {
  const { isGuest } = useRole();
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [healthStats, setHealthStats] = useState<StatsType | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'activationStatus' | 'modelName'>('activationStatus');
  const [wakeUpLoading, setWakeUpLoading] = useState(false);

  const fetchProviders = async () => {
    setLoading(true);
    try {
      const [provRes, statsRes] = await Promise.all([
        authUtils.fetchWithAuth('/getConfiguredProviders'),
        authUtils.fetchWithAuth('/getProviderHealthStats')
      ]);
      if (provRes.ok) {
        const result = await provRes.json();
        setProviders(result.data?.providers || result.data || []);
      }
      if (statsRes.ok) {
        const stats = await statsRes.json();
        setHealthStats(stats.data);
      }
    } catch (err) {
      setProviders([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProviders(); }, []);

  const handleWakeUpTest = async () => {
    setWakeUpLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/self-healing/health-check/now', { method: 'POST' });
      if (res.ok) {
        message.success('Neural link verification complete');
        fetchProviders();
      }
    } catch (err) {
      message.error('Neural link failure');
    } finally {
      setWakeUpLoading(false);
    }
  };

  const handleEdit = (record: Provider) => {
    setEditingProvider(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/providers/${id}`, { method: 'DELETE' });
      if (response.ok) {
        message.success('Endpoint disconnected');
        fetchProviders();
      }
    } catch (err) {
      message.error('Failed to disconnect');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
       {/* Cinematic Header */}
       <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(0, 243, 255, 0.1)', paddingBottom: 24 }}>
         <Row justify="space-between" align="bottom">
           <Col>
             <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
               <GlobalOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
               <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>NEURAL ENDPOINTS</Text>
             </div>
             <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
               AI <span className="text-gradient">Providers</span>
             </Title>
             <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Manage global LLM orchestration and model health.</Text>
           </Col>
           <Col>
             <Space>
               <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 12px', borderRadius: 6, background: 'rgba(82, 196, 26, 0.15)', border: '1px solid rgba(82, 196, 26, 0.4)' }}>
                 <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#52c41a', boxShadow: '0 0 8px #52c41a' }} />
                 <Text style={{ color: '#52c41a', fontSize: 12, fontWeight: 700, letterSpacing: 1 }}>LOCAL MODE ACTIVE</Text>
               </div>
               <Button
                 icon={<ThunderboltOutlined />}
                 onClick={handleWakeUpTest}
                 loading={wakeUpLoading}
                 className="glass-action-button"
               >
                 Wake Up Test
               </Button>
               <Button
                 icon={<ReloadOutlined spin={loading} />}
                 onClick={fetchProviders}
                 className="glass-action-button"
               >
                 Sync
               </Button>
               <Button
                 type="primary"
                 icon={<PlusOutlined />}
                 onClick={() => { setEditingProvider(null); setModalVisible(true); }}
                 className="cyber-button"
               >
                 Connect Model
               </Button>
             </Space>
           </Col>
         </Row>
       </div>

      <Row gutter={[24, 24]}>
        {/* Stats Summary Cards */}
        <Col span={24}>
           <Row gutter={[24, 24]}>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Active Models</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{providers.filter(p => p.status === 'active').length}</div>
                       </div>
                       <AppstoreOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-purple)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Avg Latency</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{healthStats?.avgLatency || 0}ms</div>
                       </div>
                       <ThunderboltOutlined style={{ color: 'var(--neon-purple)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>System Sync</Text>
                          <div style={{ color: 'var(--success)', fontSize: 24, fontWeight: 800 }}>STABLE</div>
                       </div>
                       <SafetyCertificateOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
              <Col xs={12} lg={6}>
                 <div className="glass-card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                       <div>
                          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Endpoint Shards</Text>
                          <div style={{ color: '#fff', fontSize: 24, fontWeight: 800 }}>{providers.length}</div>
                       </div>
                       <DatabaseOutlined style={{ color: 'var(--text-dim)', fontSize: 24 }} />
                    </div>
                 </div>
              </Col>
           </Row>
        </Col>

        {/* Search & Table */}
        <Col span={24}>
           <div className="glass-card" style={{ minHeight: 600 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
                 <div style={{ display: 'flex', gap: 16, flex: 1, maxWidth: 600 }}>
                    <Input
                       prefix={<SearchOutlined style={{ color: 'rgba(255,255,255,0.3)' }} />}
                       placeholder="Filter by Model, Provider or Deployment Source..."
                       className="cyber-input"
                       value={searchTerm}
                       onChange={e => setSearchTerm(e.target.value)}
                       style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 8, color: '#fff' }}
                    />
                    <Select
                       value={sortBy}
                       onChange={setSortBy}
                       style={{ width: 200 }}
                       className="cyber-select"
                       dropdownStyle={{ background: '#080810', border: '1px solid var(--neon-blue)' }}
                    >
                       <Select.Option value="activationStatus">Status Rank</Select.Option>
                       <Select.Option value="modelName">Model Name</Select.Option>
                    </Select>
                 </div>
              </div>

              <div style={{ marginTop: 12 }}>
                 {loading && providers.length === 0 ? (
                    <div style={{ padding: 100, textAlign: 'center' }}>
                       <Spin size="large" tip="SYNCING NEURAL ENDPOINTS..." />
                    </div>
                 ) : (
                    <ProvidersTable
                       providers={{ all: providers }}
                       sortedGroupKeys={['all']}
                       getGroupLabel={() => 'NEURAL MATRIX'}
                       loading={loading}
                       onEdit={handleEdit}
                       onDelete={handleDelete}
                    />
                 )}
              </div>
           </div>
        </Col>
      </Row>

      <ProviderModal
        visible={modalVisible}
        editingProvider={editingProvider}
        onCancel={() => setModalVisible(false)}
        onSubmit={async (v) => {
          try {
            const isEdit = !!editingProvider;
            const res = await authUtils.fetchWithAuth(
              isEdit ? `/api/admin/providers/${editingProvider.id}` : '/api/admin/providers',
              {
                method: isEdit ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(v),
              }
            );
            if (res.ok) {
              message.success(isEdit ? 'Provider updated successfully' : 'Provider connected successfully');
              setModalVisible(false);
              fetchProviders();
            } else {
              message.error('Operation failed');
            }
          } catch (err) {
            message.error('Failed to save provider');
          }
        }}
      />

      <style>{`
        .cyber-input input::placeholder { color: rgba(255,255,255,0.2) !important; }
        .cyber-select .ant-select-selector {
           background: rgba(255,255,255,0.03) !important;
           border: 1px solid rgba(255,255,255,0.05) !important;
           border-radius: 8px !important;
           color: #fff !important;
        }
      `}</style>
    </motion.div>
  );
};

export default AdminProviders;
