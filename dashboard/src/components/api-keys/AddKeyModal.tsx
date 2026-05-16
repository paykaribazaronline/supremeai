import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space, Typography, Tag, Divider } from 'antd';
import { SafetyOutlined, LinkOutlined, TagOutlined, RobotOutlined } from '@ant-design/icons';
import { ModelSearchResult, SavedAPIKey } from './types';
import { ModelSearchSelect } from './ModelSearchSelect';
import { getProviderEndpoint } from './constants';

const { Text } = Typography;

interface AddKeyModalProps {
    visible: boolean;
    editingKey: SavedAPIKey | null;
    onCancel: () => void;
    onSave: (values: any) => Promise<void>;
    loading?: boolean;
}

const AddKeyModal: React.FC<AddKeyModalProps> = ({
    visible,
    editingKey,
    onCancel,
    onSave,
    loading
}) => {
    const [form] = Form.useForm();

    useEffect(() => {
        if (visible) {
            if (editingKey) {
                form.setFieldsValue(editingKey);
            } else {
                form.resetFields();
            }
        }
    }, [visible, editingKey, form]);

    const handleModelChange = (model: ModelSearchResult | null) => {
        if (model) {
            form.setFieldsValue({
                provider: model.provider,
                baseUrl: model.baseUrl,
                models: [model.id]
            });
        }
    };

    const handleFinish = async (values: any) => {
        await onSave(values);
    };

    return (
        <Modal
            title={
                <Space>
                    <SafetyOutlined style={{ color: '#1890ff' }} />
                    <span>{editingKey ? 'এপিআই কী এডিট করুন' : 'নতুন এপিআই কী যোগ করুন'}</span>
                </Space>
            }
            open={visible}
            onCancel={onCancel}
            footer={null}
            width={700}
            centered
            className="premium-modal"
        >
            <Form
                form={form}
                layout="vertical"
                onFinish={handleFinish}
                initialValues={{ provider: 'openai', models: [] }}
            >
                <div style={{ background: '#f9f9f9', padding: '20px', borderRadius: '12px', border: '1px solid #eee', marginBottom: '20px' }}>
                    <Title level={5} style={{ marginTop: 0 }}>মডেল ডিসকভারি (প্রস্তাবিত)</Title>
                    <Text type="secondary" style={{ display: 'block', marginBottom: '15px' }}>
                        সরাসরি HuggingFace বা OpenRouter থেকে মডেল সার্চ করে কনফিগারেশন অটো-ফিল করুন।
                    </Text>
                    <ModelSearchSelect onChange={handleModelChange} />
                </div>

                <Divider>বা ম্যানুয়ালি কনফিগার করুন</Divider>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <Form.Item
                        name="label"
                        label={<span><TagOutlined /> লেবেল</span>}
                        rules={[{ required: true, message: 'একটি লেবেল দিন (যেমন: My OpenAI Key)' }]}
                    >
                        <Input placeholder="উদাঃ My Production Gemini" size="large" />
                    </Form.Item>

                    <Form.Item
                        name="provider"
                        label={<span><RobotOutlined /> প্রোভাইডার</span>}
                        rules={[{ required: true }]}
                    >
                        <Select 
                            size="large"
                            onChange={(val) => form.setFieldValue('baseUrl', getProviderEndpoint(val))}
                        >
                            <Select.Option value="openai">OpenAI</Select.Option>
                            <Select.Option value="anthropic">Anthropic</Select.Option>
                            <Select.Option value="google">Google Gemini</Select.Option>
                            <Select.Option value="deepseek">DeepSeek</Select.Option>
                            <Select.Option value="groq">Groq</Select.Option>
                            <Select.Option value="mistral">Mistral AI</Select.Option>
                            <Select.Option value="openrouter">OpenRouter</Select.Option>
                            <Select.Option value="huggingface">HuggingFace</Select.Option>
                            <Select.Option value="custom">Custom (OpenAI Compatible)</Select.Option>
                        </Select>
                    </Form.Item>
                </div>

                <Form.Item
                    name="apiKey"
                    label={<span><SafetyOutlined /> এপিআই কী (API Key)</span>}
                    rules={[{ required: true, message: 'আপনার এপিআই কী প্রদান করুন' }]}
                >
                    <Input.Password placeholder="sk-..." size="large" />
                </Form.Item>

                <Form.Item
                    name="baseUrl"
                    label={<span><LinkOutlined /> বেস ইউআরএল (Base URL)</span>}
                    rules={[{ required: true, message: 'প্রোভাইডার এপিআই এন্ডপয়েন্ট প্রয়োজন' }]}
                    extra="OpenAI কম্প্যাটিবল এপিআই-এর জন্য এটি পরিবর্তনযোগ্য।"
                >
                    <Input placeholder="https://api.openai.com/v1" size="large" />
                </Form.Item>

                <Form.Item
                    name="models"
                    label="নির্দিষ্ট মডেলসমূহ (ঐচ্ছিক)"
                    extra="খালি রাখলে এই কী সব মডেলের জন্য প্রযোজ্য হবে।"
                >
                    <Select mode="tags" style={{ width: '100%' }} placeholder="উদাঃ gpt-4, claude-3-opus" size="large" />
                </Form.Item>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                    <Button onClick={onCancel} size="large">বাতিল</Button>
                    <Button type="primary" htmlType="submit" size="large" loading={loading} icon={<SafetyOutlined />}>
                        {editingKey ? 'আপডেট করুন' : 'কী সংরক্ষণ করুন'}
                    </Button>
                </div>
            </Form>
        </Modal>
    );
};

const Title = Typography.Title;

export default AddKeyModal;
