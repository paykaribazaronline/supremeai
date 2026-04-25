// ChatWithAI.tsx - Chat Interface for Commanding AI Agents

import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, Space, message, Empty, Tag, List, Divider, Row, Col, Tooltip, Alert, Typography } from 'antd';
import { SendOutlined, DeleteOutlined, CopyOutlined, RobotOutlined, CheckCircleOutlined, CloseCircleOutlined, InfoCircleOutlined } from '@ant-design/icons';

const { Text, Paragraph } = Typography;

interface ChatMessage {
    id: string;
    sender: 'user' | 'ai';
    agent: string;
    content: string;
    timestamp: string;
    confidence?: number;
    status?: 'pending' | 'completed' | 'error';
    processingTimeMs?: number;
    modelsUsed?: number;
    errorCode?: string;
}

interface ModelStatus {
    name: string;
    supportsImages: boolean;
    supportsVision: boolean;
    status: 'online' | 'offline' | 'degraded';
    lastChecked: string;
}

const ChatWithAI: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [selectedAgent, setSelectedAgent] = useState('all');
    const [agents, setAgents] = useState<any[]>([]);
    const [modelStatuses, setModelStatuses] = useState<ModelStatus[]>([
        { name: 'Google Gemini', supportsImages: true, supportsVision: true, status: 'online', lastChecked: new Date().toISOString() },
        { name: 'OpenAI GPT-4', supportsImages: true, supportsVision: true, status: 'online', lastChecked: new Date().toISOString() },
        { name: 'Meta Llama', supportsImages: false, supportsVision: false, status: 'online', lastChecked: new Date().toISOString() },
        { name: 'Tencent Hunyuan', supportsImages: false, supportsVision: false, status: 'online', lastChecked: new Date().toISOString() },
    ]);
    const [systemStatus, setSystemStatus] = useState<{ status: string; message: string; version: string }>({
        status: 'UP',
        message: 'Checking...',
        version: '6.0.0'
    });
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        fetchAgents();
        fetchChatHistory();
        fetchSystemStatus();
        const interval = setInterval(() => {
            fetchChatHistory();
            fetchSystemStatus();
        }, 5000);
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

    const fetchSystemStatus = async () => {
        try {
            const response = await fetch('/api/status');
            if (response.ok) {
                const data = await response.json();
                setSystemStatus(data);
            }
        } catch (error) {
            setSystemStatus({ status: 'DOWN', message: 'Unable to reach server', version: 'unknown' });
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
                    agent: data.agentName || 'AI System',
                    content: data.response || data.message,
                    timestamp: new Date().toLocaleTimeString(),
                    confidence: data.confidence,
                    status: data.status || 'completed',
                    processingTimeMs: data.processingTimeMs,
                    modelsUsed: data.modelsUsed,
                };
                setMessages((prev) => [...prev, aiMessage]);
            } else {
                const errorData = await response.json().catch(() => ({}));
                const errorMessage: ChatMessage = {
                    id: (Date.now() + 1).toString(),
                    sender: 'ai',
                    agent: 'System',
                    content: errorData.error || `ERROR: ${response.status} - Failed to process request`,
                    timestamp: new Date().toLocaleTimeString(),
                    status: 'error',
                    errorCode: errorData.error?.includes('image') ? 'UNSUPPORTED_MEDIA_TYPE' : 'REQUEST_FAILED',
                };
                setMessages((prev) => [...prev, errorMessage]);
            }
        } catch (error: any) {
            const errorMessage: ChatMessage = {
                id: (Date.now() + 1).toString(),
                sender: 'ai',
                agent: 'System',
                content: `ERROR: ${error.message || 'Communication failed'}`,
                timestamp: new Date().toLocaleTimeString(),
                status: 'error',
                errorCode: 'NETWORK_ERROR',
            };
            setMessages((prev) => [...prev, errorMessage]);
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
            {/* System Status Bar */}
            <Card size="small" style={{ marginBottom: '16px' }}>
                <Row gutter={16} align="middle">
                    <Col>
                        <Space>
                            <Tag color={systemStatus.status === 'UP' ? 'green' : 'red'} icon={systemStatus.status === 'UP' ? <CheckCircleOutlined /> : <CloseCircleOutlined />}>
                                System: {systemStatus.status}
                            </Tag>
                            <Text type="secondary">v{systemStatus.version}</Text>
                        </Space>
                    </Col>
                    <Col flex="auto">
                        <Space wrap>
                            {modelStatuses.map((model) => (
                                <Tooltip key={model.name} title={`${model.name} - Images: ${model.supportsImages ? 'Yes' : 'No'} | Vision: ${model.supportsVision ? 'Yes' : 'No'}`}>
                                    <Tag color={model.status === 'online' ? 'blue' : 'orange'}>
                                        {model.name}
                                        {!model.supportsImages && <span style={{ marginLeft: '4px' }}>🚫🖼️</span>}
                                    </Tag>
                                </Tooltip>
                            ))}
                        </Space>
                    </Col>
                </Row>
            </Card>

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
                                            backgroundColor: msg.sender === 'user' ? '#1890ff' : msg.status === 'error' ? '#fff2f0' : '#f0f2f5',
                                            color: msg.sender === 'user' ? '#fff' : '#000',
                                            border: msg.status === 'error' ? '1px solid #ffccc7' : 'none',
                                        }}
                                    >
                                        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '4px' }}>
                                            {msg.sender === 'ai' && (
                                                <>
                                                    <RobotOutlined style={{ marginRight: '8px' }} />
                                                    <strong>{msg.agent}</strong>
                                                    {msg.status === 'error' && <CloseCircleOutlined style={{ marginLeft: '8px', color: '#ff4d4f' }} />}
                                                </>
                                            )}
                                        </div>
                                        <div>
                                            {msg.status === 'error' ? (
                                                <Alert
                                                    type="error"
                                                    message={msg.content}
                                                    description={msg.errorCode && <Text code>{msg.errorCode}</Text>}
                                                    showIcon
                                                    style={{ padding: '8px', margin: 0 }}
                                                />
                                            ) : (
                                                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{msg.content}</Paragraph>
                                            )}
                                        </div>
                                        <div
                                            style={{
                                                fontSize: '11px',
                                                marginTop: '8px',
                                                opacity: 0.8,
                                            }}
                                        >
                                            {msg.timestamp}
                                            {msg.confidence && ` | Confidence: ${msg.confidence}%`}
                                            {msg.processingTimeMs && ` | ${msg.processingTimeMs}ms`}
                                            {msg.modelsUsed && ` | ${msg.modelsUsed} models`}
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
                                        {msg.sender === 'ai' && msg.status !== 'error' && (
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

                <Divider style={{ margin: '16px 0' }} />

                <div style={{ fontSize: '11px', color: '#999' }}>
                    <Space>
                        <InfoCircleOutlined />
                        <Text type="secondary">
                            Models without 🖼️ icon don't support image input.
                            Errors will show in chat with error codes for debugging.
                        </Text>
                    </Space>
                </div>
            </Card>
        </div>
    );
};

export default ChatWithAI;
