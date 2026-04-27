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
      form.setFieldsValue({
        activeModel: data.activeModel,
        smallModel: data.smallModel,
        maintenanceMode: data.maintenanceMode,
        fullAuthority: data.fullAuthority,
        shareMode: data.shareMode,
        enableExternalDirectory: data.enableExternalDirectory,
        emailNotifications: data.emailNotifications,
        smsAlerts: data.smsAlerts,
        systemMessage: data.systemMessage,
      });
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

  const handleQuotaUpdate = async (tier: string, field: 'tierQuotas' | 'tierMaxApis' | 'tierMaxSimulatorInstalls', value: number) => {
    try {
      const token = authUtils.getToken();
      // PATCH endpoint expects path param for tier and query param for limit
      // Actually endpoint: PATCH /api/admin/config/quotas/{tier}?limit=...
      // That updates only tierQuotas. For other maps, we might need custom endpoint or PUT full config.
      // For simplicity, we'll update via full config PUT for now (backend merges)
      const newConfig = { ...config };
      if (!newConfig[field]) newConfig[field] = {};
      newConfig[field] = { ...(newConfig[field] as Record<string, number>), [tier]: value };
      const response = await fetch('/api/admin/config', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(newConfig),
      });
      if (!response.ok) throw new Error('Failed to update quota');
      message.success(`Quota for ${tier} updated to ${value}`);
      fetchConfig();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Failed to update quota');
    }
  };

  const quotaColumns = [
    {
      title: 'Tier',
      dataIndex: 'tier',
      key: 'tier',
      render: (tier: string) => <Tag color="blue">{tier}</Tag>,
    },
    {
      title: 'Monthly Quota (installs/calls)',
      dataIndex: 'quota',
      key: 'quota',
      render: (quota: number, record: any) => (
        <Input
          type="number"
          defaultValue={quota}
          onPressEnter={(e) => {
            handleQuotaUpdate(record.tier, 'tierQuotas', Number(e.currentTarget.value));
          }}
          onBlur={(e) => {
            handleQuotaUpdate(record.tier, 'tierQuotas', Number(e.currentTarget.value));
          }}
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
          onPressEnter={(e) => {
            handleQuotaUpdate(record.tier, 'tierMaxApis', Number(e.currentTarget.value));
          }}
          onBlur={(e) => {
            handleQuotaUpdate(record.tier, 'tierMaxApis', Number(e.currentTarget.value));
          }}
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
              <Input placeholder="e.g., google/gemini-1.5-flash" />
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
      key: 'advanced',
      label: 'Advanced',
      children: (
        <Card style={{ marginTop: 16 }}>
          <h3>API Keys Management</h3>
          <p>Manage OpenAI, Anthropic, and Google AI API keys.</p>
          <Alert
            message="API keys are currently stored securely in the backend."
            type="info"
            style={{ marginBottom: 16 }}
          />
          <Button type="primary">Manage API Keys (Coming Soon)</Button>

          <h3 style={{ marginTop: 24 }}>Log Retention Policy</h3>
          <p>Configure how long activity logs, error logs, and audit trails are retained.</p>
          <Button>Configure Retention (Coming Soon)</Button>
        </Card>
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
