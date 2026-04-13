import React, { useState, useEffect } from 'react';
import {
    Card, Row, Col, Badge, Tag, Timeline, Spin, Alert, Button,
    Statistic, Tabs, List, Typography, Space, Progress,
} from 'antd';
import {
    CheckCircleOutlined,
    WarningOutlined,
    ThunderboltOutlined,
    SafetyOutlined,
    DollarOutlined,
    RocketOutlined,
    ToolOutlined,
    ApiOutlined,
    NodeIndexOutlined,
    StarOutlined,
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface PhaseEntry {
    phase: number;
    name: string;
    description: string;
    statusUrl: string;
    status: string;
}

interface AllPhasesResponse {
    totalPhases: number;
    operationalCount: number;
    systemStatus: string;
    phases: PhaseEntry[];
    timestamp: number;
}

interface PhaseDetail {
    status?: string;
    agentCount?: number;
    agents?: string[];
    capabilities?: string[];
    services?: Record<string, unknown>;
    [key: string]: unknown;
}

const PHASE_META: Record<number, { icon: React.ReactNode; color: string; label: string }> = {
    1:  { icon: <ToolOutlined />,        color: '#1890ff', label: 'Optimization' },
    6:  { icon: <NodeIndexOutlined />,   color: '#722ed1', label: 'Integration' },
    7:  { icon: <RocketOutlined />,      color: '#13c2c2', label: 'Multi-Platform' },
    8:  { icon: <SafetyOutlined />,      color: '#f5222d', label: 'Security' },
    9:  { icon: <DollarOutlined />,      color: '#52c41a', label: 'Cost Intelligence' },
    10: { icon: <StarOutlined />,         color: '#eb2f96', label: 'Self-Improvement' },
};

const PhasesOverview: React.FC = () => {
    const [allPhases, setAllPhases] = useState<AllPhasesResponse | null>(null);
    const [details, setDetails] = useState<Record<number, PhaseDetail>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const authHeaders = () => ({
        'Authorization': `Bearer ${authUtils.getToken()}`,
        'Content-Type': 'application/json',
    });

    const fetchAll = async () => {
        setLoading(true);
        setError(null);
        try {
            const overviewRes = await fetch('/api/v1/agents/all-phases', { headers: authHeaders() });
            if (!overviewRes.ok) throw new Error('Failed to load phases overview');
            const overview: AllPhasesResponse = await overviewRes.json();
            setAllPhases(overview);

            // Fetch per-phase details in parallel
            const detailEndpoints: Record<number, string> = {
                1:  '/api/v1/optimization/health',
                6:  '/api/v1/phase6/health',
                7:  '/api/phase7/agents/summary',
                8:  '/api/v1/agents/phase8/summary',
                9:  '/api/v1/agents/phase9/summary',
                10: '/api/v1/agents/phase10/summary',
            };

            const detailResults = await Promise.allSettled(
                Object.entries(detailEndpoints).map(async ([phase, url]) => {
                    const res = await fetch(url, { headers: authHeaders() });
                    const data: PhaseDetail = res.ok ? await res.json() : {};
                    return { phase: parseInt(phase), data };
                })
            );

            const newDetails: Record<number, PhaseDetail> = {};
            detailResults.forEach(result => {
                if (result.status === 'fulfilled') {
                    newDetails[result.value.phase] = result.value.data;
                }
            });
            setDetails(newDetails);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load phases');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchAll(); }, []);

    if (loading) return <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />;
    if (error) return <Alert type="error" message={error} action={<Button onClick={fetchAll}>Retry</Button>} />;
    if (!allPhases) return null;

    const statusColor = allPhases.systemStatus === 'fully_operational' ? 'success' : 'warning';

    return (
        <div>
            {/* System-level status bar */}
            <Card style={{ marginBottom: 24 }}>
                <Row gutter={24} align="middle">
                    <Col xs={24} sm={8}>
                        <Statistic
                            title="Total Phases"
                            value={allPhases.totalPhases}
                            prefix={<NodeIndexOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={8}>
                        <Statistic
                            title="Operational"
                            value={allPhases.operationalCount}
                            suffix={`/ ${allPhases.totalPhases}`}
                            valueStyle={{ color: allPhases.operationalCount === allPhases.totalPhases ? '#52c41a' : '#faad14' }}
                            prefix={<CheckCircleOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={8}>
                        <div>
                            <Text type="secondary">System Status</Text>
                            <br />
                            <Badge
                                status={statusColor}
                                text={
                                    <Text strong style={{ textTransform: 'capitalize' }}>
                                        {allPhases.systemStatus.replace(/_/g, ' ')}
                                    </Text>
                                }
                            />
                        </div>
                    </Col>
                </Row>
                <Progress
                    percent={Math.round((allPhases.operationalCount / allPhases.totalPhases) * 100)}
                    status={allPhases.operationalCount === allPhases.totalPhases ? 'success' : 'active'}
                    style={{ marginTop: 16 }}
                />
            </Card>

            {/* Per-phase tabs */}
            <Tabs
                type="card"
                items={allPhases.phases.map(phase => {
                    const meta = PHASE_META[phase.phase] ?? { icon: <ApiOutlined />, color: '#666', label: phase.name };
                    const detail = details[phase.phase] ?? {};
                    const isOp = phase.status === 'operational';

                    return {
                        key: String(phase.phase),
                        label: (
                            <Space size={4}>
                                {meta.icon}
                                Phase {phase.phase}
                                <Badge dot status={isOp ? 'success' : 'warning'} />
                            </Space>
                        ),
                        children: (
                            <Row gutter={[16, 16]}>
                                {/* Header card */}
                                <Col xs={24}>
                                    <Card
                                        bordered={false}
                                        style={{ background: `${meta.color}10`, borderLeft: `4px solid ${meta.color}` }}
                                    >
                                        <Space size="middle">
                                            <span style={{ fontSize: 28, color: meta.color }}>{meta.icon}</span>
                                            <div>
                                                <Title level={4} style={{ margin: 0 }}>
                                                    Phase {phase.phase}: {phase.name}
                                                </Title>
                                                <Text type="secondary">{phase.description}</Text>
                                            </div>
                                            <Tag
                                                color={isOp ? 'success' : 'warning'}
                                                icon={isOp ? <CheckCircleOutlined /> : <WarningOutlined />}
                                                style={{ marginLeft: 'auto' }}
                                            >
                                                {isOp ? 'Operational' : 'Partial'}
                                            </Tag>
                                        </Space>
                                    </Card>
                                </Col>

                                {/* Agent count (if available) */}
                                {detail.agentCount != null && (
                                    <Col xs={12} sm={6}>
                                        <Card>
                                            <Statistic
                                                title="Agents"
                                                value={detail.agentCount as number}
                                                prefix={<ThunderboltOutlined />}
                                            />
                                        </Card>
                                    </Col>
                                )}

                                {/* Agents list */}
                                {Array.isArray(detail.agents) && detail.agents.length > 0 && (
                                    <Col xs={24} sm={12}>
                                        <Card title="🤖 Agents" size="small">
                                            <List
                                                size="small"
                                                dataSource={detail.agents as string[]}
                                                renderItem={item => (
                                                    <List.Item>
                                                        <Tag color="blue">{item}</Tag>
                                                    </List.Item>
                                                )}
                                            />
                                        </Card>
                                    </Col>
                                )}

                                {/* Capabilities */}
                                {Array.isArray(detail.capabilities) && detail.capabilities.length > 0 && (
                                    <Col xs={24}>
                                        <Card title="⚡ Capabilities" size="small">
                                            <Timeline
                                                items={(detail.capabilities as string[]).map(cap => ({
                                                    color: 'green',
                                                    children: cap,
                                                }))}
                                            />
                                        </Card>
                                    </Col>
                                )}

                                {/* Services (Phase 1 health map) */}
                                {detail.services && typeof detail.services === 'object' && (
                                    <Col xs={24}>
                                        <Card title="⚙️ Services" size="small">
                                            <Row gutter={[8, 8]}>
                                                {Object.entries(detail.services as Record<string, unknown>).map(([svc, info]) => {
                                                    const infoMap = info as Record<string, unknown>;
                                                    const svcStatus = String(infoMap?.status ?? '');
                                                    const ok = svcStatus.includes('OK') || svcStatus.includes('Active');
                                                    return (
                                                        <Col key={svc} xs={12} sm={6}>
                                                            <Tag color={ok ? 'success' : 'warning'} style={{ width: '100%', textAlign: 'center' }}>
                                                                {svc}: {ok ? '✅' : '⚠️'}
                                                            </Tag>
                                                        </Col>
                                                    );
                                                })}
                                            </Row>
                                        </Card>
                                    </Col>
                                )}

                                {/* Status URL */}
                                <Col xs={24}>
                                    <Text type="secondary" style={{ fontSize: 11 }}>
                                        Status endpoint: <code>{phase.statusUrl}</code>
                                    </Text>
                                </Col>
                            </Row>
                        ),
                    };
                })}
            />
        </div>
    );
};

export default PhasesOverview;
