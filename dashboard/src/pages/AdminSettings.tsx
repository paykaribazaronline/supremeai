// AdminSettings.tsx - System Configuration Page
// Migrated from admin-settings.html to React SPA

import React, { useState, useEffect } from 'react';
import { Layout, Card, Tabs, Form, Input, Select, Switch, Button, message, Spin, Alert, Table, Space, Tag } from 'antd';
import { SettingOutlined, ApiOutlined, BellOutlined, FileTextOutlined } from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { TextArea } = Input;
const { Option } = Select;

interface SystemConfig {
  id?: string;
  activeModel?: string;
  smallModel?: string;
  version?: number;
  maintenanceMode?: boolean;
  fullAuthority?: boolean;
  shareMode?: string;
  enableExternalDirectory?: boolean;
  emailNotifications?: boolean;
  smsAlerts?: boolean;
  systemMessage?: string;
  tierQuotas?: Record<string, number>;
  tierMaxApis?: Record<string, number>;
  tierMaxSimulatorInstalls?: Record<string, number>;
  timeouts?: Record<string, number>;
  thresholds?: Record<string, number>;
  settings?: Record<string, any>;
  collections?: Record<string, string>;
}

const AdminSettings: React.FC = () => {
  const [config, setConfig] = useState<SystemConfig>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const fetchConfig = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = authUtils.getToken();
      const response = await fetch('/api/admin/config', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to load configuration');
      const data: SystemConfig = await response.json();
      setConfig(data);
      form.setFieldsValue(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  const handleSaveGeneral = async (values: any) => {
    setSaving(true);
    try {
      const token = authUtils.getToken();
      const payload = {
        ...config,
        ...values,
      };
      const response = await fetch('/api/admin/config', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });
      if (!response.ok) throw new Error('Failed to save settings');
      message.success('General settings saved successfully');
      fetchConfig();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  const updateMapValue = async (field: keyof SystemConfig, key: string, value: any) => {
    try {
      const token = authUtils.getToken();
      const newConfig = { ...config };
      const map = { ...(newConfig[field] as Record<string, any>) };
      map[key] = value;
      (newConfig as any)[field] = map;
      
      const response = await fetch('/api/admin/config', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(newConfig),
      });
      if (!response.ok) throw new Error('Failed to update value');
      message.success(`Updated ${key} successfully`);
      fetchConfig();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to update value');
    }
  };

  const renderMapEditor = (field: keyof SystemConfig, title: string) => {
    const data = Object.entries(config[field] || {}).map(([key, value]) => ({
      key,
      value,
    }));

    const columns = [
      { title: 'Key', dataIndex: 'key', key: 'key' },
      { 
        title: 'Value', 
        dataIndex: 'value', 
        key: 'value',
        render: (text: any, record: any) => (
          <Input 
            defaultValue={text}
            onBlur={(e) => updateMapValue(field, record.key, e.target.value)}
            onPressEnter={(e) => updateMapValue(field, record.key, (e.target as any).value)}
          />
        )
      }
    ];

    return (
      <Card title={title} size="small" style={{ marginBottom: 16 }}>
        <Table dataSource={data} columns={columns} pagination={false} size="small" rowKey="key" />
      </Card>
    );
  };

  const quotaColumns = [
    {
      title: 'Tier',
      dataIndex: 'tier',
      key: 'tier',
      render: (tier: string) => <Tag color="blue">{tier}</Tag>,
    },
    {
      title: 'Monthly Quota',
      dataIndex: 'quota',
      key: 'quota',
      render: (quota: number, record: any) => (
        <Input
          type="number"
          defaultValue={quota}
          onBlur={(e) => updateMapValue('tierQuotas', record.tier, Number(e.target.value))}
          style={{ width: 120 }}
        />
      ),
    },
    {
      title: 'Max API Keys',
      dataIndex: 'maxApis',
      key: 'maxApis',
      render: (maxApis: number, record: any) => (
        <Input
          type="number"
          defaultValue={maxApis}
          onBlur={(e) => updateMapValue('tierMaxApis', record.tier, Number(e.target.value))}
          style={{ width: 100 }}
        />
      ),
    },
  ];

  const tiers = ['GUEST', 'FREE', 'BASIC', 'PRO', 'ENTERPRISE', 'ADMIN'];

  const quotaData = tiers.map((tier) => ({
    tier,
    quota: config.tierQuotas?.[tier] ?? 0,
    maxApis: config.tierMaxApis?.[tier] ?? 0,
  }));

  const tabItems = [
    {
      key: 'general',
      label: <><SettingOutlined /> General</>,
      children: (
        <Card style={{ marginTop: 16 }}>
          <Form form={form} layout="vertical" onFinish={handleSaveGeneral}>
            <Form.Item name="activeModel" label="Active AI Model" rules={[{ required: true }]}>
              <Input placeholder="e.g., gpt-4o" />
            </Form.Item>
            <Form.Item name="smallModel" label="Small/Fast Model">
              <Input placeholder="e.g., supremeai/1.5-flash" />
            </Form.Item>
            <Form.Item name="systemMessage" label="System Prompt">
              <TextArea rows={4} placeholder="You are SupremeAI..." />
            </Form.Item>
            <Form.Item name="maintenanceMode" label="Maintenance Mode" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="fullAuthority" label="Full Authority Mode" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="shareMode" label="Share Mode">
              <Select>
                <Option value="manual">Manual</Option>
                <Option value="automatic">Automatic</Option>
              </Select>
            </Form.Item>
            <Form.Item name="enableExternalDirectory" label="Enable External Directory" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>
                Save General Settings
              </Button>
            </Form.Item>
          </Form>
        </Card>
      ),
    },
    {
      key: 'quotas',
      label: <><ApiOutlined /> Quotas & Limits</>,
      children: (
        <Card style={{ marginTop: 16 }} title="Tier Quotas and Limits">
          <p>Edit quotas per tier. Changes are saved automatically.</p>
          <Table
            columns={quotaColumns}
            dataSource={quotaData}
            rowKey="tier"
            pagination={false}
          />
        </Card>
      ),
    },
    {
      key: 'notifications',
      label: <><BellOutlined /> Notifications</>,
      children: (
        <Card style={{ marginTop: 16 }}>
          <Form layout="vertical">
            <Form.Item name="emailNotifications" label="Email Notifications" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="smsAlerts" label="SMS Alerts" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Button
              type="primary"
              onClick={() => {
                message.info('Notification preferences saved (mock)');
              }}
            >
              Save Notification Settings
            </Button>
          </Form>
        </Card>
      ),
    },
    {
      key: 'engine',
      label: <><FileTextOutlined /> Engine Settings</>,
      children: (
        <div style={{ marginTop: 16 }}>
          {renderMapEditor('timeouts', 'System Timeouts (ms)')}
          {renderMapEditor('thresholds', 'Logic Thresholds (0.0 - 1.0)')}
          {renderMapEditor('settings', 'Generic System Settings')}
          {renderMapEditor('collections', 'Database Collections')}
        </div>
      ),
    },
  ];

  return (
    <AdminLayout title="System Settings">
      {loading && <Spin style={{ display: 'block', margin: '20px auto' }} />}
      {error && <Alert type="error" message={error} action={<Button onClick={fetchConfig}>Retry</Button>} />}
      {!loading && !error && (
        <Tabs defaultActiveKey="general" items={tabItems} />
      )}
    </AdminLayout>
  );
};

export default AdminSettings;
