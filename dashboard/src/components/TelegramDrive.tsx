import React, { useState, useEffect } from 'react';
import { Card, Button, Input, Form, Switch, Tag, Space, Typography, Divider, Row, Col, Progress, message, Statistic, Empty, List, Badge, Tabs, Tooltip } from 'antd';
import { 
    CloudServerOutlined, 
    SyncOutlined, 
    CheckCircleOutlined, 
    ExclamationCircleOutlined,
    CloudUploadOutlined,
    RobotOutlined,
    CodeOutlined,
    BookOutlined,
    HistoryOutlined,
    DownloadOutlined,
    ThunderboltOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title } = Typography;
const { TabPane } = Tabs;

const TelegramDrive: React.FC = () => {
    const [config, setConfig] = useState<any>(null);
    const [status, setStatus] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [artifacts, setArtifacts] = useState<any[]>([]);
    const [learningArchives, setLearningArchives] = useState<any[]>([]);
    const [codebaseBackups, setCodebaseBackups] = useState<any[]>([]);
    const [uploading, setUploading] = useState(false);
    const [form] = Form.useForm();

    const [latency, setLatency] = useState<number>(0);
    const [searchUserId, setSearchUserId] = useState<string>('');
    const [chatArchives, setChatArchives] = useState<any[]>([]);
    const [fetchingArchives, setFetchingArchives] = useState(false);
    const [actionLoading, setActionLoading] = useState<string | null>(null);

    const fetchStatus = async () => {
        const start = Date.now();
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/telegram/status');
            const end = Date.now();
            setLatency(end - start);
            if (response.ok) {
                const result = await response.json();
                if (result.success) {
                    setStatus(result.data);
                }
            }
        } catch (error) {
            console.error("Failed to fetch Telegram status", error);
        }
    };

    const fetchConfig = async () => {
        try {
            // Fetch both general config and teldrive_settings
            const [configRes, teldriveRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/config'),
                authUtils.fetchWithAuth('/api/admin/firestore/system_configs/teldrive_settings')
            ]);

            if (configRes.ok) {
                const configData = await configRes.json();
                const tgConfig = configData.telegramConfig || {};
                
                if (teldriveRes.ok) {
                    const teldriveApiResponse = await teldriveRes.json();
                    const teldriveData = teldriveApiResponse.data || {};
                    
                    // Merge data into form
                    const mergedConfig = {
                        ...tgConfig,
                        teldriveUrl: teldriveData.teldriveUrl,
                        storageUsed: teldriveData.storageUsed,
                        supabaseDbUrl: teldriveData.dbUrl,
                        supabasePassword: teldriveData.password,
                        ...(teldriveData.telegram || {})
                    };
                    setConfig(mergedConfig);
                    form.setFieldsValue(mergedConfig);
                } else {
                    setConfig(tgConfig);
                    form.setFieldsValue(tgConfig);
                }
            }
        } catch (error) {
            console.error("Failed to fetch config", error);
        }
    };

    const fetchLists = async () => {
        try {
            const [artRes, learnRes, codeRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/telegram/artifacts'),
                authUtils.fetchWithAuth('/api/admin/telegram/archives/learning'),
                authUtils.fetchWithAuth('/api/admin/telegram/backups/codebase')
            ]);

            if (artRes.ok) {
                const result = await artRes.json();
                setArtifacts(result.data || []);
            }
            if (learnRes.ok) {
                const result = await learnRes.json();
                setLearningArchives(result.data || []);
            }
            if (codeRes.ok) {
                const result = await codeRes.json();
                setCodebaseBackups(result.data || []);
            }
        } catch (error) {
            console.error("Failed to fetch lists", error);
        }
    };

    useEffect(() => {
        fetchConfig();
        fetchStatus();
        fetchLists();
        const interval = setInterval(() => {
            fetchStatus();
            fetchLists();
        }, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    const handleUpdateConfig = async (values: any) => {
        setLoading(true);
        try {
            // Update Teldrive Settings in Firestore
            const teldrivePayload = {
                teldriveUrl: values.teldriveUrl,
                dbUrl: values.supabaseDbUrl,
                password: values.supabasePassword,
                storageUsed: config?.storageUsed || '0 B',
                telegram: {
                    apiId: values.apiId,
                    apiHash: values.apiHash,
                    botToken: values.botToken,
                    channelId: values.channelId
                }
            };

            const firestoreRes = await authUtils.fetchWithAuth('/api/admin/firestore/system_configs/teldrive_settings', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(teldrivePayload)
            });

if (!firestoreRes.ok) throw new Error('Firestore update failed');

             // Also update legacy config if needed
             const configRes = await authUtils.fetchWithAuth('/api/admin/config');
             const fullConfig = await configRes.json();

             fullConfig.telegramConfig = {
                 ...fullConfig.telegramConfig,
                 ...values
             };

             fullConfig.supabaseConfig = {
                 ...fullConfig.supabaseConfig,
                 dbUrl: values.supabaseDbUrl,
                 password: values.supabasePassword
             };

             const updateRes = await authUtils.fetchWithAuth('/api/admin/config', {
                 method: 'PUT',
                 headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify(fullConfig)
             });

            if (updateRes.ok) {
                message.success('Configuration synchronized to Firebase & Backend');
                fetchConfig();
                fetchStatus();
                fetchLists();
            } else {
                throw new Error('Legacy config update failed');
            }
        } catch (error) {
            message.error('Failed to update configuration');
        } finally {
            setLoading(false);
        }
    };

    const handleTriggerAction = async (action: string, endpoint: string) => {
        setActionLoading(action);
        try {
            const response = await authUtils.fetchWithAuth(`/api/admin/telegram/${endpoint}`, { method: 'POST' });
            if (response.ok) {
                const result = await response.json();
                if (result.success) {
                    message.success(`${action} triggered successfully`);
                    fetchLists();
                    fetchConfig();
                } else {
                    message.error(result.message || `${action} failed`);
                }
            }
        } catch (error) {
            message.error(`${action} request failed`);
        } finally {
            setActionLoading(null);
        }
    };

    const fetchUserArchives = async () => {
        if (!searchUserId) {
            message.warning('Please enter a User ID');
            return;
        }
        setFetchingArchives(true);
        try {
            const response = await authUtils.fetchWithAuth(`/api/admin/telegram/archives/chat/${searchUserId}`);
            if (response.ok) {
                const result = await response.json();
                if (result.success) {
                    setChatArchives(result.data || []);
                    if (result.data?.length === 0) message.info('No archives found for this user');
                }
            }
        } catch (error) {
            message.error('Failed to fetch chat archives');
        } finally {
            setFetchingArchives(false);
        }
    };

    const handleDownload = async (fileId: string) => {
        try {
            const response = await authUtils.fetchWithAuth(`/api/admin/telegram/archives/download/${fileId}`);
            if (response.ok) {
                const result = await response.json();
                if (result.success && result.data) {
                    window.open(result.data, '_blank');
                }
            }
        } catch (error) {
            message.error('Failed to get download link');
        }
    };

    const getStatusTag = () => {
        const state = status?.status || config?.status || 'UNKNOWN';
        switch (state) {
            case 'CONNECTED':
                return <Tag color="#10b981" className="border-0 bg-emerald-500/10 text-emerald-500 font-black" icon={<CheckCircleOutlined />}>CONNECTED</Tag>;
            case 'DISCONNECTED':
                return <Tag color="default" className="border-0 bg-white/5 text-white/40 font-black">DISCONNECTED</Tag>;
            case 'ERROR':
                return <Tag color="#ef4444" className="border-0 bg-red-500/10 text-red-500 font-black" icon={<ExclamationCircleOutlined />}>ERROR</Tag>;
            default:
                return <Tag color="#f59e0b" className="border-0 bg-amber-500/10 text-amber-500 font-black">UNKNOWN</Tag>;
        }
    };

    const formatSize = (bytes: number) => {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    return (
        <div className="space-y-6">
            <Row gutter={24}>
                <Col span={16}>
                    <Card 
                        className="glass-card bg-black/40 border-white/5 overflow-hidden"
                        title={
                            <Space>
                                <CloudServerOutlined className="text-emerald-500" />
                                <span className="text-white uppercase tracking-[0.2em] text-[11px] font-black">Supreme Hybrid Storage</span>
                            </Space>
                        }
                        extra={
                            <Space>
                                {getStatusTag()}
                                <Tooltip title="Trigger Sync">
                                    <Button 
                                        type="primary" 
                                        size="small" 
                                        icon={<SyncOutlined spin={actionLoading === 'sync'} />} 
                                        onClick={() => handleTriggerAction('sync', 'sync')}
                                        className="bg-emerald-500 border-0 text-[10px] font-black tracking-widest h-7"
                                    >
                                        SYNC
                                    </Button>
                                </Tooltip>
                            </Space>
                        }
                    >
                        <Row gutter={24}>
                            <Col span={8}>
                                <Statistic 
                                    title={<span className="text-white/40 text-[9px] uppercase font-bold tracking-[0.2em]">Cold Storage Used</span>}
                                    value={config?.storageUsed || '0 B'}
                                    valueStyle={{ color: '#10b981', fontWeight: 900, fontSize: '28px', letterSpacing: '-0.02em' }}
                                />
                            </Col>
                            <Col span={8}>
                                <Statistic 
                                    title={<span className="text-white/40 text-[9px] uppercase font-bold tracking-[0.2em]">Active Telegram Bot</span>}
                                    value={status?.bot_name || config?.bot_name || 'N/A'}
                                    valueStyle={{ color: '#fff', fontSize: '20px', fontWeight: 800 }}
                                    prefix={<RobotOutlined className="text-emerald-500 mr-2" />}
                                />
                            </Col>
                            <Col span={8}>
                                <Statistic 
                                    title={<span className="text-white/40 text-[9px] uppercase font-bold tracking-[0.2em]">Network Ping</span>}
                                    value={latency}
                                    suffix="ms"
                                    valueStyle={{ color: latency < 300 ? '#10b981' : '#f59e0b', fontSize: '20px', fontWeight: 800 }}
                                />
                            </Col>
                        </Row>

                        <Divider className="border-white/5 my-6" />

                        <div className="grid grid-cols-3 gap-4">
                            <Button 
                                icon={<CodeOutlined />} 
                                loading={actionLoading === 'backup'}
                                onClick={() => handleTriggerAction('backup', 'backup/codebase')}
                                className="bg-white/5 border-white/10 text-white text-[10px] font-black h-12 uppercase tracking-widest hover:bg-emerald-500/20"
                            >
                                Backup Code
                            </Button>
                            <Button 
                                icon={<HistoryOutlined />} 
                                loading={actionLoading === 'chat_archive'}
                                onClick={() => handleTriggerAction('chat_archive', 'archive/chats')}
                                className="bg-white/5 border-white/10 text-white text-[10px] font-black h-12 uppercase tracking-widest hover:bg-cyan-500/20"
                            >
                                Archive Chats
                            </Button>
                            <Button 
                                icon={<BookOutlined />} 
                                loading={actionLoading === 'learn_archive'}
                                onClick={() => handleTriggerAction('learn_archive', 'archive/learning')}
                                className="bg-white/5 border-white/10 text-white text-[10px] font-black h-12 uppercase tracking-widest hover:bg-amber-500/20"
                            >
                                Archive Learning
                            </Button>
                        </div>
                    </Card>

                    <Card className="glass-card bg-black/40 border-white/5 mt-6 p-0 overflow-hidden">
                        <Tabs defaultActiveKey="chats" className="custom-dark-tabs px-6">
                            <TabPane 
                                tab={<span className="text-[10px] font-black uppercase tracking-widest"><HistoryOutlined /> Chat Archives</span>} 
                                key="chats"
                            >
                                <div className="py-4">
                                    <Space.Compact style={{ width: '100%' }} className="mb-6">
                                        <Input 
                                            placeholder="User ID (e.g. user_2024...)" 
                                            value={searchUserId}
                                            onChange={(e) => setSearchUserId(e.target.value)}
                                            className="bg-black/60 border-white/10 text-white h-10 font-mono text-[12px]"
                                        />
                                        <Button 
                                            type="primary" 
                                            onClick={fetchUserArchives} 
                                            loading={fetchingArchives}
                                            className="bg-cyan-500 border-0 h-10 font-black text-[10px] tracking-widest px-8"
                                        >
                                            FETCH
                                        </Button>
                                    </Space.Compact>

                                    <List
                                        size="small"
                                        dataSource={chatArchives}
                                        renderItem={item => (
                                            <List.Item 
                                                className="border-white/5 px-0 py-3"
                                                actions={[
                                                    <Button 
                                                        size="small" icon={<DownloadOutlined />}
                                                        className="text-cyan-400 font-black text-[9px] uppercase tracking-widest border-cyan-400/20 bg-cyan-400/5"
                                                        onClick={() => handleDownload(item.id)}
                                                    >
                                                        DOWNLOAD
                                                    </Button>
                                                ]}
                                            >
                                                <Space direction="vertical" size={0}>
                                                    <Text className="text-white text-[11px] font-bold">{item.name}</Text>
                                                    <Text className="text-white/30 text-[9px] uppercase font-black">{formatSize(item.size)} • {new Date(item.updated_at).toLocaleString()}</Text>
                                                </Space>
                                            </List.Item>
                                        )}
                                        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<span className="text-white/10 text-[10px] font-black uppercase tracking-widest mt-4">Enter User ID</span>} /> }}
                                    />
                                </div>
                            </TabPane>
                            <TabPane 
                                tab={<span className="text-[10px] font-black uppercase tracking-widest"><BookOutlined /> Learning</span>} 
                                key="learning"
                            >
                                <div className="py-4">
                                    <List
                                        size="small"
                                        dataSource={learningArchives}
                                        renderItem={item => (
                                            <List.Item 
                                                className="border-white/5 px-0 py-3"
                                                actions={[
                                                    <Button 
                                                        size="small" icon={<DownloadOutlined />}
                                                        className="text-amber-400 font-black text-[9px] uppercase tracking-widest border-amber-400/20 bg-amber-400/5"
                                                        onClick={() => handleDownload(item.id)}
                                                    >
                                                        DOWNLOAD
                                                    </Button>
                                                ]}
                                            >
                                                <Space direction="vertical" size={0}>
                                                    <Text className="text-white text-[11px] font-bold">{item.name}</Text>
                                                    <Text className="text-white/30 text-[9px] uppercase font-black">{formatSize(item.size)} • {new Date(item.updated_at).toLocaleString()}</Text>
                                                </Space>
                                            </List.Item>
                                        )}
                                        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<span className="text-white/10 text-[10px] font-black uppercase tracking-widest mt-4">No learning archives</span>} /> }}
                                    />
                                </div>
                            </TabPane>
                            <TabPane 
                                tab={<span className="text-[10px] font-black uppercase tracking-widest"><CodeOutlined /> Code Backups</span>} 
                                key="backups"
                            >
                                <div className="py-4">
                                    <List
                                        size="small"
                                        dataSource={codebaseBackups}
                                        renderItem={item => (
                                            <List.Item 
                                                className="border-white/5 px-0 py-3"
                                                actions={[
                                                    <Button 
                                                        size="small" icon={<DownloadOutlined />}
                                                        className="text-emerald-400 font-black text-[9px] uppercase tracking-widest border-emerald-400/20 bg-emerald-400/5"
                                                        onClick={() => handleDownload(item.id)}
                                                    >
                                                        RESTORE / DOWNLOAD
                                                    </Button>
                                                ]}
                                            >
                                                <Space direction="vertical" size={0}>
                                                    <Text className="text-white text-[11px] font-bold">{item.name}</Text>
                                                    <Text className="text-white/30 text-[9px] uppercase font-black">{formatSize(item.size)} • {new Date(item.updated_at).toLocaleString()}</Text>
                                                </Space>
                                            </List.Item>
                                        )}
                                        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<span className="text-white/10 text-[10px] font-black uppercase tracking-widest mt-4">No codebase backups</span>} /> }}
                                    />
                                </div>
                            </TabPane>
                        </Tabs>
                    </Card>
                </Col>

                <Col span={8}>
                    <Card 
                        className="glass-card bg-black/40 border-white/5 h-full"
                        title={<span className="text-white uppercase tracking-[0.2em] text-[11px] font-black">Storage Configuration</span>}
                    >
                        <Form
                            form={form}
                            layout="vertical"
                            onFinish={handleUpdateConfig}
                            className="dark-form-modern"
                        >
                            <Form.Item 
                                name="enabled" 
                                label={<span className="text-white/60 text-[9px] uppercase font-black tracking-widest">Hybrid Strategy Status</span>} 
                                valuePropName="checked"
                            >
                                <Switch className="custom-switch-emerald" checkedChildren="ON" unCheckedChildren="OFF" />
                            </Form.Item>
                            <Form.Item 
                                name="teldriveUrl" 
                                label={<span className="text-white/60 text-[9px] uppercase font-black tracking-widest">Teldrive API Endpoint</span>}
                            >
                                <Input placeholder="http://teldrive-server:8080" className="bg-black/60 border-white/10 text-white font-mono text-[12px]" />
                            </Form.Item>
                            <Form.Item 
                                name="apiToken" 
                                label={<span className="text-white/60 text-[9px] uppercase font-black tracking-widest">JWT Secure Token</span>}
                            >
                                <Input.Password className="bg-black/60 border-white/10 text-white font-mono text-[12px]" />
                            </Form.Item>
                            <Form.Item 
                                name="channelId" 
                                label={<span className="text-white/60 text-[9px] uppercase font-black tracking-widest">Telegram Channel ID</span>}
                            >
                                <Input className="bg-black/60 border-white/10 text-white font-mono text-[12px]" />
                            </Form.Item>
                            
                            <Divider className="border-white/5 my-4" />
                            <Text className="text-amber-500/60 text-[8px] uppercase font-black tracking-[0.2em] block mb-4">Advanced Teldrive Credentials</Text>
                            
                            <Row gutter={12}>
                                <Col span={12}>
                                    <Form.Item 
                                        name="apiId" 
                                        label={<span className="text-white/60 text-[8px] uppercase font-black tracking-widest">API ID</span>}
                                    >
                                        <Input placeholder="123456" className="bg-black/60 border-white/10 text-white font-mono text-[11px]" />
                                    </Form.Item>
                                </Col>
                                <Col span={12}>
                                    <Form.Item 
                                        name="apiHash" 
                                        label={<span className="text-white/60 text-[8px] uppercase font-black tracking-widest">API Hash</span>}
                                    >
                                        <Input.Password className="bg-black/60 border-white/10 text-white font-mono text-[11px]" />
                                    </Form.Item>
                                </Col>
                            </Row>
                            
                            <Form.Item 
                                name="botToken" 
                                label={<span className="text-white/60 text-[8px] uppercase font-black tracking-widest">Bot Token</span>}
                            >
                                <Input.Password placeholder="123456789:ABC..." className="bg-black/60 border-white/10 text-white font-mono text-[11px]" />
                            </Form.Item>

                            <Divider className="border-white/5 my-4" />
                            <Text className="text-blue-500/60 text-[8px] uppercase font-black tracking-[0.2em] block mb-4">Supabase Database Backend</Text>

                            <Form.Item 
                                name="supabaseDbUrl" 
                                label={<span className="text-white/60 text-[8px] uppercase font-black tracking-widest">Postgres Connection URL</span>}
                            >
                                <Input placeholder="postgres://..." className="bg-black/60 border-white/10 text-white font-mono text-[11px]" />
                            </Form.Item>

                            <Form.Item 
                                name="supabasePassword" 
                                label={<span className="text-white/60 text-[8px] uppercase font-black tracking-widest">DB Password</span>}
                            >
                                <Input.Password className="bg-black/60 border-white/10 text-white font-mono text-[11px]" />
                            </Form.Item>

                            <Button 
                                type="primary" 
                                htmlType="submit" 
                                loading={loading}
                                block
                                className="bg-emerald-500 border-0 h-10 font-black uppercase tracking-[0.2em] text-[11px] mt-2"
                            >
                                UPDATE CONFIG
                            </Button>

                            <Button 
                                icon={<CloudUploadOutlined />} 
                                loading={actionLoading === 'deploy_teldrive'}
                                onClick={() => handleTriggerAction('Deploy Teldrive', 'deploy')}
                                block
                                className="bg-blue-500/10 border-blue-500/20 text-blue-400 text-[10px] font-black h-10 uppercase tracking-widest mt-3 hover:bg-blue-500/20"
                            >
                                Auto-Deploy Teldrive
                            </Button>
                        </Form>

                        <Divider className="border-white/5 my-6" />
                        
                        <div className="space-y-4">
                            <h4 className="text-white/30 text-[9px] font-black uppercase tracking-[0.3em] m-0">Live Entity Status</h4>
                            <div className="space-y-4">
                                <div className="flex justify-between items-center">
                                    <Text className="text-white/40 text-[9px] uppercase font-black tracking-widest">Bot Status</Text>
                                    <Badge status={status?.status === 'CONNECTED' ? 'success' : 'error'} text={<span className="text-white/80 text-[10px] font-bold uppercase">{status?.status || 'OFFLINE'}</span>} />
                                </div>
                                <div className="flex justify-between items-center">
                                    <Text className="text-white/40 text-[9px] uppercase font-black tracking-widest">Last Sync</Text>
                                    <Text className="text-white/80 text-[10px] font-bold">{config?.lastSync ? new Date(config.lastSync).toLocaleTimeString() : 'N/A'}</Text>
                                </div>
                                <div className="flex justify-between items-center">
                                    <Text className="text-white/40 text-[9px] uppercase font-black tracking-widest">Encryption</Text>
                                    <Tag color="blue" className="m-0 border-0 bg-blue-500/10 text-blue-400 font-black text-[9px] uppercase tracking-widest">AES-256-GCM</Tag>
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-lg">
                            <Space align="start">
                                <ThunderboltOutlined className="text-emerald-500 mt-1" />
                                <div>
                                    <Text className="text-emerald-500 text-[10px] font-black uppercase tracking-widest block mb-1">Hybrid Logic Active</Text>
                                    <Text className="text-white/40 text-[9px] font-medium leading-tight">
                                        Messages &gt; 50 are automatically sharded to Telegram. System learning is mirrored to cold storage every 12h.
                                    </Text>
                                </div>
                            </Space>
                        </div>
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

export default TelegramDrive;
;
