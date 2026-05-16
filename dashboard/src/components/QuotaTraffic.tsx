import React, { useState, useEffect } from 'react';
import { Typography, Progress, Space, Tag, List, Statistic, Spin, Divider } from 'antd';
import { ThunderboltOutlined, AreaChartOutlined, LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title } = Typography;

const QuotaTraffic: React.FC = () => {
    const [stats, setStats] = useState<any>(null);
    const [providers, setProviders] = useState<any[]>([]);
    const [rankings, setRankings] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [contractRes, providersRes, rankingsRes] = await Promise.all([
                    authUtils.fetchWithAuth('/api/admin/dashboard/contract'),
                    authUtils.fetchWithAuth('/api/admin/providers/configured'),
                    authUtils.fetchWithAuth('/api/admin/providers/rankings')
                ]);

                if (contractRes.ok) {
                    const contractData = await contractRes.json();
                    setStats(contractData.data?.stats || contractData.stats);
                }

                if (providersRes.ok) {
                    const providersData = await providersRes.json();
                    setProviders(providersData.data?.providers || providersData.providers || []);
                }

                if (rankingsRes.ok) {
                    const rankingsData = await rankingsRes.json();
                    setRankings(rankingsData.data?.rankings || rankingsData.rankings);
                }
            } catch (error) {
                console.error('Failed to fetch quota data:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
        const interval = setInterval(fetchData, 5000); // Synchronized to 5s
        return () => clearInterval(interval);
    }, []);

    if (loading && !stats) return <div className="p-12 text-center"><Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} /></div>;

    return (
        <div className="p-4 animate-fade-in">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                <div className="ai-card p-6 border border-white/10 bg-black/40">
                    <Statistic 
                        title={<span className="text-[10px] font-black text-dim uppercase tracking-widest">Total Active Users</span>}
                        value={stats?.activeUsers || 0}
                        prefix={<AreaChartOutlined style={{ color: 'var(--neon-blue)' }} />}
                        valueStyle={{ color: 'var(--text-main)', fontSize: '28px', fontWeight: '900', fontFamily: 'var(--font-mono)' }}
                    />
                    <div className="mt-3 flex items-center gap-2">
                        <span className="text-[10px] font-black text-neon bg-neon/10 px-2 py-0.5 rounded border border-neon/20 uppercase">
                            Out of {stats?.totalUsers || 0} total
                        </span>
                    </div>
                </div>
                <div className="ai-card p-6 border border-white/10 bg-black/40">
                    <Statistic 
                        title={<span className="text-[10px] font-black text-dim uppercase tracking-widest">System Health</span>}
                        value={stats?.systemHealthScore || 0}
                        suffix="%"
                        valueStyle={{ color: stats?.systemHealthScore > 90 ? 'var(--success)' : 'var(--warning)', fontSize: '28px', fontWeight: '900', fontFamily: 'var(--font-mono)' }}
                    />
                    <div className="mt-3 flex items-center gap-2">
                        <Text className={`text-[10px] font-black uppercase ${stats?.systemHealthScore > 90 ? 'text-success' : 'text-warning'}`}>
                            {stats?.systemHealthStatus || 'Healthy'}
                        </Text>
                    </div>
                </div>
                <div className="ai-card p-6 border border-white/10 bg-black/40">
                    <Statistic 
                        title={<span className="text-[10px] font-black text-dim uppercase tracking-widest">Active Connections</span>}
                        value={stats?.activeConnections || 0}
                        prefix={<ThunderboltOutlined style={{ color: 'var(--neon-purple)' }} />}
                        valueStyle={{ color: 'var(--text-main)', fontSize: '28px', fontWeight: '900', fontFamily: 'var(--font-mono)' }}
                    />
                    <div className="mt-3 flex items-center gap-2">
                        <span className="text-[10px] font-black text-purple bg-purple/10 px-2 py-0.5 rounded border border-purple/20 uppercase" style={{ color: 'var(--neon-purple)', borderColor: 'rgba(188, 19, 254, 0.2)', backgroundColor: 'rgba(188, 19, 254, 0.1)' }}>
                            {stats?.serverUptime || '0m'} Uptime
                        </span>
                    </div>
                </div>
            </div>

            <div className="ai-card p-8 border border-white/10">
                <Title level={5} className="!text-main !text-[14px] font-black uppercase tracking-[0.2em] flex items-center gap-3 mb-8">
                    <ThunderboltOutlined className="text-warning" /> Resource Quota by Provider
                </Title>
                
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                    <div>
                        <Text className="text-dim text-[10px] uppercase font-black block mb-5 tracking-widest">Connected Infrastructure</Text>
                        <List
                            dataSource={providers}
                            renderItem={(item: any) => (
                                <List.Item className="border-b border-white/5 px-0 py-4">
                                    <div className="flex items-center justify-between w-full">
                                        <div className="flex items-center gap-4">
                                            {item.status === 'ONLINE' ? 
                                                <CheckCircleOutlined className="text-success text-[12px]" /> : 
                                                <CloseCircleOutlined className="text-error text-[12px]" />
                                            }
                                            <div className="flex flex-col">
                                                <Text className="text-main text-[12px] font-black uppercase tracking-tight">{item.name}</Text>
                                                <Text className="text-dim text-[9px] font-mono uppercase opacity-70">{item.type || 'LLM'}</Text>
                                            </div>
                                        </div>
                                        <span className={`text-[9px] font-black uppercase px-2 py-0.5 rounded border ${item.status === 'ONLINE' ? 'bg-success/10 text-success border-success/20' : 'bg-error/10 text-error border-error/20'}`}>
                                            {item.status}
                                        </span>
                                    </div>
                                </List.Item>
                            )}
                        />
                    </div>
                    
                    <div className="bg-white/[0.03] p-6 rounded-2xl border border-white/10">
                        <Text className="text-dim text-[10px] uppercase font-black block mb-6 tracking-widest">Neural Ranking Matrix</Text>
                        {rankings ? (
                            <div className="space-y-6">
                                {Object.entries(rankings).map(([name, data]: [string, any]) => {
                                    const successRate = (data.successCount / (data.totalTasks || 1)) * 100;
                                    return (
                                        <div key={name} className="space-y-2">
                                            <div className="flex justify-between items-end">
                                                <Text className="text-[11px] font-black text-main uppercase">{name}</Text>
                                                <Text className="text-[10px] font-mono text-neon font-bold">{successRate.toFixed(1)}% SUCCESS</Text>
                                            </div>
                                            <Progress 
                                                percent={successRate} 
                                                showInfo={false} 
                                                size={[100, 4]} 
                                                strokeColor={successRate > 90 ? 'var(--success)' : successRate > 70 ? 'var(--warning)' : 'var(--error)'}
                                                trailColor="rgba(255,255,255,0.08)"
                                            />
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div className="h-40 flex items-center justify-center border border-dashed border-white/10 rounded-xl">
                                <Text className="text-dim text-[11px] uppercase font-black opacity-40">No ranking data synchronized</Text>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default QuotaTraffic;

