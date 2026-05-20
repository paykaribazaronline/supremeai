import React, { useState, useEffect } from 'react';
import { Card, message, Spin, Alert, Button, Space, Typography, Tag, Statistic, Row, Col, Input, Select } from 'antd';
import { ReloadOutlined, PlusOutlined, WarningOutlined, CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
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

  type SortKey = 'activationStatus' | 'modelName';

  const DEPLOYMENT_GROUPS: Record<string, { label: string; icon: string; order: number }> = {
    api:       { label: 'API Key',       icon: '🔑', order: 1 },
    gcloud:    { label: 'GCloud Deploy', icon: '☁️', order: 2 },
    local:     { label: 'Local / Ollama', icon: '🖥️', order: 3 },
    ollama:    { label: 'Local / Ollama', icon: '🖥️', order: 3 },
  };

  const getDeploymentGroup = (provider: Provider): string => {
    const source = (provider.deploymentSource || 'api').toLowerCase();
    if (DEPLOYMENT_GROUPS[source]) return source;
    return 'other';
  };

  const groupedProviders = React.useMemo(() => {
    let filtered = [...providers];

    if (searchTerm) {
      const lower = searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        (p.name?.toLowerCase() || '').includes(lower) || 
        (p.type?.toLowerCase() || '').includes(lower) ||
        p.models?.some(m => m.toLowerCase().includes(lower)) ||
        (p.hints?.toLowerCase() || '').includes(lower) ||
        (p.deploymentSource?.toLowerCase() || '').includes(lower)
      );
    }

    const groups: Record<string, Provider[]> = {};
    filtered.forEach(p => {
      const groupKey = getDeploymentGroup(p);
      if (!groups[groupKey]) groups[groupKey] = [];
      groups[groupKey].push(p);
    });

    const statusOrder: Record<string, number> = { active: 0, rotating: 1, inactive: 2, error: 3, dead: 4 };

    const sortProviders = (a: Provider, b: Provider): number => {
      if (sortBy === 'modelName') {
        const aModel = (a.models?.[0] || a.name || '').toLowerCase();
        const bModel = (b.models?.[0] || b.name || '').toLowerCase();
        if (aModel !== bModel) return aModel.localeCompare(bModel);
        // Secondary: activation status
        const aOrder = statusOrder[a.status] ?? 99;
        const bOrder = statusOrder[b.status] ?? 99;
        if (aOrder !== bOrder) return aOrder - bOrder;
        return (a.name || '').localeCompare(b.name || '');
      }

      // Default: activation status first, then model name, then name
      const aOrder = statusOrder[a.status] ?? 99;
      const bOrder = statusOrder[b.status] ?? 99;
      if (aOrder !== bOrder) return aOrder - bOrder;

      const aModel = (a.models?.[0] || '').toLowerCase();
      const bModel = (b.models?.[0] || '').toLowerCase();
      if (aModel !== bModel) return aModel.localeCompare(bModel);

      return (a.name || '').localeCompare(b.name || '');
    };

    Object.keys(groups).forEach(key => {
      groups[key].sort(sortProviders);
    });

    return groups;
  }, [providers, searchTerm, sortBy]);

  const sortedGroupKeys = React.useMemo(() => {
    const apiGroups = Object.keys(groupedProviders)
      .filter(k => DEPLOYMENT_GROUPS[k])
      .sort((a, b) => (DEPLOYMENT_GROUPS[a]?.order ?? 99) - (DEPLOYMENT_GROUPS[b]?.order ?? 99));
    if (groupedProviders['other']) apiGroups.push('other');
    return apiGroups;
  }, [groupedProviders]);

  const getGroupLabel = (key: string): string => {
    if (DEPLOYMENT_GROUPS[key]) return `${DEPLOYMENT_GROUPS[key].icon} ${DEPLOYMENT_GROUPS[key].label}`;
    return '⚙️ Other';
  };

  const fetchProviders = async () => {
    setLoading(true);
    setError(null);
    try {
      const [provRes, statsRes] = await Promise.all([
        authUtils.fetchWithAuth('/getConfiguredProviders'),
        authUtils.fetchWithAuth('/getProviderHealthStats')
      ]);

      if (!provRes.ok) throw new Error('Failed to fetch providers');
      const result = await provRes.json();
      const rawData = result.data?.providers || (Array.isArray(result.data) ? result.data : []);

      setProviders(rawData);

      if (statsRes.ok) {
        const stats = await statsRes.json();
        setHealthStats(stats.data);
      }
    } catch (err: any) {
      console.warn('Provider fetch (emulator mode):', err.message);
      if (err.message?.includes('401') || err.status === 401) {
        window.location.href = '/login';
        return;
      }
      // Emulator / missing backend: show empty state instead of error
      setProviders([]);
      setHealthStats(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  const handleSubmit = async (values: any) => {
    try {
      let response;
      if (editingProvider && editingProvider.id) {
        response = await authUtils.fetchWithAuth(`/api/admin/providers/${editingProvider.id}`, {
          method: 'PUT',
          body: JSON.stringify(values),
        });
      } else {
        response = await authUtils.fetchWithAuth('/api/admin/providers/add', {
          method: 'POST',
          body: JSON.stringify(values),
        });
      }
      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.error || err.message || 'Failed to save provider');
      }
      message.success('প্রোভাইডার কনফিগারেশন সেভ হয়েছে');
      setModalVisible(false);
      fetchProviders();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const handleWakeUpTest = async () => {
    setWakeUpLoading(true);
    setError(null);
    try {
      const res = await authUtils.fetchWithAuth('/api/self-healing/health-check/now', { method: 'POST' });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.error || 'Health check failed');
      }
      const data = await res.json();
      const summary = data?.summary;
      message.success(`Health check complete: ${summary?.active ?? '?'} active, ${summary?.inactive ?? '?'} inactive`);
      // Refetch providers to reflect updated statuses
      await fetchProviders();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Health check failed';
      message.error(msg);
      setError(msg);
    } finally {
      setWakeUpLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/providers/${id}`, {
        method: 'DELETE',
      });
      if (!response.ok) throw new Error('Failed to delete provider');
      message.success('Provider deleted');
      fetchProviders();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  return (
    <AdminLayout title="AI Orchestration Center">
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} md={18}>
          <div className="glass-card p-6 rounded-2xl" style={{ height: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
              <div>
                <Title level={3} style={{ margin: 0 }}>System AI Providers</Title>
                <Text type="secondary">Manage your LLM ecosystem and API endpoints</Text>
              </div>
              <Space>
                <Button 
                  icon={<ThunderboltOutlined />} 
                  onClick={handleWakeUpTest}
                  loading={wakeUpLoading}
                >
                  Wake Up Test
                </Button>
                <Button 
                  icon={<ReloadOutlined />} 
                  onClick={fetchProviders} 
                  loading={loading}
                >
                  Refresh
                </Button>
                <Button 
                  type="primary" 
                  icon={<PlusOutlined />} 
                  onClick={() => {
                    setEditingProvider(null);
                    setModalVisible(true);
                  }}
                  size="large"
                >
                  Add New Key / Model
                </Button>
              </Space>
            </div>

            <Card className="glass-card" style={{ background: 'rgba(0,0,0,0.3)', marginBottom: '24px' }}>
              <Space style={{ width: '100%' }} wrap>
                <Input
                  placeholder="Search by Model Name, Provider or API Hints..."
                  prefix={<ReloadOutlined rotate={90} />}
                  size="large"
                  allowClear
                  onChange={e => setSearchTerm(e.target.value)}
                  value={searchTerm}
                  style={{ borderRadius: '8px', minWidth: 320 }}
                />
                <Select
                  value={sortBy}
                  onChange={setSortBy}
                  style={{ width: 220 }}
                  placeholder="Sort by"
                >
                  <Select.Option value="activationStatus">🔄 Activation Status</Select.Option>
                  <Select.Option value="modelName">📋 Model Name</Select.Option>
                </Select>
              </Space>
            </Card>

            {loading && providers.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '50px' }}><Spin size="large" tip="Orchestrating Providers..." /></div>
            ) : error ? (
              <Alert type="error" message={error} action={<Button onClick={fetchProviders}>Retry</Button>} />
            ) : providers.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '60px 20px', color: '#888' }}>
                <p style={{ fontSize: '18px', marginBottom: '8px' }}>🔑 No API key added</p>
                <p style={{ fontSize: '14px' }}>উপরের "Add New Key / Model" বাটনে ক্লিক করে প্রথম AI প্রোভাইডার যোগ করুন</p>
              </div>
            ) : (
              <ProvidersTable
                providers={groupedProviders}
                sortedGroupKeys={sortedGroupKeys}
                getGroupLabel={getGroupLabel}
                loading={loading}
                onEdit={(record) => {
                  setEditingProvider(record);
                  setModalVisible(true);
                }}
                onDelete={handleDelete}
              />
            )}
          </div>
        </Col>

        <Col xs={24} md={6}>
          <div className="glass-card p-6 rounded-2xl" style={{ height: '100%', background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)', color: 'white' }}>
            <Title level={4} style={{ color: 'white', marginBottom: '20px' }}>Provider Health</Title>
            <Space direction="vertical" style={{ width: '100%' }} size="large">
              <Card size="small" className="glass-card" style={{ borderRadius: '12px' }}>
                <Statistic 
                  title="Total Active Providers" 
                  value={providers.filter(p => p.status === 'active').length} 
                  prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />} 
                />
              </Card>
              <Card size="small" className="glass-card" style={{ borderRadius: '12px' }}>
                <Statistic 
                  title="Deployment Groups" 
                  value={sortedGroupKeys.length} 
                  prefix={<CheckCircleOutlined style={{ color: '#1890ff' }} />} 
                />
              </Card>
              <Card size="small" className="glass-card" style={{ borderRadius: '12px' }}>
                <Statistic 
                  title="Error Streak / Latency" 
                  value={healthStats?.avgLatency || 0} 
                  suffix="ms"
                  prefix={healthStats?.dead ? <WarningOutlined style={{ color: '#ff4d4f' }} /> : <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                  valueStyle={{ color: healthStats?.dead ? '#cf1322' : '#3f8600' }}
                />
                {healthStats?.dead ? <Tag color="error">System is experiencing high failure rates</Tag> : <Tag color="success">System Stability: Optimal</Tag>}
              </Card>
              
              <div style={{ marginTop: '20px', padding: '12px', background: 'rgba(255,255,255,0.1)', borderRadius: '8px' }}>
                <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: '12px' }}>
                  Pro-Tip: Use the "Test Key" feature in the modal to ensure 100% uptime before saving new endpoints.
                </Text>
              </div>
            </Space>
          </div>
        </Col>
      </Row>

      <ProviderModal
        visible={modalVisible}
        editingProvider={editingProvider}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleSubmit}
      />
    </AdminLayout>
  );
};

export default AdminProviders;
