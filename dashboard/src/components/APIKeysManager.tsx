// APIKeysManager.tsx - API Keys & Dynamic AI Model Discovery
// Uses HuggingFace Hub API for real-time model search (no hardcoded models)

import React, { useState, useEffect, useCallback, useRef } from 'react';
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
    ReloadOutlined, CodeOutlined, RobotOutlined, QuestionCircleOutlined
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
    // StepFun (阶跃星辰) - Free tier available
    { id: 'step-3.5-flash', name: 'Step 3.5 Flash', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Free & fast - excellent reasoning & coding', category: 'StepFun' },
    { id: 'step-3.5-pro', name: 'Step 3.5 Pro', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Advanced reasoning & coding capabilities', category: 'StepFun' },
  ];

// ─── Provider Endpoint Auto-Detection ────────────────────────────────────────

const PROVIDER_ENDPOINTS: Record<string, string> = {
    openai: 'https://api.openai.com/v1',
    anthropic: 'https://api.anthropic.com/v1',
    google: 'https://generativelanguage.googleapis.com/v1beta/openai',
    gemini: 'https://generativelanguage.googleapis.com/v1beta/openai',
    huggingface: 'https://api-inference.huggingface.co',
    meta: 'https://openrouter.ai/api/v1',
    openrouter: 'https://openrouter.ai/api/v1',
    mistral: 'https://api.mistral.ai/v1',
    groq: 'https://api.groq.com/openai/v1',
    deepseek: 'https://api.deepseek.com/v1',
    xai: 'https://api.x.ai/v1',
    perplexity: 'https://api.perplexity.ai',
    cohere: 'https://api.cohere.ai/v1',
    together: 'https://api.together.xyz/v1',
    anyscale: 'https://api.endpoints.anyscale.com/v1',
    azure: 'https://YOUR_RESOURCE.openai.azure.com',
    aws: 'https://YOUR_REGION.bedrock.aws.amazon.com',
    stepfun: 'https://api.stepfun.com/v1',
};

const getProviderEndpoint = (providerName: string): string => {
    const lower = providerName.toLowerCase();
    return PROVIDER_ENDPOINTS[lower] || `https://${lower}.com/api/v1`;
};

// ─── Intelligent Model Search from Internet APIs ─────────────────────────────

interface ModelSearchResult {
    id: string;
    name: string;
    provider: string;
    providerTitle: string;
    baseUrl: string;
    description: string;
    category: string;
    downloads?: number;
    likes?: number;
    pipelineTag?: string;
}

const searchHuggingFace = async (query: string, signal?: AbortSignal): Promise<ModelSearchResult[]> => {
    try {
        const params = new URLSearchParams({
            search: query,
            limit: '20',
            sort: 'likes',
            direction: '-1'
        });
        const response = await fetch(`https://huggingface.co/api/models?${params}`, { signal });
        if (!response.ok) return [];
        const data = await response.json();
        return data.map((m: any) => ({
            id: m.id,
            name: m.id,
            provider: 'huggingface',
            providerTitle: 'HuggingFace',
            baseUrl: 'https://api-inference.huggingface.co',
            description: m.pipeline_tag || 'Machine Learning Model',
            category: m.pipeline_tag || 'General',
            downloads: m.downloads,
            likes: m.likes,
            pipelineTag: m.pipeline_tag,
        }));
    } catch (error) {
        console.warn('HuggingFace search failed:', error);
        return [];
    }
};

const searchOpenRouter = async (query: string, signal?: AbortSignal): Promise<ModelSearchResult[]> => {
    try {
        const response = await fetch('https://openrouter.ai/api/v1/models', { signal });
        if (!response.ok) return [];
        const data = await response.json();
        const models = (data.data || []) as any[];
        const lower = query.toLowerCase();
        const filtered = query
            ? models.filter((m) => m.id.toLowerCase().includes(lower) || m.name.toLowerCase().includes(lower))
            : models;
        return filtered.slice(0, 20).map((m: any) => ({
            id: m.id,
            name: m.name || m.id,
            provider: 'openrouter',
            providerTitle: 'OpenRouter',
            baseUrl: 'https://openrouter.ai/api/v1',
            description: m.description || 'AI Model via OpenRouter',
            category: 'OpenRouter',
        }));
    } catch (error) {
        console.warn('OpenRouter search failed:', error);
        return [];
    }
};

// ─── Debounced Multi-Source Search Hook ─────────────────────────────────────

const useDebouncedModelSearch = (
    delay: number = 500,
    sources: ('huggingface' | 'openrouter')[] = ['huggingface', 'openrouter']
) => {
    const [results, setResults] = useState<ModelSearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const timeoutRef = useRef<NodeJS.Timeout | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);

    const search = useCallback(async (query: string) => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        timeoutRef.current = setTimeout(async () => {
            if (!query.trim()) {
                setResults([]);
                setLoading(false);
                return;
            }

            setLoading(true);
            setError(null);
            abortControllerRef.current = new AbortController();
            const signal = abortControllerRef.current.signal;

            try {
                const promises: Array<Promise<ModelSearchResult[]>> = [];
                if (sources.includes('huggingface')) {
                    promises.push(searchHuggingFace(query, signal));
                }
                if (sources.includes('openrouter')) {
                    promises.push(searchOpenRouter(query, signal));
                }

                const allResults = await Promise.all(promises);
                const flattened = allResults.flat();
                // Deduplicate by id, keep first occurrence
                const seen = new Set<string>();
                const unique = flattened.filter(m => {
                    if (seen.has(m.id)) return false;
                    seen.add(m.id);
                    return true;
                });
                // Sort by downloads/likes if available
                unique.sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
                setResults(unique.slice(0, 30));
            } catch (err: any) {
                if (err.name !== 'AbortError') {
                    setError('Search failed. Please try again.');
                    setResults([]);
                }
            } finally {
                setLoading(false);
            }
        }, delay);
    }, [sources, delay]);

    useEffect(() => {
        return () => {
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
            if (abortControllerRef.current) abortControllerRef.current.abort();
        };
    }, []);

    return { results, loading, error, search, setResults };
};

