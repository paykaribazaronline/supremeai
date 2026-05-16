import React from 'react';
import { Layout, Typography, Empty, Card, Badge, Space, Tag, Button } from 'antd';
import { SafetyCertificateOutlined, BulbOutlined } from '@ant-design/icons';
import AISuggestionInformer from './AISuggestionInformer';
import { useAISuggestions } from '../lib/suggestionService';
import { message } from 'antd';

const { Content } = Layout;
const { Title, Text } = Typography;

const AdminApprovals: React.FC = () => {
    const { suggestions, approve, decline, refresh, count } = useAISuggestions();
    const [approvingAll, setApprovingAll] = React.useState(false);

    const handleApprove = async (id: string) => {
        const success = await approve(id);
        if (success) {
            message.success('System optimization approved and executed.');
        } else {
            message.error('Approval failed. Please check system logs.');
        }
    };

    const handleDecline = async (id: string) => {
        const success = await decline(id);
        if (success) {
            message.info('Suggestion declined.');
        } else {
            message.error('Decline failed.');
        }
    };

    const handleApproveAll = async () => {
        setApprovingAll(true);
        const hide = message.loading('Approving all suggestions...', 0);
        try {
            for (const s of suggestions) {
                await approve(s.id);
            }
            message.success('All pending optimizations have been approved.');
            refresh();
        } finally {
            setApprovingAll(false);
            hide();
        }
    };

    return (
        <div className="approvals-container" style={{ padding: '24px' }}>
            <div className="glass-card px-6 py-4 flex items-center justify-between border-l-4 border-amber-500 bg-black/40 mb-6 rounded-xl">
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-500 shadow-[0_0_20px_rgba(245,158,11,0.15)]">
                        <SafetyCertificateOutlined className="text-lg" />
                    </div>
                    <div className="flex flex-col">
                        <h1 className="text-lg font-black uppercase tracking-[0.2em] text-white m-0">System Approvals & Insights</h1>
                        <p className="text-[10px] font-bold text-amber-500/60 uppercase tracking-widest m-0">Review and Grant Permissions for Autonomous Optimizations</p>
                    </div>
                </div>
                <Space>
                    <Button 
                        icon={<BulbOutlined />} 
                        onClick={() => refresh()}
                        className="cyber-button-small"
                        ghost
                    >
                        Refresh Insights
                    </Button>
                    {suggestions.length > 0 && (
                        <Button 
                            type="primary" 
                            onClick={handleApproveAll} 
                            loading={approvingAll}
                            danger
                            className="cyber-button-primary"
                        >
                            Approve All ({count})
                        </Button>
                    )}
                </Space>
            </div>

            {suggestions.length === 0 ? (
                <Card className="glass-card flex flex-col items-center justify-center py-20 bg-black/20" style={{ borderRadius: '16px' }}>
                    <Empty 
                        image={<BulbOutlined style={{ fontSize: 60, color: 'rgba(255,255,255,0.1)' }} />}
                        description={
                            <div className="text-center">
                                <Text className="text-white/40 uppercase tracking-widest font-black block">No Pending Approvals</Text>
                                <Text className="text-white/20 text-[11px] uppercase tracking-tighter mt-2 block">System is currently running at peak efficiency</Text>
                            </div>
                        }
                    />
                </Card>
            ) : (
                <AISuggestionInformer 
                    title="Required Permissions"
                    context="Dynamic Orchestration"
                    suggestions={suggestions}
                    onApprove={handleApprove}
                    onDecline={handleDecline}
                    style={{ borderLeft: '4px solid #f59e0b', borderRadius: '16px' }}
                />
            )}

            <Card className="glass-card bg-emerald-500/5 border border-emerald-500/10 p-4 mt-6" style={{ borderRadius: '12px' }}>
                <Text className="text-[11px] text-emerald-500/60 uppercase tracking-widest font-bold">
                    🛡️ Security Note: Autonomous actions are only executed after explicit administrator approval. All changes are logged for audit.
                </Text>
            </Card>
        </div>
    );
};

export default AdminApprovals;

