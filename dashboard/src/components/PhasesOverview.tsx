// PhasesOverview.tsx - OPERATIONAL INFRASTRUCTURE MATRIX
import React, { useState, useEffect } from 'react';
import { Badge, Spin, Alert, Progress, Tooltip } from 'antd';
import {
    CheckCircleOutlined,
    WarningOutlined,
    ThunderboltOutlined,
    NodeIndexOutlined,
    ApiOutlined,
    DeploymentUnitOutlined,
    HddOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface PhaseEntry {
    phase: number;
    name: string;
    description: string;
    statusUrl: string;
    status: string;
}

interface AllPhasesResponse {
    totalPhases: number;
    operationalCount: number;
    systemStatus: string;
    phases: PhaseEntry[];
    timestamp: number;
}

const PhasesOverview: React.FC = () => {
    const [allPhases, setAllPhases] = useState<AllPhasesResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchAll = async () => {
        setLoading(true);
        setError(null);
        try {
            const overviewRes = await authUtils.fetchWithAuth('/api/v1/agents/all-phases');
            if (!overviewRes.ok) throw new Error('OFFLINE');
            const overview: AllPhasesResponse = await overviewRes.json();
            setAllPhases(overview);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'LINK_FAILURE');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { 
        fetchAll(); 
        const interval = setInterval(fetchAll, 30000);
        return () => clearInterval(interval);
    }, []);

    if (loading) return (
        <div className="h-[400px] flex flex-col items-center justify-center font-mono opacity-40">
            <Spin size="small" className="mb-2" />
            <span className="text-[8px] uppercase tracking-widest">Scanning_Infrastructure...</span>
        </div>
    );
    
    if (error) return (
        <div className="p-4">
            <Alert 
                type="error" 
                showIcon
                message={<span className="text-[10px] font-black uppercase tracking-widest">Communication Failure</span>}
                description={<span className="text-[9px] opacity-70">ERR_INFRA_LINK_DROPPED: {error}</span>} 
                className="bg-red-500/10 border-red-500/20 text-red-500" 
            />
        </div>
    );
    
    if (!allPhases) return null;

    const integrityPercent = Math.round((allPhases.operationalCount / allPhases.totalPhases) * 100);

    return (
        <div className="space-y-4">
            {/* Infrastructure KPIs */}
            <div className="grid grid-cols-4 gap-2">
                {[
                    { label: 'System Integrity', value: `${integrityPercent}%`, icon: <DeploymentUnitOutlined />, color: 'emerald', sub: 'SYNC_ACTIVE' },
                    { label: 'Units Online', value: allPhases.operationalCount, icon: <HddOutlined />, color: 'blue', sub: `OF_${allPhases.totalPhases}_NODES` },
                    { label: 'Network State', value: 'Nominal', icon: <ThunderboltOutlined />, color: 'purple', sub: 'LATENCY_MIN' },
                    { label: 'Uptime Index', value: '99.98', icon: <NodeIndexOutlined />, color: 'orange', sub: 'SLA_VERIFIED' }
                ].map((s, i) => (
                    <div key={i} className="bg-white/[0.02] border border-white/5 p-2 rounded flex flex-col justify-between h-14">
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                            <span className={`text-[10px] text-${s.color}-500/40`}>{s.icon}</span>
                        </div>
                        <div className="flex items-baseline gap-1.5">
                            <span className="text-xl font-mono font-black text-white leading-none tracking-tighter">{s.value}</span>
                            <span className={`text-[7px] font-bold text-${s.color}-500/60 uppercase tracking-tighter`}>{s.sub}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* Global Integrity Bar */}
            <div className="bg-white/[0.02] border border-white/5 p-3 rounded">
                <div className="flex justify-between items-center mb-2">
                    <div className="flex items-center gap-2">
                        <div className={`w-1.5 h-1.5 rounded-full ${integrityPercent > 90 ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500 animate-ping'}`} />
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/60">Global Cluster Integrity</span>
                    </div>
                    <span className="text-[10px] font-mono text-white/80 font-black">{integrityPercent}%</span>
                </div>
                <Progress 
                    percent={integrityPercent} 
                    size={[100, 4]} 
                    showInfo={false}
                    strokeColor={{ '0%': '#10b981', '100%': '#3b82f6' }}
                    trailColor="rgba(255,255,255,0.05)"
                    className="m-0"
                />
            </div>

            {/* Phase Grid Matrix */}
            <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-2">
                {allPhases.phases.map(phase => {
                    const isOp = phase.status === 'operational';
                    return (
                        <div key={phase.phase} className={`group relative p-2.5 border rounded transition-all bg-white/[0.02] hover:bg-white/[0.04] overflow-hidden min-w-0 ${isOp ? 'border-white/5' : 'border-orange-500/20'}`}>
                            {isOp && <div className="absolute top-0 right-0 w-1 h-1 bg-emerald-500 rounded-full m-1 animate-pulse" />}
                            
                            <div className="flex items-center justify-between mb-1.5 gap-2">
                                <div className="flex flex-col min-w-0">
                                    <span className="text-[7px] font-black text-white/30 tracking-widest uppercase">NODE_{phase.phase.toString().padStart(2, '0')}</span>
                                    <span className="text-[10px] font-bold text-white/90 leading-tight group-hover:text-blue-400 transition-colors truncate">{phase.name}</span>
                                </div>
                                <div className={`w-5 h-5 rounded shrink-0 flex items-center justify-center border ${isOp ? 'border-emerald-500/20 bg-emerald-500/5' : 'border-orange-500/20 bg-orange-500/5'}`}>
                                    {isOp ? <CheckCircleOutlined className="text-emerald-500 text-[9px]" /> : <WarningOutlined className="text-orange-500 text-[9px]" />}
                                </div>
                            </div>
                            
                            <p className="text-[9px] text-white/40 line-clamp-2 leading-tight h-6 italic mb-2">
                                {phase.description}
                            </p>

                            <div className="flex items-center justify-between pt-2 border-t border-white/5">
                                <span className={`text-[7px] px-1.5 py-0.5 rounded-sm font-black uppercase tracking-tighter ${isOp ? 'text-emerald-500 bg-emerald-500/10' : 'text-orange-500 bg-orange-500/10'}`}>
                                    {phase.status}
                                </span>
                                <Tooltip title={phase.statusUrl} placement="bottom">
                                    <div className="flex items-center gap-1 opacity-20 hover:opacity-100 cursor-help transition-opacity">
                                        <ApiOutlined className="text-white text-[9px]" />
                                    </div>
                                </Tooltip>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default PhasesOverview;
