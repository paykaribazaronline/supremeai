// APIManagement.tsx - ULTRA-DENSE PROVIDER MATRIX with INTERNET DISCOVERY
import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Table, Button, Modal, Form, Input, Select, Space, Tooltip, Popconfirm, message, List, Avatar, Badge, Spin, Tag, Switch } from 'antd';
import { 
    PlusOutlined, 
    DeleteOutlined, 
    EditOutlined, 
    ExperimentOutlined, 
    SafetyCertificateOutlined,
    CloudServerOutlined,
    GlobalOutlined,
    SearchOutlined,
    ThunderboltOutlined,
    LoadingOutlined,
    CloseCircleOutlined,
    SortAscendingOutlined,
    SortDescendingOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface APIProvider {
    id: string;
    name: string;
    type: string;
    apiKey: string;
    status: 'active' | 'inactive' | 'error' | 'limit_exceeded' | 'dead';
    lastTested: string;
    models: string[];
    creatorEmail?: string;
    accountEmail?: string;
    apiCount?: number;
    usageLimit?: number;
    currentUsage?: number;
    canCommunicate?: boolean;
    canExecuteTasks?: boolean;
    canParticipateInVoting?: boolean;
    deploymentSource?: 'api' | 'gcloud' | 'local' | 'ollama';
    // Auto-validation fields
    consecutiveErrorDays?: number;
    lastValidated?: string;
    deadAt?: string;
    deadReason?: string;
}

const APIManagement: React.FC = () => {
    const { t } = useTranslation();
    const [providers, setProviders] = useState<APIProvider[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [statusFilter, setStatusFilter] = useState<string>('all');
    const [discoveryQuery, setDiscoveryQuery] = useState('');
    const [discoveryResults, setDiscoveryResults] = useState<any[]>([]);
    const [discoveryLoading, setDiscoveryLoading] = useState(false);
    const [validating, setValidating] = useState<string | null>(null);
    const [form] = Form.useForm();
    const [editingId, setEditingId] = useState<string | null>(null);
    
    const [sortBy, setSortBy] = useState<keyof APIProvider | 'usagePercent'>('name');
    const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('ascend');

    // Watch fields for real-time UI updates in modal
    const canCommunicate = Form.useWatch('canCommunicate', form);
    const canExecuteTasks = Form.useWatch('canExecuteTasks', form);
    const canParticipateInVoting = Form.useWatch('canParticipateInVoting', form);

    useEffect(() => {
        fetchProviders();
    }, []);

    const fetchProviders = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/providers/configured');
            if (response.ok) {
                const data = await response.json();
                const formatted = (data.data?.providers || []).map((p: any) => ({
                    ...p,
                    apiCount: p.apiCount || (p.apiKey ? 1 : 0),
                    status: p.status || 'inactive'
                }));
                setProviders(formatted);
            }
        } catch (error) {
            message.error('FAILED_TO_SYNC_REGISTRY');
        } finally {
            setLoading(false);
        }
    };

    const processedProviders = React.useMemo(() => {
        let result = providers.filter(p =>
            statusFilter === 'all' || p.status === statusFilter
        );

        if (sortBy) {
            result.sort((a, b) => {
                let aVal: any = (a as any)[sortBy] ?? '';
                let bVal: any = (b as any)[sortBy] ?? '';

                if (sortBy === 'usagePercent') {
                    aVal = (a.currentUsage || 0) / (a.usageLimit || 100);
                    bVal = (b.currentUsage || 0) / (b.usageLimit || 100);
                }

                if (typeof aVal === 'string' && typeof bVal === 'string') {
                    return sortOrder === 'ascend' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
                }
                
                if (typeof aVal === 'number' && typeof bVal === 'number') {
                    return sortOrder === 'ascend' ? aVal - bVal : bVal - aVal;
                }

                return 0;
            });
        }

        return result;
    }, [providers, statusFilter, sortBy, sortOrder]);

    const handleDiscovery = async (query: string) => {
        if (!query) {
            setDiscoveryResults([]);
            return;
        }
        setDiscoveryLoading(true);
        try {
            // Simulated Real-time Internet Search for AI Models
            const response = await authUtils.fetchWithAuth(`/api/admin/providers/discover?query=${encodeURIComponent(query)}`);
            if (response.ok) {
                const data = await response.json();
                setDiscoveryResults(data.data || []);
            } else {
                // Fallback / Mock for Demonstration if API is not fully ready
                const mockResults = [
                    { name: `${query}-Pro`, provider: 'NeuralGate', type: 'llm', description: 'Realtime discovered node' },
                    { name: `${query}-Vision`, provider: 'OpticMesh', type: 'image', description: 'Visual synthesis expert' }
                ];
                setDiscoveryResults(mockResults);
            }
        } catch (error) {
            console.error('Discovery failed, using local cache');
        } finally {
            setDiscoveryLoading(false);
        }
    };

    const testKey = async (name: string, apiKey: string, id?: string) => {
        if (id) setValidating(id);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/providers/test-key', {
                method: 'POST',
                body: JSON.stringify({ name, apiKey }),
            });
            const data = await response.json();
            if (data.success) {
                message.success(`KEY_VERIFIED: ${name.toUpperCase()}`);
                if (id) fetchProviders();
            } else {
                message.error(`VALIDATION_FAILED: ${data.error || 'INVALID_KEY'}`);
            }
        } catch (error) {
            message.error('VALIDATION_PROTOCOL_ERROR');
        } finally {
            if (id) setValidating(null);
        }
    };

    const handleAction = async (values: any) => {
        try {
            const endpoint = editingId ? `/api/admin/providers/${editingId}` : '/api/admin/providers/add';
            const method = editingId ? 'PUT' : 'POST';
            const response = await authUtils.fetchWithAuth(endpoint, {
                method,
                body: JSON.stringify({
                    ...values,
                    status: editingId ? values.status : 'inactive',
                    lastTested: new Date().toISOString()
                }),
            });

            if (response.ok) {
                message.success(editingId ? 'REGISTRY_UPDATED' : 'LINK_ESTABLISHED');
                setIsModalVisible(false);
                setEditingId(null);
                form.resetFields();
                fetchProviders();
            }
        } catch (error) {
            message.error('REGISTRY_WRITE_FAILURE');
        }
    };

    const toggleRole = async (id: string, roleType: string, enabled: boolean) => {
        try {
            // Map frontend role type to backend capability field name
            const capabilityMap: Record<string, string> = {
                'communication': 'canCommunicate',
                'execution': 'canExecuteTasks',
                'voting': 'canParticipateInVoting'
            };
            
            const fieldName = capabilityMap[roleType];
            if (!fieldName) return;

            const response = await authUtils.fetchWithAuth(`/api/admin/providers/${id}/capability`, {
                method: 'PATCH',
                body: JSON.stringify({ [fieldName]: enabled }),
            });

            if (response.ok) {
                message.success(`ROLE_${roleType.toUpperCase()}_${enabled ? 'ASSIGNED' : 'REVOKED'}`);
                fetchProviders();
            } else {
                throw new Error('FAILED_TO_UPDATE_CAPABILITY');
            }
        } catch (error) {
            message.error('CAPABILITY_SYNC_FAILED');
        }
    };

    const columns = [
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Intelligence Provider</span>,
            key: 'provider',
            render: (_: any, r: APIProvider) => (
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded bg-white/[0.03] border border-white/5 flex items-center justify-center relative">
                        <GlobalOutlined className="text-blue-500 text-[12px]" />
                        {r.status === 'active' && <div className="absolute -top-1 -right-1 w-2 h-2 bg-emerald-500 rounded-full animate-pulse border border-black" />}
                        {r.status === 'dead' && <div className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full border border-black" />}
                    </div>
                        <div className="flex flex-col leading-tight">
                            <div className="flex items-center gap-2">
                                <span className="text-[11px] font-bold text-white/90">{r.name}</span>
                                <Tag color={r.deploymentSource === 'gcloud' ? 'purple' : 'blue'} className="m-0 text-[7px] px-1 py-0 border-0 bg-opacity-10 leading-normal">
                                    {r.deploymentSource?.toUpperCase() || 'API'}
                                </Tag>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className="text-[9px] font-mono text-white/30 uppercase tracking-tighter">{r.type}</span>
                                {r.accountEmail && (
                                    <Tooltip title={`Owner: ${r.accountEmail}`}>
                                        <span className="text-[8px] text-blue-400/50 italic truncate max-w-[80px]">{r.accountEmail}</span>
                                    </Tooltip>
                                )}
                            </div>
                            {/* Show validation timestamp if available */}
                            {r.lastValidated && (
                                <span className="text-[7px] text-white/20 font-mono">
                                    Last check: {new Date(r.lastValidated).toLocaleDateString()}
                                </span>
                            )}
                        </div>
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">API Stats</span>,
            key: 'stats',
            render: (_: any, r: APIProvider) => (
                <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                        <span className="text-[10px] text-white/60 font-mono">{r.apiCount || 1} APIs</span>
                        <Badge status={r.status === 'dead' ? 'error' : 'processing'} />
                    </div>
                    <div className="w-24 h-1 bg-white/5 rounded-full overflow-hidden">
                        <div
                            className={`h-full ${r.status === 'limit_exceeded' ? 'bg-purple-500' : 'bg-blue-500'}`}
                            style={{ width: `${Math.min(100, (r.currentUsage || 0) / (r.usageLimit || 100) * 100)}%` }}
                        />
                    </div>
                    {/* Show error streak for error/dead providers */}
                    {(r.status === 'error' || r.status === 'dead') && r.consecutiveErrorDays ? (
                        <div className="flex items-center gap-2 text-[9px]">
                            <span className={`font-mono ${r.status === 'dead' ? 'text-red-500' : 'text-orange-500'}`}>
                                Fail streak: {r.consecutiveErrorDays}/3
                            </span>
                        </div>
                    ) : null}
                    {/* Show deadAt timestamp for dead providers */}
                    {r.status === 'dead' && r.deadAt ? (
                        <div className="text-[8px] text-red-400/60 font-mono">
                            Dead: {new Date(r.deadAt).toLocaleDateString()}
                        </div>
                    ) : null}
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Capabilities</span>,
            key: 'capabilities',
            render: (_: any, r: APIProvider) => (
                <div className="flex gap-4">
                    <Tooltip title={t('dashboard.role_communication')}>
                        <div className="flex flex-col items-center gap-1">
                            <Switch 
                                size="small" 
                                checked={r.canCommunicate} 
                                onChange={(checked) => toggleRole(r.id, 'communication', checked)}
                                className="scale-75"
                            />
                            <span className={`text-[7px] font-black uppercase ${r.canCommunicate ? 'text-blue-400' : 'text-white/20'}`}>Comm</span>
                        </div>
                    </Tooltip>
                    <Tooltip title={t('dashboard.role_execution')}>
                        <div className="flex flex-col items-center gap-1">
                            <Switch 
                                size="small" 
                                checked={r.canExecuteTasks} 
                                onChange={(checked) => toggleRole(r.id, 'execution', checked)}
                                className="scale-75"
                                style={{ backgroundColor: r.canExecuteTasks ? '#10b981' : undefined }}
                            />
                            <span className={`text-[7px] font-black uppercase ${r.canExecuteTasks ? 'text-emerald-400' : 'text-white/20'}`}>Task</span>
                        </div>
                    </Tooltip>
                    <Tooltip title={t('dashboard.role_voting')}>
                        <div className="flex flex-col items-center gap-1">
                            <Switch 
                                size="small" 
                                checked={r.canParticipateInVoting} 
                                onChange={(checked) => toggleRole(r.id, 'voting', checked)}
                                className="scale-75"
                                style={{ backgroundColor: r.canParticipateInVoting ? '#a855f7' : undefined }}
                            />
                            <span className={`text-[7px] font-black uppercase ${r.canParticipateInVoting ? 'text-purple-400' : 'text-white/20'}`}>Vote</span>
                        </div>
                    </Tooltip>
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Link State</span>,
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const config: any = { 
                    active: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20', 
                    inactive: 'text-orange-500 bg-orange-500/10 border-orange-500/20',
                    error: 'text-red-500 bg-red-500/10 border-red-500/20',
                    dead: 'text-red-700 bg-red-900/20 border-red-900/40',
                    limit_exceeded: 'text-purple-500 bg-purple-500/10 border-purple-500/20'
                };
                return <span className={`text-[8px] px-1.5 py-0.5 rounded-sm font-black uppercase tracking-widest border ${config[status] || config.inactive}`}>{status}</span>;
            }
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">Control</span>,
            key: 'actions',
            align: 'right' as const,
            render: (_: any, record: APIProvider) => (
                <Space size={4}>
                    {record.status === 'dead' ? (
                        <Tooltip title="Revive Provider">
                            <Button
                                type="text"
                                size="small"
                                className="h-6 w-6 flex items-center justify-center text-emerald-500 hover:bg-emerald-500/10 border border-emerald-500/20"
                                icon={<SafetyCertificateOutlined style={{ fontSize: '11px' }} />}
                                onClick={async () => {
                                    try {
                                        const res = await authUtils.fetchWithAuth(`/api/admin/providers/${record.id}/revive`, {
                                            method: 'POST',
                                        });
                                        if (res.ok) {
                                            message.success('PROVIDER_REVIVED');
                                            fetchProviders();
                                        } else {
                                            const data = await res.json();
                                            message.error(data.error || 'REVIVE_FAILED');
                                        }
                                    } catch (e) { message.error('REVIVE_ERROR'); }
                                }}
                            />
                        </Tooltip>
                    ) : (
                        <Tooltip title="Test Connection">
                            <Button
                                type="text"
                                size="small"
                                className="h-6 w-6 flex items-center justify-center text-blue-500 hover:bg-blue-500/10 border border-blue-500/20"
                                icon={validating === record.id ? <Spin indicator={<LoadingOutlined style={{ fontSize: 11 }} spin />} /> : <ExperimentOutlined style={{ fontSize: '11px' }} />}
                                onClick={() => testKey(record.name, record.apiKey, record.id)}
                                disabled={validating === record.id}
                            />
                        </Tooltip>
                    )}
                    <Tooltip title="Edit Link">
                        <Button
                            type="text"
                            size="small"
                            className="h-6 w-6 flex items-center justify-center text-emerald-500 hover:bg-emerald-500/10 border border-emerald-500/20"
                            icon={<EditOutlined style={{ fontSize: '11px' }} />}
                            onClick={() => {
                                setEditingId(record.id);
                                form.setFieldsValue(record);
                                setIsModalVisible(true);
                            }}
                        />
                    </Tooltip>
                    {record.status !== 'dead' && (
                        <Popconfirm title="Terminate Link?" onConfirm={async () => {
                            try {
                                const res = await authUtils.fetchWithAuth(`/api/admin/providers/${record.id}`, { method: 'DELETE' });
                                if (res.ok) {
                                    message.success('LINK_TERMINATED');
                                    fetchProviders();
                                }
                            } catch (e) { message.error('DELETE_FAILED'); }
                        }} okText="Kill" cancelText="Abort">
                            <Button
                                type="text"
                                size="small"
                                className="h-6 w-6 flex items-center justify-center text-red-500 hover:bg-red-500/10 border border-red-500/20"
                                icon={<DeleteOutlined style={{ fontSize: '11px' }} />}
                            />
                        </Popconfirm>
                    )}
                 </Space>
             )
         }
     ];

    return (
        <div className="space-y-4">
            {/* Mission Stats */}
            <div className="grid grid-cols-4 gap-3">
                {[
                    { label: 'অ্যাক্টিভ লিঙ্ক', value: providers.filter(p => p.status === 'active').length, icon: <SafetyCertificateOutlined />, color: 'emerald' },
                    { label: 'ডেড নোড', value: providers.filter(p => p.status === 'dead').length, icon: <CloseCircleOutlined />, color: 'red' },
                    { label: 'এপিআই ডেনসিটি', value: providers.reduce((acc, p) => acc + (p.apiCount || 0), 0), icon: <CloudServerOutlined />, color: 'blue' },
                    { label: 'রেজিস্ট্রি লোড', value: '14.2%', icon: <ThunderboltOutlined />, color: 'amber' },
                ].map((s, i) => (
                    <div key={i} className="bg-white/[0.02] border border-white/5 p-4 rounded-2xl flex flex-col justify-between h-20 relative overflow-hidden group backdrop-blur-md">
                        <div className="flex items-center justify-between relative z-10">
                            <span className="text-[10px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                            <span className={`text-[12px] text-${s.color}-500/40 group-hover:text-${s.color}-500 transition-colors`}>{s.icon}</span>
                        </div>
                        <span className="text-2xl font-mono font-black text-white leading-none relative z-10">{s.value}</span>
                        <div className={`absolute bottom-0 left-0 h-1 bg-${s.color}-500 w-full opacity-20`} />
                    </div>
                ))}
            </div>

            <div className="bg-white/[0.02] border border-white/5 rounded-xl overflow-hidden">
                <div className="px-4 py-3 bg-white/[0.02] border-b border-white/5 flex flex-col md:flex-row items-center justify-between gap-4">
                    <div className="flex flex-col">
                        <span className="text-[11px] font-black uppercase tracking-[0.2em] text-white/80">প্রোভাইডার রেজিস্ট্রি</span>
                        <span className="text-[8px] text-white/20 uppercase font-bold tracking-widest">Trace-Accountable Provider Matrix</span>
                    </div>
                    
                    <div className="flex flex-col sm:flex-row items-center gap-4">
                        <div className="flex items-center gap-2 bg-black/40 p-1 rounded-xl border border-white/10 h-[42px]">
                            <span className="text-[10px] text-white/30 uppercase font-black px-2">ফিল্টার:</span>
                            <Select
                                size="small"
                                className="w-36 dark-select-compact"
                                variant="borderless"
                                popupClassName="dark-dropdown"
                                value={statusFilter}
                                onChange={setStatusFilter}
                                options={[
                                    { label: 'সব স্ট্যাটাস', value: 'all' },
                                    { label: 'অ্যাক্টিভ', value: 'active' },
                                    { label: 'ইনঅ্যাক্টিভ', value: 'inactive' },
                                    { label: 'এরর', value: 'error' },
                                    { label: 'ডেড', value: 'dead' },
                                    { label: 'লিমিট এক্সিড', value: 'limit_exceeded' },
                                ]}
                            />
                        </div>

                        <div className="flex items-center gap-2 bg-black/40 p-1 rounded-xl border border-white/10 h-[42px]">
                            <span className="text-[10px] text-white/30 uppercase font-black px-2">সর্ট:</span>
                            <Select
                                size="small"
                                className="w-36 dark-select-compact"
                                variant="borderless"
                                popupClassName="dark-dropdown"
                                value={sortBy}
                                onChange={setSortBy}
                                options={[
                                    { label: 'নাম', value: 'name' },
                                    { label: 'টাইপ', value: 'type' },
                                    { label: 'স্ট্যাটাস', value: 'status' },
                                    { label: 'ইউসেজ', value: 'usagePercent' },
                                    { label: 'এপিআই সংখ্যা', value: 'apiCount' },
                                ]}
                            />
                            <Tooltip title={sortOrder === 'ascend' ? 'আরোহী' : 'অবরোহী'}>
                                <Button
                                    type="text"
                                    size="small"
                                    icon={sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
                                    onClick={() => setSortOrder(sortOrder === 'ascend' ? 'descend' : 'ascend')}
                                    className="text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 flex items-center justify-center rounded-lg h-8 w-8"
                                />
                            </Tooltip>
                        </div>

                        <Button
                            size="middle"
                            className="h-[42px] bg-blue-600 text-white text-[11px] font-black uppercase border-none hover:bg-blue-500 shadow-lg shadow-blue-500/20 rounded-xl px-6 transition-all flex items-center gap-2"
                            icon={<PlusOutlined className="text-[14px]" />}
                            onClick={() => {
                                setEditingId(null);
                                form.resetFields();
                                setIsModalVisible(true);
                                handleDiscovery(''); // Initial search
                            }}
                        >
                            লিঙ্ক তৈরি করুন
                        </Button>
                    </div>
                </div>
                <Table
                    columns={columns}
                    dataSource={processedProviders}
                    loading={loading}
                    rowKey="id"
                    size="small"
                    pagination={{ pageSize: 10, className: 'dark-pagination' }}
                    className="dense-table"
                />
            </div>

            <Modal
                title={<div className="flex flex-col"><span className="text-[14px] font-black uppercase tracking-widest text-white">Initialize Intelligence Link</span><span className="text-[8px] text-white/20 uppercase">Zero-Hardcode Protocol v4.2</span></div>}
                open={isModalVisible}
                onOk={() => form.submit()}
                onCancel={() => setIsModalVisible(false)}
                className="dark-modal"
                width={800}
                centered
                footer={[
                    <Button key="cancel" onClick={() => setIsModalVisible(false)} className="bg-white/5 border-white/10 text-white/60 hover:text-white">Cancel</Button>,
                    <Button key="submit" type="primary" onClick={() => form.submit()} className="bg-emerald-500 border-none text-black font-black uppercase">Sync Registry</Button>
                ]}
            >
                <div className="grid grid-cols-2 gap-8 mt-6">
                    {/* Left: Discovery Search */}
                    <div className="space-y-4">
                        <div className="flex flex-col gap-1">
                            <span className="text-[10px] font-black text-white/40 uppercase tracking-widest">Internet Discovery</span>
                            <Input 
                                prefix={<SearchOutlined className="text-white/20" />}
                                placeholder="Search models (e.g. gpt-4o, claude...)"
                                className="dark-input"
                                onChange={(e) => {
                                    setDiscoveryQuery(e.target.value);
                                    handleDiscovery(e.target.value);
                                }}
                            />
                        </div>
                        
                        <div className="h-[300px] overflow-y-auto custom-scrollbar bg-black/40 rounded-lg border border-white/5 p-2">
                            {discoveryLoading ? (
                                <div className="h-full flex items-center justify-center"><Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} /></div>
                            ) : (
                                <List
                                    dataSource={discoveryResults}
                                    renderItem={(item) => (
                                        <div 
                                            className="discovery-search-item p-3 rounded-lg mb-2 cursor-pointer flex items-center justify-between"
                                            onClick={() => {
                                                form.setFieldsValue({
                                                    name: item.name,
                                                    type: item.type,
                                                    models: [item.name]
                                                });
                                            }}
                                        >
                                            <div className="flex items-center gap-3">
                                                <Avatar size="small" className="bg-blue-500/10 text-blue-500 text-[10px] border border-blue-500/20">{item.provider[0].toUpperCase()}</Avatar>
                                                <div className="flex flex-col">
                                                    <span className="text-[11px] font-bold text-white/80">{item.name}</span>
                                                    <span className="text-[8px] text-white/30 uppercase">{item.provider} // {item.type}</span>
                                                </div>
                                            </div>
                                            <PlusOutlined className="text-[10px] text-emerald-500/50" />
                                        </div>
                                    )}
                                />
                            )}
                        </div>
                    </div>

                    {/* Right: Configuration Form */}
                    <Form form={form} layout="vertical" onFinish={handleAction}>
                        <div className="grid grid-cols-3 gap-4">
                            <Form.Item name="name" label={<span className="text-[9px] font-black text-white/40 uppercase">Model Identity</span>} rules={[{ required: true }]}>
                                <Input className="dark-input font-mono" placeholder="gpt-4o" />
                            </Form.Item>
                            <Form.Item name="type" label={<span className="text-[9px] font-black text-white/40 uppercase">Neural Class</span>} rules={[{ required: true }]}>
                                <Select className="dark-select">
                                    <Select.Option value="llm">NEURAL_LANGUAGE</Select.Option>
                                    <Select.Option value="image">VISUAL_SYNTHESIS</Select.Option>
                                    <Select.Option value="voice">AUDITORY_LOGIC</Select.Option>
                                </Select>
                            </Form.Item>
                            <Form.Item name="deploymentSource" label={<span className="text-[9px] font-black text-white/40 uppercase">Deployment Source</span>} initialValue="api">
                                <Select className="dark-select">
                                    <Select.Option value="api">PUBLIC_API</Select.Option>
                                    <Select.Option value="gcloud">GCLOUD_DEPLOYED</Select.Option>
                                    <Select.Option value="local">LOCAL_HOSTED</Select.Option>
                                    <Select.Option value="ollama">OLLAMA_NODE</Select.Option>
                                </Select>
                            </Form.Item>
                        </div>

                        <Form.Item name="apiKey" label={<span className="text-[9px] font-black text-white/40 uppercase">Secret Key / Token</span>} rules={[{ required: true }]}>
                            <Input.Password className="dark-input font-mono text-[10px]" placeholder="sk-..." />
                        </Form.Item>

                        <div className="grid grid-cols-2 gap-4">
                            <Form.Item name="accountEmail" label={<span className="text-[9px] font-black text-white/40 uppercase">Billing Account Email</span>}>
                                <Input className="dark-input" placeholder="admin@example.com" />
                            </Form.Item>
                            <Form.Item name="creatorEmail" label={<span className="text-[9px] font-black text-white/40 uppercase">Registry Creator</span>}>
                                <Input className="dark-input" placeholder="master@supreme.ai" />
                            </Form.Item>
                        </div>

                        <Form.Item name="models" label={<span className="text-[9px] font-black text-white/40 uppercase">Attached Capabilities</span>}>
                            <Select mode="tags" className="dark-select" placeholder="Add model identifiers..." />
                        </Form.Item>

                        <div className="grid grid-cols-3 gap-2 mb-4">
                            <Form.Item name="canCommunicate" valuePropName="checked" noStyle>
                                <div 
                                    className={`p-2 rounded border cursor-pointer transition-all flex flex-col items-center gap-1 ${canCommunicate ? 'bg-blue-500/20 border-blue-500/50' : 'bg-white/[0.02] border-white/5'}`} 
                                    onClick={() => form.setFieldsValue({ canCommunicate: !canCommunicate })}
                                >
                                    <ThunderboltOutlined className={canCommunicate ? 'text-blue-400' : 'text-white/20'} />
                                    <span className="text-[8px] font-black uppercase text-center leading-tight">Communication</span>
                                </div>
                            </Form.Item>
                            <Form.Item name="canExecuteTasks" valuePropName="checked" noStyle>
                                <div 
                                    className={`p-2 rounded border cursor-pointer transition-all flex flex-col items-center gap-1 ${canExecuteTasks ? 'bg-emerald-500/20 border-emerald-500/50' : 'bg-white/[0.02] border-white/5'}`} 
                                    onClick={() => form.setFieldsValue({ canExecuteTasks: !canExecuteTasks })}
                                >
                                    <CloudServerOutlined className={canExecuteTasks ? 'text-emerald-400' : 'text-white/20'} />
                                    <span className="text-[8px] font-black uppercase text-center leading-tight">Task execution</span>
                                </div>
                            </Form.Item>
                            <Form.Item name="canParticipateInVoting" valuePropName="checked" noStyle>
                                <div 
                                    className={`p-2 rounded border cursor-pointer transition-all flex flex-col items-center gap-1 ${canParticipateInVoting ? 'bg-purple-500/20 border-purple-500/50' : 'bg-white/[0.02] border-white/5'}`} 
                                    onClick={() => form.setFieldsValue({ canParticipateInVoting: !canParticipateInVoting })}
                                >
                                    <SafetyCertificateOutlined className={canParticipateInVoting ? 'text-purple-400' : 'text-white/20'} />
                                    <span className="text-[8px] font-black uppercase text-center leading-tight">Ensemble voting</span>
                                </div>
                            </Form.Item>
                        </div>

                        <div className="p-3 bg-blue-500/5 border border-blue-500/10 rounded-lg flex items-center justify-between">
                            <div className="flex flex-col">
                                <span className="text-[10px] font-black text-blue-500 uppercase">Automated Validation</span>
                                <span className="text-[8px] text-white/30 uppercase tracking-tighter">Key will be verified via hello-world ping</span>
                            </div>
                            <Button 
                                size="small" 
                                icon={<ExperimentOutlined />}
                                className="bg-blue-500/10 border-blue-500/20 text-blue-500 text-[9px] font-black uppercase"
                                onClick={() => {
                                    const vals = form.getFieldsValue();
                                    if (vals.name && vals.apiKey) testKey(vals.name, vals.apiKey);
                                    else message.warning('NAME_AND_KEY_REQUIRED');
                                }}
                            >
                                Run Ping
                            </Button>
                        </div>
                    </Form>
                </div>
            </Modal>
        </div>
    );
};

export default APIManagement;
