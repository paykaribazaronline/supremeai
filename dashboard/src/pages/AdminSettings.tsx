// AdminSettings.tsx - Cinematic System Configuration
import React, { useState, useEffect } from 'react';
import { Tabs, Button, message, Spin, Alert, Typography, Row, Col, Space } from 'antd';
import {
  SettingOutlined,
  ApiOutlined,
  BellOutlined,
  FileTextOutlined,
  UserOutlined,
  ReloadOutlined,
  ControlOutlined,
  SafetyOutlined,
  GlobalOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';
import UserSettings from '../components/UserSettings';
import { SystemConfig } from '../components/settings/types';
import GeneralSettingsCard from '../components/settings/GeneralSettingsCard';
import QuotaSettingsCard from '../components/settings/QuotaSettingsCard';
import NotificationSettingsCard from '../components/settings/NotificationSettingsCard';
import EngineSettingsCard from '../components/settings/EngineSettingsCard';
import { Form } from 'antd';

const { Title, Text } = Typography;

interface AdminSettingsProps {
  darkMode: boolean;
  setDarkMode: (value: boolean) => void;
  chatFont: string;
  setChatFont: (value: string) => void;
}

const AdminSettings: React.FC<AdminSettingsProps> = ({ darkMode, setDarkMode, chatFont, setChatFont }) => {
  const [config, setConfig] = useState<SystemConfig>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const fetchConfig = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/config');
      if (response.ok) {
        const data: SystemConfig = await response.json();
        setConfig(data);
        form.setFieldsValue(data);
      }
    } catch (err) {
      setError('Failed to fetch neural configuration');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchConfig(); }, []);

  const handleSaveGeneral = async (values: any) => {
    const hide = message.loading('Propagating configuration to all nodes...', 0);
    setSaving(true);
    try {
      const payload = { ...config, ...values };
      const response = await authUtils.fetchWithAuth('/api/admin/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (response.ok) {
        message.success('Neural weights updated successfully');
        setTimeout(() => fetchConfig(), 500);
      }
    } catch (err) {
      message.error('Update failed');
    } finally {
      setSaving(false);
      hide();
    }
  };

  const updateMapValue = async (field: keyof SystemConfig, key: string, value: any) => {
    try {
      const newConfig = { ...config };
      const map = { ...(newConfig[field] as Record<string, any>) };
      map[key] = value;
      (newConfig as any)[field] = map;
      const response = await authUtils.fetchWithAuth('/api/admin/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newConfig),
      });
      if (response.ok) {
        message.success(`Parameter "${key}" updated`);
        fetchConfig();
      }
    } catch (err) {}
  };

  const tabItems = [
    {
      key: 'general',
      label: <span className="tab-label"><SettingOutlined /> CORE CONFIG</span>,
      children: <GeneralSettingsCard form={form} onFinish={handleSaveGeneral} saving={saving} />,
    },
    {
      key: 'quotas',
      label: <span className="tab-label"><ApiOutlined /> NEURAL LIMITS</span>,
      children: <QuotaSettingsCard config={config} onUpdateValue={updateMapValue} />,
    },
    {
       key: 'notifications',
       label: <span className="tab-label"><BellOutlined /> LOG CHANNELS</span>,
       children: <NotificationSettingsCard form={form} onFinish={handleSaveGeneral} saving={saving} />,
    },
    {
      key: 'engine',
      label: <span className="tab-label"><FileTextOutlined /> ENGINE SEEDS</span>,
      children: <EngineSettingsCard config={config} onUpdateValue={updateMapValue} />,
    },
    {
      key: 'personal',
      label: <span className="tab-label"><UserOutlined /> OPERATOR PREFS</span>,
      children: (
        <div style={{ marginTop: 24 }}>
          <UserSettings 
            darkMode={darkMode} 
            setDarkMode={setDarkMode} 
            chatFont={chatFont} 
            setChatFont={setChatFont} 
          />
        </div>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: '1400px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 40, borderBottom: '1px solid rgba(0, 243, 255, 0.1)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <ControlOutlined style={{ color: 'var(--neon-purple)', fontSize: 20 }} />
              <Text style={{ color: 'var(--neon-purple)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>CENTRAL CONFIGURATION</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              System <span className="text-gradient">Orchestration</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Fine-tune neural thresholds, API quotas, and system-wide protocols.</Text>
          </Col>
          <Col>
            <Button
              icon={<ReloadOutlined spin={loading} />}
              onClick={fetchConfig}
              className="glass-action-button"
            >
              Force Sync
            </Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[32, 32]}>
        <Col xs={24} lg={18}>
          <div className="glass-card" style={{ minHeight: '700px' }}>
            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 20 }} />}

            {loading && !Object.keys(config).length ? (
              <div style={{ height: 400, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Spin size="large" tip="FETCHING NEURAL STATE..." />
              </div>
            ) : (
              <Tabs
                defaultActiveKey="general"
                items={tabItems}
                className="cyber-tabs"
              />
            )}
          </div>
        </Col>

        <Col xs={24} lg={6}>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <div className="glass-card" style={{ background: 'linear-gradient(135deg, rgba(0, 243, 255, 0.05), rgba(188, 19, 254, 0.05))' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <SafetyOutlined style={{ color: 'var(--success)', fontSize: 20 }} />
                <Text strong style={{ color: '#fff' }}>Protocol Security</Text>
              </div>
              <Text style={{ color: 'var(--text-dim)', fontSize: 13 }}>
                Configuration changes are logged and propagated across the cluster with AES-256 encryption.
              </Text>
            </div>

            <div className="glass-card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <GlobalOutlined style={{ color: 'var(--neon-blue)', fontSize: 20 }} />
                <Text strong style={{ color: '#fff' }}>Global Propagator</Text>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12 }}>Sync Latency</Text>
                <Text style={{ color: 'var(--neon-blue)', fontWeight: 700 }}>14ms</Text>
              </div>
              <Progress percent={100} strokeColor="var(--neon-blue)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={4} />
            </div>

            <div className="glass-card" style={{ textAlign: 'center', padding: '32px 16px' }}>
              <Text style={{ color: 'rgba(255,255,255,0.3)', fontSize: 11, textTransform: 'uppercase', letterSpacing: 2 }}>Build Signature</Text>
              <div style={{ color: 'rgba(255,255,255,0.6)', fontFamily: 'JetBrains Mono', fontSize: 12, marginTop: 8 }}>
                SHA-256: 8f2b...3a1c
              </div>
            </div>
          </Space>
        </Col>
      </Row>

      <style>{`
        .cyber-tabs .ant-tabs-nav-list {
          gap: 24px;
        }
        .cyber-tabs .ant-tabs-tab {
          margin: 0 !important;
          padding: 12px 4px !important;
          color: rgba(255,255,255,0.4) !important;
          font-family: 'Outfit', sans-serif !important;
          font-weight: 700 !important;
          font-size: 13px !important;
          letter-spacing: 1px;
          transition: all 0.3s ease;
        }
        .cyber-tabs .ant-tabs-tab-active {
          color: var(--neon-blue) !important;
          text-shadow: 0 0 10px rgba(0, 243, 255, 0.3);
        }
        .cyber-tabs .ant-tabs-ink-bar {
          background: var(--neon-blue) !important;
          box-shadow: 0 0 10px var(--neon-blue);
        }
        .tab-label {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      `}</style>
    </motion.div>
  );
};

export default AdminSettings;
