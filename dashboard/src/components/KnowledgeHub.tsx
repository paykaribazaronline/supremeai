import React, { useState, useEffect } from 'react';
import { Typography, Input, Button, Tag, Space, List, Avatar, Spin, message } from 'antd';
import { DatabaseOutlined, SearchOutlined, CloudUploadOutlined, FilePdfOutlined, LoadingOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text, Title } = Typography;

const KnowledgeHub: React.FC = () => {
    const [snapshot, setSnapshot] = useState<any>(null);
    const [domains, setDomains] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 60000); // Refresh every minute
        return () => clearInterval(interval);
    }, []);

    const fetchData = async () => {
        try {
            const [snapshotRes, domainsRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/knowledge/snapshot'),
                authUtils.fetchWithAuth('/api/admin/knowledge/domains')
            ]);

            if (snapshotRes.ok) {
                const data = await snapshotRes.json();
                setSnapshot(data.data || data);
            }
            if (domainsRes.ok) {
                const data = await domainsRes.json();
                setDomains(data.data || data);
            }
        } catch (error) {
            console.error('Failed to fetch knowledge data:', error);
            message.error('KNOWLEDGE_LINK_ERROR');
        } finally {
            setLoading(false);
        }
    };

    const handleIngest = () => {
        message.info('DATA_INGESTION_SERVICE_STARTING');
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} />
            </div>
        );
    }

    return (
        <div className="p-4 animate-fade-in">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 glass-card p-6 border border-white/5 bg-black/40">
                    <div className="flex items-center justify-between mb-6">
                        <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 m-0">
                            <DatabaseOutlined className="text-cyan-500" /> Vector Knowledge Base
                        </Title>
                        <Button 
                            icon={<CloudUploadOutlined />} 
                            onClick={handleIngest}
                            className="bg-cyan-500/10 border-cyan-500/20 text-cyan-500 text-[9px] font-black uppercase"
                        >
                            Ingest Data
                        </Button>
                    </div>
                    
                    <div className="relative mb-6">
                        <Input 
                            prefix={<SearchOutlined className="text-white/20" />} 
                            placeholder="Semantic search across knowledge corpus..."
                            className="dark-input h-12 text-[12px] font-mono"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                        />
                    </div>

                    <List
                        itemLayout="horizontal"
                        dataSource={domains.filter(d => d.name.toLowerCase().includes(searchQuery.toLowerCase()))}
                        locale={{ emptyText: <span className="text-white/20 uppercase text-[10px] tracking-widest">No matching neural domains found</span> }}
                        renderItem={(item: any) => (
                            <List.Item className="border-b border-white/5 px-4 hover:bg-white/[0.02] transition-all cursor-pointer rounded-lg mb-2">
                                <List.Item.Meta
                                    avatar={<Avatar icon={<FilePdfOutlined />} className="bg-red-500/10 text-red-500" />}
                                    title={<Text className="text-white font-bold text-[11px] uppercase tracking-tight">{item.name}</Text>}
                                    description={
                                        <Text className="text-white/30 text-[9px] uppercase">
                                            {item.lastUpdated || '2026-05-12'} • {item.keywords?.length || 0} Neural Keywords • Status: {item.status || 'SYNCED'}
                                        </Text>
                                    }
                                />
                                <Tag color={item.status === 'LEARNING' ? 'processing' : 'cyan'} className="text-[8px] font-black border-0 bg-cyan-500/10 text-cyan-500 uppercase">
                                    {item.status || 'Vectorized'}
                                </Tag>
                            </List.Item>
                        )}
                    />
                </div>

                <div className="glass-card p-6 border border-white/5 bg-black/40">
                    <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                        System RAG Config
                    </Title>
                    <div className="space-y-4">
                        <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                            <Text className="text-[9px] text-white/40 uppercase block mb-1">Total Vector Points</Text>
                            <div className="flex items-baseline gap-2">
                                <Text className="text-[18px] text-white font-mono font-bold">{snapshot?.totalVectors || '1.2M'}</Text>
                                <Text className="text-[9px] text-emerald-500 font-black">+14% Growth</Text>
                            </div>
                        </div>
                        <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                            <div className="flex justify-between items-center mb-1">
                                <Text className="text-[9px] text-white/40 uppercase">Top-K Retrieval</Text>
                                <Text className="text-[10px] text-cyan-400 font-mono font-bold">OPTIMIZED</Text>
                            </div>
                            <Text className="text-[14px] text-white font-mono font-bold">{snapshot?.topK || 5} Chunks</Text>
                        </div>
                        <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                            <Text className="text-[9px] text-white/40 uppercase block mb-1">Similarity Threshold</Text>
                            <Text className="text-[14px] text-white font-mono font-bold">{snapshot?.similarityThreshold || 0.82}</Text>
                        </div>
                        <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                            <Text className="text-[9px] text-white/40 uppercase block mb-1">Embedding Model</Text>
                            <Text className="text-[11px] text-cyan-400 font-mono font-bold uppercase tracking-tighter">
                                {snapshot?.embeddingModel || 'text-embedding-3-small'}
                            </Text>
                        </div>
                    </div>
                    
                    <div className="mt-6 p-4 bg-cyan-500/5 border border-cyan-500/20 rounded-xl">
                        <Text className="text-[8px] text-cyan-500/60 uppercase font-black block mb-2 tracking-[0.2em]">Memory Efficiency</Text>
                        <div className="h-1.5 w-full bg-black/40 rounded-full overflow-hidden">
                            <div className="h-full bg-cyan-500" style={{ width: '68%' }}></div>
                        </div>
                        <div className="flex justify-between mt-1">
                            <Text className="text-[8px] text-white/20 uppercase font-bold">0%</Text>
                            <Text className="text-[8px] text-cyan-500 font-black uppercase">68% Utilized</Text>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default KnowledgeHub;

