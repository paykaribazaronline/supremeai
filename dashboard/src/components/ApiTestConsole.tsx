import React, { useState } from 'react';
import { Modal, Select, Input, Button, Form, Typography, Spin, Alert, Tabs, Space } from 'antd';
import { SendOutlined, CodeOutlined } from '@ant-design/icons';

const { TextArea } = Input;
const { Text, Title } = Typography;

interface ApiTestConsoleProps {
    visible: boolean;
    onClose: () => void;
    apiKeys: Array<{ id: string; label: string; provider: string; baseUrl: string }>;
}

const ApiTestConsole: React.FC<ApiTestConsoleProps> = ({ visible, onClose, apiKeys }) => {
    const [selectedKeyId, setSelectedKeyId] = useState<string>(apiKeys[0]?.id || '');
    const [method, setMethod] = useState<'GET' | 'POST' | 'PUT' | 'DELETE'>('GET');
    const [endpoint, setEndpoint] = useState<string>('');
    const [headers, setHeaders] = useState<string>('{\n  "Content-Type": "application/json"\n}');
    const [body, setBody] = useState<string>('{\n  "key": "value"\n}');
    const [loading, setLoading] = useState(false);
    const [response, setResponse] = useState<{
        status: number;
        statusText: string;
        headers: Record<string, string>;
        body: string;
    } | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleSend = async () => {
        if (!selectedKeyId) {
            setError('Please select an API key');
            return;
        }
        if (!endpoint) {
            setError('Please enter an endpoint URL');
            return;
        }

        setLoading(true);
        setError(null);
        setResponse(null);

        try {
            const token = localStorage.getItem('authToken');
            const res = await fetch('/api/apikeys/test-request', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    keyId: selectedKeyId,
                    method,
                    endpoint,
                    headers: JSON.parse(headers || '{}'),
                    body: method !== 'GET' ? JSON.parse(body || '{}') : undefined,
                }),
            });

            const data = await res.json();
            if (res.ok) {
                setResponse({
                    status: data.status,
                    statusText: data.statusText,
                    headers: data.headers,
                    body: JSON.stringify(data.body, null, 2),
                });
            } else {
                setError(data.error || 'Request failed');
            }
        } catch (err) {
            setError('Failed to send request: ' + (err instanceof Error ? err.message : String(err)));
        } finally {
            setLoading(false);
        }
    };

    return (
        <Modal
            title={
                <Space>
                    <CodeOutlined />
                    <Title level={4} style={{ margin: 0 }}>API Testing Console</Title>
                </Space>
            }
            open={visible}
            onCancel={onClose}
            footer={null}
            width={800}
            destroyOnClose
        >
            <Form layout="vertical" style={{ marginTop: 16 }}>
                <Form.Item label="Select API Key">
                    <Select
                        value={selectedKeyId}
                        onChange={setSelectedKeyId}
                        options={apiKeys.map(k => ({
                            value: k.id,
                            label: `${k.label} (${k.provider})`,
                        }))}
                        placeholder="Select an API key"
                    />
                </Form.Item>

                <Space style={{ width: '100%' }} direction="vertical">
                    <Space>
                        <Select
                            value={method}
                            onChange={setMethod}
                            options={[
                                { value: 'GET', label: 'GET' },
                                { value: 'POST', label: 'POST' },
                                { value: 'PUT', label: 'PUT' },
                                { value: 'DELETE', label: 'DELETE' },
                            ]}
                            style={{ width: 100 }}
                        />
                        <Input
                            value={endpoint}
                            onChange={(e) => setEndpoint(e.target.value)}
                            placeholder="https://api.example.com/v1/endpoint"
                            style={{ width: 600 }}
                        />
                    </Space>

                    <Tabs
                        defaultActiveKey="headers"
                        items={[
                            {
                                key: 'headers',
                                label: 'Headers',
                                children: (
                                    <TextArea
                                        value={headers}
                                        onChange={(e) => setHeaders(e.target.value)}
                                        rows={6}
                                        placeholder="Enter headers as JSON"
                                    />
                                ),
                            },
                            {
                                key: 'body',
                                label: 'Body',
                                children: (
                                    <TextArea
                                        value={body}
                                        onChange={(e) => setBody(e.target.value)}
                                        rows={6}
                                        placeholder="Enter request body as JSON (for POST/PUT)"
                                        disabled={method === 'GET' || method === 'DELETE'}
                                    />
                                ),
                            },
                        ]}
                    />
                </Space>

                {error && (
                    <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />
                )}

                <Button
                    type="primary"
                    icon={<SendOutlined />}
                    onClick={handleSend}
                    loading={loading}
                    style={{ marginBottom: 16 }}
                >
                    Send Request
                </Button>

                {response && (
                    <div style={{ border: '1px solid #eee', borderRadius: 8, padding: 16 }}>
                        <Text strong style={{ color: response.status < 400 ? '#52c41a' : '#ff4d4f' }}>
                            Status: {response.status} {response.statusText}
                        </Text>
                        <div style={{ marginTop: 8 }}>
                            <Text strong>Response Headers:</Text>
                            <pre style={{ background: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                {JSON.stringify(response.headers, null, 2)}
                            </pre>
                        </div>
                        <div style={{ marginTop: 8 }}>
                            <Text strong>Response Body:</Text>
                            <pre style={{ background: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                {response.body}
                            </pre>
                        </div>
                    </div>
                )}
            </Form>
        </Modal>
    );
};

export default ApiTestConsole;
