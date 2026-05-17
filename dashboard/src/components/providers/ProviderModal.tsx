import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space, message, Divider, Tag, Spin } from 'antd';
import { ThunderboltOutlined, SearchOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { Provider } from './types';
import { authUtils } from '../../lib/authUtils';

const { Option } = Select;

interface Props {
  visible: boolean;
  editingProvider: Provider | null;
  onCancel: () => void;
  onSubmit: (values: any) => void;
}

const ProviderModal: React.FC<Props> = ({ visible, editingProvider, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ status: 'idle' | 'success' | 'error'; message?: string }>({ status: 'idle' });
  const [discoveredModels, setDiscoveredModels] = useState<any[]>([]);
  const [loadingModels, setLoadingModels] = useState(false);

  useEffect(() => {
    if (visible) {
      if (editingProvider) {
        form.setFieldsValue({
          ...editingProvider,
          models: editingProvider.models
        });
      } else {
        form.resetFields();
        setTestResult({ status: 'idle' });
      }
      fetchDiscoveredModels();
    }
  }, [visible, editingProvider, form]);

  const fetchDiscoveredModels = async (query: string = '') => {
    setLoadingModels(true);
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/providers/discover?query=${query}`);
      if (response.ok) {
        const result = await response.json();
        setDiscoveredModels(result.data || []);
      }
    } catch (err) {
      console.error('Failed to discover models', err);
    } finally {
      setLoadingModels(false);
    }
  };

  const handleTestKey = async () => {
    const values = await form.validateFields(['apiKey', 'type']);
    if (!values.apiKey) {
      message.warning('দয়া করে আগে এপিআই কী দিন');
      return;
    }

    setTesting(true);
    setTestResult({ status: 'idle' });
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/providers/test-key', {
        method: 'POST',
        body: JSON.stringify({
          name: values.type, // We use type as provider name for testing
          apiKey: values.apiKey
        }),
      });

      if (response.ok) {
        const result = await response.json();
        setTestResult({ status: 'success', message: result.data.message });
        message.success('এপিআই কী ভ্যালিড!');
      } else {
        const err = await response.json();
        setTestResult({ status: 'error', message: err.message || 'ভ্যালিডেশন ফেইল করেছে' });
        message.error('ভ্যালিডেশন ফেইল করেছে');
      }
    } catch (err) {
      setTestResult({ status: 'error', message: 'সার্ভার কানেকশন এরর' });
      message.error('ভ্যালিডেশন ফেইল করেছে');
    } finally {
      setTesting(false);
    }
  };

  const handleSuggestRoles = async () => {
    if (!editingProvider?.id) return;
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/providers/${editingProvider.id}/suggest-roles`);
      if (response.ok) {
        const result = await response.json();
        const suggestions = result.data || [];
        form.setFieldsValue({ assignedRoles: suggestions });
        message.success(`সিস্টেম ${suggestions.join(', ')} রোলগুলো সাজেস্ট করেছে`);
      }
    } catch (err) {
      message.error('সাজেশন লোড করা যায়নি');
    }
  };

  return (
    <Modal
      title={editingProvider ? 'Edit Provider' : 'Add New Provider'}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={650}
      className="provider-modal"
    >
      <Form form={form} layout="vertical" onFinish={onSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <Form.Item name="name" label="Label Name (For UI)" rules={[{ required: true }]}>
            <Input placeholder="e.g., My Primary GPT-4" />
          </Form.Item>
          
          <Form.Item name="type" label="Provider Architecture" rules={[{ required: true }]}>
            <Select placeholder="Select architecture">
              <Option value="openai">OpenAI Architecture</Option>
              <Option value="anthropic">Anthropic Claude</Option>
              <Option value="google">Google Gemini</Option>
              <Option value="custom">Custom OpenAI-Compatible</Option>
            </Select>
          </Form.Item>
        </div>

        <Form.Item label="Search & Choose AI Model" required>
          <Form.Item name="models" noStyle rules={[{ required: true, message: 'কমপক্ষে একটি মডেল সিলেক্ট করুন' }]}>
            <Select 
              mode="multiple" 
              placeholder="Search model (e.g. gpt-4, claude-3, gemini-pro)"
              showSearch
              filterOption={false}
              onSearch={fetchDiscoveredModels}
              loading={loadingModels}
              style={{ width: '100%' }}
              dropdownRender={(menu) => (
                loadingModels ? <div style={{ padding: 12, textAlign: 'center' }}><Spin size="small" /></div> : menu
              )}
            >
              {discoveredModels.map(m => (
                <Option key={m.id} value={m.id}>
                  <Space>
                    <span>{m.name || m.id}</span>
                    <Tag color="blue" style={{ fontSize: '10px' }}>{m.provider}</Tag>
                  </Space>
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form.Item>

        <Form.Item name="baseUrl" label="API Base URL" initialValue="https://api.openai.com/v1">
          <Input placeholder="https://api.openai.com/v1" />
        </Form.Item>

        <Divider style={{ margin: '12px 0' }} />

        <div className="api-key-section" style={{ background: '#f8f9fa', padding: '16px', borderRadius: '12px', border: '1px solid #e9ecef' }}>
          <Form.Item 
            name="apiKey" 
            label="API Secret Key" 
            rules={[{ required: true }]}
            help={testResult.status === 'error' ? <span style={{ color: 'red' }}><ExclamationCircleOutlined /> {testResult.message}</span> : null}
            validateStatus={testResult.status === 'error' ? 'error' : testResult.status === 'success' ? 'success' : undefined}
          >
            <Input.Password 
              placeholder="sk-..." 
              addonAfter={
                <Button 
                  type="link" 
                  size="small" 
                  loading={testing} 
                  onClick={handleTestKey}
                  icon={testResult.status === 'success' ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : <ThunderboltOutlined />}
                >
                  Test Key
                </Button>
              }
            />
          </Form.Item>

          <Form.Item name="hints" label="API Key Hints / Note">
            <Input placeholder="e.g., Personal key from MyAccount1" />
          </Form.Item>
        </div>

        <Form.Item name="status" label="Provider Status" initialValue="active" style={{ marginTop: '16px' }}>
          <Select>
            <Option value="active">Active</Option>
            <Option value="inactive">Inactive</Option>
            <Option value="error">Error</Option>
            <Option value="rotating">Rotating</Option>
            <Option value="dead">Dead</Option>
          </Select>
        </Form.Item>

        <Form.Item label="Assign Work Roles (What this model should do)">
          <Form.Item name="assignedRoles" noStyle>
            <Select mode="multiple" placeholder="Select roles for this key">
              <Option value="coding">Coding & Software Dev</Option>
              <Option value="security">Security Audit & Hacking</Option>
              <Option value="reasoning">Advanced Reasoning</Option>
              <Option value="fast_chat">Fast Chat & UI Help</Option>
              <Option value="multimodal">Vision & Multimodal</Option>
              <Option value="general_chat">General Chat</Option>
            </Select>
          </Form.Item>
          {editingProvider?.id && (
            <Button 
              size="small" 
              icon={<ThunderboltOutlined />} 
              onClick={handleSuggestRoles}
              style={{ marginTop: '8px' }}
              type="dashed"
            >
              Auto-Analyze Best Roles
            </Button>
          )}
        </Form.Item>

        <Form.Item style={{ marginTop: 32, marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Cancel</Button>
            <Button type="primary" htmlType="submit" size="large" disabled={testResult.status !== 'success' && !editingProvider}>
              {editingProvider ? 'Update Configuration' : 'Validate & Save Provider'}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ProviderModal;
