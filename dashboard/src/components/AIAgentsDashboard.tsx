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
                    load: a.load !== undefined && a.load !== null ? a.load : 0,
                    tasks: a.tasks !== undefined && a.tasks !== null ? a.tasks : 0
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
            title: <span className="text-[10px] font-black uppercase tracking-widest text-dim">Agent Neural Identity</span>, 
            key: 'identity',
            render: (_: any, r: AIAgent) => (
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-lg bg-white/[0.05] border border-white/10 flex items-center justify-center relative overflow-hidden shadow-lg">
                        <RobotOutlined className={`${r.status === 'ACTIVE' ? 'text-neon' : 'text-dim'} text-xl z-10`} />
                        {r.status === 'ACTIVE' && <div className="absolute inset-0 bg-neon/10 animate-pulse" />}
                    </div>
                    <div className="flex flex-col leading-tight">
                        <span className="text-md font-black text-main uppercase tracking-tight">{r.name}</span>
                        <span className="text-[11px] font-mono text-neon font-bold uppercase tracking-widest">{r.id}</span>
                    </div>
                </div>
            )
        },
        { 
            title: <span className="text-[10px] font-black uppercase tracking-widest text-dim text-center">Class</span>, 
            dataIndex: 'type', 
            key: 'type',
            align: 'center' as const,
            render: (t: string) => (
                <div className="flex flex-col items-center">
                    <span className="text-[10px] text-neon font-black tracking-widest uppercase px-3 py-1 bg-neon/5 border border-neon/20 rounded-md">
                        {t}
                    </span>
                </div>
            )
        },
        {
            title: <span className="text-[10px] font-black uppercase tracking-widest text-dim">Cognitive Load</span>,
            key: 'load',
            width: 120,
            render: (_: any, r: AIAgent) => (
                <div className="flex flex-col gap-1">
                    <div className="flex justify-between items-center text-[9px] font-mono text-dim uppercase">
                        <span>Utilization</span>
                        <span className="text-main font-bold">{r.load}%</span>
                    </div>
                    <Progress 
                        percent={r.load} 
                        size={[100, 3]} 
                        showInfo={false} 
                        strokeColor={r.load! > 80 ? 'var(--error)' : r.status === 'ACTIVE' ? 'var(--neon-blue)' : 'var(--text-dim)'}
                        trailColor="rgba(255,255,255,0.08)"
                    />
                </div>
            )
        },
        {
            title: <span className="text-[10px] font-black uppercase tracking-widest text-dim">Status</span>,
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const isOnline = status === 'ACTIVE';
                const isError = status === 'ERROR';
                return (
                    <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-success animate-pulse' : isError ? 'bg-error' : 'bg-dim'}`} />
                        <span className={`text-[10px] font-black tracking-widest uppercase px-2 py-1 rounded-sm ${isOnline ? 'bg-success text-black' : isError ? 'bg-error text-white' : 'bg-dim text-black'}`}>
                            {status}
                        </span>
                    </div>
                );
            }
        },
        {
            title: <span className="text-[10px] font-black uppercase tracking-widest text-dim text-right">Command</span>,
            key: 'action',
            align: 'right' as const,
            render: (_: any, record: AIAgent) => (
                <Space size={6}>
                    <Tooltip title="Neural Inspection">
                        <Button
                            size="small"
                            type="text"
                            className="h-7 w-7 flex items-center justify-center text-neon hover:bg-neon/10 border border-neon/30"
                            icon={<DotChartOutlined style={{ fontSize: '14px' }} />}
                        />
                    </Tooltip>
                    {record.status === 'ACTIVE' ? (
                        <Tooltip title="Force Standby">
                            <Button
                                size="small"
                                type="text"
                                className="h-7 w-7 flex items-center justify-center text-warning hover:bg-warning/10 border border-warning/30"
                                icon={<PauseCircleOutlined style={{ fontSize: '14px' }} />}
                                onClick={() => updateStatus(record.id, 'IDLE')}
                            />
                        </Tooltip>
                    ) : (
                        <Tooltip title="Initiate Neural Link">
                            <Button
                                size="small"
                                type="text"
                                className="h-7 w-7 flex items-center justify-center text-success hover:bg-success/10 border border-success/30"
                                icon={<PlayCircleOutlined style={{ fontSize: '14px' }} />}
                                onClick={() => updateStatus(record.id, 'ACTIVE')}
                            />
                        </Tooltip>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div className="space-y-8">
            {/* Command Center KPIs */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {[
                    { label: 'Neural Fleet', value: stats.totalAgents, icon: <ApartmentOutlined />, color: 'neon' },
                    { label: 'Active Links', value: stats.activeAgents, sub: 'UPTIME_SYNCED', color: 'success', icon: <ThunderboltOutlined /> },
                    { label: 'Standby Mode', value: stats.idleAgents, sub: 'LATENCY_OPTIMAL', color: 'warning', icon: <BlockOutlined /> },
                    { label: 'System Health', value: '100%', sub: 'ALL_MODULES_GO', color: 'purple', icon: <ThunderboltOutlined /> }
                ].map((s, idx) => (
                    <div key={idx} className="ai-card p-6 flex flex-col justify-between min-h-[100px]">
                        <div className="flex items-center justify-between">
                            <span className="text-[10px] font-black uppercase tracking-[0.2em] text-dim">{s.label}</span>
                            <span className={`text-xl text-${s.color}`} style={{ color: `var(--${s.color === 'neon' ? 'neon-blue' : s.color})` }}>{s.icon}</span>
                        </div>
                        <div className="flex items-baseline gap-3 mt-4">
                            <span className="text-4xl font-mono font-black text-main leading-none tracking-tighter">{s.value}</span>
                            {s.sub && (
                                <span className="text-[10px] font-black text-main bg-white/10 px-2 py-0.5 rounded uppercase tracking-widest">
                                    {s.sub}
                                </span>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {/* Neural Matrix Table */}
            <div className="ai-card overflow-hidden !p-0 border border-white/10">
                <div className="px-6 py-4 bg-white/[0.03] border-b border-white/10 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <div className="w-2.5 h-2.5 bg-neon rounded-full animate-ping" />
                        <span className="text-[11px] font-black uppercase tracking-widest text-main">Neural Agent Synchronization Matrix</span>
                    </div>
                    <span className="text-[10px] font-mono text-dim uppercase tracking-[0.2em]">Scan Frequency: 5.0s</span>
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
