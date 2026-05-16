// ScenarioOrchestration.tsx - AI Model Role Management
import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, Switch, Space, Tag, Spin, message, Tooltip, Empty } from 'antd';
import { 
    ThunderboltOutlined, 
    CloudServerOutlined, 
    SafetyCertificateOutlined,
    RobotOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

interface APIProvider {
    id: string;
    name: string;
    type: string;
    status: string;
    canCommunicate?: boolean;
    canExecuteTasks?: boolean;
    canParticipateInVoting?: boolean;
}

const ScenarioOrchestration: React.FC = () => {
    const { t } = useTranslation();
    const [providers, setProviders] = useState<APIProvider[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchProviders();
    }, []);

    const fetchProviders = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/providers/configured');
            if (response.ok) {
                const data = await response.json();
                setProviders(data.data?.providers || []);
            }
        } catch (error) {
            message.error('FAILED_TO_LOAD_SCENARIO_DATA');
        } finally {
            setLoading(false);
        }
    };

    const toggleCapability = async (id: string, capability: string, value: boolean) => {
        try {
            const response = await authUtils.fetchWithAuth(`/api/admin/providers/${id}/capability`, {
                method: 'PATCH',
                body: JSON.stringify({ [capability]: value })
            });
            if (response.ok) {
                message.success('PROTOCOL_UPDATED');
                setProviders(prev => prev.map(p => p.id === id ? { ...p, [capability]: value } : p));
            }
        } catch (error) {
            message.error('PROTOCOL_UPDATE_FAILURE');
        }
    };

    if (loading) return <div className="p-20 text-center"><Spin size="large" /></div>;
    if (providers.length === 0) return <Empty description="No AI Models found in registry" className="py-20" />;

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 p-4">
            {providers.map(provider => (
                <Card 
                    key={provider.id} 
                    className="bg-white/[0.02] border-white/5 hover:border-emerald-500/30 transition-all duration-500 group"
                    title={
                        <div className="flex items-center gap-3 py-1">
                            <Avatar size="small" icon={<RobotOutlined />} className="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" />
                            <div className="flex flex-col">
                                <span className="text-[12px] font-black uppercase text-white tracking-widest">{provider.name}</span>
                                <span className="text-[8px] text-white/20 uppercase font-mono">{provider.id}</span>
                            </div>
                        </div>
                    }
                    extra={<Tag color={provider.status === 'active' ? 'emerald' : 'default'} className="m-0 text-[8px] uppercase font-black">{provider.status}</Tag>}
                >
                    <div className="space-y-4">
                        <div className="flex items-center justify-between p-3 bg-white/[0.02] border border-white/5 rounded-lg group-hover:bg-white/[0.04] transition-all">
                            <div className="flex items-center gap-3">
                                <ThunderboltOutlined className={provider.canCommunicate ? 'text-blue-400' : 'text-white/10'} />
                                <div className="flex flex-col">
                                    <span className="text-[10px] font-black uppercase text-white/80">{t('dashboard.role_communication')}</span>
                                    <span className="text-[8px] text-white/20 uppercase">Helper / Mediator Role</span>
                                </div>
                            </div>
                            <Switch 
                                size="small" 
                                checked={provider.canCommunicate} 
                                onChange={(val) => toggleCapability(provider.id, 'canCommunicate', val)}
                                className={provider.canCommunicate ? 'bg-blue-500' : 'bg-white/10'}
                            />
                        </div>

                        <div className="flex items-center justify-between p-3 bg-white/[0.02] border border-white/5 rounded-lg group-hover:bg-white/[0.04] transition-all">
                            <div className="flex items-center gap-3">
                                <CloudServerOutlined className={provider.canExecuteTasks ? 'text-emerald-400' : 'text-white/10'} />
                                <div className="flex flex-col">
                                    <span className="text-[10px] font-black uppercase text-white/80">{t('dashboard.role_execution')}</span>
                                    <span className="text-[8px] text-white/20 uppercase">Code / Tool Execution Role</span>
                                </div>
                            </div>
                            <Switch 
                                size="small" 
                                checked={provider.canExecuteTasks} 
                                onChange={(val) => toggleCapability(provider.id, 'canExecuteTasks', val)}
                                className={provider.canExecuteTasks ? 'bg-emerald-500' : 'bg-white/10'}
                            />
                        </div>

                        <div className="flex items-center justify-between p-3 bg-white/[0.02] border border-white/5 rounded-lg group-hover:bg-white/[0.04] transition-all">
                            <div className="flex items-center gap-3">
                                <SafetyCertificateOutlined className={provider.canParticipateInVoting ? 'text-purple-400' : 'text-white/10'} />
                                <div className="flex flex-col">
                                    <span className="text-[10px] font-black uppercase text-white/80">{t('dashboard.role_voting')}</span>
                                    <span className="text-[8px] text-white/20 uppercase">Quality Assessment Role</span>
                                </div>
                            </div>
                            <Switch 
                                size="small" 
                                checked={provider.canParticipateInVoting} 
                                onChange={(val) => toggleCapability(provider.id, 'canParticipateInVoting', val)}
                                className={provider.canParticipateInVoting ? 'bg-purple-500' : 'bg-white/10'}
                            />
                        </div>
                    </div>
                </Card>
            ))}
        </div>
    );
};

// Internal Avatar shim if not imported
const Avatar = ({ size, icon, className }: any) => (
    <div className={`flex items-center justify-center rounded-full ${className}`} style={{ width: size === 'small' ? 24 : 32, height: size === 'small' ? 24 : 32 }}>
        {icon}
    </div>
);

export default ScenarioOrchestration;