// ─── ModelSearchSelect Component ─────────────────────────────────────────────

interface ModelSearchSelectProps {
    value?: string;
    onChange: (model: ModelSearchResult | null) => void;
    placeholder?: string;
}

const ModelSearchSelect: React.FC<ModelSearchSelectProps> = ({
    value,
    onChange,
    placeholder = "Search AI models... e.g., 'GPT-4', 'Llama', 'Claude'"
}) => {
    const [query, setQuery] = useState('');
    const [open, setOpen] = useState(false);
    const { results, loading, search, setResults } = useDebouncedModelSearch(500, ['huggingface', 'openrouter']);

    const selectedModel = React.useMemo(() => {
        if (!value) return null;
        // First check popular models, then check results
        const popular = POPULAR_MODELS.find(m => m.id === value);
        if (popular) return popular;
        // If it's a search result, we might not have full details - construct minimal
        return {
            id: value,
            name: value,
            provider: 'custom',
            providerTitle: 'Custom',
            baseUrl: getProviderEndpoint(value.split('/')[0] || ''),
            description: 'Custom or discovered model',
            category: 'Discovered',
        };
    }, [value]);

    useEffect(() => {
        if (open && query.trim()) {
            search(query);
        }
    }, [query, open, search]);

    const handleSearch = (newQuery: string) => {
        setQuery(newQuery);
        if (newQuery.trim()) {
            search(newQuery);
        } else {
            setResults([]);
        }
    };

    const handleSelect = (model: ModelSearchResult) => {
        setQuery('');
        setOpen(false);
        setResults([]);
        onChange(model);
    };

    const mergedResults = React.useMemo(() => {
        // Combine popular models with search results, deduplicate
        const popularAsModels: ModelSearchResult[] = POPULAR_MODELS.map(m => ({
            id: m.id,
            name: m.name,
            provider: m.provider,
            providerTitle: m.providerTitle,
            baseUrl: m.baseUrl,
            description: m.description,
            category: m.category,
        }));
        const all = [...popularAsModels, ...results];
        const seen = new Set<string>();
        return all.filter(m => {
            if (seen.has(m.id)) return false;
            seen.add(m.id);
            return true;
        });
    }, [results]);

    // Quick-access popular models as clickable tags
    const quickAccessModels = POPULAR_MODELS.slice(0, 8);

    return (
        <div style={{ position: 'relative' }}>
            {!value && (
                <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12, marginRight: 8 }}>Popular:</Text>
                    {quickAccessModels.map(model => (
                        <Tag
                            key={model.id}
                            style={{ cursor: 'pointer', marginBottom: 4 }}
                            color="blue"
                            onClick={() => {
                                handleSelect({
                                    id: model.id,
                                    name: model.name,
                                    provider: model.provider,
                                    providerTitle: model.providerTitle,
                                    baseUrl: model.baseUrl,
                                    description: model.description,
                                    category: model.category,
                                });
                            }}
                        >
                            {model.name}
                        </Tag>
                    ))}
                </div>
            )}
            <Select
                showSearch
                size="large"
                value={selectedModel?.id || undefined}
                placeholder={placeholder}
                onSearch={handleSearch}
                onFocus={() => setOpen(true)}
                onBlur={() => setTimeout(() => setOpen(false), 200)}
                onChange={(val) => {
                    const model = mergedResults.find(m => m.id === val) || null;
                    onChange(model);
                }}
                filterOption={false}
                style={{ width: '100%' }}
                notFoundContent={loading ? <Spin size="small" /> : (query ? 'No models found. Try a different search term.' : 'Type to search HuggingFace & OpenRouter...')}
            >
                {mergedResults.slice(0, 30).map((model) => (
                    <Select.Option key={model.id} value={model.id}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <Space>
                                <Text strong>{model.name}</Text>
                                <Tag color="blue" style={{ marginLeft: 4 }}>{model.providerTitle}</Tag>
                            </Space>
                            <Text type="secondary" style={{ fontSize: 11 }}>
                                {model.description}
                            </Text>
                            {model.downloads !== undefined && model.downloads > 0 && (
                                <Text type="secondary" style={{ fontSize: 10 }}>
                                    ⬇ {model.downloads >= 1000000 ? (model.downloads / 1000000).toFixed(1) + 'M' : model.downloads >= 1000 ? (model.downloads / 1000).toFixed(1) + 'K' : model.downloads} downloads
                                </Text>
                            )}
                        </div>
                    </Select.Option>
                ))}
            </Select>
            {loading && (
                <div style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)' }}>
                    <Spin size="small" />
                </div>
            )}
        </div>
    );
};

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
                width={editingKey ? 600 : 700}
            >
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    {editingKey ? (
                        // ── Edit mode: show all fields normally ──
                        <>
                            <Form.Item name="provider" label="Provider Name" rules={[{ required: true, message: 'Enter provider name' }]}>
                                <Input placeholder="Provider name" />
                            </Form.Item>
                            <Form.Item name="label" label="Label (optional)">
                                <Input placeholder="e.g. Production Key, Dev Key..." />
                            </Form.Item>
                            <Form.Item name="apiKey" label="API Key" rules={[{ required: true, message: 'Enter the API key' }]}>
                                <Input.Password placeholder="sk-... or key-... or your API key" />
                            </Form.Item>
                            <Form.Item name="baseUrl" label="Base URL (optional)">
                                <Input placeholder="e.g. https://api.openai.com/v1" />
                            </Form.Item>
                            <Form.Item name="models" label="Restrict to Models (optional)">
                                <Select mode="tags" placeholder="Type model names (leave empty for all)" />
                            </Form.Item>
                        </>
                    ) : (
                        // ── Add mode: intelligent AI-powered model search ──
                        <>
                            {/* Step 1: Smart Model Search — searches HuggingFace + OpenRouter in real-time */}
                            <Form.Item
                                label={
                                    <Space>
                                        <RobotOutlined />
                                        <span>Search AI Model</span>
                                        <Tooltip title="Type any model name (e.g., 'GPT-4', 'Llama 3', 'Claude') to search across HuggingFace and OpenRouter. Selecting a model auto-fills all fields below.">
                                            <QuestionCircleOutlined style={{ color: '#999' }} />
                                        </Tooltip>
                                    </Space>
                                }
                                rules={[{ required: true, message: 'Search and select an AI model' }]}
                            >
                                <ModelSearchSelect 
                                    placeholder="Search models... e.g. 'GPT-4', 'Llama 3.1', 'Claude Sonnet'"
                                    onChange={(model) => {
                                        if (model) {
                                            form.setFieldsValue({
                                                provider: model.providerTitle,
                                                baseUrl: model.baseUrl,
                                                models: [model.id],
                                                label: `${model.providerTitle} — ${model.name}`,
                                            });
                                        } else {
                                            form.setFieldsValue({
                                                provider: '',
                                                baseUrl: '',
                                                models: [],
                                                label: '',
                                            });
                                        }
                                    }}
                                />
                            </Form.Item>

                            <Alert
                                message="Intelligent Auto-Fill"
                                description="The model you selected will automatically fill the Provider, Endpoint URL, and Model Name below. You can still edit these fields if needed."
                                type="info"
                                showIcon
                                style={{ marginBottom: 16 }}
                            />

                            {/* Step 2: Paste API key */}
                            <Form.Item name="apiKey" label="API Key" rules={[{ required: true, message: 'Enter your API key' }]}>
                                <Input.Password size="large" placeholder="Paste your API key here (sk-... or key-...)" />
                            </Form.Item>

                            {/* Auto-filled fields with smart provider detection */}
                            <Row gutter={12}>
                                <Col span={12}>
                                    <Form.Item name="provider" label="Provider Name" rules={[{ required: true, message: 'Provider is required' }]}>
                                        <Input 
                                            placeholder="e.g. OpenAI, Anthropic, Google" 
                                            onChange={(e) => {
                                                const val = e.target.value;
                                                const currentBaseUrl = form.getFieldValue('baseUrl');
                                                // Only auto-fill endpoint if it's empty or matches a previous auto-fill pattern
                                                const isAutoFill = currentBaseUrl && currentBaseUrl.includes(val.toLowerCase());
                                                if (!currentBaseUrl || isAutoFill) {
                                                    const endpoint = getProviderEndpoint(val);
                                                    if (endpoint && endpoint !== `https://${val.toLowerCase()}.com/api/v1`) {
                                                        form.setFieldsValue({ baseUrl: endpoint });
                                                    }
                                                }
                                            }}
                                        />
                                    </Form.Item>
                                </Col>
                                <Col span={12}>
                                    <Form.Item name="label" label="Label (optional)">
                                        <Input placeholder="e.g. Production Key, Dev Key..." />
                                    </Form.Item>
                                </Col>
                            </Row>

                            <Form.Item name="baseUrl" label="Endpoint URL">
                                <Input 
                                    placeholder="e.g. https://api.openai.com/v1" 
                                    addonAfter={
                                        <Tooltip title="Common endpoints: OpenAI (https://api.openai.com/v1), Anthropic (https://api.anthropic.com/v1), Google (https://generativelanguage.googleapis.com/v1beta/openai)">
                                            <QuestionCircleOutlined style={{ color: '#999' }} />
                                        </Tooltip>
                                    }
                                />
                            </Form.Item>

                            {/* Model(s) field - supports multiple models per key */}
                            <Form.Item name="models" label="Allowed Models (optional)">
                                <Select mode="tags" placeholder="Leave empty for all models, or specify which models this key can use" />
                            </Form.Item>
                        </>
                    )}

                    <Form.Item style={{ marginTop: 24 }}>
                        <Space>
                            <Button type="primary" htmlType="submit" size="large">
                                {editingKey ? 'Update' : 'Save Key'}
                            </Button>
                            <Button onClick={() => { setIsModalVisible(false); setEditingKey(null); }} size="large">Cancel</Button>
                        </Space>
                    </Form.Item>
                </Form>
            </Modal>

            <ApiTestConsole
                visible={testConsoleVisible}
                onClose={() => setTestConsoleVisible(false)}
                apiKeys={keys.map(k => ({ id: k.id, label: k.label, provider: k.provider, baseUrl: k.baseUrl }))}
            />
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
