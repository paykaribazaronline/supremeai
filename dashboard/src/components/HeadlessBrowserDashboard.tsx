import React, { useState, useEffect } from 'react';
import { authUtils } from '../lib/authUtils';
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
    Alert,
    Spin,
    Space,
    Drawer,
    Timeline,
    Badge,
    Tooltip,
    Checkbox,
    InputNumber,
    Select,
    Empty,
    Progress,
    Switch,
    Divider,
    Popconfirm,
} from 'antd';
import {
    ChromeOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    RocketOutlined,
    CameraOutlined,
    DownloadOutlined,
    ReloadOutlined,
    ExclamationOutlined,
    DatabaseOutlined,
    BarChartOutlined,
} from '@ant-design/icons';

interface BrowserStats {
    puppeteerAvailable: boolean;
    quotaRemaining: number;
    quotaLimit: number;
    dailyUsage: number;
    lastUsed: string;
    healthStatus: 'healthy' | 'warning' | 'degraded';
}

interface ScrapeResult {
    url: string;
    status: 'success' | 'failed' | 'pending';
    title?: string;
    content?: string;
    screenshotPath?: string;
    timestamp: string;
    duration: number; // ms
}

interface AuditLog {
    id: string;
    url: string;
    action: 'scrape' | 'screenshot' | 'extract';
    status: 'success' | 'failed';
    timestamp: string;
    user: string;
    duration: number;
}

