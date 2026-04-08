// ChatWithAI.tsx - Chat Interface for Commanding AI Agents

import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, Space, message, Spin, Empty, Tag, List, Divider, Row, Col, Avatar, Tooltip } from 'antd';
import { SendOutlined, DeleteOutlined, CopyOutlined, RobotOutlined } from '@ant-design/icons';

interface ChatMessage {
    id: string;
    sender: 'user' | 'ai';
    agent: string;
    content: string;
    timestamp: string;
    confidence?: number;
    status?: 'pending' | 'completed' | 'error';
}

const ChatWithAI: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [selectedAgent, setSelectedAgent] = useState('all');
    const [agents, setAgents] = useState<any[]>([]);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        fetchAgents();
        fetchChatHistory();
        const interval = setInterval(fetchChatHistory, 5000);
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
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/ai/agents', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setAgents(data);
            }
        } catch (error) {
            console.error('Failed to fetch agents');
        }
    };

    const fetchChatHistory = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const url = selectedAgent === 'all'
                ? '/api/chat/history'
                : `/api/chat/history?agent=${selectedAgent}`;

            const response = await fetch(url, {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setMessages(data);
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
            agent: selectedAgent,
            content: input,
            timestamp: new Date().toLocaleTimeString(),
        };

        setMessages([...messages, userMessage]);
        setInput('');
        setLoading(true);

        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/chat/send', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: input,
                    agent: selectedAgent === 'all' ? undefined : selectedAgent,
                }),
            });

            if (response.ok) {
                const data = await response.json();
                const aiMessage: ChatMessage = {
                    id: (Date.now() + 1).toString(),
                    sender: 'ai',
                    agent: data.agentName,
                    content: data.response,
                    timestamp: new Date().toLocaleTimeString(),
                    confidence: data.confidence,
                    status: data.status,
                };
                setMessages((prev) => [...prev, aiMessage]);
            } else {
                message.error('Failed to send message');
            }
        } catch (error) {
            message.error('Error communicating with AI');
        } finally {
            setLoading(false);
        }
    };

    const handleClearChat = () => {
        setMessages([]);
        message.success('Chat cleared');
    };

    const handleCopyMessage = (content: string) => {
        navigator.clipboard.writeText(content);
        message.success('Copied to clipboard');
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 'calc(100vh - 300px)' }}>
            <Card
                title="Chat with AI Agents"
                style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
                bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: '16px' }}
                extra={
                    <Space>
                        {agents.length > 0 && (
                            <>
                                <span style={{ marginRight: '8px' }}>Agent:</span>
                                <select
                                    value={selectedAgent}
                                    onChange={(e) => {
                                        setSelectedAgent(e.target.value);
                                        fetchChatHistory();
                                    }}
                                    style={{
                                        padding: '4px 8px',
                                        borderRadius: '4px',
                                        border: '1px solid #d9d9d9',
                                    }}
                                >
                                    <option value="all">All Agents</option>
                                    {agents.map((agent) => (
                                        <option key={agent.id} value={agent.id}>
                                            {agent.name}
                                        </option>
                                    ))}
                                </select>
                            </>
                        )}
                        <Button size="small" danger onClick={handleClearChat}>
                            Clear Chat
                        </Button>
                    </Space>
                }
            >
                <div
                    style={{
                        flex: 1,
                        overflowY: 'auto',
                        marginBottom: '16px',
                        padding: '16px',
                        backgroundColor: '#fafafa',
                        borderRadius: '4px',
                        minHeight: '300px',
                    }}
                >
                    {messages.length === 0 ? (
                        <Empty description="No messages yet. Start chatting!" />
                    ) : (
                        <List
                            dataSource={messages}
                            renderItem={(msg) => (
                                <div
                                    key={msg.id}
                                    style={{
                                        marginBottom: '16px',
                                        display: 'flex',
                                        justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                                    }}
                                >
                                    <div
                                        style={{
                                            maxWidth: '70%',
                                            padding: '12px 16px',
                                            borderRadius: '8px',
                                            backgroundColor: msg.sender === 'user' ? '#1890ff' : '#f0f2f5',
                                            color: msg.sender === 'user' ? '#fff' : '#000',
                                        }}
                                    >
                                        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '4px' }}>
                                            {msg.sender === 'ai' && (
                                                <>
                                                    <RobotOutlined style={{ marginRight: '8px' }} />
                                                    <strong>{msg.agent}</strong>
                                                </>
                                            )}
                                        </div>
                                        <div>{msg.content}</div>
                                        <div
                                            style={{
                                                fontSize: '11px',
                                                marginTop: '8px',
                                                opacity: 0.8,
                                            }}
                                        >
                                            {msg.timestamp}
                                            {msg.confidence && ` | Confidence: ${msg.confidence}%`}
                                            {msg.status && (
                                                <>
                                                    {' '}
                                                    |{' '}
                                                    <Tag color={msg.status === 'completed' ? 'green' : msg.status === 'error' ? 'red' : 'orange'}>
                                                        {msg.status.toUpperCase()}
                                                    </Tag>
                                                </>
                                            )}
                                        </div>
                                        {msg.sender === 'ai' && (
                                            <Button
                                                type="text"
                                                size="small"
                                                icon={<CopyOutlined />}
                                                onClick={() => handleCopyMessage(msg.content)}
                                                style={{ marginTop: '8px' }}
                                            >
                                                Copy
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            )}
                        />
                    )}
                    <div ref={messagesEndRef} />
                </div>

                <Divider />

                <form onSubmit={handleSendMessage}>
                    <Space.Compact style={{ width: '100%' }}>
                        <Input
                            placeholder="Type a command or question... e.g., 'Analyze last 7 days sales', 'Optimize database', 'Generate report'"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            disabled={loading}
                            size="large"
                        />
                        <Button
                            type="primary"
                            icon={<SendOutlined />}
                            loading={loading}
                            onClick={(e) => handleSendMessage(e as any)}
                            size="large"
                        >
                            Send
                        </Button>
                    </Space.Compact>
                </form>

                <div style={{ marginTop: '16px', fontSize: '12px', color: '#999' }}>
                    <strong>Quick Commands:</strong>
                    <div>• analyze [data/topic]</div>
                    <div>• optimize [system/code/process]</div>
                    <div>• generate [report/insight/plan]</div>
                    <div>• execute [task/command]</div>
                    <div>• status [component/system]</div>
                </div>
            </Card>
        </div>
    );
};

export default ChatWithAI;
