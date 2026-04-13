// ChatHistoryDashboard.tsx - Real-time Chat & Work Process History
import React, { useState, useEffect, useRef } from 'react';
import {
    Alert,
    Card,
    Row,
    Col,
    Statistic,
    Table,
    Button,
    Input,
    Tag,
    Badge,
    Timeline,
    Empty,
    Spin,
    Space,
    Tooltip,
    Tabs,
    Divider,
    Select,
} from 'antd';
import {
    WechatOutlined as ChatOutlined,
    BugOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    FilterOutlined,
    ClearOutlined,
    ReloadOutlined,
    DownloadOutlined,
    SearchOutlined,
} from '@ant-design/icons';

interface ChatMessage {
    id: string;
    timestamp: string;
    sender: 'admin' | 'system' | 'user';
    message: string;
    type: 'info' | 'warning' | 'error' | 'success';
    projectId?: string;
    duration?: number;
}

interface WorkProcess {
    eventId: string;
    timestamp: string;
    eventType: string;
    projectId: string;
    status: 'pending' | 'running' | 'success' | 'failed';
    duration: number;
    details: string;
}

const ChatHistoryDashboard: React.FC = () => {
    const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
    const [workProcesses, setWorkProcesses] = useState<WorkProcess[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<'all' | 'errors' | 'success' | 'warnings'>('all');
    const [searchText, setSearchText] = useState('');
    const [autoRefresh, setAutoRefresh] = useState(true);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        loadChatHistory();
        loadWorkProcess();

        const interval = setInterval(() => {
            if (autoRefresh) {
                loadChatHistory();
                loadWorkProcess();
            }
        }, 5000); // Auto-refresh every 5 seconds

        return () => clearInterval(interval);
    }, [autoRefresh]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [chatMessages]);

    const loadChatHistory = async () => {
        try {
            const response = await fetch('/api/admin/chat-history?limit=5');
            if (response.ok) {
                const data = await response.json();
                // Keep only last 5 messages to reduce Firebase quota usage
                const messages = (data.messages || []).slice(-5);
                setChatMessages(messages);
            }
        } catch (error) {
            console.error('Failed to load chat history:', error);
        }
    };

    const loadWorkProcess = async () => {
        try {
            const response = await fetch('/api/admin/work-process');
            if (response.ok) {
                const data = await response.json();
                setWorkProcesses(data.processes || []);
                setLoading(false);
            }
        } catch (error) {
            console.error('Failed to load work process:', error);
            setLoading(false);
        }
    };

    const getMessageTypeColor = (type: string) => {
        switch (type) {
            case 'error':
                return 'red';
            case 'warning':
                return 'orange';
            case 'success':
                return 'green';
            default:
                return 'blue';
        }
    };

    const getSenderColor = (sender: string) => {
        switch (sender) {
            case 'admin':
                return 'purple';
            case 'system':
                return 'cyan';
            case 'user':
                return 'blue';
            default:
                return 'default';
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'success':
                return 'success';
            case 'failed':
                return 'error';
            case 'running':
                return 'processing';
            case 'pending':
                return 'warning';
            default:
                return 'default';
        }
    };

    const filteredMessages = chatMessages.filter((msg) => {
        if (filter === 'errors' && msg.type !== 'error') return false;
        if (filter === 'success' && msg.type !== 'success') return false;
        if (filter === 'warnings' && msg.type !== 'warning') return false;
        if (searchText && !msg.message.toLowerCase().includes(searchText.toLowerCase())) {
            return false;
        }
        return true;
    });

    const chatColumns = [
        {
            title: 'Time',
            dataIndex: 'timestamp',
            key: 'timestamp',
            width: 180,
            render: (text: string) => <span>{new Date(text).toLocaleTimeString()}</span>,
        },
        {
            title: 'Sender',
            dataIndex: 'sender',
            key: 'sender',
            width: 100,
            render: (sender: string) => (
                <Tag color={getSenderColor(sender)}>{sender.toUpperCase()}</Tag>
            ),
        },
        {
            title: 'Type',
            dataIndex: 'type',
            key: 'type',
            width: 100,
            render: (type: string) => (
                <Tag color={getMessageTypeColor(type)}>{type}</Tag>
            ),
        },
        {
            title: 'Message',
            dataIndex: 'message',
            key: 'message',
            ellipsis: true,
            render: (text: string) => (
                <Tooltip title={text}>
                    <span>{text}</span>
                </Tooltip>
            ),
        },
    ];

    const processColumns = [
        {
            title: 'Event ID',
            dataIndex: 'eventId',
            key: 'eventId',
            width: 120,
            render: (id: string) => <code>{id.substring(0, 8)}</code>,
        },
        {
            title: 'Type',
            dataIndex: 'eventType',
            key: 'eventType',
            width: 120,
        },
        {
            title: 'Project',
            dataIndex: 'projectId',
            key: 'projectId',
            width: 100,
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (status: string) => (
                <Badge
                    status={getStatusColor(status) as any}
                    text={status.toUpperCase()}
                />
            ),
        },
        {
            title: 'Duration (ms)',
            dataIndex: 'duration',
            key: 'duration',
            width: 120,
            render: (duration: number) => <span>{duration}ms</span>,
        },
        {
            title: 'Details',
            dataIndex: 'details',
            key: 'details',
            ellipsis: true,
            render: (text: string) => (
                <Tooltip title={text}>
                    <span>{text}</span>
                </Tooltip>
            ),
        },
    ];

    return (
        <div style={{ padding: '20px' }}>
            {/* Header Stats */}
            <Row gutter={16} style={{ marginBottom: '20px' }}>
                <Col xs={24} sm={6}>
                    <Statistic
                        title="Recent Messages"
                        value={chatMessages.length}
                        suffix="/ 5"
                        prefix={<ChatOutlined />}
                    />
                </Col>
                <Col xs={24} sm={6}>
                    <Statistic
                        title="Total Processes"
                        value={workProcesses.length}
                        suffix=" tasks"
                        precision={0}
                    />
                </Col>
                <Col xs={24} sm={6}>
                    <Statistic
                        title="Errors"
                        value={chatMessages.filter((m) => m.type === 'error').length}
                        valueStyle={{ color: '#ff4d4f' }}
                        prefix={<BugOutlined />}
                    />
                </Col>
                <Col xs={24} sm={6}>
                    <Statistic
                        title="Success"
                        value={workProcesses.filter((p) => p.status === 'success').length}
                        valueStyle={{ color: '#52c41a' }}
                        prefix={<CheckCircleOutlined />}
                    />
                </Col>
            </Row>

            <Divider />

            {/* Controls */}
            <Card style={{ marginBottom: '20px' }}>
                <Space wrap>
                    <Input
                        placeholder="Search messages..."
                        prefix={<SearchOutlined />}
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                        style={{ width: 250 }}
                    />
                    <Select
                        value={filter}
                        onChange={setFilter}
                        style={{ width: 150 }}
                        options={[
                            { label: 'All Messages', value: 'all' },
                            { label: 'Errors Only', value: 'errors' },
                            { label: 'Success Only', value: 'success' },
                            { label: 'Warnings Only', value: 'warnings' },
                        ]}
                    />
                    <Button
                        type={autoRefresh ? 'primary' : 'default'}
                        onClick={() => setAutoRefresh(!autoRefresh)}
                    >
                        {autoRefresh ? '🔄 Auto Refresh ON' : '⏸ Auto Refresh OFF'}
                    </Button>
                    <Button icon={<ReloadOutlined />} onClick={() => {
                        loadChatHistory();
                        loadWorkProcess();
                    }} />
                    <Button
                        icon={<ClearOutlined />}
                        onClick={() => {
                            setSearchText('');
                            setFilter('all');
                        }}
                    >
                        Clear
                    </Button>
                </Space>
            </Card>

            <Spin spinning={loading}>
                <Tabs
                    defaultActiveKey="chat"
                    items={[
                        {
                            key: 'chat',
                            label: `💬 Recent Messages (${filteredMessages.length} max 5)`,
                            children: (
                                <Card>
                                    <Alert
                                        message="Showing last 5 messages only (optimized for Firebase quota)"
                                        type="info"
                                        showIcon
                                        style={{ marginBottom: '16px' }}
                                    />
                                    {filteredMessages.length > 0 ? (
                                        <div style={{
                                            maxHeight: '600px',
                                            overflowY: 'auto',
                                            border: '1px solid #f0f0f0',
                                            padding: '10px',
                                            borderRadius: '4px',
                                        }}>
                                            <Timeline items={filteredMessages.map((msg) => ({
                                                dot: <Badge status={getStatusColor(msg.type) as any} />,
                                                children: (
                                                    <div>
                                                        <Space>
                                                            <Tag color={getSenderColor(msg.sender)}>
                                                                {msg.sender}
                                                            </Tag>
                                                            <Tag color={getMessageTypeColor(msg.type)}>
                                                                {msg.type}
                                                            </Tag>
                                                            <span style={{ fontSize: '12px', color: '#999' }}>
                                                                {new Date(msg.timestamp).toLocaleTimeString()}
                                                            </span>
                                                        </Space>
                                                        <p style={{ marginTop: '8px', marginBottom: 0 }}>
                                                            {msg.message}
                                                        </p>
                                                        {msg.projectId && (
                                                            <small style={{ color: '#999' }}>
                                                                Project: {msg.projectId}
                                                            </small>
                                                        )}
                                                    </div>
                                                ),
                                            }))} />
                                            <div ref={messagesEndRef} />
                                        </div>
                                    ) : (
                                        <Empty description="No messages" />
                                    )}
                                </Card>
                            ),
                        },
                        {
                            key: 'process',
                            label: `⚙️ Work Process (${workProcesses.length})`,
                            children: (
                                <Card>
                                    <Table
                                        columns={processColumns}
                                        dataSource={workProcesses.map((p) => ({
                                            ...p,
                                            key: p.eventId,
                                        }))}
                                        pagination={{ pageSize: 10 }}
                                        size="small"
                                    />
                                </Card>
                            ),
                        },
                        {
                            key: 'combined',
                            label: '📊 Live Timeline',
                            children: (
                                <Card>
                                    <Timeline
                                        items={[
                                            ...chatMessages.map((msg) => ({
                                                label: new Date(msg.timestamp).toLocaleTimeString(),
                                                children: (
                                                    <p>
                                                        <Tag color={getSenderColor(msg.sender)}>
                                                            {msg.sender}
                                                        </Tag>{' '}
                                                        {msg.message}
                                                    </p>
                                                ),
                                            })),
                                            ...workProcesses.map((process) => ({
                                                label: new Date(process.timestamp).toLocaleTimeString(),
                                                children: (
                                                    <p>
                                                        <Badge
                                                            status={getStatusColor(process.status) as any}
                                                            text={process.eventType}
                                                        />{' '}
                                                        - {process.details}
                                                    </p>
                                                ),
                                            })),
                                        ].sort((a, b) => {
                                            const timeA = parseInt(a.label.split(':')[0]) || 0;
                                            const timeB = parseInt(b.label.split(':')[0]) || 0;
                                            return timeB - timeA; // Most recent first
                                        })}
                                    />
                                </Card>
                            ),
                        },
                    ]}
                />
            </Spin>
        </div>
    );
};

export default ChatHistoryDashboard;
