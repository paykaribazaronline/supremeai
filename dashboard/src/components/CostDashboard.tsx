// CostDashboard.tsx - ULTRA-DENSE COST INTELLIGENCE
import React, { useState, useEffect } from 'react';
import { Table, Tag, Progress, Alert, Button, Tooltip } from 'antd';
import { DollarOutlined, ThunderboltOutlined, WarningOutlined, ArrowUpOutlined, ArrowDownOutlined, RocketOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface CostData {
    total_monthly_spend: number;
    currency: string;
    cloud_breakdown: {
        [key: string]: {
            compute_cost: number;
            storage_cost: number;
            network_cost: number;
            total: number;
            health_score: number;
        }
    };
    forecasting: {
        next_30_days: number;
        next_90_days: number;
    };
    anomalies_detected: string[];
}

interface Recommendation {
    type: string;
    description: string;
    monthly_savings: number;
    priority: string;
}

const CostDashboard: React.FC = () => {
    const [costData, setCostData] = useState<CostData | null>(null);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCostData();
        fetchRecommendations();
    }, []);

    const fetchCostData = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/v1/agents/phase9/track-costs');
            if (response.ok) {
                const data = await response.json();
                setCostData(data);
            }
        } catch (error) {
            console.error('Failed to fetch cost data');
        } finally {
            setLoading(false);
        }
    };

    const fetchRecommendations = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/v1/agents/phase9/optimize-resources', { 
                method: 'POST', 
                body: JSON.stringify({}) 
            });
            if (response.ok) {
                const data = await response.json();
                setRecommendations(data.recommendations || []);
            }
        } catch (error) {
            console.error('Failed to fetch recommendations');
        }
    };

    const columns = [
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Type</span>,
            dataIndex: 'type',
            key: 'type',
            width: 80,
            render: (text: string) => <span className="text-[8px] font-black uppercase text-blue-500 bg-blue-500/10 px-1 py-0.5 rounded-sm border border-blue-500/20">{text}</span>,
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Optimization Protocol</span>,
            dataIndex: 'description',
            key: 'description',
            render: (text: string) => <span className="text-[10px] text-white/70 font-bold">{text}</span>
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Est. Delta</span>,
            dataIndex: 'monthly_savings',
            key: 'monthly_savings',
            width: 90,
            render: (val: number) => <span className="text-[10px] font-mono text-emerald-500 font-black">-${val.toFixed(2)}/mo</span>,
        },
        {
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">Priority</span>,
            dataIndex: 'priority',
            key: 'priority',
            align: 'right' as const,
            width: 80,
            render: (p: string) => {
                const colors: any = { CRITICAL: 'text-red-500', HIGH: 'text-orange-500', DEFAULT: 'text-emerald-500' };
                return <span className={`text-[8px] font-black uppercase tracking-widest ${colors[p] || colors.DEFAULT}`}>{p}</span>;
            },
        },
    ];

    if (!costData) return (
        <div className="p-12 flex flex-col items-center justify-center font-mono opacity-30">
            <div className="w-10 h-10 border-t border-white/20 rounded-full animate-spin mb-4" />
            <span className="text-[10px] uppercase tracking-[0.2em]">Resolving Cost Intelligence</span>
        </div>
    );

    return (
        <div className="space-y-4">
            {/* KPI Strip */}
            <div className="grid grid-cols-4 gap-2">
                {[
                    { label: 'Current Spend', val: `$${costData.total_monthly_spend.toLocaleString()}`, sub: 'Monthly Cycle', color: 'emerald' },
                    { label: '30D Forecast', val: `$${costData.forecasting.next_30_days.toLocaleString()}`, sub: 'Estimated', color: 'blue' },
                    { label: 'Optimized Delta', val: '32.4%', sub: 'Potential Savings', color: 'purple' },
                    { label: 'Budget Efficiency', val: '82%', sub: 'Healthy', color: 'amber' }
                ].map((s, idx) => (
                    <div key={idx} className="bg-white/[0.02] border border-white/5 p-3 rounded flex flex-col justify-between h-16">
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                            <div className={`w-1.5 h-1.5 rounded-full bg-${s.color}-500 shadow-[0_0_8px_rgba(var(--${s.color}-500),0.4)]`} />
                        </div>
                        <div className="flex flex-col">
                            <span className="text-xl font-mono font-black text-white leading-none tracking-tighter">{s.val}</span>
                            <span className="text-[7px] font-bold text-white/20 uppercase mt-1">{s.sub}</span>
                        </div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-12 gap-4">
                {/* Cloud Breakdown */}
                <div className="col-span-12 lg:col-span-5 bg-white/[0.02] border border-white/5 rounded-lg overflow-hidden flex flex-col">
                    <div className="px-3 py-2 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/50">Provider Saturation</span>
                        <DollarOutlined className="text-[10px] text-white/20" />
                    </div>
                    <div className="p-4 space-y-4 flex-1">
                        {Object.entries(costData.cloud_breakdown).map(([cloud, metrics]) => (
                            <div key={cloud} className="space-y-1.5">
                                <div className="flex justify-between items-end">
                                    <div className="flex flex-col">
                                        <span className="text-[10px] font-black text-white uppercase">{cloud}</span>
                                        <span className="text-[8px] text-white/40 uppercase font-bold tracking-tighter">Health Score: {metrics.health_score}%</span>
                                    </div>
                                    <span className="text-[11px] font-mono font-bold text-white/80">${metrics.total.toFixed(2)}</span>
                                </div>
                                <div className="h-1 bg-white/5 rounded-full overflow-hidden">
                                    <div 
                                        className="h-full bg-emerald-500/60" 
                                        style={{ width: `${metrics.health_score}%` }}
                                    ></div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Anomalies */}
                <div className="col-span-12 lg:col-span-7 bg-white/[0.02] border border-white/5 rounded-lg overflow-hidden">
                    <div className="px-3 py-2 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/50">Neural Anomalies Detected</span>
                        <WarningOutlined className="text-[10px] text-orange-500" />
                    </div>
                    <div className="p-3 space-y-2 max-h-[160px] overflow-y-auto scrollbar-hide">
                        {costData.anomalies_detected.map((anomaly, idx) => (
                            <div key={idx} className="bg-orange-500/5 border border-orange-500/10 p-2 rounded flex items-center gap-3">
                                <div className="w-1.5 h-1.5 rounded-full bg-orange-500 animate-pulse" />
                                <span className="text-[10px] font-bold text-orange-500/80 uppercase tracking-tighter">{anomaly}</span>
                            </div>
                        ))}
                        {costData.anomalies_detected.length === 0 && (
                            <div className="h-24 flex flex-col items-center justify-center opacity-20">
                                <ThunderboltOutlined className="text-xl mb-1" />
                                <span className="text-[8px] font-black uppercase">Grid Integrity Nominal</span>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Recommendations Table */}
            <div className="bg-white/[0.02] border border-white/5 rounded-lg overflow-hidden">
                <div className="px-3 py-2 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
                    <span className="text-[9px] font-black uppercase tracking-widest text-white/50">Optimization Backlog</span>
                    <Button 
                        size="small" 
                        className="h-5 px-2 bg-emerald-500/10 border-emerald-500/20 text-emerald-500 text-[8px] font-black uppercase hover:bg-emerald-500 hover:text-white"
                        icon={<RocketOutlined className="text-[9px]" />}
                    >
                        Apply All Protocols
                    </Button>
                </div>
                <Table
                    dataSource={recommendations}
                    columns={columns}
                    pagination={false}
                    rowKey="description"
                    size="small"
                    className="dense-table"
                />
            </div>
        </div>
    );
};

export default CostDashboard;
