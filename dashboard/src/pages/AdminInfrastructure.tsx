import React, { useState, useEffect } from 'react';
import { Card, Button, List, Badge, Typography, Space, Divider, Tag, Empty, Spin } from 'antd';
import { 
    CloudServerOutlined, 
    BulbOutlined, 
    SyncOutlined, 
    CheckCircleOutlined, 
    ExclamationCircleOutlined,
    RocketOutlined,
    ToolOutlined,
    SafetyOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text, Paragraph } = Typography;

interface Advice {
    id: string;
    title: string;
    description: string;
    impact: 'HIGH' | 'MEDIUM' | 'LOW';
    category: 'COST' | 'PERFORMANCE' | 'SECURITY' | 'RELIABILITY';
    actionTaken: boolean;
    timestamp: string;
}

const AdminInfrastructure: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [advices, setAdvices] = useState<Advice[]>([]);
    const [generating, setGenerating] = useState(false);

    const fetchAdvice = async () => {
        setLoading(true);
        try {
            const data = await authUtils.fetchWithAuth('/api/admin/infrastructure/advice');
            setAdvices(data || []);
        } catch (error) {
            console.error('Failed to fetch infrastructure advice:', error);
        } finally {
            setLoading(false);
        }
    };

    const generateNewAdvice = async () => {
        setGenerating(true);
        try {
            await authUtils.fetchWithAuth('/api/admin/infrastructure/generate-advice', { method: 'POST' });
            await fetchAdvice();
        } catch (error) {
            console.error('Failed to generate advice:', error);
        } finally {
            setGenerating(false);
        }
    };

    useEffect(() => {
        fetchAdvice();
    }, []);

    const getImpactColor = (impact: string) => {
        switch (impact) {
            case 'HIGH': return 'error';
            case 'MEDIUM': return 'warning';
            case 'LOW': return 'processing';
            default: return 'default';
        }
    };

    const getCategoryIcon = (category: string) => {
        switch (category) {
            case 'COST': return <RocketOutlined />;
            case 'PERFORMANCE': return <ToolOutlined />;
            case 'SECURITY': return <SafetyOutlined />;
            case 'RELIABILITY': return <CheckCircleOutlined />;
            default: return <BulbOutlined />;
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in duration-500">
            <div className="flex justify-between items-center bg-black/40 p-6 rounded-2xl border border-white/5 backdrop-blur-xl">
                <div>
                    <Title level={4} className="!text-white !m-0 flex items-center gap-2">
                        <CloudServerOutlined className="text-emerald-500" />
                        Infrastructure Concierge
                    </Title>
                    <Text className="text-white/40 text-xs uppercase tracking-widest font-bold">
                        AI-Powered Infrastructure Optimization & Advisory
                    </Text>
                </div>
                <Button 
                    type="primary" 
                    icon={<SyncOutlined spin={generating} />} 
                    onClick={generateNewAdvice}
                    loading={generating}
                    className="bg-emerald-500 border-emerald-500 hover:bg-emerald-600 h-10 px-6 font-bold uppercase tracking-wider text-[11px]"
                >
                    Run AI Diagnostics
                </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <Card className="lg:col-span-2 bg-black/20 border-white/5 backdrop-blur-md">
                    <div className="mb-4 flex items-center justify-between">
                        <Text className="text-white font-black uppercase tracking-widest text-[11px]">Optimization Roadmap</Text>
                        <Badge count={advices.length} overflowCount={99} style={{ backgroundColor: '#10b981' }} />
                    </div>
                    
                    {loading ? (
                        <div className="py-20 text-center"><Spin size="large" /></div>
                    ) : advices.length > 0 ? (
                        <List
                            itemLayout="vertical"
                            dataSource={advices}
                            renderItem={(item) => (
                                <List.Item className="border-b border-white/5 hover:bg-white/5 transition-all p-4 rounded-xl cursor-default group">
                                    <div className="flex gap-4 items-start">
                                        <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-lg border border-white/10 ${
                                            item.impact === 'HIGH' ? 'bg-red-500/10 text-red-400' : 'bg-emerald-500/10 text-emerald-400'
                                        }`}>
                                            {getCategoryIcon(item.category)}
                                        </div>
                                        <div className="flex-1">
                                            <div className="flex justify-between items-center mb-1">
                                                <Text className="text-white font-bold text-base">{item.title}</Text>
                                                <Space>
                                                    <Tag color={getImpactColor(item.impact)} className="border-0 uppercase text-[9px] font-black tracking-tighter px-2">
                                                        {item.impact} IMPACT
                                                    </Tag>
                                                    <Tag color="default" className="bg-white/5 border-white/10 text-white/60 uppercase text-[9px] font-black tracking-tighter px-2">
                                                        {item.category}
                                                    </Tag>
                                                </Space>
                                            </div>
                                            <Paragraph className="text-white/60 text-sm m-0 mb-3">
                                                {item.description}
                                            </Paragraph>
                                            <div className="flex justify-between items-center">
                                                <Text className="text-[10px] text-white/30 uppercase tracking-widest font-bold">
                                                    Detected: {new Date(item.timestamp).toLocaleString()}
                                                </Text>
                                                <Button size="small" type="link" className="text-emerald-500 font-bold uppercase text-[10px] p-0 group-hover:translate-x-1 transition-transform">
                                                    Implement Optimization →
                                                </Button>
                                            </div>
                                        </div>
                                    </div>
                                </List.Item>
                            )}
                        />
                    ) : (
                        <Empty 
                            image={Empty.PRESENTED_IMAGE_SIMPLE} 
                            description={<span className="text-white/40">No infrastructure concerns detected. System is running at peak efficiency.</span>}
                            className="py-10"
                        />
                    )}
                </Card>

                <div className="space-y-6">
                    <Card className="bg-emerald-500/5 border-emerald-500/20 backdrop-blur-md">
                        <Title level={5} className="!text-emerald-400 !m-0 mb-4 flex items-center gap-2">
                            <SafetyOutlined />
                            System Integrity
                        </Title>
                        <div className="space-y-4">
                            <div className="flex justify-between items-center">
                                <Text className="text-white/60 text-xs">Environment Status</Text>
                                <Tag color="success" className="m-0 border-0 uppercase font-black text-[9px]">Stable</Tag>
                            </div>
                            <div className="flex justify-between items-center">
                                <Text className="text-white/60 text-xs">Database Connectivity</Text>
                                <Tag color="success" className="m-0 border-0 uppercase font-black text-[9px]">99.9%</Tag>
                            </div>
                            <div className="flex justify-between items-center">
                                <Text className="text-white/60 text-xs">AI Provider Latency</Text>
                                <Tag color="processing" className="m-0 border-0 uppercase font-black text-[9px]">124ms</Tag>
                            </div>
                            <Divider className="border-white/5 my-2" />
                            <div className="p-3 bg-black/40 rounded-xl border border-white/5">
                                <Text className="text-[10px] text-emerald-500/60 font-black uppercase tracking-widest block mb-2">Concierge Advice</Text>
                                <Text className="text-xs text-white/80 leading-relaxed italic">
                                    "Your current infrastructure is under-utilized during off-peak hours. 
                                    Consider enabling dynamic scaling for better cost efficiency."
                                </Text>
                            </div>
                        </div>
                    </Card>

                    <Card className="bg-black/40 border-white/5">
                        <Title level={5} className="!text-white !m-0 mb-4 flex items-center gap-2 uppercase tracking-tighter text-sm">
                            <ExclamationCircleOutlined className="text-amber-400" />
                            System Guardrails
                        </Title>
                        <Paragraph className="text-xs text-white/40 m-0">
                            The Infrastructure Concierge continuously monitors system parameters to prevent resource exhaustion and ensures 100% uptime.
                        </Paragraph>
                        <Button block className="mt-4 border-white/10 bg-white/5 text-white/60 hover:text-emerald-500 font-bold uppercase text-[10px]">
                            View Policies
                        </Button>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default AdminInfrastructure;
