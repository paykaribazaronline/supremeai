import React, { useState, useEffect } from 'react';
import { Select, Input, Button, Form, Typography, Spin, Alert, Tabs, Space, Card, Row, Col } from 'antd';
import { SendOutlined, CodeOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { TextArea } = Input;
const { Text, Title } = Typography;

const APITestingPanel: React.FC = () => {
    const [apiKeys, setApiKeys] = useState<any[]>([]);
    const [selectedKeyId, setSelectedKeyId] = useState<string>('');
    const [method, setMethod] = useState<'GET' | 'POST' | 'PUT' | 'DELETE'>('GET');
    const [endpoint, setEndpoint] = useState<string>('');
    const [headers, setHeaders] = useState<string>('{\n  "Content-Type": "application/json"\n}');
    const [body, setBody] = useState<string>('{\n  "key": "value"\n}');
    const [loading, setLoading] = useState(false);
    const [fetchingKeys, setFetchingKeys] = useState(true);
    const [response, setResponse] = useState<{
        status: number;
        statusText: string;
        headers: Record<string, string>;
        body: string;
    } | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchApiKeys();
    }, []);

    const fetchApiKeys = async () => {
        try {
            const token = authUtils.getToken();
            const res = await fetch('/api/apikeys', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                const data = await res.json();
                setApiKeys(data);
                if (data.length > 0) setSelectedKeyId(data[0].id);
            }
        } catch (err) {
            console.error('Failed to fetch API keys');
        } finally {
            setFetchingKeys(false);
        }
    };

    const handleSend = async () => {
        if (!endpoint) {
            setError('Please enter an endpoint URL');
            return;
        }

        setLoading(true);
        setError(null);
        setResponse(null);

        try {
            const token = authUtils.getToken();
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
        <Card title={<span><CodeOutlined /> API Testing Console</span>}>
            <Form layout="vertical">
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item label="Select Managed API Key (Optional)">
                            <Select
                                value={selectedKeyId}
                                onChange={setSelectedKeyId}
                                loading={fetchingKeys}
                                options={[
                                    { value: '', label: 'None (Direct Request)' },
                                    ...apiKeys.map(k => ({
                                        value: k.id,
                                        label: `${k.label} (${k.provider})`,
                                    }))
                                ]}
                                placeholder="Select an API key to use its credentials"
                            />
                        </Form.Item>
                    </Col>
                    <Col span={4}>
                        <Form.Item label="Method">
                            <Select
                                value={method}
                                onChange={setMethod}
                                options={[
                                    { value: 'GET', label: 'GET' },
                                    { value: 'POST', label: 'POST' },
                                    { value: 'PUT', label: 'PUT' },
                                    { value: 'DELETE', label: 'DELETE' },
                                ]}
                            />
                        </Form.Item>
                    </Col>
                    <Col span={8}>
                        <Form.Item label="Endpoint URL">
                            <Input
                                value={endpoint}
                                onChange={(e) => setEndpoint(e.target.value)}
                                placeholder="https://api.example.com/v1/resource"
                            />
                        </Form.Item>
                    </Col>
                </Row>

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
                                    rows={4}
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
                                    rows={4}
                                    placeholder="Enter request body as JSON (for POST/PUT)"
                                    disabled={method === 'GET' || method === 'DELETE'}
                                />
                            ),
                        },
                    ]}
                />

                <Button
                    type="primary"
                    icon={<SendOutlined />}
                    onClick={handleSend}
                    loading={loading}
                    style={{ marginTop: 16, marginBottom: 16 }}
                >
                    Send Request
                </Button>

                {error && (
                    <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />
                )}

                {response && (
                    <div style={{ border: '1px solid #eee', borderRadius: 8, padding: 16, background: '#fafafa' }}>
                        <Text strong style={{ color: response.status < 400 ? '#52c41a' : '#ff4d4f' }}>
                            Status: {response.status} {response.statusText}
                        </Text>
                        <Tabs
                            size="small"
                            style={{ marginTop: 8 }}
                            items={[
                                {
                                    key: 'body',
                                    label: 'Response Body',
                                    children: <pre style={{ maxHeight: 300, overflow: 'auto', fontSize: '12px' }}>{response.body}</pre>
                                },
                                {
                                    key: 'headers',
                                    label: 'Response Headers',
                                    children: <pre style={{ maxHeight: 200, overflow: 'auto', fontSize: '12px' }}>{JSON.stringify(response.headers, null, 2)}</pre>
                                }
                            ]}
                        />
                    </div>
                )}
            </Form>
        </Card>
    );
};

export default APITestingPanel;
