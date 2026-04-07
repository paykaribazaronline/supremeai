// APIKeysManager.tsx - API Keys & Dynamic AI Model Discovery
// Uses HuggingFace Hub API for real-time model search (no hardcoded models)

import React, { useState, useEffect, useCallback } from 'react';
import {
    Card, Tabs, Button, Input, Table, Tag, Space, Modal, Form, Select,
    message, Popconfirm, Empty, List, Spin, Row, Col, Typography, Tooltip, Badge, Switch,
    Statistic as AntStatistic
} from 'antd';
import {
    KeyOutlined, SearchOutlined, PlusOutlined, DeleteOutlined,
    EditOutlined, EyeOutlined, EyeInvisibleOutlined, CheckCircleOutlined,
    ExperimentOutlined, LinkOutlined, CloudDownloadOutlined, StarOutlined,
    GlobalOutlined, ThunderboltOutlined, SafetyOutlined, BarChartOutlined
} from '@ant-design/icons';

const { TabPane } = Tabs;
const { Text, Title, Paragraph } = Typography;
const { Search } = Input;

// ─── Types ───────────────────────────────────────────────────────────────────

interface SavedAPIKey {
    id: string;
    provider: string;
    label: string;
    apiKey: string;
    baseUrl: string;
    models: string[];
    status: 'active' | 'inactive' | 'error';
    addedAt: string;
    lastTested: string | null;
}

interface HuggingFaceModel {
    id: string;            // e.g. "meta-llama/Llama-3-70B"
    modelId: string;
    author: string;
    sha: string;
    lastModified: string;
    private: boolean;
    tags: string[];
    pipeline_tag: string;  // e.g. "text-generation", "image-classification"
    downloads: number;
    likes: number;
    library_name: string;  // e.g. "transformers", "diffusers"
}

interface OpenRouterModel {
    id: string;
    name: string;
    description: string;
    pricing: { prompt: string; completion: string };
    context_length: number;
    architecture: { tokenizer: string; instruct_type: string };
    top_provider: { max_completion_tokens: number };
}

// ─── API Key Management Tab ──────────────────────────────────────────────────

