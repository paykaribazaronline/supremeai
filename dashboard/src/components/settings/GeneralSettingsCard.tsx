import React from 'react';
import { Card, Form, Input, Switch, Select, Button } from 'antd';
import AISuggestionInformer from '../AISuggestionInformer';
import { SystemConfig } from './types';

const { TextArea } = Input;
const { Option } = Select;

interface GeneralSettingsCardProps {
  form: any;
  onFinish: (values: any) => void;
  saving: boolean;
}

const GeneralSettingsCard: React.FC<GeneralSettingsCardProps> = ({ form, onFinish, saving }) => {
  return (
    <Card className="glass-card" style={{ marginTop: 16, borderRadius: '12px' }}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="activeModel" label="Primary AI Orchestrator" rules={[{ required: true }]}>
          <Input 
            placeholder="e.g., gpt-4o" 
            suffix={<AISuggestionInformer 
              context="active_model" 
              onSelect={(val) => form.setFieldValue('activeModel', val)} 
            />}
            style={{ borderRadius: '8px' }}
          />
        </Form.Item>
        <Form.Item name="smallModel" label="Secondary / Fast Model">
          <Input 
            placeholder="e.g., supremeai/1.5-flash" 
            suffix={<AISuggestionInformer 
              context="small_model" 
              onSelect={(val) => form.setFieldValue('smallModel', val)} 
            />}
            style={{ borderRadius: '8px' }}
          />
        </Form.Item>
        <Form.Item name="systemMessage" label="Core System Prompt">
          <div style={{ position: 'relative' }}>
            <TextArea rows={4} placeholder="You are SupremeAI..." style={{ borderRadius: '8px' }} />
            <div style={{ position: 'absolute', bottom: 10, right: 10 }}>
              <AISuggestionInformer 
                context="system_prompt" 
                onSelect={(val) => form.setFieldValue('systemMessage', val)} 
              />
            </div>
          </div>
        </Form.Item>
        
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
          <Form.Item name="maintenanceMode" label="Maintenance Mode" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Switch />
          </Form.Item>
          <Form.Item name="fullAuthority" label="Full Authority Mode" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Switch />
          </Form.Item>
          <Form.Item name="enableExternalDirectory" label="External Directory" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Switch />
          </Form.Item>
          <Form.Item name="autonomousLearningEnabled" label="Autonomous Learning" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Switch />
          </Form.Item>
          <Form.Item name="autonomousAuditEnabled" label="Self-Audit Mode" valuePropName="checked" style={{ marginBottom: 0 }}>
            <Switch />
          </Form.Item>
        </div>

        <Form.Item name="shareMode" label="Project Sharing Mode">
          <Select style={{ borderRadius: '8px' }}>
            <Option value="manual">Manual Approval</Option>
            <Option value="automatic">Automatic Sharing</Option>
          </Select>
        </Form.Item>

        <Form.Item style={{ marginTop: 24, marginBottom: 0 }}>
          <Button type="primary" htmlType="submit" loading={saving} style={{ borderRadius: '8px', paddingLeft: '32px', paddingRight: '32px' }}>
            Save General Configuration
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default GeneralSettingsCard;
