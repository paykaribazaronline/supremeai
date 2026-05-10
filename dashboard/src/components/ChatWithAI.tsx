// ChatWithAI.tsx - NEURAL LINK COMMAND INTERFACE
import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Space, message, Empty, Tag, List, Divider, Row, Col, Tooltip, Typography, Progress, Badge } from 'antd';
import { 
    SendOutlined, 
    RobotOutlined, 
    CheckCircleOutlined, 
    CloseCircleOutlined, 
    CopyOutlined,
    ThunderboltOutlined,
    ApiOutlined,
    SafetyCertificateOutlined,
    CloudServerOutlined,
    CodeOutlined,
    SyncOutlined,
    DatabaseOutlined,
    BulbOutlined,
    FileTextOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text } = Typography;

interface ChatMessage {
    id: string;
    sender: 'user' | 'ai';
    agent: string;
    content: string;
    timestamp: string;
    confidence?: number;
    intent?: string;
    status?: 'pending' | 'completed' | 'error';
}

const ChatWithAI: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [selectedAgent, setSelectedAgent] = useState('all');
    const [agents, setAgents] = useState<any[]>([]);
    const [knowledge, setKnowledge] = useState<{rules: any[], plans: any[]}>({ rules: [], plans: [] });
    const [systemStatus, setSystemStatus] = useState({ status: 'UP', version: '6.0.0-PRO' });
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        fetchAgents();
        fetchChatHistory();
        fetchKnowledge();
        const interval = setInterval(() => {
            fetchChatHistory();
            fetchKnowledge();
        }, 15000);
        return () => clearInterval(interval);
    }, []);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const fetchAgents = async () => {
        try {
            const response = await authUtils.fetchWithAuth('/api/ai/agents');
            if (response.ok) {
                const data = await response.json();
                setAgents(data);
            }
        } catch (error) {
            console.error('Failed to fetch agents');
        }
    };

    const fetchKnowledge = async () => {
        try {
            const [rulesRes, plansRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/rules'),
                authUtils.fetchWithAuth('/api/admin/plans')
            ]);
            if (rulesRes.ok && plansRes.ok) {
                const rules = await rulesRes.json();
                const plans = await plansRes.json();
                setKnowledge({ rules: rules.slice(0, 5), plans: plans.slice(0, 5) });
            }
        } catch (error) {
            console.error('Failed to fetch knowledge context');
        }
    };

    const fetchChatHistory = async () => {
        try {
            const user = authUtils.getCurrentUser();
            const userId = user?.uid || 'anonymous';
            const response = await authUtils.fetchWithAuth(`/api/chat/history?user_id=${userId}&limit=50`);
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.chat_history) {
                    const historyMessages = data.chat_history.map((item: any) => ({
                        id: item.id,
                        sender: item.is_admin ? 'ai' : 'user',
                        agent: item.is_admin ? 'SupremeAI' : 'Operator',
                        content: item.message,
                        timestamp: new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                        status: 'completed' as const,
                        intent: item.intent || 'NORMAL'
                    }));
                    setMessages(historyMessages);
                }
            }
        } catch (error) {
            console.error('Failed to fetch chat history');
        }
    };

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!input.trim()) return;

        const userMessage: ChatMessage = {
            id: Date.now().toString(),
            sender: 'user',
            agent: 'Operator',
            content: input,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        };

        setMessages(prev => [...prev, userMessage]);
        const currentInput = input;
        setInput('');
        setLoading(true);

        try {
            // Intelligent Intent Detection (Local Analysis before Backend)
            let detectedIntent = 'NORMAL';
            if (currentInput.toLowerCase().includes('rule') || currentInput.toLowerCase().includes('must') || currentInput.toLowerCase().includes('always')) {
                detectedIntent = 'RULE';
            } else if (currentInput.toLowerCase().includes('plan') || currentInput.toLowerCase().includes('roadmap') || currentInput.toLowerCase().includes('step')) {
                detectedIntent = 'PROJECT_PLAN';
            } else if (currentInput.toLowerCase().includes('run') || currentInput.toLowerCase().includes('execute') || currentInput.toLowerCase().includes('cmd')) {
                detectedIntent = 'COMMAND';
            }

            const response = await authUtils.fetchWithAuth('/api/chat/send', {
                method: 'POST',
                body: JSON.stringify({
                    message: currentInput,
                    agent: selectedAgent === 'all' ? null : selectedAgent,
                    detected_intent: detectedIntent // Pass hint to backend
                }),
            });

            if (response.ok) {
                const data = await response.json();
                const aiMessage: ChatMessage = {
                    id: (Date.now() + 1).toString(),
                    sender: 'ai',
                    agent: data.agent_name || 'Neural Core',
                    content: data.message || 'Processing optimized.',
                    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                    confidence: data.confidence ? Math.round(data.confidence * 100) : 98,
                    intent: data.intent || detectedIntent,
                    status: 'completed',
                };
                setMessages((prev) => [...prev, aiMessage]);
                
                // If a rule or plan was captured, update the knowledge sidepanel
                if (aiMessage.intent === 'RULE' || aiMessage.intent === 'PROJECT_PLAN') {
                    message.success(`SYSTEM_INTEL: Captured new ${aiMessage.intent}`);
                    fetchKnowledge();
                }
            }
        } catch (error: any) {
            message.error('NEURAL_LINK_ERROR: Request Timeout');
        } finally {
            setLoading(false);
        }
    };

    const getIntentColor = (intent?: string) => {
        switch (intent) {
            case 'RULE': return '#f5222d';
            case 'COMMAND': return '#52c41a';
            case 'PROJECT_PLAN': return '#1890ff';
            case 'DEBUG': return '#faad14';
            case 'INFO_COLLECTION': return '#722ed1';
            default: return 'rgba(255,255,255,0.2)';
        }
    };

    return (
        <div className="flex h-[700px] bg-[#050505] font-mono text-white overflow-hidden border border-white/5 shadow-2xl rounded-xl">
            {/* Left Column: Chat Interface */}
            <div className="flex-1 flex flex-col border-r border-white/5 relative">
                {/* Header / Telemetry Bar */}
                <div className="px-4 py-3 bg-white/[0.02] border-b border-white/5 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                            <CodeOutlined className="text-emerald-500 text-[14px]" />
                            <span className="text-[11px] font-black uppercase tracking-widest">Neural Link Command</span>
                        </div>
                        <div className="h-4 w-[1px] bg-white/10"></div>
                        <div className="flex items-center gap-1.5">
                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
                            <span className="text-[9px] font-black uppercase text-emerald-500/60">Core Sync: {systemStatus.status}</span>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <select 
                            value={selectedAgent} 
                            onChange={(e) => setSelectedAgent(e.target.value)}
                            className="bg-black/40 border border-white/10 text-[9px] px-2 py-1 rounded text-white/60 uppercase font-black outline-none hover:border-emerald-500/30 transition-colors"
                        >
                            <option value="all">Global Matrix</option>
                            {agents.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                        </select>
                        <span className="text-[9px] text-white/20 uppercase font-black tracking-tighter">v{systemStatus.version}</span>
                    </div>
                </div>

                {/* Chat Area */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar bg-[radial-gradient(circle_at_center,_transparent_0%,_rgba(0,0,0,0.4)_100%)]">
                    {messages.length === 0 ? (
                        <div className="h-full flex flex-col items-center justify-center opacity-10">
                            <RobotOutlined className="text-6xl mb-4" />
                            <span className="text-[12px] font-black uppercase tracking-[0.5em]">Awaiting Instruction</span>
                        </div>
                    ) : (
                        messages.map((msg) => (
                            <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} animate-fadeIn`}>
                                <div className={`max-w-[85%] flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'} gap-2`}>
                                    <div className="flex items-center gap-2 px-1">
                                        {msg.sender === 'ai' && <RobotOutlined className="text-[11px] text-emerald-500" />}
                                        <span className="text-[9px] font-black uppercase tracking-widest text-white/40">{msg.agent}</span>
                                        {msg.intent && msg.intent !== 'NORMAL' && (
                                            <Tag color={getIntentColor(msg.intent)} className="text-[8px] font-black border-none rounded-sm px-1.5 py-0 leading-tight">
                                                {msg.intent}
                                            </Tag>
                                        )}
                                        <span className="text-[8px] font-mono text-white/10">{msg.timestamp}</span>
                                    </div>
                                    <div className={`px-4 py-3 rounded-xl text-[12px] leading-relaxed relative glass-morphism ${
                                        msg.sender === 'user' 
                                        ? 'bg-blue-600/10 border border-blue-500/30 text-blue-100/90 rounded-tr-none' 
                                        : 'bg-white/[0.04] border border-white/10 text-white/80 rounded-tl-none shadow-[0_0_20px_rgba(0,0,0,0.5)]'
                                    }`}>
                                        {msg.content}
                                        {msg.sender === 'ai' && msg.confidence && (
                                            <div className="mt-3 pt-3 border-t border-white/5 flex items-center gap-3 opacity-50">
                                                <span className="text-[8px] font-black uppercase tracking-tighter">Confidence</span>
                                                <div className="flex-1 h-[2px] bg-white/5 rounded-full overflow-hidden">
                                                    <div 
                                                        className="h-full bg-emerald-500 shadow-[0_0_8px_#10b981]" 
                                                        style={{ width: `${msg.confidence}%` }}
                                                    ></div>
                                                </div>
                                                <span className="text-[8px] font-mono">{msg.confidence}%</span>
                                            </div>
                                        )}
                                    </div>
                                    {msg.sender === 'ai' && (
                                        <div className="flex gap-3 px-1 mt-1">
                                            <button onClick={() => { navigator.clipboard.writeText(msg.content); message.success('COPIED'); }} className="text-[8px] font-black uppercase text-white/20 hover:text-white/60 transition-colors flex items-center gap-1"><CopyOutlined /> Copy Payload</button>
                                            <button className="text-[8px] font-black uppercase text-white/20 hover:text-white/60 transition-colors flex items-center gap-1"><CodeOutlined /> Inspect Node</button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                    <div ref={messagesEndRef} />
                </div>

                {/* Input Area */}
                <div className="p-6 bg-white/[0.02] border-t border-white/5">
                    <form onSubmit={handleSendMessage} className="relative">
                        <Input
                            placeholder="INPUT COMMAND OR DEFINE RULE..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            disabled={loading}
                            className="bg-black/60 border-white/10 text-white placeholder:text-white/10 text-[12px] h-12 px-5 rounded-lg font-mono focus:border-emerald-500/40 transition-all shadow-[inset_0_1px_10px_rgba(0,0,0,0.8)]"
                        />
                        <button 
                            type="submit"
                            disabled={loading || !input.trim()}
                            className="absolute right-3 top-3 h-6 px-4 bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 text-emerald-500 rounded text-[10px] font-black uppercase transition-all disabled:opacity-20 flex items-center gap-2"
                        >
                            {loading ? <SyncOutlined spin /> : <><ThunderboltOutlined /> Run</>}
                        </button>
                    </form>
                    <div className="mt-4 flex items-center justify-between opacity-40">
                        <div className="flex gap-6">
                            <div className="flex items-center gap-2">
                                <ApiOutlined className="text-[12px] text-emerald-500/70" />
                                <span className="text-[8px] font-black uppercase tracking-widest">Multi-Agent Voting</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <SafetyCertificateOutlined className="text-[12px] text-blue-500/70" />
                                <span className="text-[8px] font-black uppercase tracking-widest">Autonomous Sync</span>
                            </div>
                        </div>
                        <span className="text-[8px] font-mono text-white/30 uppercase tracking-widest">Secure Terminal Session</span>
                    </div>
                </div>
            </div>

            {/* Right Column: Neural Knowledge Context */}
            <div className="w-[320px] bg-white/[0.01] flex flex-col p-5 overflow-y-auto custom-scrollbar border-l border-white/5">
                <div className="flex items-center gap-2 mb-6">
                    <DatabaseOutlined className="text-emerald-500 text-[16px]" />
                    <span className="text-[12px] font-black uppercase tracking-widest">Neural Knowledge</span>
                </div>

                <div className="space-y-8">
                    {/* Active Rules Section */}
                    <section>
                        <div className="flex items-center justify-between mb-3 border-b border-white/5 pb-2">
                            <div className="flex items-center gap-2">
                                <BulbOutlined className="text-red-500 text-[12px]" />
                                <span className="text-[10px] font-black uppercase tracking-widest text-white/60">Persistent Rules</span>
                            </div>
                            <Badge count={knowledge.rules.length} overflowCount={9} style={{ backgroundColor: '#f5222d', fontSize: '8px', height: '14px', lineHeight: '14px', minWidth: '14px' }} />
                        </div>
                        <div className="space-y-2">
                            {knowledge.rules.length === 0 ? (
                                <div className="text-[9px] text-white/10 uppercase italic text-center py-4">No active constraints detected</div>
                            ) : (
                                knowledge.rules.map((rule, idx) => (
                                    <div key={idx} className="p-3 bg-white/[0.02] border border-white/5 rounded-lg group hover:border-red-500/20 transition-all">
                                        <div className="text-[10px] text-white/70 line-clamp-2 leading-relaxed font-mono">
                                            {rule.content || rule.message}
                                        </div>
                                        <div className="mt-2 flex items-center justify-between opacity-30 group-hover:opacity-60 transition-opacity">
                                            <span className="text-[7px] font-black uppercase tracking-tighter">Auto-Captured</span>
                                            <span className="text-[7px] font-mono">CONF: {Math.round((rule.confidence || 0.9) * 100)}%</span>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </section>

                    {/* Project Plans Section */}
                    <section>
                        <div className="flex items-center justify-between mb-3 border-b border-white/5 pb-2">
                            <div className="flex items-center gap-2">
                                <FileTextOutlined className="text-blue-500 text-[12px]" />
                                <span className="text-[10px] font-black uppercase tracking-widest text-white/60">Project Blueprints</span>
                            </div>
                            <Badge count={knowledge.plans.length} overflowCount={9} style={{ backgroundColor: '#1890ff', fontSize: '8px', height: '14px', lineHeight: '14px', minWidth: '14px' }} />
                        </div>
                        <div className="space-y-2">
                            {knowledge.plans.length === 0 ? (
                                <div className="text-[9px] text-white/10 uppercase italic text-center py-4">No strategic roadmaps defined</div>
                            ) : (
                                knowledge.plans.map((plan, idx) => (
                                    <div key={idx} className="p-3 bg-white/[0.02] border border-white/5 rounded-lg group hover:border-blue-500/20 transition-all">
                                        <div className="text-[10px] text-white/70 line-clamp-2 leading-relaxed font-mono">
                                            {plan.content || plan.title}
                                        </div>
                                        <div className="mt-2 flex items-center justify-between opacity-30 group-hover:opacity-60 transition-opacity">
                                            <span className="text-[7px] font-black uppercase tracking-tighter">Roadmap Node</span>
                                            <div className="flex gap-1">
                                                <div className="w-1 h-1 rounded-full bg-blue-500"></div>
                                                <div className="w-1 h-1 rounded-full bg-blue-500/30"></div>
                                                <div className="w-1 h-1 rounded-full bg-blue-500/30"></div>
                                            </div>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </section>

                    {/* Operational Summary */}
                    <section className="mt-auto pt-10 border-t border-white/5">
                        <div className="p-4 rounded-xl bg-gradient-to-br from-emerald-500/10 to-transparent border border-emerald-500/20">
                            <div className="text-[9px] font-black uppercase tracking-widest text-emerald-500 mb-2">Neural Status</div>
                            <div className="flex justify-between text-[11px] mb-1">
                                <span className="text-white/40">Knowledge Nodes</span>
                                <span className="text-white font-mono">1,242</span>
                            </div>
                            <div className="flex justify-between text-[11px] mb-1">
                                <span className="text-white/40">Active Constraints</span>
                                <span className="text-white font-mono">{knowledge.rules.length}</span>
                            </div>
                            <div className="flex justify-between text-[11px]">
                                <span className="text-white/40">Memory Usage</span>
                                <span className="text-white font-mono">0.04%</span>
                            </div>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    );
};

export default ChatWithAI;
