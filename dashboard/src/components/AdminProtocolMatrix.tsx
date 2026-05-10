// AdminProtocolMatrix.tsx - NEURAL GOVERNANCE PROTOCOLS
import React, { useState, useEffect } from 'react';
import { Table, Tag, Switch, Typography, Button, message } from 'antd';
import { 
    SafetyCertificateOutlined, 
    SecurityScanOutlined,
    EditOutlined,
    DeleteOutlined,
    PlusOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import authUtils from '../lib/authUtils';

interface ProtocolRule {
    id: string;
    name: string;
    type: string;
    pattern: string;
    action: string;
    severity: 'critical' | 'high' | 'medium' | 'low';
    isActive: boolean;
    lastTriggered?: string;
}

const AdminProtocolMatrix: React.FC = () => {
    const [rules, setRules] = useState<ProtocolRule[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchProtocols();
    }, []);

    const fetchProtocols = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/rules');
            const data = await response.json();
            if (data.success) {
                setRules(data.rules);
            }
        } catch (error) {
            console.error('Failed to sync governance protocols:', error);
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        {
            title: 'PROTOCOL_ID',
            dataIndex: 'id',
            key: 'id',
            width: 100,
            render: (id: string) => <span className="text-[10px] font-mono text-white/30 uppercase">{id.slice(0, 8)}</span>
        },
        {
            title: 'MANIFEST_NAME',
            dataIndex: 'name',
            key: 'name',
            render: (n: string, record: ProtocolRule) => (
                <div className="flex flex-col">
                    <span className="text-[10px] font-black text-white uppercase tracking-tighter">{n}</span>
                    <span className="text-[8px] text-white/30 font-mono leading-tight">{record.type}</span>
                </div>
            )
        },
        {
            title: 'SEVERITY_TIER',
            dataIndex: 'severity',
            key: 'severity',
            render: (s: string) => {
                const colors: any = {
                    critical: 'text-red-500 bg-red-500/10 border-red-500/20',
                    high: 'text-orange-500 bg-orange-500/10 border-orange-500/20',
                    medium: 'text-yellow-500 bg-yellow-500/10 border-yellow-500/20',
                    low: 'text-blue-500 bg-blue-500/10 border-blue-500/20'
                };
                return (
                    <div className={`px-2 py-0.5 rounded border ${colors[s] || colors.low} w-fit`}>
                        <span className="text-[8px] font-black uppercase tracking-widest">{s}</span>
                    </div>
                );
            }
        },
        {
            title: 'PATTERN_SIGNATURE',
            dataIndex: 'pattern',
            key: 'pattern',
            render: (p: string) => <span className="text-[9px] font-mono text-white/50 bg-white/[0.03] px-1.5 py-0.5 rounded truncate max-w-[150px] inline-block">{p}</span>
        },
        {
            title: 'OPERATIONAL_STATUS',
            dataIndex: 'isActive',
            key: 'isActive',
            width: 120,
            render: (active: boolean) => (
                <div className="flex items-center gap-2">
                    <div className={`w-1.5 h-1.5 rounded-full ${active ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'}`} />
                    <span className={`text-[9px] font-black uppercase tracking-widest ${active ? 'text-emerald-500/80' : 'text-red-500/80'}`}>
                        {active ? 'SYSTEM_ACTIVE' : 'OFFLINE'}
                    </span>
                </div>
            )
        },
        {
            title: 'ACTIONS',
            key: 'actions',
            width: 80,
            render: () => (
                <div className="flex items-center gap-2">
                    <button className="text-white/40 hover:text-white transition-colors"><EditOutlined className="text-[10px]" /></button>
                    <button className="text-white/40 hover:text-red-500 transition-colors"><DeleteOutlined className="text-[10px]" /></button>
                </div>
            )
        }
    ];

    return (
        <div className="space-y-3">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
                        <SecurityScanOutlined className="text-blue-500" />
                    </div>
                    <div>
                        <h3 className="text-[11px] font-black text-white uppercase tracking-tighter m-0">Neural Governance Matrix</h3>
                        <p className="text-[8px] text-white/30 uppercase tracking-widest m-0 leading-tight">Protocol Compliance & Behavioral Guardrails</p>
                    </div>
                </div>
                <button className="bg-white/[0.05] border border-white/10 hover:bg-white/[0.1] text-white px-3 py-1 rounded text-[9px] font-black uppercase tracking-widest transition-all flex items-center gap-2">
                    <PlusOutlined /> New Protocol
                </button>
            </div>

            <div className="bg-[#0d0d0d] border border-white/5 rounded overflow-hidden">
                <div className="px-3 py-2 border-b border-white/5 flex items-center justify-between bg-white/[0.01]">
                    <span className="text-[8px] font-black uppercase tracking-widest text-white/40">Active Guardrail Manifest</span>
                    <div className="flex items-center gap-4">
                        <div className="flex items-center gap-1.5">
                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                            <span className="text-[8px] font-mono text-white/40">COMPLIANT</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <div className="w-1.5 h-1.5 rounded-full bg-red-500" />
                            <span className="text-[8px] font-mono text-white/40">VIOLATIONS: 0</span>
                        </div>
                    </div>
                </div>
                
                <Table 
                    dataSource={rules} 
                    columns={columns} 
                    loading={loading}
                    rowKey="id"
                    pagination={false}
                    className="admin-table-dense"
                />
            </div>

            <div className="p-3 bg-blue-500/5 border border-blue-500/10 rounded flex items-start gap-3">
                <InfoCircleOutlined className="text-blue-500/50 mt-0.5 text-[10px]" />
                <p className="text-[8px] text-blue-500/70 uppercase tracking-widest leading-relaxed m-0">
                    Protocols are enforced at the neural inference layer. Modifications to critical severity rules require L3 override authorization. 
                    Live scanning is enabled across all project vectors.
                </p>
            </div>
            
            <style>{`
                .admin-table-dense .ant-table {
                    background: transparent !important;
                    color: rgba(255,255,255,0.9) !important;
                }
                .admin-table-dense .ant-table-thead > tr > th {
                    background: rgba(255,255,255,0.02) !important;
                    color: rgba(255,255,255,0.3) !important;
                    font-size: 8px !important;
                    font-weight: 900 !important;
                    text-transform: uppercase !important;
                    letter-spacing: 0.1em !important;
                    border-bottom: 1px solid rgba(255,255,255,0.05) !important;
                    padding: 8px 12px !important;
                }
                .admin-table-dense .ant-table-tbody > tr > td {
                    border-bottom: 1px solid rgba(255,255,255,0.02) !important;
                    padding: 6px 12px !important;
                    background: transparent !important;
                }
                .admin-table-dense .ant-table-tbody > tr:hover > td {
                    background: rgba(255,255,255,0.03) !important;
                }
            `}</style>
        </div>
    );
};

export default AdminProtocolMatrix;
