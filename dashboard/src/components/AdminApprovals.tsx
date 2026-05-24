// AdminApprovals.tsx - Cinematic Approval Center
import React from 'react';
import { Typography, Empty, Badge, Space, Button, Row, Col, message } from 'antd';
import {
  SafetyCertificateOutlined,
  BulbOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  ThunderboltOutlined,
  SecurityScanOutlined,
  FireOutlined
} from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
import AISuggestionInformer from './AISuggestionInformer';
import { useAISuggestions } from '../lib/suggestionService';

const { Title, Text } = Typography;

const AdminApprovals: React.FC = () => {
    const { suggestions, approve, decline, refresh, count } = useAISuggestions();
    const [approvingAll, setApprovingAll] = React.useState(false);

    const handleApprove = async (id: string) => {
        const success = await approve(id);
        if (success) message.success('Optimization sequence initiated');
    };

    const handleDecline = async (id: string) => {
        const success = await decline(id);
        if (success) message.info('Optimization discarded');
    };

    const handleApproveAll = async () => {
        setApprovingAll(true);
        try {
            for (const s of suggestions) await approve(s.id);
            message.success('All global optimizations approved');
            refresh();
        } finally {
            setApprovingAll(false);
        }
    };

    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5 }}
            style={{ maxWidth: '1400px', margin: '0 auto' }}
        >
            {/* Cinematic Header */}
            <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(245, 158, 11, 0.2)', paddingBottom: 24 }}>
                <Row justify="space-between" align="bottom">
                    <Col>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                            <SafetyCertificateOutlined style={{ color: '#f59e0b', fontSize: 20 }} />
                            <Text style={{ color: '#f59e0b', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>PERMISSION PROTOCOL</Text>
                        </div>
                        <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
                            Neural <span style={{ color: '#f59e0b', textShadow: '0 0 10px rgba(245, 158, 11, 0.3)' }}>Approvals</span>
                        </Title>
                        <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Review and authorize autonomous system optimizations and security patches.</Text>
                    </Col>
                    <Col>
                        <Space>
                            <Button
                                icon={<SyncOutlined />}
                                onClick={() => refresh()}
                                className="glass-action-button"
                            >
                                Refresh Insights
                            </Button>
                            {suggestions.length > 0 && (
                                <Button
                                    type="primary"
                                    onClick={handleApproveAll}
                                    loading={approvingAll}
                                    style={{ background: '#f59e0b', border: 'none', color: '#000', fontWeight: 700 }}
                                    className="cyber-button"
                                >
                                    AUTHORIZE ALL ({count})
                                </Button>
                            )}
                        </Space>
                    </Col>
                </Row>
            </div>

            <Row gutter={[24, 24]}>
                {/* Stats Row */}
                <Col span={24}>
                    <Row gutter={[24, 24]}>
                        <Col xs={24} md={8}>
                            <div className="glass-card" style={{ borderLeft: '4px solid #f59e0b' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Pending Optimizations</Text>
                                        <div style={{ color: '#fff', fontSize: 24, fontWeight: 800, marginTop: 4 }}>{count}</div>
                                    </div>
                                    <BulbOutlined style={{ color: '#f59e0b', fontSize: 24 }} />
                                </div>
                            </div>
                        </Col>
                        <Col xs={24} md={8}>
                            <div className="glass-card" style={{ borderLeft: '4px solid var(--success)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Auto-Defense Status</Text>
                                        <div style={{ color: '#fff', fontSize: 20, fontWeight: 800, marginTop: 4 }}>OPERATIONAL</div>
                                    </div>
                                    <SecurityScanOutlined style={{ color: 'var(--success)', fontSize: 24 }} />
                                </div>
                            </div>
                        </Col>
                        <Col xs={24} md={8}>
                            <div className="glass-card" style={{ borderLeft: '4px solid var(--neon-blue)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, textTransform: 'uppercase' }}>Last Authorization</Text>
                                        <div style={{ color: '#fff', fontSize: 18, fontWeight: 800, marginTop: 4 }}>2m ago</div>
                                    </div>
                                    <ThunderboltOutlined style={{ color: 'var(--neon-blue)', fontSize: 24 }} />
                                </div>
                            </div>
                        </Col>
                    </Row>
                </Col>

                {/* Main Content */}
                <Col span={24}>
                    <AnimatePresence mode="wait">
                        {suggestions.length === 0 ? (
                            <motion.div
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                            >
                                <div className="glass-card" style={{ height: 400, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
                                    <div style={{ width: 80, height: 80, borderRadius: '50%', background: 'rgba(255,255,255,0.02)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24, border: '1px solid rgba(255,255,255,0.05)' }}>
                                        <CheckCircleOutlined style={{ fontSize: 40, color: 'var(--success)', opacity: 0.5 }} />
                                    </div>
                                    <Title level={4} style={{ color: '#fff', margin: 0, letterSpacing: 1 }}>ALL SYSTEMS OPTIMIZED</Title>
                                    <Text style={{ color: 'var(--text-dim)', marginTop: 8 }}>No pending authorization required at this time.</Text>
                                </div>
                            </motion.div>
                        ) : (
                            <motion.div
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                            >
                                <AISuggestionInformer
                                    title="Protocol Proposals"
                                    context="Autonomous Grid"
                                    suggestions={suggestions}
                                    onApprove={handleApprove}
                                    onDecline={handleDecline}
                                />
                            </motion.div>
                        )}
                    </AnimatePresence>
                </Col>

                {/* Footer Security Badge */}
                <Col span={24}>
                    <div className="glass-card" style={{ background: 'rgba(16, 185, 129, 0.05)', border: '1px solid rgba(16, 185, 129, 0.1)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                            <FireOutlined style={{ color: 'var(--success)' }} />
                            <Text style={{ color: 'var(--success)', fontSize: 11, fontWeight: 800, letterSpacing: 1, textTransform: 'uppercase' }}>
                                Authorization Trace: Secured & Encrypted. All actions recorded to immutable system logs.
                            </Text>
                        </div>
                    </div>
                </Col>
            </Row>
        </motion.div>
    );
};

export default AdminApprovals;