const HeadlessBrowserDashboard: React.FC = () => {
    const [browserStats, setBrowserStats] = useState<BrowserStats | null>(null);
    const [scrapeHistory, setScrapeHistory] = useState<ScrapeResult[]>([]);
    const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(true);
    const [scraping, setScraping] = useState(false);
    const [form] = Form.useForm();
    const [credentialsForm] = Form.useForm();
    const [scrapeModalVisible, setScrapeModalVisible] = useState(false);
    const [resultDrawerVisible, setResultDrawerVisible] = useState(false);
    const [credentialsModalVisible, setCredentialsModalVisible] = useState(false);
    const [selectedResult, setSelectedResult] = useState<ScrapeResult | null>(null);
    const [useAuth, setUseAuth] = useState(false);
    const [autoLoginEnabled, setAutoLoginEnabled] = useState(false);
    const [storedCredentials, setStoredCredentials] = useState<any[]>([]);

    useEffect(() => {
        fetchBrowserData();
        const interval = setInterval(fetchBrowserData, 60000); // Refresh every 60s
        return () => clearInterval(interval);
    }, []);

    const getToken = () => authUtils.getToken();

    const fetchBrowserData = async () => {
        setLoading(true);
        try {
            const token = getToken();
            const headers = { 'Authorization': `Bearer ${token}` };

            // Fetch browser stats
            const statsRes = await fetch('/api/browser/stats', { headers });
            if (statsRes.ok) {
                const data = await statsRes.json();
                setBrowserStats(data.stats);
            }

            // Fetch audit logs
            const auditRes = await fetch('/api/browser/audit', { headers });
            if (auditRes.ok) {
                const data = await auditRes.json();
                setAuditLogs(data.logs || []);
            }

            // Fetch auto-login settings
            const autoLoginRes = await fetch('/api/browser/auto-login/settings', { headers });
            if (autoLoginRes.ok) {
                const data = await autoLoginRes.json();
                setAutoLoginEnabled(data.enabled || false);
                setStoredCredentials(data.credentials || []);
            }
        } catch (error) {
            console.error('Failed to fetch browser data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleScrapeURL = async (values: any) => {
        try {
            setScraping(true);
            const token = getToken();
            const endpoint = useAuth ? '/api/browser/scrape-auth' : '/api/browser/scrape';

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    url: values.url,
                    includeScreenshot: values.screenshot || false,
                    timeout: values.timeout || 30,
                    username: useAuth ? values.username : undefined,
                    password: useAuth ? values.password : undefined,
                    autoLogin: autoLoginEnabled,
                }),
            });

            if (response.ok) {
                const data = await response.json();
                const result: ScrapeResult = {
                    url: values.url,
                    status: 'success',
                    title: data.title,
                    content: data.content,
                    screenshotPath: data.screenshot,
                    timestamp: new Date().toLocaleString(),
                    duration: data.duration || 0,
                };

                setScrapeHistory([result, ...scrapeHistory]);
                Modal.success({
                    title: 'Scraping Complete',
                    content: `Successfully scraped ${values.url}`,
                });
                form.resetFields();
                setScrapeModalVisible(false);
            } else {
                Modal.error({
                    title: 'Scrape Failed',
                    content: (await response.json()).message || 'Unknown error',
                });
            }
        } catch (error) {
            Modal.error({
                title: 'Error',
                content: String(error),
            });
        } finally {
            setScraping(false);
        }
    };

    const handleTakeScreenshot = async (url: string) => {
        try {
            setScraping(true);
            const token = getToken();

            const response = await fetch('/api/browser/screenshot', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ url, timeout: 30 }),
            });

            if (response.ok) {
                const data = await response.json();
                Modal.success({
                    title: 'Screenshot Captured',
                    content: `Screenshot saved: ${data.path}`,
                    okText: 'OK',
                });
            } else {
                Modal.error({
                    title: 'Failed',
                    content: 'Could not capture screenshot',
                });
            }
        } catch (error) {
            Modal.error({
                title: 'Error',
                content: String(error),
            });
        } finally {
            setScraping(false);
        }
    };

    const handleToggleAutoLogin = async (enabled: boolean) => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/auto-login/toggle', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ enabled }),
            });

            if (response.ok) {
                setAutoLoginEnabled(enabled);
                Modal.success({
                    title: 'Auto-Login Updated',
                    content: `Auto-Login ${enabled ? 'enabled' : 'disabled'}`,
                });
            } else {
                Modal.error({ title: 'Failed', content: 'Could not update setting' });
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const handleSaveCredentials = async (values: any) => {
        try {
            const token = getToken();
            const response = await fetch('/api/browser/auto-login/credentials', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    website: values.website,
                    username: values.username,
                    password: values.password,
                    selectorUsername: values.selectorUsername,
                    selectorPassword: values.selectorPassword,
                    selectorSubmit: values.selectorSubmit,
                }),
            });

            if (response.ok) {
                Modal.success({ title: 'Success', content: 'Credentials saved securely!' });
                credentialsForm.resetFields();
                setCredentialsModalVisible(false);
                fetchBrowserData();
            } else {
                Modal.error({ title: 'Failed', content: 'Could not save credentials' });
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const handleDeleteCredential = async (website: string) => {
        try {
            const token = getToken();
            const response = await fetch(`/api/browser/auto-login/credentials/${website}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` },
            });

            if (response.ok) {
                Modal.success({ title: 'Deleted', content: 'Credential removed' });
                fetchBrowserData();
            } else {
                Modal.error({ title: 'Failed', content: 'Could not delete credential' });
            }
        } catch (error) {
            Modal.error({ title: 'Error', content: String(error) });
        }
    };

    const auditColumns = [
        {
            title: 'URL',
            dataIndex: 'url',
            key: 'url',
            render: (text: string) => (
                <Tooltip title={text}>
                    <span>{text.substring(0, 40)}...</span>
                </Tooltip>
            ),
            ellipsis: true,
            width: 250,
        },
        {
            title: 'Action',
            dataIndex: 'action',
            key: 'action',
            render: (action: string) => (
                <Tag color={action === 'scrape' ? 'blue' : 'cyan'}>{action.toUpperCase()}</Tag>
            ),
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => (
                <Badge
                    status={status === 'success' ? 'success' : 'error'}
                    text={status.toUpperCase()}
                />
            ),
        },
        {
            title: 'Duration',
            dataIndex: 'duration',
            key: 'duration',
            render: (duration: number) => `${duration}ms`,
        },
        {
            title: 'Timestamp',
            dataIndex: 'timestamp',
            key: 'timestamp',
            render: (time: string) => new Date(time).toLocaleString(),
        },
    ];

    const quotaUsagePercent =
        browserStats &&
        Math.round(
            ((browserStats.quotaLimit - browserStats.quotaRemaining) / browserStats.quotaLimit) *
            100
        );

    return (
        <div style={{ padding: '20px' }}>
            <Card
                title="🌐 Headless Browser Automation (Puppeteer)"
                bordered={false}
                extra={
                    <Button
                        icon={<ReloadOutlined />}
                        onClick={fetchBrowserData}
                        loading={loading}
                    >
                        Refresh
                    </Button>
                }
            >
                {/* Warning */}
                <Alert
                    message="Browser Fallback Service"
                    description="Puppeteer is used as an emergency fallback when APIs fail. Limited to 10 requests per day."
                    type="warning"
                    showIcon
                    style={{ marginBottom: '20px' }}
                />
                {/* Auto-Login Control */}
                <Card
                    title="🔐 Auto-Login Control"
                    style={{ marginBottom: '20px' }}
                    type="inner"
                    extra={
                        <Switch
                            checked={autoLoginEnabled}
                            onChange={handleToggleAutoLogin}
                            loading={loading}
                        />
                    }
                >
                    <Space direction="vertical" style={{ width: '100%' }}>
                        <Alert
                            message="Auto-Login Enabled"
                            description="When enabled, the browser will automatically use stored credentials for website login"
                            type={autoLoginEnabled ? 'success' : 'info'}
                            showIcon
                        />
                        <Button
                            type="primary"
                            onClick={() => setCredentialsModalVisible(true)}
                            disabled={!autoLoginEnabled}
                        >
                            Manage Stored Credentials ({storedCredentials.length})
                        </Button>
                    </Space>
                </Card>
                {/* Stats */}
                {browserStats && (
                    <Row gutter={16} style={{ marginBottom: '20px' }}>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic
                                title="Status"
                                value={browserStats.puppeteerAvailable ? 'READY' : 'OFFLINE'}
                                valueStyle={{
                                    color: browserStats.puppeteerAvailable ? '#52c41a' : '#f5222d',
                                }}
                                prefix={<ChromeOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic
                                title="Daily Quota"
                                value={browserStats.quotaRemaining}
                                suffix={`/ ${browserStats.quotaLimit}`}
                                prefix={<BarChartOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic
                                title="Today's Usage"
                                value={browserStats.dailyUsage}
                                prefix={<DatabaseOutlined />}
                            />
                        </Col>
                        <Col xs={24} sm={12} md={6}>
                            <Statistic
                                title="Health"
                                value={browserStats.healthStatus.toUpperCase()}
                                valueStyle={{
                                    color:
                                        browserStats.healthStatus === 'healthy'
                                            ? '#52c41a'
                                            : '#faad14',
                                }}
                            />
                        </Col>
                    </Row>
                )}

                {/* Quota Progress */}
                {browserStats && (
                    <Card style={{ marginBottom: '20px' }} type="inner">
                        <Row align="middle" gutter={16}>
                            <Col span={16}>
                                <Progress
                                    percent={quotaUsagePercent}
                                    status={
                                        quotaUsagePercent! > 80
                                            ? 'exception'
                                            : quotaUsagePercent! > 50
                                            ? 'normal'
                                            : 'success'
                                    }
                                />
                            </Col>
                            <Col span={8} style={{ textAlign: 'right' }}>
                                <span style={{ fontSize: '14px', fontWeight: 'bold' }}>
                                    {quotaUsagePercent}% Used
                                </span>
                            </Col>
                        </Row>
                    </Card>
                )}

                {/* Action Buttons */}
                <Row gutter={16} style={{ marginBottom: '20px' }}>
                    <Col xs={24} sm={12} md={8}>
                        <Button
                            type="primary"
                            block
                            icon={<RocketOutlined />}
                            onClick={() => setScrapeModalVisible(true)}
                            disabled={!browserStats?.puppeteerAvailable}
                            loading={scraping}
                        >
                            Scrape URL
                        </Button>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                        <Button
                            type="default"
                            block
                            icon={<CameraOutlined />}
                            disabled={!browserStats?.puppeteerAvailable}
                        >
                            Take Screenshot
                        </Button>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                        <Button
                            type="dashed"
                            block
                            icon={<DownloadOutlined />}
                            disabled={!browserStats?.puppeteerAvailable}
                        >
                            Export Logs
                        </Button>
                    </Col>
                </Row>

                {/* Audit Log */}
                <Card
                    title="📋 Activity Log"
                    style={{ marginBottom: '20px' }}
                    type="inner"
                >
                    <Spin spinning={loading}>
                        {auditLogs.length > 0 ? (
                            <Table
                                columns={auditColumns}
                                dataSource={auditLogs}
                                size="small"
                                pagination={{ pageSize: 10 }}
                                rowKey="id"
                            />
                        ) : (
                            <Empty description="No activity yet" />
                        )}
                    </Spin>
                </Card>

                {/* Scrape History */}
                <Card title="🔍 Recent Scrapes" type="inner">
                    {scrapeHistory.length > 0 ? (
                        <Timeline
                            items={scrapeHistory.map((result) => ({
                                dot:
                                    result.status === 'success' ? (
                                        <CheckCircleOutlined
                                            style={{ fontSize: '16px', color: '#52c41a' }}
                                        />
                                    ) : (
                                        <CloseCircleOutlined
                                            style={{ fontSize: '16px', color: '#f5222d' }}
                                        />
                                    ),
                                children: (
                                    <div>
                                        <strong>{result.url}</strong>
                                        <p>{result.timestamp}</p>
                                        {result.title && <p> Title: {result.title}</p>}
                                        <p>Duration: {result.duration}ms</p>
                                        {result.screenshotPath && (
                                            <Button
                                                size="small"
                                                onClick={() => {
                                                    setSelectedResult(result);
                                                    setResultDrawerVisible(true);
                                                }}
                                            >
                                                View Result
                                            </Button>
                                        )}
                                    </div>
                                ),
                            }))}
                        />
                    ) : (
                        <Empty description="No scrape history" />
                    )}
                </Card>
            </Card>

            {/* Scrape Modal */}
            <Modal
                title="Scrape URL with Puppeteer"
                visible={scrapeModalVisible}
                onCancel={() => setScrapeModalVisible(false)}
                footer={null}
            >
                <Form form={form} layout="vertical" onFinish={handleScrapeURL}>
                    <Form.Item
                        label="URL"
                        name="url"
                        rules={[
                            { required: true, message: 'Please enter URL' },
                            {
                                pattern: /^https?:\/\//,
                                message: 'Must start with http:// or https://',
                            },
                        ]}
                    >
                        <Input placeholder="https://example.com" />
                    </Form.Item>

                    <Form.Item label="Options" name="options">
                        <Space>
                            <Checkbox name="screenshot">Include Screenshot</Checkbox>
                            <Checkbox
                                onChange={(e) => setUseAuth(e.target.checked)}
                            >
                                Use Authentication
                            </Checkbox>
                        </Space>
                    </Form.Item>

                    {useAuth && (
                        <>
                            <Form.Item
                                label="Username"
                                name="username"
                                rules={[
                                    { required: true, message: 'Username required' },
                                ]}
                            >
                                <Input placeholder="Username" />
                            </Form.Item>

                            <Form.Item
                                label="Password"
                                name="password"
                                rules={[
                                    { required: true, message: 'Password required' },
                                ]}
                            >
                                <Input.Password placeholder="Password" />
                            </Form.Item>
                        </>
                    )}

                    <Form.Item
                        label="Timeout (seconds)"
                        name="timeout"
                        initialValue={30}
                    >
                        <InputNumber min={5} max={60} />
                    </Form.Item>

                    <Button type="primary" htmlType="submit" block loading={scraping}>
                        Start Scrape
                    </Button>
                </Form>
            </Modal>

            {/* Result Drawer */}
            <Drawer
                title="Scrape Result"
                onClose={() => setResultDrawerVisible(false)}
                open={resultDrawerVisible}
                width={800}
            >
                {selectedResult && (
                    <div>
                        <p>
                            <strong>URL:</strong> {selectedResult.url}
                        </p>
                        <p>
                            <strong>Title:</strong> {selectedResult.title}
                        </p>
                        <p>
                            <strong>Duration:</strong> {selectedResult.duration}ms
                        </p>
                        <p>
                            <strong>Timestamp:</strong> {selectedResult.timestamp}
                        </p>
                        {selectedResult.screenshotPath && (
                            <div>
                                <strong>Screenshot:</strong>
                                <img
                                    src={selectedResult.screenshotPath}
                                    style={{
                                        maxWidth: '100%',
                                        marginTop: '10px',
                                        border: '1px solid #d9d9d9',
                                    }}
                                    alt="Screenshot"
                                />
                            </div>
                        )}
                        {selectedResult.content && (
                            <div style={{ marginTop: '20px' }}>
                                <strong>Content:</strong>
                                <pre
                                    style={{
                                        backgroundColor: '#f5f5f5',
                                        padding: '10px',
                                        borderRadius: '4px',
                                        maxHeight: '400px',
                                        overflowY: 'auto',
                                        fontSize: '12px',
                                    }}
                                >
                                    {selectedResult.content}
                                </pre>
                            </div>
                        )}
                    </div>
                )}
            </Drawer>

            {/* Credentials Modal */}
            <Modal
                title="Add Auto-Login Credentials"
                open={credentialsModalVisible}
                onCancel={() => {
                    setCredentialsModalVisible(false);
                    credentialsForm.resetFields();
                }}
                footer={null}
                width={600}
            >
                <Form
                    form={credentialsForm}
                    layout="vertical"
                    onFinish={handleSaveCredentials}
                >
                    <Form.Item
                        label="Website URL"
                        name="website"
                        rules={[
                            { required: true, message: 'Website URL required' },
                            { pattern: /^https?:\/\/.+/, message: 'Must start with http:// or https://' },
                        ]}
                    >
                        <Input placeholder="https://example.com" />
                    </Form.Item>

                    <Form.Item
                        label="Username/Email"
                        name="username"
                        rules={[{ required: true, message: 'Username or email required' }]}
                    >
                        <Input placeholder="username or email" />
                    </Form.Item>

                    <Form.Item
                        label="Password"
                        name="password"
                        rules={[{ required: true, message: 'Password required' }]}
                    >
                        <Input.Password placeholder="password" />
                    </Form.Item>

                    <Divider>Login Form Selectors (Optional)</Divider>

                    <Form.Item
                        label="Username Input Selector"
                        name="usernameSelector"
                        tooltip="CSS selector for username input field (e.g., #login-username, input[type='email'])"
                    >
                        <Input placeholder="e.g., #email" />
                    </Form.Item>

                    <Form.Item
                        label="Password Input Selector"
                        name="passwordSelector"
                        tooltip="CSS selector for password input field (e.g., #login-password, input[type='password'])"
                    >
                        <Input placeholder="e.g., #password" />
                    </Form.Item>

                    <Form.Item
                        label="Submit Button Selector"
                        name="submitSelector"
                        tooltip="CSS selector for login submit button (e.g., button[type='submit'], #login-btn)"
                    >
                        <Input placeholder="e.g., button[type='submit']" />
                    </Form.Item>

                    <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                        <Button onClick={() => {
                            setCredentialsModalVisible(false);
                            credentialsForm.resetFields();
                        }}>
                            Cancel
                        </Button>
                        <Button type="primary" htmlType="submit">
                            Save Credentials
                        </Button>
                    </Space>
                </Form>
            </Modal>

            {/* Stored Credentials Table Modal */}
            <Modal
                title="Manage Auto-Login Credentials"
                open={credentialsModalVisible && storedCredentials.length > 0}
                onCancel={() => setCredentialsModalVisible(false)}
                footer={null}
                width={700}
            >
                <Table
                    columns={[
                        {
                            title: 'Website',
                            dataIndex: 'website',
                            key: 'website',
                            render: (text) => <a href={text} target="_blank" rel="noopener noreferrer">{text}</a>,
                        },
                        {
                            title: 'Username',
                            dataIndex: 'username',
                            key: 'username',
                        },
                        {
                            title: 'Actions',
                            key: 'actions',
                            render: (_, record) => (
                                <Popconfirm
                                    title="Delete Credential?"
                                    description="This action cannot be undone."
                                    onConfirm={() => handleDeleteCredential(record.website)}
                                    okText="Yes"
                                    cancelText="No"
                                >
                                    <Button danger size="small">Delete</Button>
                                </Popconfirm>
                            ),
                        },
                    ]}
                    dataSource={storedCredentials.map((cred) => ({
                        ...cred,
                        key: cred.website,
                    }))}
                    pagination={false}
                    bordered
                />
                <Button
                    type="primary"
                    block
                    style={{ marginTop: '16px' }}
                    onClick={() => credentialsForm.resetFields()}
                >
                    Add New Credential
                </Button>
            </Modal>
        </div>
    );
};

export default HeadlessBrowserDashboard;
