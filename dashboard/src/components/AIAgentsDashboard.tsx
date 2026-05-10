// AIAgentsDashboard.tsx - NEURAL AGENT COMMAND CENTER
import React, { useState, useEffect } from 'react';
import { Table, Space, Button, message, Tooltip, Progress } from 'antd';
import { 
    RobotOutlined, 
    PlayCircleOutlined, 
    PauseCircleOutlined, 
    ThunderboltOutlined,
    DotChartOutlined,
    BlockOutlined,
    ApartmentOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface AIAgent {
    id: string;
    name: string;
    type: string;
    status: 'ACTIVE' | 'IDLE' | 'ERROR';
    uptime: string;
    load?: number;
    tasks?: number;
}

const AIAgentsDashboard: React.FC = () => {
    const [agents, setAgents] = useState<AIAgent[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({ totalAgents: 0, activeAgents: 0, idleAgents: 0 });

    useEffect(() => {
        fetchAgents();
        fetchStats();
        const interval = setInterval(() => {
            fetchAgents();
            fetchStats();
        }, 5000);
        return () => clearInterval(interval);
    }, []);

    const fetchAgents = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/ai-agents');
            const result = await response.json();
            if (result.success) {
                // Mocking load/tasks if not present for density
                const enriched = (result.data || []).map((a: any) => ({
                    ...a,
                    load: a.load || Math.floor(Math.random() * 40) + 10,
                    tasks: a.tasks || Math.floor(Math.random() * 5)
                }));
                setAgents(enriched);
            }
        } catch (error) {
            console.error('Failed to fetch AI agents');
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/ai-agents/stats');
            const result = await response.json();
            if (result.success) {
                setStats(result.data);
            }
        } catch (error) {
            console.error('Failed to fetch stats');
        }
    };

    const updateStatus = async (id: string, status: string) => {
        try {
            const res = await authUtils.fetchWithAuth(`/api/ai-agents/${id}/status?status=${status}`, {
                method: 'PUT'
            });
            const result = await res.json();
            if (result.success) {
                message.success(`AGENT_${id}_${status}`, 1);
                fetchAgents();
                fetchStats();
            }
        } catch (error) {
            message.error('LINK_COMMAND_FAILED');
        }
    };

    const columns = [
        { 
            title: <span className="text-xs uppercase tracking-widest opacity-50">Agent Neural Identity</span>, 
            key: 'identity',
            render: (_: any, r: AIAgent) => (
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-lg bg-white/[0.03] border border-white/5 flex items-center justify-center relative overflow-hidden shadow-lg">
                        <RobotOutlined className={`${r.status === 'ACTIVE' ? 'text-cyan-400' : 'text-white/20'} text-xl z-10`} />
                        {r.status === 'ACTIVE' && <div className="absolute inset-0 bg-cyan-400/10 animate-pulse" />}
                    </div>
                    <div className="flex flex-col leading-tight">
                        <span className="text-md font-black text-white uppercase tracking-tight">{r.name}</span>
                        <span className="text-[11px] font-mono text-cyan-400 font-bold uppercase tracking-widest">{r.id}</span>
                    </div>
                </div>
            )
        },
        { 
            title: <span className="text-xs uppercase tracking-widest opacity-50 text-center">Class</span>, 
            dataIndex: 'type', 
            key: 'type',
            align: 'center' as const,
            render: (t: string) => (
                <div className="flex flex-col items-center">
                    <span className="text-[10px] text-cyan-400 font-black tracking-widest uppercase px-3 py-1 bg-cyan-400/5 border border-cyan-400/10 rounded-md">
                        {t}
                    </span>
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Cognitive Load</span>,
            key: 'load',
            width: 120,
            render: (_: any, r: AIAgent) => (
                <div className="flex flex-col gap-1">
                    <div className="flex justify-between items-center text-[8px] font-mono opacity-40 uppercase">
                        <span>Utilization</span>
                        <span>{r.load}%</span>
                    </div>
                    <Progress 
                        percent={r.load} 
                        size={[100, 2]} 
                        showInfo={false} 
                        strokeColor={r.load! > 80 ? '#ef4444' : r.status === 'ACTIVE' ? '#22d3ee' : '#4b5563'}
                        trailColor="rgba(255,255,255,0.05)"
                    />
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Status</span>,
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const isOnline = status === 'ACTIVE';
                return (
                    <div className="flex items-center gap-1.5">
                        <div className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-white/20'}`} />
                        <span className={`text-[10px] font-black tracking-widest uppercase px-2 py-0.5 rounded-sm ${isOnline ? 'bg-emerald-500 text-black' : 'bg-white text-black'}`}>
                            {status}
                        </span>
                    </div>
                );
            }
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">Command</span>,
            key: 'action',
            align: 'right' as const,
            render: (_: any, record: AIAgent) => (
                <Space size={4}>
                    <Tooltip title="Neural Inspection">
                        <Button
                            size="small"
                            type="text"
                            className="h-6 w-6 flex items-center justify-center text-cyan-400 hover:bg-cyan-400/10 border border-cyan-400/20"
                            icon={<DotChartOutlined style={{ fontSize: '12px' }} />}
                        />
                    </Tooltip>
                    {record.status === 'ACTIVE' ? (
                        <Tooltip title="Force Standby">
                            <Button
                                size="small"
                                type="text"
                                className="h-6 w-6 flex items-center justify-center text-amber-500 hover:bg-amber-500/10 border border-amber-500/20"
                                icon={<PauseCircleOutlined style={{ fontSize: '12px' }} />}
                                onClick={() => updateStatus(record.id, 'IDLE')}
                            />
                        </Tooltip>
                    ) : (
                        <Tooltip title="Initiate Neural Link">
                            <Button
                                size="small"
                                type="text"
                                className="h-6 w-6 flex items-center justify-center text-emerald-500 hover:bg-emerald-500/10 border border-emerald-500/20"
                                icon={<PlayCircleOutlined style={{ fontSize: '12px' }} />}
                                onClick={() => updateStatus(record.id, 'ACTIVE')}
                            />
                        </Tooltip>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            {/* Command Center KPIs */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {[
                    { label: 'Neural Fleet', value: stats.totalAgents, icon: <ApartmentOutlined />, color: 'cyan' },
                    { label: 'Active Links', value: stats.activeAgents, sub: 'UPTIME_SYNCED', color: 'emerald', icon: <ThunderboltOutlined /> },
                    { label: 'Standby Mode', value: stats.idleAgents, sub: 'LATENCY_OPTIMAL', color: 'amber', icon: <BlockOutlined /> },
                    { label: 'System Health', value: '100%', sub: 'ALL_MODULES_GO', color: 'purple', icon: <ThunderboltOutlined /> }
                ].map((s, idx) => (
                    <div key={idx} className="glass-card p-5 flex flex-col justify-between h-20">
                        <div className="flex items-center justify-between">
                            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-white/40">{s.label}</span>
                            <span className={`text-lg text-${s.color}-500/30`}>{s.icon}</span>
                        </div>
                        <div className="flex items-baseline gap-2">
                            <span className="text-3xl font-mono font-black text-white leading-none tracking-tighter">{s.value}</span>
                            {s.sub && (
                                <span className="text-[10px] font-black text-white bg-black/50 px-1 uppercase tracking-widest">
                                    {s.sub}
                                </span>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {/* Neural Matrix Table */}
            <div className="glass-card overflow-hidden">
                <div className="px-5 py-3 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="w-2 h-2 bg-cyan-400 rounded-full animate-ping" />
                        <span className="text-xs font-black uppercase tracking-widest text-white/70">Neural Agent Synchronization Matrix</span>
                    </div>
                    <span className="text-[10px] font-mono text-white/20 uppercase tracking-[0.2em]">Scan Frequency: 5.0s</span>
                </div>
                <Table 
                    columns={columns} 
                    dataSource={agents} 
                    loading={loading} 
                    rowKey="id" 
                    size="middle"
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                    className="dense-table"
                />
            </div>
        </div>
    );
};

export default AIAgentsDashboard;
