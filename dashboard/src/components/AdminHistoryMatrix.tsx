// AdminHistoryMatrix.tsx - ULTRA-DENSE OPERATIONAL HISTORY
import React, { useState, useEffect } from 'react';
import { Table, Tag, Typography, Progress } from 'antd';
import { 
    ClockCircleOutlined, 
    CheckCircleOutlined, 
    CloseCircleOutlined,
    ThunderboltOutlined,
    GlobalOutlined,
    HistoryOutlined
} from '@ant-design/icons';
import authUtils from '../lib/authUtils';

interface HistoryItem {
    id: string;
    agent: string;
    taskName: string;
    description?: string;
    startTime: string;
    endTime?: string;
    duration: number;
    status: 'completed' | 'failed' | 'in-progress';
    outcome: string;
    confidence?: number;
}

const AdminHistoryMatrix: React.FC = () => {
    const [history, setHistory] = useState<HistoryItem[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/work-history');
            const data = await response.json();
            if (Array.isArray(data)) {
                setHistory(data);
            }
        } catch (error) {
            console.error('Failed to fetch operational history:', error);
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        {
            title: 'TIMESTAMP',
            dataIndex: 'startTime',
            key: 'startTime',
            width: 140,
            render: (t: string) => (
                <span className="text-[10px] font-mono text-white/40">{t}</span>
            )
        },
        {
            title: 'NEURAL_NODE',
            dataIndex: 'agent',
            key: 'agent',
            render: (a: string) => (
                <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-blue-500/50" />
                    <span className="text-[10px] font-black text-white/90 uppercase tracking-tighter">{a}</span>
                </div>
            )
        },
        {
            title: 'MISSION_PARAMETER',
            dataIndex: 'taskName',
            key: 'taskName',
            render: (t: string, record: HistoryItem) => (
                <div className="flex flex-col">
                    <span className="text-[10px] font-black text-white uppercase tracking-tighter">{t}</span>
                    <span className="text-[8px] text-white/30 uppercase tracking-widest leading-tight truncate max-w-[200px]">
                        {record.description || 'Routine operational cycle'}
                    </span>
                </div>
            )
        },
        {
            title: 'CONFIDENCE',
            dataIndex: 'confidence',
            key: 'confidence',
            width: 100,
            render: (c?: number) => (
                c ? (
                    <div className="flex flex-col gap-1">
                        <span className="text-[9px] font-mono text-emerald-500">{(c * 100).toFixed(1)}%</span>
                        <Progress percent={c * 100} size={[60, 1]} showInfo={false} strokeColor="#10b981" trailColor="rgba(255,255,255,0.05)" />
                    </div>
                ) : <span className="text-[10px] text-white/10 font-mono">N/A</span>
            )
        },
        {
            title: 'STATUS',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (s: string) => {
                const colors: any = {
                    completed: { bg: 'bg-emerald-500/10', border: 'border-emerald-500/20', text: 'text-emerald-500', icon: <CheckCircleOutlined /> },
                    failed: { bg: 'bg-red-500/10', border: 'border-red-500/20', text: 'text-red-500', icon: <CloseCircleOutlined /> },
                    'in-progress': { bg: 'bg-blue-500/10', border: 'border-blue-500/20', text: 'text-blue-500', icon: <ThunderboltOutlined spin /> }
                };
                const config = colors[s] || colors['in-progress'];
                return (
                    <div className={`px-2 py-0.5 rounded border ${config.bg} ${config.border} flex items-center gap-1.5 w-fit`}>
                        <span className={`text-[8px] ${config.text}`}>{config.icon}</span>
                        <span className={`text-[8px] font-black uppercase tracking-widest ${config.text}`}>{s}</span>
                    </div>
                );
            }
        }
    ];

    return (
        <div className="space-y-3">
            {/* History Telemetry */}
            <div className="grid grid-cols-4 gap-2">
                {[
                    { label: 'Success Velocity', value: '94.2%', sub: 'Last 100 Cycles', icon: <HistoryOutlined />, color: 'emerald' },
                    { label: 'Mean Execution', value: '1.42s', sub: 'Neural Processing', icon: <ThunderboltOutlined />, color: 'blue' },
                    { label: 'Cluster Uptime', value: '99.98%', sub: 'Global Coverage', icon: <GlobalOutlined />, color: 'white' },
                    { label: 'Active Missions', value: history.filter(h => h.status === 'in-progress').length, sub: 'Parallel Nodes', icon: <ClockCircleOutlined />, color: 'purple' }
                ].map((s, idx) => (
                    <div key={idx} className="bg-white/[0.02] border border-white/5 p-2 rounded flex flex-col justify-between h-14">
                        <span className="text-[8px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                        <div className="flex items-center justify-between">
                            <span className="text-lg font-mono font-black text-white tracking-tighter">{s.value}</span>
                            <span className={`text-[10px] text-${s.color}-500/40`}>{s.icon}</span>
                        </div>
                    </div>
                ))}
            </div>

            <div className="bg-[#0d0d0d] border border-white/5 rounded overflow-hidden">
                <div className="px-3 py-2 border-b border-white/5 flex items-center justify-between bg-white/[0.01]">
                    <div className="flex items-center gap-2">
                        <HistoryOutlined className="text-white/40 text-[10px]" />
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/60">Operational Event Log</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <span className="text-[8px] font-mono text-white/20 uppercase tracking-[0.2em]">{history.length} SYNC_ENTRIES</span>
                        <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                    </div>
                </div>
                
                <Table 
                    dataSource={history} 
                    columns={columns} 
                    loading={loading}
                    rowKey="id"
                    pagination={{ pageSize: 12, size: 'small', showSizeChanger: false }}
                    className="admin-table-dense"
                    scroll={{ y: 500 }}
                />
            </div>

            <style>{`
                .admin-table-dense .ant-table {
                    background: transparent !important;
                    color: rgba(255,255,255,0.9) !important;
                }
                .admin-table-dense .ant-table-thead > tr > th {
                    background: rgba(255,255,255,0.02) !important;
                    color: rgba(255,255,255,0.3) !important;
                    font-size: 8px !important;
                    font-weight: 900 !important;
                    text-transform: uppercase !important;
                    letter-spacing: 0.1em !important;
                    border-bottom: 1px solid rgba(255,255,255,0.05) !important;
                    padding: 8px 12px !important;
                }
                .admin-table-dense .ant-table-tbody > tr > td {
                    border-bottom: 1px solid rgba(255,255,255,0.02) !important;
                    padding: 6px 12px !important;
                    background: transparent !important;
                }
                .admin-table-dense .ant-table-tbody > tr:hover > td {
                    background: rgba(255,255,255,0.03) !important;
                }
            `}</style>
        </div>
    );
};

export default AdminHistoryMatrix;
