// GitHubDashboard.tsx - GitHub Integration & Monitoring Tab
import React, { useState, useEffect } from 'react';
import {
    Card,
    Row,
    Col,
    Statistic,
    Table,
    Button,
    Form,
    Input,
    Modal,
    Tag,
    Steps,
    Alert,
    Spin,
    Space,
    Drawer,
    Timeline,
    Badge,
    Tooltip,
    Empty,
} from 'antd';
import {
    GithubOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    RocketOutlined,
    BugOutlined,
    PullRequestOutlined,
    FileOutlined,
    ReloadOutlined,
    DownloadOutlined,
} from '@ant-design/icons';

interface WorkflowRun {
    id: number;
    name: string;
    status: 'success' | 'failure' | 'pending' | 'in_progress';
    conclusion: string;
    created_at: string;
    updated_at: string;
    run_number: number;
    head_commit: { message: string };
}

interface Commit {
    hash: string;
    message: string;
}

interface RepositoryInfo {
    name: string;
    description: string;
    stars: number;
    forks: number;
    open_issues: number;
    created_at: string;
}

const GitHubDashboard: React.FC = () => {
    const [workflowRuns, setWorkflowRuns] = useState<WorkflowRun[]>([]);
    const [commits, setCommits] = useState<Commit[]>([]);
    const [repoInfo, setRepoInfo] = useState<RepositoryInfo | null>(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('workflows');
    const [form] = Form.useForm();
    const [issueModalVisible, setIssueModalVisible] = useState(false);
    const [rawLogsVisible, setRawLogsVisible] = useState(false);
    const [selectedRunLogs, setSelectedRunLogs] = useState<string>('');

    useEffect(() => {
        fetchGitHubData();
        const interval = setInterval(fetchGitHubData, 60000); // Refresh every 60s
        return () => clearInterval(interval);
    }, []);

    const getToken = () => localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');

    const fetchGitHubData = async () => {
        setLoading(true);
        try {
            const token = getToken();
            const headers = { 'Authorization': `Bearer ${token}` };

            // Fetch workflow runs
            const runsRes = await fetch('/api/github/workflow-runs?limit=10', { headers });
            if (runsRes.ok) {
                const data = await runsRes.json();
                setWorkflowRuns(data.runs || []);
            }

            // Fetch recent commits
            const commitsRes = await fetch('/api/git/logs?limit=15', { headers });
            if (commitsRes.ok) {
                const data = await commitsRes.json();
                setCommits(data.commits || []);
            }

            // Fetch repository info
            const repoRes = await fetch('/api/github/repo-info', { headers });
            if (repoRes.ok) {
                const data = await repoRes.json();
                setRepoInfo(data.repo);
            }
        } catch (error) {
            console.error('Failed to fetch GitHub data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateIssue = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch('/api/github/issue', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    title: values.title,
                    body: values.body,
                    label: values.label || 'bug',
                }),
            });

            if (response.ok) {
                const data = await response.json();
                Modal.success({
                    title: 'Issue Created',
                    content: `Issue #${data.issue_number} created successfully!`,
                });
                form.resetFields();
                setIssueModalVisible(false);
            } else {
                Modal.error({ title: 'Failed', content: 'Could not create issue' });
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const handleTriggerWorkflow = async (workflowName: string) => {
        try {
            const token = getToken();
            const response = await fetch('/api/github/trigger-workflow', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ workflow: workflowName, ref: 'main' }),
            });

            if (response.ok) {
                Modal.success({
                    title: 'Workflow Triggered',
                    content: `${workflowName} workflow has been triggered!`,
                });
                setTimeout(fetchGitHubData, 2000);
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const handleViewLogs = async (runId: number) => {
        try {
            const token = getToken();
            const response = await fetch(`/api/github/workflow-logs/${runId}`, {
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (response.ok) {
                const data = await response.json();
                setSelectedRunLogs(data.logs || 'No logs available');
                setRawLogsVisible(true);
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'success':
                return 'success';
            case 'failure':
                return 'error';
            case 'pending':
                return 'processing';
            case 'in_progress':
                return 'processing';
            default:
                return 'default';
        }
    };

    const workflowColumns = [
        {
            title: 'Workflow',
            dataIndex: 'name',
            key: 'name',
            render: (text: string) => <strong>{text}</strong>,
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Badge
                    status={getStatusColor(status)}
                    text={status.toUpperCase()}
                />
            ),
        },
        {
            title: 'Run #',
            dataIndex: 'run_number',
            key: 'run_number',
        },
        {
            title: 'Commit Message',
            dataIndex: ['head_commit', 'message'],
            key: 'message',
            ellipsis: true,
        },
        {
            title: 'Created',
            dataIndex: 'created_at',
            key: 'created_at',
            render: (date: string) => new Date(date).toLocaleString(),
        },
        {
            title: 'Actions',
            key: 'actions',
            render: (_: any, record: WorkflowRun) => (
                <Space>
                    <Button
                        type="link"
                        size="small"
                        onClick={() => handleViewLogs(record.id)}
                        icon={<DownloadOutlined />}
                    >
                        Logs
                    </Button>
                </Space>
            ),
        },
    ];

    const commitColumns = [
        {
            title: 'Hash',
            dataIndex: 'hash',
            key: 'hash',
            render: (hash: string) => (
                <Tooltip title={hash}>
                    <code>{hash.substring(0, 7)}</code>
                </Tooltip>
            ),
        },
        {
            title: 'Message',
            dataIndex: 'message',
            key: 'message',
            ellipsis: true,
            width: 400,
        },
    ];

    return (
        <div style={{ padding: '20px' }}>
            <Card title="🐙 GitHub Integration & Monitoring" bordered={false}>
                {/* Repository Overview */}
                {repoInfo && (
                    <Row gutter={16} style={{ marginBottom: '20px' }}>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic
                                title="Repository"
                                value={repoInfo.name}
                                prefix={<GithubOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic title="⭐ Stars" value={repoInfo.stars} />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic title="🍴 Forks" value={repoInfo.forks} />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic title="🐛 Open Issues" value={repoInfo.open_issues} />
                        </Col>
                    </Row>
                )}

                {/* Action Buttons */}
                <Alert
                    message="GitHub Integration Active"
                    description="Monitor CI/CD workflows, manage issues, and track commits directly from the admin dashboard."
                    type="info"
                    showIcon
                    style={{ marginBottom: '20px' }}
                />

                <Row gutter={16} style={{ marginBottom: '20px' }}>
                    <Col xs={24} sm={12} md={6}>
                        <Button
                            type="primary"
                            block
                            icon={<RocketOutlined />}
                            onClick={() => handleTriggerWorkflow('main.yml')}
                        >
                            Run Main Workflow
                        </Button>
                    </Col>
                    <Col xs={24} sm={12} md={6}>
                        <Button
                            type="default"
                            block
                            icon={<BugOutlined />}
                            onClick={() => setIssueModalVisible(true)}
                        >
                            Create Issue
                        </Button>
                    </Col>
                    <Col xs={24} sm={12} md={6}>
                        <Button
                            type="default"
                            block
                            icon={<ReloadOutlined />}
                            onClick={fetchGitHubData}
                            loading={loading}
                        >
                            Refresh
                        </Button>
                    </Col>
                </Row>

                {/* Workflow Runs */}
                <Card
                    title={
                        <span>
                            <RocketOutlined /> Latest Workflow Runs
                        </span>
                    }
                    style={{ marginBottom: '20px' }}
                >
                    <Spin spinning={loading}>
                        {workflowRuns.length > 0 ? (
                            <Table
                                columns={workflowColumns}
                                dataSource={workflowRuns}
                                size="small"
                                pagination={{ pageSize: 5 }}
                                rowKey="id"
                            />
                        ) : (
                            <Empty description="No workflow runs found" />
                        )}
                    </Spin>
                </Card>

                {/* Recent Commits */}
                <Card
                    title={
                        <span>
                            <FileOutlined /> Recent Commits
                        </span>
                    }
                    style={{ marginBottom: '20px' }}
                >
                    <Spin spinning={loading}>
                        {commits.length > 0 ? (
                            <Table
                                columns={commitColumns}
                                dataSource={commits}
                                size="small"
                                pagination={{ pageSize: 8 }}
                                rowKey="hash"
                            />
                        ) : (
                            <Empty description="No commits found" />
                        )}
                    </Spin>
                </Card>
            </Card>

            {/* Create Issue Modal */}
            <Modal
                title="Create GitHub Issue"
                visible={issueModalVisible}
                onCancel={() => setIssueModalVisible(false)}
                footer={null}
            >
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={handleCreateIssue}
                >
                    <Form.Item
                        label="Title"
                        name="title"
                        rules={[{ required: true, message: 'Please enter issue title' }]}
                    >
                        <Input placeholder="Issue title" />
                    </Form.Item>

                    <Form.Item
                        label="Description"
                        name="body"
                        rules={[{ required: true, message: 'Please enter description' }]}
                    >
                        <Input.TextArea
                            rows={4}
                            placeholder="Detailed description..."
                        />
                    </Form.Item>

                    <Form.Item label="Label" name="label">
                        <Input placeholder="bug, enhancement, etc." />
                    </Form.Item>

                    <Button type="primary" htmlType="submit" block>
                        Create Issue
                    </Button>
                </Form>
            </Modal>

            {/* Raw Logs Drawer */}
            <Drawer
                title="Workflow Logs"
                onClose={() => setRawLogsVisible(false)}
                open={rawLogsVisible}
                width={800}
            >
                <pre
                    style={{
                        backgroundColor: '#f5f5f5',
                        padding: '10px',
                        borderRadius: '4px',
                        maxHeight: '600px',
                        overflowY: 'auto',
                        fontSize: '12px',
                    }}
                >
                    {selectedRunLogs}
                </pre>
            </Drawer>
        </div>
    );
};

export default GitHubDashboard;
