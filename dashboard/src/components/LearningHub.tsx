import React, { useState, useEffect } from 'react';
import { Typography, Progress, Button, Space, Card, Tag, Empty, message, Select, Badge, Divider, Avatar } from 'antd';
import { BulbOutlined, RocketOutlined, ExperimentOutlined, HistoryOutlined, SyncOutlined, ThunderboltOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title } = Typography;
const { Option } = Select;

const LearningHub: React.FC = () => {
    const [status, setStatus] = useState<any>(null);
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 30000); // Sync every 30s
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

    return (
        <div className="p-4 animate-fade-in space-y-6">
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
            
            <div className="glass-card p-6 border border-white/5 bg-black/40">
                <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                    <HistoryOutlined className="text-white/40" /> Active Training Nodes
                </Title>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="p-4 bg-white/[0.03] border border-white/10 rounded-xl flex items-center gap-4 opacity-50">
                        <Avatar shape="square" className="bg-orange-500/10 text-orange-500" icon={<BulbOutlined />} />
                        <div className="flex flex-col">
                            <span className="text-[11px] font-black text-white/80 uppercase">Supreme-Coder-V2</span>
                            <span className="text-[8px] text-white/20 uppercase">In Queue • Priority 0</span>
                        </div>
                    </div>
                    <div className="p-4 bg-white/[0.03] border border-white/10 rounded-xl flex items-center gap-4 opacity-50">
                        <Avatar shape="square" className="bg-blue-500/10 text-blue-500" icon={<SyncOutlined />} />
                        <div className="flex flex-col">
                            <span className="text-[11px] font-black text-white/80 uppercase">Nexus-Translator</span>
                            <span className="text-[8px] text-white/20 uppercase">Awaiting Dataset</span>
                        </div>
                    </div>
                    <div className="p-4 bg-white/[0.03] border border-white/10 rounded-xl flex items-center gap-4 opacity-50">
                        <Avatar shape="square" className="bg-purple-500/10 text-purple-500" icon={<RocketOutlined />} />
                        <div className="flex flex-col">
                            <span className="text-[11px] font-black text-white/80 uppercase">Vision-Refiner</span>
                            <span className="text-[8px] text-white/20 uppercase">Manual Trigger Required</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LearningHub;
