import React from 'react';
import { Typography, Empty, Space, Tag, List, Card } from 'antd';
import { SafetyCertificateOutlined, NodeIndexOutlined, CheckCircleOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

const ConsensusMap: React.FC = () => {
    return (
        <div className="p-4 animate-fade-in">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="glass-card p-6 border border-white/5 bg-black/40">
                    <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                        <NodeIndexOutlined className="text-purple-500" /> Multi-Agent Consensus Logic
                    </Title>
                    <div className="space-y-4">
                        <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                            <div className="flex justify-between items-center mb-2">
                                <Text className="text-white font-black text-[10px] uppercase">Voting Protocol: Majority Wins</Text>
                                <Tag color="purple" className="text-[8px] font-black uppercase">Active</Tag>
                            </div>
                            <Text className="text-white/40 text-[9px] uppercase leading-tight block">
                                Requester asks a question. 3 Models (GPT-4o, Claude 3.5, Gemini 1.5) generate answers. 
                                Evaluator model checks for consistency. If 2/3 agree, response is accepted.
                            </Text>
                        </div>
                        <div className="p-4 bg-white/[0.02] border border-white/5 rounded-xl opacity-50">
                            <div className="flex justify-between items-center mb-2">
                                <Text className="text-white/40 font-black text-[10px] uppercase">Voting Protocol: Critique & Refine</Text>
                                <Tag color="default" className="text-[8px] font-black uppercase">Standby</Tag>
                            </div>
                            <Text className="text-white/20 text-[9px] uppercase leading-tight block">
                                Model A generates code. Model B critiques and identifies bugs. Model A fixes based on feedback.
                            </Text>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-6 border border-white/5 bg-black/40">
                    <Title level={5} className="!text-white !text-[12px] uppercase tracking-widest flex items-center gap-2 mb-6">
                        <SafetyCertificateOutlined className="text-emerald-500" /> Active Consensus Sessions
                    </Title>
                    <Empty 
                        image={Empty.PRESENTED_IMAGE_SIMPLE} 
                        description={<span className="text-[10px] text-white/20 uppercase tracking-widest font-black">No active logic sessions detected</span>}
                        className="py-12"
                    />
                </div>
            </div>
        </div>
    );
};

export default ConsensusMap;
