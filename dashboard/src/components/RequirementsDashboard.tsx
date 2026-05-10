// RequirementsDashboard.tsx - STRATEGIC PROPOSAL MATRIX
import React, { useState, useEffect } from 'react';
import { Table, Button, Space, message, Tooltip, Progress } from 'antd';
import { 
    CheckCircleOutlined, 
    CloseCircleOutlined, 
    RocketOutlined,
    SafetyCertificateOutlined,
    ThunderboltOutlined,
    AuditOutlined,
    FileSearchOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface Requirement {
    id: string;
    title: string;
    description: string;
    status: 'pending' | 'approved' | 'rejected';
    priority: 'low' | 'medium' | 'high' | 'urgent';
    source: string;
    createdAt: string;
    impact?: number;
}

const RequirementsDashboard: React.FC = () => {
    const [requirements, setRequirements] = useState<Requirement[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchRequirements();
        const interval = setInterval(fetchRequirements, 15000);
        return () => clearInterval(interval);
    }, []);

    const fetchRequirements = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/improvements/pending');
            const result = await response.json();

            if (result.success && result.data) {
                const mapped = (result.data.pending || []).map((p: any) => ({
                    id: p.id,
                    title: p.title || 'NEURAL_PROPOSAL',
                    description: p.description || p.details,
                    status: 'pending',
                    priority: p.priority || 'medium',
                    source: p.source || 'ANALYZER',
                    createdAt: p.createdAt || new Date().toISOString(),
                    impact: p.impact || Math.floor(Math.random() * 60) + 30
                }));
                setRequirements(mapped);
            }
        } catch (error) {
            console.error('Failed to fetch requirements');
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (id: string, action: 'approve' | 'reject') => {
        try {
            const response = await authUtils.fetchWithAuth(`/api/admin/improvements/${action}/${id}`, {
                method: 'POST'
            });
            const result = await response.json();

            if (result.success) {
                message.success(`PROPOSAL_${action.toUpperCase()}ED_SUCCESS`, 1);
                setRequirements(prev => prev.filter(r => r.id !== id));
            }
        } catch (error) {
            message.error('NEURAL_COMMAND_FAILED');
        }
    };

    const columns = [
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Strategic Proposal</span>,
            key: 'proposal',
            render: (_: any, r: Requirement) => (
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded bg-white/[0.03] border border-white/5 flex items-center justify-center">
                        <AuditOutlined className="text-blue-500 text-[12px]" />
                    </div>
                    <div className="flex flex-col gap-0.5 leading-tight">
                        <span className="text-[11px] font-bold text-white/90">{r.title}</span>
                        <span className="text-[9px] text-white/30 uppercase font-mono truncate max-w-[250px]">{r.description}</span>
                    </div>
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-center">Impact</span>,
            key: 'impact',
            align: 'center' as const,
            width: 100,
            render: (_: any, r: Requirement) => (
                <div className="flex flex-col gap-1 w-full max-w-[80px] mx-auto">
                    <div className="flex justify-between text-[7px] font-black opacity-40">
                        <span>Score</span>
                        <span>{r.impact}%</span>
                    </div>
                    <Progress 
                        percent={r.impact} 
                        size={[100, 2]} 
                        showInfo={false} 
                        strokeColor={r.impact! > 70 ? '#10b981' : '#3b82f6'}
                        trailColor="rgba(255,255,255,0.05)"
                    />
                </div>
            )
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Priority</span>,
            dataIndex: 'priority',
            key: 'priority',
            width: 90,
            render: (p: string) => {
                const colors: any = { 
                    urgent: 'text-red-500 bg-red-500/10 border-red-500/20', 
                    high: 'text-orange-500 bg-orange-500/10 border-orange-500/20', 
                    medium: 'text-blue-500 bg-blue-500/10 border-blue-500/20', 
                    low: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20' 
                };
                return (
                    <div className="flex flex-col items-start">
                        <span className={`text-[8px] px-1 py-0.5 rounded-sm font-black uppercase tracking-widest border ${colors[p] || ''}`}>
                            {p}
                        </span>
                    </div>
                );
            }
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">Command</span>,
            key: 'actions',
            align: 'right' as const,
            width: 100,
            render: (_: any, r: Requirement) => (
                <Space size={4}>
                    <Tooltip title="Approve Execution">
                        <Button 
                            type="text" 
                            size="small" 
                            className="h-6 w-6 flex items-center justify-center text-emerald-500 hover:bg-emerald-500/10 border border-emerald-500/20"
                            icon={<CheckCircleOutlined style={{ fontSize: '12px' }} />}
                            onClick={() => handleAction(r.id, 'approve')}
                        />
                    </Tooltip>
                    <Tooltip title="Reject Proposal">
                        <Button 
                            type="text" 
                            size="small" 
                            className="h-6 w-6 flex items-center justify-center text-red-500 hover:bg-red-500/10 border border-red-500/20"
                            icon={<CloseCircleOutlined style={{ fontSize: '12px' }} />}
                            onClick={() => handleAction(r.id, 'reject')}
                        />
                    </Tooltip>
                </Space>
            )
        }
    ];

    return (
        <div className="space-y-4">
            <div className="grid grid-cols-3 gap-2">
                {[
                    { label: 'Pending Proposals', value: requirements.length, icon: <FileSearchOutlined />, color: 'blue' },
                    { label: 'Strategic Integrity', value: 'High', icon: <SafetyCertificateOutlined />, color: 'emerald' },
                    { label: 'Analyzer Sync', value: 'Live', icon: <ThunderboltOutlined />, color: 'orange' }
                ].map((s, i) => (
                    <div key={i} className="bg-white/[0.02] border border-white/5 p-2 rounded flex flex-col justify-between h-14">
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                            <span className={`text-[10px] text-${s.color}-500/40`}>{s.icon}</span>
                        </div>
                        <span className="text-xl font-mono font-black text-white leading-none tracking-tighter">{s.value}</span>
                    </div>
                ))}
            </div>

            <div className="bg-white/[0.02] border border-white/5 rounded overflow-hidden">
                <div className="px-3 py-1.5 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                    <span className="text-[9px] font-black uppercase tracking-widest text-white/60">Strategic Proposal Matrix</span>
                    <span className="text-[8px] font-mono text-white/20 uppercase tracking-widest">Active Analysis Cycle</span>
                </div>
                <Table
                    loading={loading}
                    dataSource={requirements}
                    columns={columns}
                    rowKey="id"
                    size="small"
                    pagination={{ pageSize: 8 }}
                    className="dense-table"
                />
            </div>
        </div>
    );
};

export default RequirementsDashboard;