const APIKeysTab: React.FC = () => {
    const [keys, setKeys] = useState<SavedAPIKey[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingKey, setEditingKey] = useState<SavedAPIKey | null>(null);
    const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());
    const [form] = Form.useForm();

    useEffect(() => { fetchKeys(); }, []);

    const fetchKeys = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                setKeys(await response.json());
            }
        } catch {
            // Backend may not exist yet - show empty state
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const endpoint = editingKey ? `/api/apikeys/${editingKey.id}` : '/api/apikeys';
            const method = editingKey ? 'PUT' : 'POST';
            const response = await fetch(endpoint, {
                method,
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            if (response.ok) {
                message.success(editingKey ? 'API key updated!' : 'API key added!');
                setIsModalVisible(false);
                form.resetFields();
                setEditingKey(null);
                fetchKeys();
            } else {
                message.error('Failed to save API key');
            }
        } catch {
            message.error('Error saving API key');
        }
    };

    const handleDelete = async (id: string) => {
        try {
            const token = localStorage.getItem('authToken');
            await fetch(`/api/apikeys/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            message.success('API key removed');
            fetchKeys();
        } catch {
            message.error('Error removing API key');
        }
    };

    const handleTest = async (id: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/apikeys/${id}/test`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('API key is valid!');
                fetchKeys();
            } else {
                message.error('API key test failed');
            }
        } catch {
            message.error('Error testing API key');
        }
    };

    const toggleKeyVisibility = (id: string) => {
        setVisibleKeys((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const maskKey = (key: string) => {
        if (key.length <= 8) return '••••••••';
        return key.slice(0, 4) + '••••••••' + key.slice(-4);
    };

    const columns = [
        {
            title: 'Provider',
            dataIndex: 'provider',
            key: 'provider',
            render: (p: string) => <Tag color="blue">{p}</Tag>,
        },
        { title: 'Label', dataIndex: 'label', key: 'label' },
        {
            title: 'API Key',
            dataIndex: 'apiKey',
            key: 'apiKey',
            render: (key: string, record: SavedAPIKey) => (
                <Space>
                    <Text code copyable={{ text: key }}>
                        {visibleKeys.has(record.id) ? key : maskKey(key)}
                    </Text>
                    <Button
                        type="text"
                        size="small"
                        icon={visibleKeys.has(record.id) ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                        onClick={() => toggleKeyVisibility(record.id)}
                    />
                </Space>
            ),
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (s: string) => (
                <Tag color={s === 'active' ? 'green' : s === 'inactive' ? 'orange' : 'red'}>
                    {s.toUpperCase()}
                </Tag>
            ),
        },
        {
            title: 'Models',
            dataIndex: 'models',
            key: 'models',
            render: (models: string[]) =>
                models?.length ? models.map((m) => <Tag key={m}>{m}</Tag>) : <Text type="secondary">Any</Text>,
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: SavedAPIKey) => (
                <Space>
                    <Tooltip title="Test Key">
                        <Button size="small" icon={<ExperimentOutlined />} onClick={() => handleTest(record.id)} />
                    </Tooltip>
                    <Tooltip title="Edit">
                        <Button
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => {
                                setEditingKey(record);
                                form.setFieldsValue(record);
                                setIsModalVisible(true);
                            }}
                        />
                    </Tooltip>
                    <Popconfirm title="Delete this API key?" onConfirm={() => handleDelete(record.id)}>
                        <Button size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
                <Title level={5} style={{ margin: 0 }}>
                    <SafetyOutlined /> Saved API Keys
                </Title>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => {
                        setEditingKey(null);
                        form.resetFields();
                        setIsModalVisible(true);
                    }}
                >
                    Add API Key
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={keys}
                rowKey="id"
                loading={loading}
                locale={{ emptyText: <Empty description="No API keys configured yet. Add one to get started!" /> }}
                pagination={false}
            />

            <Modal
                title={editingKey ? 'Edit API Key' : 'Add New API Key'}
                open={isModalVisible}
                onCancel={() => { setIsModalVisible(false); setEditingKey(null); }}
                footer={null}
                width={600}
            >
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    <Form.Item name="provider" label="Provider Name" rules={[{ required: true, message: 'Enter provider name' }]}>
                        <Input placeholder="e.g. OpenAI, Anthropic, Google, Mistral, Groq, Together..." />
                    </Form.Item>
                    <Form.Item name="label" label="Label (optional)">
                        <Input placeholder="e.g. Production Key, Dev Key, Personal..." />
                    </Form.Item>
                    <Form.Item name="apiKey" label="API Key" rules={[{ required: true, message: 'Enter the API key' }]}>
                        <Input.Password placeholder="sk-... or key-... or your API key" />
                    </Form.Item>
                    <Form.Item name="baseUrl" label="Base URL (optional)">
                        <Input placeholder="e.g. https://api.openai.com/v1 (leave blank for default)" />
                    </Form.Item>
                    <Form.Item name="models" label="Restrict to Models (optional)">
                        <Select mode="tags" placeholder="Type model names to restrict (leave empty for all models)" />
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Button type="primary" htmlType="submit">
                                {editingKey ? 'Update' : 'Save Key'}
                            </Button>
                            <Button onClick={() => { setIsModalVisible(false); setEditingKey(null); }}>Cancel</Button>
                        </Space>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

// ─── Dynamic AI Model Search Tab (HuggingFace + OpenRouter) ─────────────────

const ModelDiscoveryTab: React.FC = () => {
    const [query, setQuery] = useState('');
    const [hfModels, setHfModels] = useState<HuggingFaceModel[]>([]);
    const [orModels, setOrModels] = useState<OpenRouterModel[]>([]);
    const [loading, setLoading] = useState(false);
    const [source, setSource] = useState<'huggingface' | 'openrouter' | 'all'>('all');
    const [pipelineFilter, setPipelineFilter] = useState<string>('');

    const searchHuggingFace = useCallback(async (q: string, pipeline?: string) => {
        const params = new URLSearchParams({ search: q, limit: '30', sort: 'likes', direction: '-1' });
        if (pipeline) params.set('pipeline_tag', pipeline);
        const response = await fetch(`https://huggingface.co/api/models?${params}`);
        if (response.ok) return (await response.json()) as HuggingFaceModel[];
        return [];
    }, []);

    const searchOpenRouter = useCallback(async (q: string) => {
        try {
            const response = await fetch('https://openrouter.ai/api/v1/models');
            if (!response.ok) return [];
            const data = await response.json();
            const models = (data.data || []) as OpenRouterModel[];
            if (!q) return models.slice(0, 30);
            const lower = q.toLowerCase();
            return models.filter(
                (m) => m.id.toLowerCase().includes(lower) || m.name.toLowerCase().includes(lower)
            ).slice(0, 30);
        } catch {
            return [];
        }
    }, []);

    const handleSearch = async (searchQuery?: string) => {
        const q = (searchQuery ?? query).trim();
        if (!q) {
            message.info('Type a model name, provider, or capability to search');
            return;
        }
        setLoading(true);
        try {
            const [hf, or] = await Promise.all([
                (source === 'openrouter') ? Promise.resolve([]) : searchHuggingFace(q, pipelineFilter),
                (source === 'huggingface') ? Promise.resolve([]) : searchOpenRouter(q),
            ]);
            setHfModels(hf);
            setOrModels(or);
            if (hf.length === 0 && or.length === 0) {
                message.info('No models found. Try a different search term.');
            }
        } catch {
            message.error('Search failed. Check your connection.');
        } finally {
            setLoading(false);
        }
    };

    const formatNumber = (n: number) => {
        if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
        if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
        return n.toString();
    };

    const pipelineOptions = [
        { value: '', label: 'All Types' },
        { value: 'text-generation', label: 'Text Generation (LLM)' },
        { value: 'text2text-generation', label: 'Text-to-Text' },
        { value: 'text-classification', label: 'Text Classification' },
        { value: 'image-classification', label: 'Image Classification' },
        { value: 'object-detection', label: 'Object Detection' },
        { value: 'image-to-text', label: 'Image to Text' },
        { value: 'text-to-image', label: 'Text to Image' },
        { value: 'text-to-speech', label: 'Text to Speech' },
        { value: 'automatic-speech-recognition', label: 'Speech Recognition' },
        { value: 'translation', label: 'Translation' },
        { value: 'summarization', label: 'Summarization' },
        { value: 'question-answering', label: 'Question Answering' },
        { value: 'feature-extraction', label: 'Embeddings' },
        { value: 'fill-mask', label: 'Fill Mask' },
        { value: 'zero-shot-classification', label: 'Zero-Shot Classification' },
        { value: 'image-segmentation', label: 'Image Segmentation' },
        { value: 'video-classification', label: 'Video Classification' },
    ];

    return (
        <div>
            <Card style={{ marginBottom: 16 }}>
                <Row gutter={[12, 12]} align="middle">
                    <Col xs={24} md={10}>
                        <Search
                            placeholder="Search any AI model... (e.g. GPT, Llama, Mistral, Gemma, SDXL, Whisper)"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            onSearch={handleSearch}
                            enterButton={<><SearchOutlined /> Search</>}
                            size="large"
                            loading={loading}
                        />
                    </Col>
                    <Col xs={12} md={5}>
                        <Select
                            value={source}
                            onChange={setSource}
                            style={{ width: '100%' }}
                            size="large"
                            options={[
                                { value: 'all', label: '🌐 All Sources' },
                                { value: 'huggingface', label: '🤗 HuggingFace' },
                                { value: 'openrouter', label: '🔀 OpenRouter' },
                            ]}
                        />
                    </Col>
                    <Col xs={12} md={5}>
                        <Select
                            value={pipelineFilter}
                            onChange={setPipelineFilter}
                            style={{ width: '100%' }}
                            size="large"
                            options={pipelineOptions}
                            placeholder="Filter by type"
                        />
                    </Col>
                    <Col xs={24} md={4}>
                        <Button
                            type="primary"
                            size="large"
                            block
                            icon={<ThunderboltOutlined />}
                            loading={loading}
                            onClick={() => handleSearch()}
                        >
                            Search
                        </Button>
                    </Col>
                </Row>
                <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                    <GlobalOutlined /> Live search across HuggingFace Hub & OpenRouter — finds any model, including ones released today.
                </Paragraph>
            </Card>

            <Spin spinning={loading}>
                {hfModels.length === 0 && orModels.length === 0 && !loading ? (
                    <Card>
                        <Empty
                            description={
                                <span>
                                    Search to discover AI models from the global registry.<br />
                                    <Text type="secondary">Try: "llama", "whisper", "stable diffusion", "gemma", "phi", "qwen"...</Text>
                                </span>
                            }
                        />
                    </Card>
                ) : (
                    <>
                        {hfModels.length > 0 && (
                            <Card
                                title={<><span>🤗 HuggingFace Models</span> <Tag>{hfModels.length} results</Tag></>}
                                style={{ marginBottom: 16 }}
                            >
                                <List
                                    dataSource={hfModels}
                                    renderItem={(model) => (
                                        <List.Item
                                            key={model.id}
                                            actions={[
                                                <Tooltip title="View on HuggingFace" key="link">
                                                    <Button
                                                        type="link"
                                                        icon={<LinkOutlined />}
                                                        href={`https://huggingface.co/${model.id}`}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                    >
                                                        View
                                                    </Button>
                                                </Tooltip>,
                                                <Button
                                                    key="use"
                                                    type="primary"
                                                    size="small"
                                                    icon={<PlusOutlined />}
                                                    onClick={() => {
                                                        message.success(`Model "${model.id}" copied! Use it when adding an API key.`);
                                                        navigator.clipboard.writeText(model.id);
                                                    }}
                                                >
                                                    Copy ID
                                                </Button>,
                                            ]}
                                        >
                                            <List.Item.Meta
                                                title={
                                                    <Space>
                                                        <Text strong>{model.id}</Text>
                                                        {model.pipeline_tag && <Tag color="purple">{model.pipeline_tag}</Tag>}
                                                        {model.library_name && <Tag color="cyan">{model.library_name}</Tag>}
                                                    </Space>
                                                }
                                                description={
                                                    <Space size="large">
                                                        <span>⬇ {formatNumber(model.downloads)} downloads</span>
                                                        <span>❤ {formatNumber(model.likes)} likes</span>
                                                        <span>Updated: {new Date(model.lastModified).toLocaleDateString()}</span>
                                                        {model.tags?.slice(0, 5).map((tag) => (
                                                            <Tag key={tag} style={{ fontSize: 11 }}>{tag}</Tag>
                                                        ))}
                                                    </Space>
                                                }
                                            />
                                        </List.Item>
                                    )}
                                />
                            </Card>
                        )}

                        {orModels.length > 0 && (
                            <Card title={<><span>🔀 OpenRouter Models (Hosted APIs)</span> <Tag>{orModels.length} results</Tag></>}>
                                <List
                                    dataSource={orModels}
                                    renderItem={(model) => (
                                        <List.Item
                                            key={model.id}
                                            actions={[
                                                <Tooltip title="View on OpenRouter" key="link">
                                                    <Button
                                                        type="link"
                                                        icon={<LinkOutlined />}
                                                        href={`https://openrouter.ai/models/${model.id}`}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                    >
                                                        View
                                                    </Button>
                                                </Tooltip>,
                                                <Button
                                                    key="use"
                                                    type="primary"
                                                    size="small"
                                                    icon={<PlusOutlined />}
                                                    onClick={() => {
                                                        message.success(`Model "${model.id}" copied!`);
                                                        navigator.clipboard.writeText(model.id);
                                                    }}
                                                >
                                                    Copy ID
                                                </Button>,
                                            ]}
                                        >
                                            <List.Item.Meta
                                                title={
                                                    <Space>
                                                        <Text strong>{model.name}</Text>
                                                        <Tag color="geekblue">{model.id}</Tag>
                                                    </Space>
                                                }
                                                description={
                                                    <Space size="large">
                                                        <span>Context: {formatNumber(model.context_length)} tokens</span>
                                                        <span>
                                                            Pricing: ${model.pricing?.prompt}/prompt, ${model.pricing?.completion}/completion
                                                        </span>
                                                        {model.description && (
                                                            <Text type="secondary" style={{ maxWidth: 400 }} ellipsis>
                                                                {model.description}
                                                            </Text>
                                                        )}
                                                    </Space>
                                                }
                                            />
                                        </List.Item>
                                    )}
                                />
                            </Card>
                        )}
                    </>
                )}
            </Spin>
        </div>
    );
};

// ─── Usage / Stats Tab ───────────────────────────────────────────────────────

const UsageStatsTab: React.FC = () => {
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchUsage = async () => {
            setLoading(true);
            try {
                const token = localStorage.getItem('authToken');
                const response = await fetch('/api/apikeys/usage', {
                    headers: { 'Authorization': `Bearer ${token}` },
                });
                if (response.ok) setStats(await response.json());
            } catch {
                // Backend not available yet
            } finally {
                setLoading(false);
            }
        };
        fetchUsage();
    }, []);

    return (
        <Card loading={loading}>
            {stats ? (
                <Row gutter={16}>
                    <Col span={6}>
                        <Card size="small">
                            <AntStatistic title="Total Requests" value={stats.totalRequests ?? 0} />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small">
                            <AntStatistic title="Active Keys" value={stats.activeKeys ?? 0} />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small">
                            <AntStatistic title="Total Cost" value={stats.totalCost ?? 0} prefix="$" precision={2} />
                        </Card>
                    </Col>
                    <Col span={6}>
                        <Card size="small">
                            <AntStatistic title="Providers" value={stats.providers ?? 0} />
                        </Card>
                    </Col>
                </Row>
            ) : (
                <Empty description="Usage statistics will appear once API keys are in use." />
            )}
        </Card>
    );
};

// ─── Main Component ──────────────────────────────────────────────────────────

const APIKeysManager: React.FC = () => {
    return (
        <div>
            <Tabs defaultActiveKey="keys" size="large" type="card">
                <TabPane
                    tab={<span><KeyOutlined /> My API Keys</span>}
                    key="keys"
                >
                    <APIKeysTab />
                </TabPane>
                <TabPane
                    tab={<span><SearchOutlined /> Discover Models</span>}
                    key="discover"
                >
                    <ModelDiscoveryTab />
                </TabPane>
                <TabPane
                    tab={<span><BarChartOutlined /> Usage</span>}
                    key="usage"
                >
                    <UsageStatsTab />
                </TabPane>
            </Tabs>
        </div>
    );
};

export default APIKeysManager;
