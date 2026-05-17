import React, { useState, useEffect } from 'react';
import { Typography, Progress, Button, Space, Card, Tag, Empty, message, Select, Badge, Divider, Avatar, Input, Table, List, Tooltip } from 'antd';
import { 
    BulbOutlined, 
    RocketOutlined, 
    ExperimentOutlined, 
    HistoryOutlined, 
    SyncOutlined, 
    ThunderboltOutlined, 
    SafetyCertificateOutlined,
    SearchOutlined,
    LinkOutlined,
    AlertOutlined,
    DatabaseOutlined,
    GlobalOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title, Paragraph } = Typography;
const { Option } = Select;

interface Suggestion {
    type: string;
    message: string;
    category?: string;
    score: number;
}

const LearningHub: React.FC = () => {
    const [status, setStatus] = useState<any>(null);
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);

    // Phase 2 state variables
    const [loopHealth, setLoopHealth] = useState<any>(null);
    const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
    const [testQuery, setTestQuery] = useState('');
    const [testResult, setTestResult] = useState<any>(null);
    const [evalUrl, setEvalUrl] = useState('');
    const [evalResult, setEvalResult] = useState<any>(null);
    const [gapTaskType, setGapTaskType] = useState('code_generation');
    const [gapResult, setGapResult] = useState<any>(null);
    const [activeSection, setActiveSection] = useState<'metrics' | 'suggestions' | 'tester'>('metrics');

    useEffect(() => {
        fetchData();
        fetchLoopData();
        const interval = setInterval(() => {
            fetchData();
            fetchLoopData();
        }, 30000); // Sync every 30s
        return () => clearInterval(interval);
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [statusRes, statsRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/learning/status'),
                authUtils.fetchWithAuth('/api/system-learning/stats')
            ]);
            
            if (statusRes.ok) setStatus(await statusRes.json());
            if (statsRes.ok) setStats(await statsRes.json());
        } catch (error) {
            console.error('Failed to fetch learning data:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchLoopData = async () => {
        try {
            const [healthRes, sugRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/learning-loop/health'),
                authUtils.fetchWithAuth('/api/admin/learning-loop/suggestions')
            ]);
            
            if (healthRes.ok) setLoopHealth(await healthRes.json());
            if (sugRes.ok) {
                const data = await sugRes.json();
                if (data.status === 'ok') {
                    setSuggestions(data.modelGapSuggestions || []);
                }
            }
        } catch (error) {
            console.error('Failed to fetch learning loop phase 2 data:', error);
        }
    };

    const handleModeChange = async (mode: string) => {
        setActionLoading(true);
        try {
            const res = await authUtils.fetchWithAuth('/api/admin/learning/mode', {
                method: 'POST',
                body: JSON.stringify({ mode })
            });
            if (res.ok) {
                message.success(`LEARNING_MODE_SET: ${mode}`);
                fetchData();
            }
        } catch (e) {
            message.error('FAILED_TO_UPDATE_MODE');
        } finally {
            setActionLoading(false);
        }
    };

    const triggerImprovement = async () => {
        setActionLoading(true);
        message.loading({ content: 'INITIATING_NEURAL_IMPROVEMENT_CYCLE...', key: 'improve' });
        try {
            const res = await authUtils.fetchWithAuth('/api/system-learning/trigger-improvement', {
                method: 'POST'
            });
            if (res.ok) {
                message.success({ content: 'IMPROVEMENT_CYCLE_COMPLETE', key: 'improve' });
                fetchData();
            }
        } catch (e) {
            message.error({ content: 'IMPROVEMENT_PROTOCOL_FAILED', key: 'improve' });
        } finally {
            setActionLoading(false);
        }
    };

    // Phase 2 actions
    const testIntent = async () => {
        if (!testQuery.trim()) {
            message.warning('Please enter a query to test intent.');
            return;
        }
        setActionLoading(true);
        try {
            const res = await authUtils.fetchWithAuth('/api/admin/learning-loop/test-intent', {
                method: 'POST',
                body: JSON.stringify({ query: testQuery })
            });
            if (res.ok) {
                const data = await res.json();
                setTestResult(data);
                message.success('Intent classified successfully.');
            }
        } catch (e) {
            message.error('Failed to test intent classification.');
        } finally {
            setActionLoading(false);
        }
    };

    const evaluateLink = async () => {
        if (!evalUrl.trim()) {
            message.warning('Please enter a model URL (HuggingFace/GitHub).');
            return;
        }
        setActionLoading(true);
        message.loading({ content: 'Analyzing model parameters & architecture...', key: 'eval' });
        try {
            const res = await authUtils.fetchWithAuth('/api/admin/learning-loop/evaluate-link', {
                method: 'POST',
                body: JSON.stringify({ url: evalUrl })
            });
            if (res.ok) {
                const data = await res.json();
                setEvalResult(data);
                message.success({ content: 'Model evaluation completed!', key: 'eval' });
            }
        } catch (e) {
            message.error({ content: 'Model URL evaluation failed.', key: 'eval' });
        } finally {
            setActionLoading(false);
        }
    };

    const runGapAnalysis = async () => {
        setActionLoading(true);
        message.loading({ content: 'Running capability coverage check...', key: 'gap' });
        try {
            const res = await authUtils.fetchWithAuth('/api/admin/learning-loop/gap-analysis', {
                method: 'POST',
                body: JSON.stringify({ taskType: gapTaskType })
            });
            if (res.ok) {
                const data = await res.json();
                setGapResult(data);
                message.success({ content: 'Gap analysis reports updated!', key: 'gap' });
            }
        } catch (e) {
            message.error({ content: 'Gap analysis failed.', key: 'gap' });
        } finally {
            setActionLoading(false);
        }
    };

    const reloadKnowledge = async () => {
        setActionLoading(true);
        try {
            const res = await authUtils.fetchWithAuth('/api/admin/learning-loop/reload', {
                method: 'POST'
            });
            if (res.ok) {
                message.success('Knowledge Base reloaded from core_knowledge.json');
                fetchLoopData();
            }
        } catch (e) {
            message.error('Failed to reload Knowledge Base.');
        } finally {
            setActionLoading(false);
        }
    };

    const correctionsColumns = [
        {
            title: 'ORIGINAL INTENT',
            dataIndex: 'originalIntent',
            key: 'originalIntent',
            render: (text: string) => <Tag color="blue" className="font-mono text-[9px] uppercase">{text || 'N/A'}</Tag>
        },
        {
            title: 'CORRECTED HUB',
            dataIndex: 'correctedHub',
            key: 'correctedHub',
            render: (text: string) => <Tag color="emerald" className="font-mono text-[9px] uppercase">{text}</Tag>
        },
        {
            title: 'FEEDBACK TEXT',
            dataIndex: 'feedbackText',
            key: 'feedbackText',
            render: (text: string) => <span className="text-[10px] text-white/70 italic">"{text}"</span>
        },
        {
            title: 'TIMESTAMP',
            dataIndex: 'timestamp',
            key: 'timestamp',
            render: (time: any) => <span className="text-[9px] text-white/40 font-mono">{time ? new Date(time).toLocaleString() : 'Just Now'}</span>
        }
    ];

    return (
        <div className="p-4 animate-fade-in space-y-6">
            {/* Header tab navigation */}
            <div className="flex gap-4 border-b border-white/5 pb-4">
                <Button 
                    type={activeSection === 'metrics' ? 'primary' : 'text'}
                    icon={<ExperimentOutlined />}
                    onClick={() => setActiveSection('metrics')}
                    className={`font-black text-[11px] uppercase tracking-wider h-10 ${activeSection === 'metrics' ? 'bg-emerald-500 text-black' : 'text-white/60'}`}
                >
                    Orchestrator Stats
                </Button>
                <Button 
                    type={activeSection === 'suggestions' ? 'primary' : 'text'}
                    icon={<BulbOutlined />}
                    onClick={() => setActiveSection('suggestions')}
                    className={`font-black text-[11px] uppercase tracking-wider h-10 ${activeSection === 'suggestions' ? 'bg-emerald-500 text-black' : 'text-white/60'}`}
                >
                    Suggestions & Gaps ({suggestions.length})
                </Button>
                <Button 
                    type={activeSection === 'tester' ? 'primary' : 'text'}
                    icon={<SearchOutlined />}
                    onClick={() => setActiveSection('tester')}
                    className={`font-black text-[11px] uppercase tracking-wider h-10 ${activeSection === 'tester' ? 'bg-emerald-500 text-black' : 'text-white/60'}`}
                >
                    Intent Tester
                </Button>
            </div>

            {activeSection === 'metrics' && (
                <div className="space-y-6">
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Learning Control Center */}
                        <div className="glass-card p-6 border border-white/5 bg-black/40 lg:col-span-1">
                            <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                                <SyncOutlined className="text-emerald-500" spin={status?.learningAllowed} /> Intelligence Control
                            </Title>
                            
                            <div className="space-y-6">
                                <div className="p-4 bg-white/[0.03] border border-white/10 rounded-xl">
                                    <span className="text-[9px] text-white/30 uppercase block mb-3 font-black tracking-widest">Active Strategy</span>
                                    <Select 
                                        value={status?.mode || 'BALANCED'} 
                                        className="dark-select w-full"
                                        onChange={handleModeChange}
                                        disabled={actionLoading}
                                    >
                                        <Option value="AGGRESSIVE">AGGRESSIVE_AUTO</Option>
                                        <Option value="BALANCED">BALANCED_LEARN</Option>
                                        <Option value="MANUAL">MANUAL_ONLY</Option>
                                        <Option value="PAUSED">PAUSED</Option>
                                    </Select>
                                    <p className="text-[9px] text-emerald-500/60 mt-3 uppercase italic leading-relaxed">
                                        {status?.modeDescription || 'System is operating in standard balanced mode.'}
                                    </p>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div className="p-3 bg-white/[0.02] border border-white/5 rounded-lg">
                                        <span className="text-[8px] text-white/20 uppercase block font-black">Daily Quota</span>
                                        <div className="text-lg font-mono font-black text-white">
                                            {status?.quota?.globalUsage || 0}<span className="text-white/20">/</span>{status?.quota?.globalDailyMax || 1000}
                                        </div>
                                    </div>
                                    <div className="p-3 bg-white/[0.02] border border-white/5 rounded-lg">
                                        <span className="text-[8px] text-white/20 uppercase block font-black">Status</span>
                                        <Tag color={status?.emergencyPaused ? 'red' : 'emerald'} className="m-0 text-[8px] font-black uppercase border-0 bg-opacity-10">
                                            {status?.emergencyPaused ? 'PAUSED' : 'OPTIMAL'}
                                        </Tag>
                                    </div>
                                </div>

                                <Button 
                                    block 
                                    type="primary"
                                    icon={<ThunderboltOutlined />} 
                                    className="h-12 bg-emerald-500 text-black font-black uppercase tracking-widest text-[10px] hover:bg-emerald-400 border-none shadow-[0_0_20px_rgba(16,185,129,0.2)]"
                                    onClick={triggerImprovement}
                                    loading={actionLoading}
                                >
                                    Trigger Improvement Cycle
                                </Button>
                            </div>
                        </div>

                        {/* Performance & Metrics */}
                        <div className="glass-card p-6 border border-white/5 bg-black/40 lg:col-span-2">
                            <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                                <ExperimentOutlined className="text-purple-500" /> Neural Metrics & Knowledge Base
                            </Title>
                            
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div className="space-y-4">
                                    <div className="p-5 bg-white/[0.03] border border-white/10 rounded-2xl">
                                        <div className="flex justify-between items-start mb-4">
                                            <div>
                                                <h4 className="text-white font-black text-[12px] uppercase m-0">Knowledge Acquisition</h4>
                                                <span className="text-[9px] text-white/30 uppercase tracking-tighter">Verified Facts in Registry</span>
                                            </div>
                                            <Badge status="processing" color="#10b981" />
                                        </div>
                                        <div className="text-3xl font-mono font-black text-white mb-2">
                                            {stats?.learningCount || 0}
                                        </div>
                                        <Progress 
                                            percent={Math.min(100, ((stats?.learningCount || 0) / 10000) * 100)} 
                                            showInfo={false}
                                            strokeColor="#10b981" 
                                            trailColor="rgba(255,255,255,0.05)"
                                            className="mb-0"
                                        />
                                        <span className="text-[8px] text-white/20 uppercase font-bold mt-1 block">Target: 10,000 Nodes (V2 Baseline)</span>
                                    </div>

                                    <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <SafetyCertificateOutlined className="text-blue-500" />
                                            <span className="text-[10px] text-white/60 uppercase font-black">Confidence Level</span>
                                        </div>
                                        <span className="text-[14px] text-blue-400 font-mono font-black">94.8%</span>
                                    </div>
                                </div>

                                <div className="space-y-4">
                                     <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl">
                                        <span className="text-[10px] font-black uppercase text-white/80 block mb-4 border-b border-white/5 pb-2">Top Learning Categories</span>
                                        <div className="space-y-3">
                                            {stats?.categoryStats ? Object.entries(stats.categoryStats).map(([cat, val]: any) => (
                                                <div key={cat} className="flex items-center justify-between">
                                                    <span className="text-[9px] text-white/40 uppercase font-mono">{cat}</span>
                                                    <span className="text-[10px] text-white font-bold">{val}</span>
                                                </div>
                                            )) : (
                                                <div className="py-8 text-center text-[9px] text-white/10 uppercase italic">Awaiting categorization...</div>
                                            )}
                                        </div>
                                     </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    {/* Active Training Nodes & Phase 2 health summary */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="glass-card p-6 border border-white/5 bg-black/40 flex flex-col justify-between">
                            <div>
                                <span className="text-[9px] font-black text-amber-500 uppercase tracking-widest block mb-2">Phase 2 Learning Loop</span>
                                <Title level={5} className="!text-white !text-[14px] uppercase m-0 mb-4">Integrity & Loop Health</Title>
                                <div className="space-y-2 mb-4">
                                    <div className="flex justify-between">
                                        <span className="text-[10px] text-white/40">Total Corrections</span>
                                        <span className="text-[11px] text-white font-black font-mono">{loopHealth?.totalCorrections || 0}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-[10px] text-white/40">Knowledge Base state</span>
                                        <span className="text-[11px] text-emerald-400 font-black uppercase">CONNECTED</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-[10px] text-white/40">Vector Taxonomy Size</span>
                                        <span className="text-[11px] text-white font-black font-mono">600+ Intents</span>
                                    </div>
                                </div>
                            </div>
                            <Button 
                                type="primary" 
                                ghost 
                                icon={<SyncOutlined />} 
                                onClick={reloadKnowledge} 
                                loading={actionLoading}
                                className="h-9 text-[10px] font-black uppercase tracking-wider hover:bg-emerald-500/10 hover:text-emerald-400 border-white/10"
                            >
                                Reload Core Knowledge JSON
                            </Button>
                        </div>

                        <div className="glass-card p-6 border border-white/5 bg-black/40 md:col-span-2">
                            <span className="text-[9px] font-black text-emerald-500 uppercase tracking-widest block mb-2">Intent Distribution Map</span>
                            <Title level={5} className="!text-white !text-[14px] uppercase m-0 mb-4">Hub-Based Classification Routing</Title>
                            
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                                {loopHealth?.hubDistribution ? Object.entries(loopHealth.hubDistribution).map(([hub, count]: any) => (
                                    <div key={hub} className="p-3 bg-white/[0.02] border border-white/5 rounded-lg text-center">
                                        <span className="text-[8px] text-white/30 uppercase font-black block truncate">{hub}</span>
                                        <span className="text-lg font-mono font-black text-white">{count}</span>
                                        <span className="text-[8px] text-emerald-500/60 block mt-1">Hits Routed</span>
                                    </div>
                                )) : (
                                    <div className="col-span-4 py-8 text-center text-[10px] text-white/20 uppercase italic">
                                        No active routing logs recorded. Start testing to see metrics!
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Correction History */}
                    {loopHealth?.recentCorrections && loopHealth.recentCorrections.length > 0 && (
                        <div className="glass-card p-6 border border-white/5 bg-black/40">
                            <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                                <HistoryOutlined className="text-amber-500" /> Active System Adaptation Log
                            </Title>
                            <Table 
                                dataSource={loopHealth.recentCorrections} 
                                columns={correctionsColumns} 
                                pagination={{ pageSize: 5 }}
                                className="dark-table"
                                rowKey={(record: any) => record.timestamp || Math.random().toString()}
                            />
                        </div>
                    )}
                </div>
            )}

            {activeSection === 'suggestions' && (
                <div className="space-y-6">
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Suggestions List */}
                        <div className="lg:col-span-2 space-y-4">
                            <div className="glass-card p-6 border border-white/5 bg-black/40">
                                <div className="flex justify-between items-center mb-6">
                                    <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 m-0">
                                        <BulbOutlined className="text-amber-500 animate-pulse" /> Auto-Model Infrastructure Suggestions
                                    </Title>
                                    <Tag color="amber" className="m-0 font-black uppercase text-[9px]">{suggestions.length} Alerts</Tag>
                                </div>

                                {suggestions.length === 0 ? (
                                    <Empty 
                                        image={<AlertOutlined style={{ fontSize: 40, color: 'rgba(255,255,255,0.05)' }} />}
                                        description={
                                            <div className="text-center py-6">
                                                <Text className="text-white/40 uppercase tracking-widest font-black block">Perfect Core coverage</Text>
                                                <Paragraph className="text-white/20 text-[10px] uppercase mt-2">No task execution gaps detected with success rate &lt; 50%.</Paragraph>
                                            </div>
                                        }
                                    />
                                ) : (
                                    <List
                                        dataSource={suggestions}
                                        renderItem={(item) => (
                                            <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl mb-3 flex items-start justify-between gap-4">
                                                <div className="flex gap-3">
                                                    <Avatar shape="square" className="bg-amber-500/10 text-amber-500" icon={<AlertOutlined />} />
                                                    <div className="flex flex-col">
                                                        <span className="text-[12px] font-black text-white uppercase">{item.type}</span>
                                                        <span className="text-[10px] text-white/60 mt-1">{item.message}</span>
                                                        <div className="flex gap-2 mt-2">
                                                            <Tag color="blue" className="text-[8px] font-mono font-black uppercase m-0">Category: {item.category}</Tag>
                                                            <Tag color="red" className="text-[8px] font-mono font-black uppercase m-0">Success: {Math.round(item.score * 100)}%</Tag>
                                                        </div>
                                                    </div>
                                                </div>
                                                <Button type="primary" size="small" className="bg-emerald-500 text-black border-none text-[9px] font-black uppercase">
                                                    Resolve
                                                </Button>
                                            </div>
                                        )}
                                    />
                                )}
                            </div>
                        </div>

                        {/* Evaluation Concierge */}
                        <div className="lg:col-span-1 space-y-6">
                            {/* Link evaluation panel */}
                            <div className="glass-card p-6 border border-white/5 bg-black/40">
                                <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-4">
                                    <LinkOutlined className="text-blue-500" /> Link-Based Evaluation
                                </Title>
                                <Paragraph className="text-white/40 text-[9px] uppercase leading-relaxed mb-4">
                                    Input any HuggingFace or GitHub URL to evaluate model architecture suitability.
                                </Paragraph>
                                <Space direction="vertical" className="w-full" size="middle">
                                    <Input 
                                        placeholder="https://huggingface.co/Qwen/Qwen2.5-Coder-32B-Instruct" 
                                        value={evalUrl} 
                                        onChange={(e) => setEvalUrl(e.target.value)}
                                        className="dark-input h-10 text-[11px]"
                                    />
                                    <Button 
                                        block 
                                        type="primary" 
                                        onClick={evaluateLink} 
                                        loading={actionLoading}
                                        className="h-10 bg-blue-500 hover:bg-blue-400 border-none font-black uppercase tracking-widest text-[9px] shadow-[0_0_20px_rgba(59,130,246,0.15)]"
                                    >
                                        Evaluate Model Suitability
                                    </Button>
                                </Space>

                                {evalResult && (
                                    <div className="mt-4 p-4 bg-white/[0.03] border border-blue-500/20 rounded-xl space-y-3">
                                        <div className="flex justify-between items-center">
                                            <span className="text-[10px] font-black text-white uppercase">{evalResult.target}</span>
                                            <Tag color={evalResult.score > 0.7 ? 'emerald' : 'orange'} className="m-0 font-mono font-black text-[9px]">
                                                MATCH: {Math.round(evalResult.score * 100)}%
                                            </Tag>
                                        </div>
                                        <Paragraph className="text-[10px] text-white/60 m-0 leading-relaxed italic">
                                            "{evalResult.message}"
                                        </Paragraph>
                                    </div>
                                )}
                            </div>

                            {/* Gap coverage runner */}
                            <div className="glass-card p-6 border border-white/5 bg-black/40">
                                <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-4">
                                    <GlobalOutlined className="text-purple-500" /> Dynamic Gap Analysis
                                </Title>
                                <Paragraph className="text-white/40 text-[9px] uppercase leading-relaxed mb-4">
                                    Select a target task type to identify orchestrator capability coverage.
                                </Paragraph>
                                <Space direction="vertical" className="w-full" size="middle">
                                    <Select 
                                        value={gapTaskType} 
                                        className="dark-select w-full h-10"
                                        onChange={setGapTaskType}
                                    >
                                        <Option value="code_generation">CODE_GENERATION</Option>
                                        <Option value="translation">TRANSLATION</Option>
                                        <Option value="vector_search">VECTOR_SEARCH</Option>
                                        <Option value="multimodal_synthesis">MULTIMODAL_SYNTHESIS</Option>
                                    </Select>
                                    <Button 
                                        block 
                                        type="primary" 
                                        onClick={runGapAnalysis} 
                                        loading={actionLoading}
                                        className="h-10 bg-purple-500 hover:bg-purple-400 border-none font-black uppercase tracking-widest text-[9px] shadow-[0_0_20px_rgba(168,85,247,0.15)]"
                                    >
                                        Execute Capability Assessment
                                    </Button>
                                </Space>

                                {gapResult && (
                                    <div className="mt-4 p-4 bg-white/[0.03] border border-purple-500/20 rounded-xl space-y-2">
                                        <span className="text-[9px] font-black text-purple-400 uppercase tracking-widest block">Gap Analysis Report</span>
                                        <Paragraph className="text-[10px] text-white/80 m-0 leading-relaxed">
                                            {gapResult.message}
                                        </Paragraph>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeSection === 'tester' && (
                <div className="glass-card p-6 border border-white/5 bg-black/40 max-w-4xl mx-auto">
                    <div className="flex items-center gap-3 mb-6">
                        <Avatar shape="square" className="bg-emerald-500/10 text-emerald-500" icon={<SearchOutlined />} />
                        <div>
                            <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest m-0">Vector Intent Routing Tester</Title>
                            <span className="text-[9px] text-white/30 uppercase font-black">Verify character n-gram + Jaccard similarity classifications</span>
                        </div>
                    </div>

                    <Paragraph className="text-white/60 text-[11px] leading-relaxed mb-6">
                        Test how the supreme orchestrator categorizes queries into specialized MoE hubs and clusters.
                        This simulates system routing before executing requests on actual providers.
                    </Paragraph>

                    <div className="flex gap-4 mb-6">
                        <Input 
                            placeholder="e.g., generate a java endpoint to upload photos or write marketing copy in bangla" 
                            value={testQuery} 
                            onChange={(e) => setTestQuery(e.target.value)}
                            onPressEnter={testIntent}
                            className="dark-input h-12 text-[12px]"
                            disabled={actionLoading}
                        />
                        <Button 
                            type="primary" 
                            onClick={testIntent} 
                            loading={actionLoading}
                            className="h-12 px-8 bg-emerald-500 hover:bg-emerald-400 text-black border-none font-black uppercase tracking-widest text-[10px]"
                        >
                            Analyze Intent
                        </Button>
                    </div>

                    {testResult && (
                        <div className="p-6 bg-white/[0.02] border border-emerald-500/20 rounded-2xl space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl">
                                    <span className="text-[8px] text-white/20 uppercase block font-black mb-1">Identified Hub</span>
                                    <Tag color="emerald" className="m-0 font-mono font-black text-[12px] uppercase py-1 px-3">
                                        {testResult.identifiedHub || 'UNKNOWN'}
                                    </Tag>
                                </div>
                                <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl">
                                    <span className="text-[8px] text-white/20 uppercase block font-black mb-1">Target Cluster</span>
                                    <Tag color="blue" className="m-0 font-mono font-black text-[12px] uppercase py-1 px-3">
                                        {testResult.identifiedCluster || 'UNKNOWN'}
                                    </Tag>
                                </div>
                            </div>

                            <div className="p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-xl">
                                <h4 className="text-[10px] font-black text-emerald-400 uppercase tracking-widest mb-2">⚡ Classification Decision Path</h4>
                                <Paragraph className="text-[11px] text-white/80 m-0 leading-relaxed">
                                    System used character 3-gram similarity indexing. The query matched key vectors in the <strong>{testResult.identifiedCluster}</strong> category of the <strong>{testResult.identifiedHub}</strong>.
                                </Paragraph>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default LearningHub;
