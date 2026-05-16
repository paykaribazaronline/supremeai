import React, { useState, useEffect } from 'react';
import { Typography, Badge, Space, List, Tag, Timeline, Spin, Button, Card, Progress, Statistic, Row, Col, message, Tooltip, Empty } from 'antd';
import { 
    BugOutlined, 
    CheckCircleOutlined, 
    WarningOutlined, 
    SyncOutlined, 
    ThunderboltOutlined,
    HistoryOutlined,
    ApiOutlined,
    DashboardOutlined,
    SafetyCertificateOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title } = Typography;

interface HealingEvent {
    id: string;
    timestamp: string;
    errorType: string;
    errorMessage: string;
    strategyApplied: string;
    fixAction: string;
    success: boolean;
    reasoning: string;
    component: string;
}

interface APIHealthReport {
    id: string;
    totalKeysTested: number;
    activeKeys: number;
    deadKeys: number;
    rotationDueKeys: number;
    deadKeyDetails: Array<{id: string, label: string, provider: string, error: string}>;
    createdAt: string;
}

const SelfHealingDashboard: React.FC = () => {
    const [events, setEvents] = useState<HealingEvent[]>([]);
    const [healthReport, setHealthReport] = useState<APIHealthReport | null>(null);
    const [status, setStatus] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);

    const fetchData = async () => {
        try {
            const [historyRes, statusRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/self-healing/history'),
                authUtils.fetchWithAuth('/api/self-healing/status')
            ]);

            if (historyRes.ok) {
                const data = await historyRes.json();
                setEvents(data || []);
            }

            if (statusRes.ok) {
                const data = await statusRes.json();
                setStatus(data);
            }
        } catch (error) {
            console.error('Failed to fetch self-healing data:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 30000); // Poll every 30s
        return () => clearInterval(interval);
    }, []);

    const runHealthCheck = async () => {
        setActionLoading(true);
        try {
            const res = await authUtils.fetchWithAuth('/api/self-healing/health-check', { method: 'POST' });
            if (res.ok) {
                const data = await res.json();
                if (data && data.length > 0) {
                    setHealthReport(data[0]);
                    message.success('Proactive health check completed successfully.');
                }
            }
        } catch (error) {
            message.error('Health check failed.');
        } finally {
            setActionLoading(false);
        }
    };

    const getStatusColor = (success: boolean) => success ? '#52c41a' : '#ff4d4f';

    return (
        <div className="p-6 space-y-6 animate-fade-in bg-black/20 rounded-3xl border border-white/10 backdrop-blur-md">
            {/* Header Stats */}
            <Row gutter={[24, 24]}>
                <Col xs={24} sm={12} lg={6}>
                    <Card className="glass-card border-none bg-gradient-to-br from-black/60 to-black/40 hover:from-black/50 hover:to-black/30 transition-all duration-500">
                        <Statistic 
                            title={<span className="text-white/40 uppercase text-[10px] tracking-widest font-bold">System Stability</span>}
                            value={98.5}
                            precision={1}
                            suffix="%"
                            valueStyle={{ color: '#52c41a', fontFamily: 'Orbitron, sans-serif', fontSize: '24px' }}
                            prefix={<SafetyCertificateOutlined className="mr-2" />}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card className="glass-card border-none bg-gradient-to-br from-black/60 to-black/40 hover:from-black/50 hover:to-black/30 transition-all duration-500">
                        <Statistic 
                            title={<span className="text-white/40 uppercase text-[10px] tracking-widest font-bold">Auto-Fixes (Total)</span>}
                            value={events.filter(e => e.success).length}
                            valueStyle={{ color: '#1890ff', fontFamily: 'Orbitron, sans-serif', fontSize: '24px' }}
                            prefix={<ThunderboltOutlined className="mr-2" />}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card className="glass-card border-none bg-gradient-to-br from-black/60 to-black/40 hover:from-black/50 hover:to-black/30 transition-all duration-500">
                        <Statistic 
                            title={<span className="text-white/40 uppercase text-[10px] tracking-widest font-bold">Critical Failures</span>}
                            value={events.filter(e => !e.success).length}
                            valueStyle={{ color: '#ff4d4f', fontFamily: 'Orbitron, sans-serif', fontSize: '24px' }}
                            prefix={<BugOutlined className="mr-2" />}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                    <Card className="glass-card border-none bg-gradient-to-br from-black/60 to-black/40 hover:from-black/50 hover:to-black/30 transition-all duration-500">
                        <div className="flex flex-col">
                            <span className="text-white/40 uppercase text-[10px] tracking-widest font-bold mb-2">Engine Status</span>
                            <div className="flex items-center gap-3">
                                <div className={`w-3 h-3 rounded-full animate-pulse ${status?.status === 'active' ? 'bg-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.5)]' : 'bg-red-500'}`} />
                                <span className="text-white font-bold uppercase text-[14px] tracking-tighter" style={{ fontFamily: 'Orbitron, sans-serif' }}>{status?.status || 'OFFLINE'}</span>
                            </div>
                        </div>
                    </Card>
                </Col>
            </Row>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Healing Timeline */}
                <div className="lg:col-span-2 glass-card p-8 border border-white/10 bg-black/40 rounded-3xl backdrop-blur-xl">
                    <div className="flex justify-between items-center mb-8">
                        <Title level={4} className="!text-white !text-[14px] uppercase tracking-[0.2em] font-light flex items-center gap-3 m-0">
                            <HistoryOutlined className="text-blue-500" /> Neural Healing Trace
                        </Title>
                        <Button 
                            type="text" 
                            icon={<SyncOutlined spin={loading} className="text-[18px]" />} 
                            className="text-white/30 hover:text-white hover:bg-white/5 rounded-full w-10 h-10 flex items-center justify-center transition-all"
                            onClick={() => { setLoading(true); fetchData(); }}
                        />
                    </div>
                    
                    <div className="bg-black/60 rounded-2xl p-6 font-mono text-[11px] h-[550px] overflow-y-auto custom-scrollbar border border-white/5 shadow-inner">
                        {loading && events.length === 0 ? (
                            <div className="h-full flex flex-col items-center justify-center gap-6">
                                <div className="relative">
                                    <div className="absolute inset-0 bg-blue-500 blur-2xl opacity-20 animate-pulse" />
                                    <Spin size="large" />
                                </div>
                                <span className="text-white/30 uppercase tracking-[0.3em] text-[10px] animate-pulse">Syncing with Core Engine...</span>
                            </div>
                        ) : events.length === 0 ? (
                            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<span className="text-white/20 uppercase tracking-widest text-[10px]">No healing events recorded</span>} />
                        ) : (
                            <Timeline 
                                className="dark-timeline"
                                items={events.map(event => ({
                                    color: getStatusColor(event.success),
                                    dot: <div className={`w-3 h-3 rounded-full ${event.success ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.4)]' : 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.4)]'}`} />,
                                    children: (
                                        <div className="flex flex-col gap-3 p-5 bg-white/5 rounded-2xl border border-white/5 mb-6 hover:bg-white/10 transition-all duration-300 group">
                                            <div className="flex justify-between items-start">
                                                <div className="flex flex-col">
                                                    <span className="text-white font-bold text-[13px] tracking-tight group-hover:text-blue-400 transition-colors">{event.errorType}</span>
                                                    <span className="text-white/40 text-[10px] mt-1">{new Date(event.timestamp).toLocaleString()}</span>
                                                </div>
                                                <Tag color={event.success ? 'green' : 'red'} className="text-[9px] uppercase font-bold border-none bg-black/40 px-3 py-1 rounded-full tracking-widest">
                                                    {event.strategyApplied}
                                                </Tag>
                                            </div>
                                            <div className="bg-black/40 p-3 rounded-xl text-white/70 italic border border-white/5 text-[12px] leading-relaxed">
                                                {event.errorMessage}
                                            </div>
                                            <div className="flex items-start gap-3 mt-1">
                                                <ThunderboltOutlined className="text-yellow-500 mt-1" />
                                                <div className="flex flex-col gap-1">
                                                    <span className="text-white/90 font-bold text-[12px]">Resolved via:</span>
                                                    <span className="text-emerald-400 font-mono bg-emerald-500/10 px-3 py-1 rounded-lg border border-emerald-500/20">{event.fixAction}</span>
                                                </div>
                                            </div>
                                            {event.reasoning && (
                                                <div className="text-[11px] text-white/40 mt-2 p-3 bg-white/2 rounded-xl border border-white/5">
                                                    <span className="font-bold uppercase tracking-widest text-[9px] text-white/20 block mb-1">AI Reasoning Engine</span>
                                                    <span className="leading-relaxed italic">{event.reasoning}</span>
                                                </div>
                                            )}
                                            <div className="flex justify-end mt-2">
                                                <span className="text-[9px] text-white/20 uppercase tracking-widest font-mono">ID: {event.id.slice(0, 8)} | SRC: {event.component}</span>
                                            </div>
                                        </div>
                                    )
                                }))}
                            />
                        )}
                    </div>
                </div>

                {/* API Health & Controls */}
                <div className="space-y-8">
                    <div className="glass-card p-8 border border-white/10 bg-gradient-to-b from-black/60 to-black/40 rounded-3xl backdrop-blur-xl">
                        <Title level={5} className="!text-white !text-[12px] uppercase tracking-[0.2em] mb-8 flex items-center gap-3">
                            <ApiOutlined className="text-emerald-500" /> API Health Status
                        </Title>
                        
                        {healthReport ? (
                            <div className="space-y-8">
                                <div>
                                    <div className="flex justify-between items-end mb-3">
                                        <span className="text-white/40 text-[10px] uppercase tracking-widest font-bold">Active Capacity</span>
                                        <span className="text-emerald-400 font-bold" style={{ fontFamily: 'Orbitron, sans-serif' }}>
                                            {Math.round((healthReport.activeKeys / healthReport.totalKeysTested) * 100)}%
                                        </span>
                                    </div>
                                    <Progress 
                                        percent={Math.round((healthReport.activeKeys / healthReport.totalKeysTested) * 100)} 
                                        status="active" 
                                        strokeColor={{
                                            '0%': '#10b981',
                                            '100%': '#34d399',
                                        }}
                                        trailColor="rgba(255,255,255,0.03)"
                                        showInfo={false}
                                        strokeWidth={8}
                                    />
                                </div>
                                
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="text-center p-4 bg-white/3 rounded-2xl border border-white/5 hover:bg-white/5 transition-colors">
                                        <div className="text-[20px] font-bold text-white" style={{ fontFamily: 'Orbitron, sans-serif' }}>{healthReport.totalKeysTested}</div>
                                        <div className="text-[9px] text-white/30 uppercase tracking-tighter mt-1">Total Keys</div>
                                    </div>
                                    <div className="text-center p-4 bg-white/3 rounded-2xl border border-white/5 hover:bg-white/5 transition-colors">
                                        <div className="text-[20px] font-bold text-emerald-400" style={{ fontFamily: 'Orbitron, sans-serif' }}>{healthReport.activeKeys}</div>
                                        <div className="text-[9px] text-white/30 uppercase tracking-tighter mt-1">Active</div>
                                    </div>
                                    <div className="text-center p-4 bg-white/3 rounded-2xl border border-white/5 hover:bg-white/5 transition-colors">
                                        <div className="text-[20px] font-bold text-red-400" style={{ fontFamily: 'Orbitron, sans-serif' }}>{healthReport.deadKeys}</div>
                                        <div className="text-[9px] text-white/30 uppercase tracking-tighter mt-1">Dead</div>
                                    </div>
                                    <div className="text-center p-4 bg-white/3 rounded-2xl border border-white/5 hover:bg-white/5 transition-colors">
                                        <div className="text-[20px] font-bold text-yellow-400" style={{ fontFamily: 'Orbitron, sans-serif' }}>{healthReport.rotationDueKeys}</div>
                                        <div className="text-[9px] text-white/30 uppercase tracking-tighter mt-1">Due Rotation</div>
                                    </div>
                                </div>

                                {healthReport.deadKeyDetails.length > 0 && (
                                    <div className="mt-8 bg-red-500/5 p-4 rounded-2xl border border-red-500/10">
                                        <span className="text-red-400 text-[10px] uppercase font-black tracking-widest mb-4 block">Critical Anomalies Found</span>
                                        <List
                                            size="small"
                                            dataSource={healthReport.deadKeyDetails.slice(0, 3)}
                                            renderItem={item => (
                                                <List.Item className="border-none !p-0 mb-3 last:mb-0">
                                                    <div className="flex items-center gap-3 w-full">
                                                        <div className="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(239,68,68,0.5)]" />
                                                        <div className="flex flex-col">
                                                            <span className="text-white/80 text-[11px] font-bold uppercase tracking-tight">{item.provider}</span>
                                                            <span className="text-white/30 text-[9px] truncate max-w-[180px] font-mono">{item.error}</span>
                                                        </div>
                                                    </div>
                                                </List.Item>
                                            )}
                                        />
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="text-center py-16 bg-black/40 rounded-3xl border border-dashed border-white/5 shadow-inner">
                                <ApiOutlined className="text-5xl text-white/5 mb-6" />
                                <div className="text-[10px] text-white/20 uppercase tracking-[0.2em] font-light">Awaiting Diagnostic Signal</div>
                            </div>
                        )}

                        <Button 
                            type="primary" 
                            icon={<ThunderboltOutlined spin={actionLoading} />} 
                            block 
                            className="mt-10 h-14 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 border-none hover:from-emerald-500 hover:to-teal-500 transition-all duration-300 uppercase tracking-[0.2em] text-[11px] font-black shadow-[0_4px_15px_rgba(16,185,129,0.2)]"
                            onClick={runHealthCheck}
                            loading={actionLoading}
                        >
                            Execute Proactive Scan
                        </Button>
                    </div>

                    <div className="glass-card p-8 border border-white/10 bg-black/60 rounded-3xl backdrop-blur-md">
                        <Title level={5} className="!text-white !text-[12px] uppercase tracking-[0.2em] mb-6 font-light">
                            Operational Core
                        </Title>
                        <Space direction="vertical" className="w-full" size={16}>
                            <Button block ghost className="border-white/5 text-white/30 hover:text-white hover:border-white/20 hover:bg-white/5 h-12 rounded-xl text-[10px] uppercase tracking-widest font-bold transition-all">
                                Re-Index Healing Models
                            </Button>
                            <Button block ghost className="border-white/5 text-white/30 hover:text-white hover:border-white/20 hover:bg-white/5 h-12 rounded-xl text-[10px] uppercase tracking-widest font-bold transition-all">
                                Clear Audit Cache
                            </Button>
                            <Button block danger ghost className="h-12 rounded-xl text-[10px] uppercase tracking-[0.2em] font-black border-red-500/20 hover:bg-red-500/10 transition-all">
                                Emergency Kill Switch
                            </Button>
                        </Space>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SelfHealingDashboard;
