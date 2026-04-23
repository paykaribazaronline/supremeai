// APIKeysManager.tsx - API Keys & Dynamic AI Model Discovery
// Uses HuggingFace Hub API for real-time model search (no hardcoded models)

import React, { useState, useEffect, useCallback } from 'react';
import {
    Card, Tabs, Button, Input, Table, Tag, Space, Modal, Form, Select,
    message, Popconfirm, Empty, List, Spin, Row, Col, Typography, Tooltip, Badge, Switch,
    Statistic as AntStatistic, Alert
} from 'antd';
import ApiTestConsole from './ApiTestConsole';
import {
    KeyOutlined, SearchOutlined, PlusOutlined, DeleteOutlined,
    EditOutlined, EyeOutlined, EyeInvisibleOutlined, CheckCircleOutlined,
    ExperimentOutlined, LinkOutlined, CloudDownloadOutlined, StarOutlined,
    GlobalOutlined, ThunderboltOutlined, SafetyOutlined, BarChartOutlined,
    ReloadOutlined
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

interface GoogleAIModel {
    name: string;                   // e.g. "models/gemini-2.0-flash"
    displayName: string;            // e.g. "Gemini 2.0 Flash"
    description: string;
    supportedGenerationMethods: string[];
    inputTokenLimit: number;
    outputTokenLimit: number;
    temperature?: number;
    topP?: number;
}

interface PopularModel {
    id: string;
    name: string;
    provider: string;
    providerTitle: string;
    baseUrl: string;
    description: string;
    category: string;
}

// ─── Popular / Quick-Add Models (curated, no API call needed) ────────────────

const POPULAR_MODELS: PopularModel[] = [
    // Google Gemini
    { id: 'gemini-2.5-pro-preview-03-25', name: 'Gemini 2.5 Pro', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Latest Gemini Pro — best reasoning & coding', category: 'Gemini' },
    { id: 'gemini-2.5-flash-preview-04-17', name: 'Gemini 2.5 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Fast & efficient Gemini Flash model', category: 'Gemini' },
    { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Stable Gemini 2.0 Flash — great for most tasks', category: 'Gemini' },
    { id: 'gemini-2.0-flash-lite', name: 'Gemini 2.0 Flash Lite', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Cost-optimized Gemini Flash', category: 'Gemini' },
    { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: '1M+ token context window', category: 'Gemini' },
    { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Fast Gemini 1.5 with long context', category: 'Gemini' },
    // OpenAI
    { id: 'gpt-4.1', name: 'GPT-4.1', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Latest GPT-4.1 — advanced coding & reasoning', category: 'OpenAI' },
    { id: 'gpt-4.1-mini', name: 'GPT-4.1 Mini', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Fast & affordable GPT-4.1', category: 'OpenAI' },
    { id: 'gpt-4.1-nano', name: 'GPT-4.1 Nano', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Ultra-fast & cheapest GPT-4.1', category: 'OpenAI' },
    { id: 'gpt-4o', name: 'GPT-4o', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Multimodal GPT-4 Omni', category: 'OpenAI' },
    { id: 'o4-mini', name: 'o4-mini', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Reasoning model — efficient', category: 'OpenAI' },
    { id: 'o3', name: 'o3', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Advanced reasoning model', category: 'OpenAI' },
    // Anthropic
    { id: 'claude-sonnet-4-20250514', name: 'Claude Sonnet 4', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Latest Claude — best coding & analysis', category: 'Anthropic' },
    { id: 'claude-3-7-sonnet-20250219', name: 'Claude 3.7 Sonnet', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Extended thinking, strong coding', category: 'Anthropic' },
    { id: 'claude-3-5-haiku-20241022', name: 'Claude 3.5 Haiku', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Fast & affordable Claude', category: 'Anthropic' },
    // Meta Llama
    { id: 'llama-4-maverick-17b-128e-instruct', name: 'Llama 4 Maverick', provider: 'meta', providerTitle: 'Meta (via OpenRouter)', baseUrl: 'https://openrouter.ai/api/v1', description: 'Llama 4 MoE model', category: 'Meta' },
    { id: 'llama-3.3-70b-instruct', name: 'Llama 3.3 70B', provider: 'meta', providerTitle: 'Meta (via OpenRouter)', baseUrl: 'https://openrouter.ai/api/v1', description: 'Latest Llama 3.3 70B', category: 'Meta' },
    // Mistral
    { id: 'mistral-large-latest', name: 'Mistral Large', provider: 'mistral', providerTitle: 'Mistral AI', baseUrl: 'https://api.mistral.ai/v1', description: 'Top-tier Mistral model', category: 'Mistral' },
    { id: 'mistral-small-latest', name: 'Mistral Small', provider: 'mistral', providerTitle: 'Mistral AI', baseUrl: 'https://api.mistral.ai/v1', description: 'Fast Mistral model', category: 'Mistral' },
    // Groq (fast inference)
    { id: 'llama-3.3-70b-versatile', name: 'Llama 3.3 70B (Groq)', provider: 'groq', providerTitle: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', description: 'Ultra-fast Llama inference on Groq', category: 'Groq' },
    { id: 'llama-3.1-8b-instant', name: 'Llama 3.1 8B (Groq)', provider: 'groq', providerTitle: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', description: 'Fastest model on Groq', category: 'Groq' },
    // DeepSeek
    { id: 'deepseek-chat', name: 'DeepSeek V3', provider: 'deepseek', providerTitle: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', description: 'DeepSeek V3 — strong coding model', category: 'DeepSeek' },
    { id: 'deepseek-reasoner', name: 'DeepSeek R1', provider: 'deepseek', providerTitle: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', description: 'DeepSeek reasoning model', category: 'DeepSeek' },
    // xAI
    { id: 'grok-3', name: 'Grok 3', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Latest Grok model by xAI', category: 'xAI' },
    { id: 'grok-3-mini', name: 'Grok 3 Mini', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Fast Grok 3 Mini', category: 'xAI' },
];

// ─── API Key Management Tab ──────────────────────────────────────────────────

const APIKeysTab: React.FC = () => {
    const [keys, setKeys] = useState<SavedAPIKey[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingKey, setEditingKey] = useState<SavedAPIKey | null>(null);
    const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [bulkLoading, setBulkLoading] = useState(false);
    const [testConsoleVisible, setTestConsoleVisible] = useState(false);
    const [form] = Form.useForm();
    const [error, setError] = useState<string | null>(null);

    useEffect(() => { fetchKeys(); }, []);

    const fetchKeys = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                setKeys(await response.json());
            } else if (response.status === 401) {
                setError('Session expired. Please log in again.');
            }
        } catch (err) {
            setError('Unable to connect to the server. Please check your internet connection and try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (values: any) => {
        setError(null);
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
            } else if (response.status === 409) {
                message.error('An API key with this configuration already exists.');
            } else if (response.status === 400) {
                const errData = await response.json().catch(() => ({}));
                message.error(errData.message || 'Invalid input. Please check your API key and try again.');
            } else {
                message.error('Failed to save API key. The server encountered an error.');
            }
        } catch (err) {
            message.error('Unable to connect to the server. Please check your internet connection.');
        }
    };

    const handleDelete = async (id: string) => {
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/apikeys/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('API key removed');
                fetchKeys();
            } else {
                message.error('Failed to remove API key. Please try again.');
            }
        } catch (err) {
            message.error('Unable to connect to the server. Please check your connection.');
        }
    };

    const handleTest = async (id: string) => {
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/apikeys/${id}/test`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('API key is valid and working!');
                fetchKeys();
            } else if (response.status === 401) {
                message.error('API key is invalid or has expired. Please update it.');
            } else if (response.status === 429) {
                message.warning('Rate limit reached. Please wait a moment before testing again.');
            } else {
                message.error('API key test failed. Please verify the key and provider settings.');
            }
        } catch (err) {
            message.error('Unable to test API key. Please check your internet connection.');
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

    const rowSelection = {
        selectedRowKeys,
        onChange: (newSelectedRowKeys: React.Key[]) => {
            setSelectedRowKeys(newSelectedRowKeys);
        },
    };

    const handleBulkDelete = async () => {
        if (selectedRowKeys.length === 0) return;
        setBulkLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys/bulk', {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ keyIds: selectedRowKeys }),
            });
            if (response.ok) {
                message.success(`Deleted ${selectedRowKeys.length} API keys`);
                setSelectedRowKeys([]);
                fetchKeys();
            } else {
                message.error('Bulk delete failed');
            }
        } catch (err) {
            message.error('Unable to connect to server');
        } finally {
            setBulkLoading(false);
        }
    };

    const handleBulkRegenerate = async () => {
        if (selectedRowKeys.length === 0) return;
        setBulkLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys/bulk/regenerate', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ keyIds: selectedRowKeys }),
            });
            if (response.ok) {
                message.success(`Regenerated ${selectedRowKeys.length} API keys`);
                setSelectedRowKeys([]);
                fetchKeys();
            } else {
                message.error('Bulk regenerate failed');
            }
        } catch (err) {
            message.error('Unable to connect to server');
        } finally {
            setBulkLoading(false);
        }
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
            {error && (
                <Alert
                    message="Connection Error"
                    description={error}
                    type="error"
                    showIcon
                    closable
                    onClose={() => setError(null)}
                    action={<Button size="small" onClick={fetchKeys}>Retry</Button>}
                    style={{ marginBottom: 16 }}
                />
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, alignItems: 'center' }}>
                <Title level={5} style={{ margin: 0 }}>
                    <SafetyOutlined /> Saved API Keys
                </Title>
                <Space>
                    {selectedRowKeys.length > 0 && (
                        <>
                            <span style={{ color: '#666' }}>{selectedRowKeys.length} selected</span>
                            <Button
                                danger
                                icon={<DeleteOutlined />}
                                loading={bulkLoading}
                                onClick={handleBulkDelete}
                            >
                                Bulk Delete
                            </Button>
                            <Button
                                icon={<ReloadOutlined />}
                                loading={bulkLoading}
                                onClick={handleBulkRegenerate}
                            >
                                Bulk Regenerate
                            </Button>
                        </>
                    )}
                    <Button
                        icon={<CodeOutlined />}
                        onClick={() => setTestConsoleVisible(true)}
                    >
                        API Test Console
                    </Button>
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
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={keys}
                rowKey="id"
                loading={loading}
                rowSelection={rowSelection}
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
                        <Select
                            showSearch
                            placeholder="Select or type a provider name..."
                            options={[
                                { value: 'Google AI', label: 'Google AI (Gemini)' },
                                { value: 'OpenAI', label: 'OpenAI (GPT, o1, o3)' },
                                { value: 'Anthropic', label: 'Anthropic (Claude)' },
                                { value: 'Mistral', label: 'Mistral AI' },
                                { value: 'Groq', label: 'Groq (Fast Inference)' },
                                { value: 'DeepSeek', label: 'DeepSeek' },
                                { value: 'xAI', label: 'xAI (Grok)' },
                                { value: 'OpenRouter', label: 'OpenRouter (Multi-provider)' },
                                { value: 'Together AI', label: 'Together AI' },
                                { value: 'Fireworks AI', label: 'Fireworks AI' },
                                { value: 'Cohere', label: 'Cohere' },
                                { value: 'Ollama', label: 'Ollama (Local)' },
                            ]}
                        />
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
    const [googleModels, setGoogleModels] = useState<GoogleAIModel[]>([]);
    const [loading, setLoading] = useState(false);
    const [source, setSource] = useState<'huggingface' | 'openrouter' | 'google' | 'all'>('all');
    const [pipelineFilter, setPipelineFilter] = useState<string>('');
    const [googleApiKey, setGoogleApiKey] = useState('');
    const [showGoogleKeyInput, setShowGoogleKeyInput] = useState(false);
    const [popularFilter, setPopularFilter] = useState<string>('all');

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

    const searchGoogleAI = useCallback(async (q: string, apiKey: string) => {
        try {
            const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
            if (!response.ok) return [];
            const data = await response.json();
            const models = (data.models || []) as GoogleAIModel[];
            if (!q) return models;
            const lower = q.toLowerCase();
            return models.filter(
                (m) => m.name.toLowerCase().includes(lower) || m.displayName.toLowerCase().includes(lower)
            );
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
            const promises: Promise<any>[] = [];

            // HuggingFace
            if (source !== 'openrouter' && source !== 'google') {
                promises.push(searchHuggingFace(q, pipelineFilter));
            } else {
                promises.push(Promise.resolve([]));
            }

            // OpenRouter
            if (source !== 'huggingface' && source !== 'google') {
                promises.push(searchOpenRouter(q));
            } else {
                promises.push(Promise.resolve([]));
            }

            // Google AI
            if (source === 'google' || source === 'all') {
                if (googleApiKey.trim()) {
                    promises.push(searchGoogleAI(q, googleApiKey.trim()));
                } else {
                    promises.push(Promise.resolve([]));
                    if (source === 'google' || source === 'all') {
                        setShowGoogleKeyInput(true);
                    }
                }
            } else {
                promises.push(Promise.resolve([]));
            }

            const [hf, or, google] = await Promise.all(promises);
            setHfModels(hf);
            setOrModels(or);
            setGoogleModels(google);
            if (hf.length === 0 && or.length === 0 && google.length === 0) {
                message.info('No models found. Try a different search term or add a Google API key for Gemini models.');
            }
        } catch {
            message.error('Search failed due to network issue. Please check your internet connection and try again.');
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
            {/* Popular Models Quick-Access */}
            <Card style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                    <Title level={5} style={{ margin: 0 }}>
                        <StarOutlined style={{ color: '#faad14' }} /> Popular Models — Quick Add
                    </Title>
                    <Select
                        value={popularFilter}
                        onChange={setPopularFilter}
                        style={{ width: 160 }}
                        size="small"
                        options={[
                            { value: 'all', label: 'All Providers' },
                            { value: 'Gemini', label: 'Google Gemini' },
                            { value: 'OpenAI', label: 'OpenAI' },
                            { value: 'Anthropic', label: 'Anthropic' },
                            { value: 'Meta', label: 'Meta Llama' },
                            { value: 'Mistral', label: 'Mistral' },
                            { value: 'Groq', label: 'Groq' },
                            { value: 'DeepSeek', label: 'DeepSeek' },
                            { value: 'xAI', label: 'xAI Grok' },
                        ]}
                    />
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {POPULAR_MODELS
                        .filter(m => popularFilter === 'all' || m.category === popularFilter)
                        .map((model) => (
                            <Tooltip key={model.id} title={model.description}>
                                <Tag
                                    style={{ cursor: 'pointer', padding: '4px 10px', fontSize: 13 }}
                                    color={
                                        model.provider === 'google' ? 'blue' :
                                        model.provider === 'openai' ? 'green' :
                                        model.provider === 'anthropic' ? 'orange' :
                                        model.provider === 'groq' ? 'purple' :
                                        model.provider === 'xai' ? 'geekblue' :
                                        model.provider === 'deepseek' ? 'cyan' :
                                        'default'
                                    }
                                    onClick={() => {
                                        const text = model.id;
                                        navigator.clipboard.writeText(text);
                                        message.success(`"${model.name}" (${model.id}) copied! Add it when creating an API key for ${model.providerTitle}.`);
                                    }}
                                >
                                    {model.name}
                                    <span style={{ color: '#999', marginLeft: 4, fontSize: 11 }}>({model.providerTitle})</span>
                                </Tag>
                            </Tooltip>
                        ))}
                </div>
                <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
                    Click any model to copy its ID. Then paste it when adding an API key for that provider.
                </Paragraph>
            </Card>

            {/* Live Search */}
            <Card style={{ marginBottom: 16 }}>
                <Row gutter={[12, 12]} align="middle">
                    <Col xs={24} md={8}>
                        <Search
                            placeholder="Search any AI model... (e.g. Gemini, GPT, Claude, Llama, Mistral)"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            onSearch={handleSearch}
                            enterButton={<><SearchOutlined /> Search</>}
                            size="large"
                            loading={loading}
                        />
                    </Col>
                    <Col xs={12} md={4}>
                        <Select
                            value={source}
                            onChange={setSource}
                            style={{ width: '100%' }}
                            size="large"
                            options={[
                                { value: 'all', label: 'All Sources' },
                                { value: 'huggingface', label: 'HuggingFace' },
                                { value: 'openrouter', label: 'OpenRouter' },
                                { value: 'google', label: 'Google AI (Gemini)' },
                            ]}
                        />
                    </Col>
                    <Col xs={12} md={4}>
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

                {/* Google AI API Key input */}
                {(source === 'google' || source === 'all') && (
                    <div style={{ marginTop: 12, padding: '8px 12px', background: '#f6f8fa', borderRadius: 8, border: '1px solid #e8e8e8' }}>
                        <Row gutter={12} align="middle">
                            <Col flex="auto">
                                <Input.Password
                                    placeholder="Enter your Google AI API key to search Gemini models..."
                                    value={googleApiKey}
                                    onChange={(e) => setGoogleApiKey(e.target.value)}
                                    size="small"
                                />
                            </Col>
                            <Col>
                                <Tooltip title="Get a free API key from aistudio.google.com">
                                    <Button
                                        type="link"
                                        size="small"
                                        icon={<LinkOutlined />}
                                        href="https://aistudio.google.com/apikey"
                                        target="_blank"
                                        rel="noopener noreferrer"
                                    >
                                        Get Key
                                    </Button>
                                </Tooltip>
                            </Col>
                        </Row>
                        <Text type="secondary" style={{ fontSize: 11 }}>
                            Required for searching Gemini / Google AI models. Your key is used only in your browser — never sent to our server.
                        </Text>
                    </div>
                )}

                <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                    <GlobalOutlined /> Live search across HuggingFace Hub, OpenRouter & Google AI — finds any model, including ones released today.
                </Paragraph>
            </Card>

            <Spin spinning={loading}>
                {hfModels.length === 0 && orModels.length === 0 && googleModels.length === 0 && !loading ? (
                    <Card>
                        <Empty
                            description={
                                <span>
                                    Search to discover AI models from the global registry.<br />
                                    <Text type="secondary">Try: "gemini", "gpt", "claude", "llama", "mistral", "phi", "qwen"...</Text>
                                </span>
                            }
                        />
                    </Card>
                ) : (
                    <>
                        {googleModels.length > 0 && (
                            <Card
                                title={<><span>Google AI Models (Gemini)</span> <Tag color="blue">{googleModels.length} results</Tag></>}
                                style={{ marginBottom: 16 }}
                            >
                                <List
                                    dataSource={googleModels}
                                    renderItem={(model) => (
                                        <List.Item
                                            key={model.name}
                                            actions={[
                                                <Tooltip title="View on Google AI" key="link">
                                                    <Button
                                                        type="link"
                                                        icon={<LinkOutlined />}
                                                        href={`https://ai.google.dev/gemini-api/docs/models`}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                    >
                                                        Docs
                                                    </Button>
                                                </Tooltip>,
                                                <Button
                                                    key="use"
                                                    type="primary"
                                                    size="small"
                                                    icon={<PlusOutlined />}
                                                    onClick={() => {
                                                        const modelId = model.name.replace('models/', '');
                                                        message.success(`Model "${model.displayName}" (${modelId}) copied! Add it when creating an API key for Google AI.`);
                                                        navigator.clipboard.writeText(modelId);
                                                    }}
                                                >
                                                    Copy ID
                                                </Button>,
                                            ]}
                                        >
                                            <List.Item.Meta
                                                title={
                                                    <Space>
                                                        <Text strong>{model.displayName}</Text>
                                                        <Tag color="blue">{model.name}</Tag>
                                                        {model.supportedGenerationMethods?.map((method) => (
                                                            <Tag key={method} color="geekblue" style={{ fontSize: 11 }}>{method}</Tag>
                                                        ))}
                                                    </Space>
                                                }
                                                description={
                                                    <Space size="large">
                                                        <span>Input: {formatNumber(model.inputTokenLimit)} tokens</span>
                                                        <span>Output: {formatNumber(model.outputTokenLimit)} tokens</span>
                                                        <Text type="secondary" style={{ maxWidth: 400 }} ellipsis>
                                                            {model.description}
                                                        </Text>
                                                    </Space>
                                                }
                                            />
                                        </List.Item>
                                    )}
                                />
                            </Card>
                        )}

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

            <ApiTestConsole
                visible={testConsoleVisible}
                onClose={() => setTestConsoleVisible(false)}
                apiKeys={keys.map(k => ({ id: k.id, label: k.label, provider: k.provider, baseUrl: k.baseUrl }))}
            />
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
