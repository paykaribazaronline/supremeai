// AdminSettings.tsx - System Configuration Page
// Migrated from admin-settings.html to React SPA

import React, { useState, useEffect } from 'react';
import { Tabs, Button, message, Spin, Alert } from 'antd';
import { SettingOutlined, ApiOutlined, BellOutlined, FileTextOutlined, UserOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';
import UserSettings from '../components/UserSettings';
import { SystemConfig } from '../components/settings/types';
import GeneralSettingsCard from '../components/settings/GeneralSettingsCard';
import QuotaSettingsCard from '../components/settings/QuotaSettingsCard';
import NotificationSettingsCard from '../components/settings/NotificationSettingsCard';
import EngineSettingsCard from '../components/settings/EngineSettingsCard';
import { Form } from 'antd';

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
    setError(null);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/config');
      if (!response.ok) throw new Error('Failed to load system configuration');
      const data: SystemConfig = await response.json();
      setConfig(data);
      form.setFieldsValue(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch settings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  const handleSaveGeneral = async (values: any) => {
    const hide = message.loading('Synchronizing configuration with Firestore...', 0);
    setSaving(true);
    try {
      const payload = { ...config, ...values };
      const response = await authUtils.fetchWithAuth('/api/admin/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) throw new Error('Failed to synchronize general settings');
      
      message.success('Core configuration synchronized successfully');
      // Small delay to allow Firestore propagation/listeners to catch up
      setTimeout(() => fetchConfig(), 500);
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Update failed');
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
      if (!response.ok) throw new Error(`Failed to update mapping for ${key}`);
      message.success(`Parameter "${key}" updated`);
      fetchConfig();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Update failed');
    }
  };

  const tabItems = [
    {
      key: 'general',
      label: <span><SettingOutlined /> General</span>,
      children: <GeneralSettingsCard form={form} onFinish={handleSaveGeneral} saving={saving} />,
    },
    {
      key: 'quotas',
      label: <span><ApiOutlined /> Quotas & Limits</span>,
      children: <QuotaSettingsCard config={config} onUpdateValue={updateMapValue} />,
    },
     {
       key: 'notifications',
       label: <span><BellOutlined /> Notifications</span>,
       children: <NotificationSettingsCard form={form} onFinish={handleSaveGeneral} saving={saving} />,
     },
    {
      key: 'engine',
      label: <span><FileTextOutlined /> Engine Core</span>,
      children: <EngineSettingsCard config={config} onUpdateValue={updateMapValue} />,
    },
    {
      key: 'personal',
      label: <span><UserOutlined /> Personal</span>,
      children: (
        <div style={{ marginTop: 16 }}>
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
    <AdminLayout 
      title="System Orchestration Settings"
      extra={[
        <Button 
          key="refresh" 
          icon={<SettingOutlined spin={loading} />} 
          onClick={fetchConfig}
          disabled={loading}
        >
          Refresh Sync
        </Button>
      ]}
    >
      <div className="admin-content-fade-in">
        {error && (
          <Alert 
            type="error" 
            message="Configuration Error" 
            description={error} 
            showIcon 
            action={<Button size="small" onClick={fetchConfig}>Retry Sync</Button>}
            style={{ marginBottom: 20, borderRadius: '8px' }}
          />
        )}
        
        {loading && !Object.keys(config).length ? (
          <div style={{ textAlign: 'center', padding: '100px' }}>
            <Spin size="large" tip="Synchronizing system configuration..." />
          </div>
        ) : (
          <Tabs 
            defaultActiveKey="general" 
            items={tabItems} 
            className="custom-tabs" 
            style={{ minHeight: '600px' }}
          />
        )}
      </div>
    </AdminLayout>
  );
};

export default AdminSettings;
