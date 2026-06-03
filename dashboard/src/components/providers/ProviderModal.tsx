import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space, message, Divider, Tag, Spin } from 'antd';
import { ThunderboltOutlined, SearchOutlined } from '@ant-design/icons';
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

  const handleSuggestRoles = async () => {
    if (!editingProvider?.id) return;
    try {
      const response = await authUtils.fetchWithAuth(`/api/admin/providers/${editingProvider.id}/roles`);
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
      className="provider-modal admin-modal"
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

        <Form.Item name="baseUrl" label="API Base URL (Optional)" initialValue="https://api.openai.com/v1">
          <Input placeholder="https://api.openai.com/v1 (required only for external API providers)" />
        </Form.Item>

         <Divider style={{ margin: '12px 0' }} />

         <Form.Item name="deploymentSource" label="Deployment Group" initialValue="local" help="Determines which group this model appears in on the admin dashboard">
          <Select>
            <Option value="api">🔑 API Key — Regular API key providers (OpenAI, Anthropic, etc.)</Option>
            <Option value="gcloud">☁️ GCloud Deploy — Firebase / Google Gemini / Vertex AI models</Option>
            <Option value="local">🖥️ Local / Ollama — Self-hosted Ollama or local models (Recommended)</Option>
          </Select>
        </Form.Item>

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
            <Button type="primary" htmlType="submit" size="large">
              {editingProvider ? 'Update Configuration' : 'Save Provider'}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ProviderModal;