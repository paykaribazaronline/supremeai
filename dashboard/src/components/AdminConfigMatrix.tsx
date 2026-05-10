// AdminConfigMatrix.tsx - SYSTEM PARAMETER SYNCHRONIZATION
import React, { useState, useEffect } from 'react';
import { Form, InputNumber, Switch, Button, message, Divider, Space, Card, Tag } from 'antd';
import { 
    SettingOutlined, 
    SaveOutlined, 
    SyncOutlined,
    SafetyOutlined,
    ClockCircleOutlined,
    DatabaseOutlined
} from '@ant-design/icons';
import authUtils from '../lib/authUtils';

const AdminConfigMatrix: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        fetchConfig();
    }, []);

    const fetchConfig = async () => {
        setLoading(true);
        try {
            const response = await authUtils.fetchWithAuth('/api/admin/config');
            const data = await response.json();
            form.setFieldsValue({
                ...data.settings,
                ...data.thresholds,
                ...data.timeouts,
                maintenance_mode: data.maintenanceMode
            });
        } catch (error) {
            console.error('Failed to sync system parameters:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (values: any) => {
        setSaving(true);
        try {
            // Re-structuring values for backend
            const payload = {
                settings: {
                    max_recent_logs: values.max_recent_logs,
                    debug_mode: values.debug_mode
                },
                thresholds: {
                    confidence_cutoff: values.confidence_cutoff,
                    anomaly_sensitivity: values.anomaly_sensitivity
                },
                timeouts: {
                    neural_inference_ms: values.neural_inference_ms,
                    socket_reconnect_ms: values.socket_reconnect_ms
                },
                maintenanceMode: values.maintenance_mode
            };

            const response = await authUtils.fetchWithAuth('/api/admin/config', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response) {
                message.success('SYSTEM_PARAMETERS_SYNCHRONIZED');
            }
        } catch (error) {
            message.error('SYNC_FAILED_AUTH_REQUIRED');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between bg-white/[0.02] border border-white/5 p-3 rounded-lg">
                <div className="flex items-center gap-3">
                    <SettingOutlined className="text-blue-500 text-lg" />
                    <div>
                        <h3 className="text-[11px] font-black text-white uppercase tracking-tighter m-0">Global Parameter Registry</h3>
                        <p className="text-[8px] text-white/30 uppercase tracking-widest m-0 leading-tight">Master System Configuration & Overrides</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <Button 
                        size="small" 
                        icon={<SyncOutlined spin={loading} />} 
                        onClick={fetchConfig}
                        className="bg-transparent border-white/10 text-white/40 text-[9px] uppercase font-black"
                    >
                        Refresh Cache
                    </Button>
                    <Button 
                        type="primary"
                        size="small" 
                        icon={<SaveOutlined />} 
                        onClick={() => form.submit()}
                        loading={saving}
                        className="bg-blue-600 border-none text-[9px] uppercase font-black"
                    >
                        Commit Changes
                    </Button>
                </div>
            </div>

            <Form
                form={form}
                layout="vertical"
                onFinish={handleSave}
                className="admin-form-dense"
            >
                <div className="grid grid-cols-2 gap-4">
                    {/* Neural Thresholds */}
                    <div className="bg-[#0d0d0d] border border-white/5 rounded-lg overflow-hidden">
                        <div className="px-3 py-1.5 bg-white/[0.02] border-b border-white/5 flex items-center gap-2">
                            <SafetyOutlined className="text-emerald-500 text-[10px]" />
                            <span className="text-[8px] font-black uppercase tracking-widest text-emerald-500/80">Neural Thresholds</span>
                        </div>
                        <div className="p-3 space-y-4">
                            <Form.Item label="CONFIDENCE_CUTOFF" name="confidence_cutoff">
                                <InputNumber step={0.01} min={0} max={1} className="w-full bg-white/[0.02] border-white/10 text-white font-mono" />
                            </Form.Item>
                            <Form.Item label="ANOMALY_SENSITIVITY" name="anomaly_sensitivity">
                                <InputNumber step={0.01} min={0} max={1} className="w-full bg-white/[0.02] border-white/10 text-white font-mono" />
                            </Form.Item>
                        </div>
                    </div>

                    {/* Operational Timeouts */}
                    <div className="bg-[#0d0d0d] border border-white/5 rounded-lg overflow-hidden">
                        <div className="px-3 py-1.5 bg-white/[0.02] border-b border-white/5 flex items-center gap-2">
                            <ClockCircleOutlined className="text-blue-500 text-[10px]" />
                            <span className="text-[8px] font-black uppercase tracking-widest text-blue-500/80">Operational Timeouts (ms)</span>
                        </div>
                        <div className="p-3 space-y-4">
                            <Form.Item label="NEURAL_INFERENCE" name="neural_inference_ms">
                                <InputNumber min={0} className="w-full bg-white/[0.02] border-white/10 text-white font-mono" />
                            </Form.Item>
                            <Form.Item label="SOCKET_RECONNECT" name="socket_reconnect_ms">
                                <InputNumber min={0} className="w-full bg-white/[0.02] border-white/10 text-white font-mono" />
                            </Form.Item>
                        </div>
                    </div>

                    {/* System State */}
                    <div className="bg-[#0d0d0d] border border-white/5 rounded-lg overflow-hidden">
                        <div className="px-3 py-1.5 bg-white/[0.02] border-b border-white/5 flex items-center gap-2">
                            <DatabaseOutlined className="text-purple-500 text-[10px]" />
                            <span className="text-[8px] font-black uppercase tracking-widest text-purple-500/80">Global State Control</span>
                        </div>
                        <div className="p-3 space-y-4">
                            <div className="flex items-center justify-between p-2 bg-white/[0.01] rounded border border-white/5">
                                <div className="flex flex-col">
                                    <span className="text-[10px] font-black text-white uppercase tracking-tighter">Maintenance Mode</span>
                                    <span className="text-[8px] text-white/30 uppercase tracking-widest">Restrict Public API Access</span>
                                </div>
                                <Form.Item name="maintenance_mode" valuePropName="checked" className="m-0">
                                    <Switch size="small" />
                                </Form.Item>
                            </div>
                            <div className="flex items-center justify-between p-2 bg-white/[0.01] rounded border border-white/5">
                                <div className="flex flex-col">
                                    <span className="text-[10px] font-black text-white uppercase tracking-tighter">Neural Debugging</span>
                                    <span className="text-[8px] text-white/30 uppercase tracking-widest">Verbose Inference Logs</span>
                                </div>
                                <Form.Item name="debug_mode" valuePropName="checked" className="m-0">
                                    <Switch size="small" />
                                </Form.Item>
                            </div>
                        </div>
                    </div>

                    {/* Registry Limits */}
                    <div className="bg-[#0d0d0d] border border-white/5 rounded-lg overflow-hidden">
                        <div className="px-3 py-1.5 bg-white/[0.02] border-b border-white/5 flex items-center gap-2">
                            <DatabaseOutlined className="text-amber-500 text-[10px]" />
                            <span className="text-[8px] font-black uppercase tracking-widest text-amber-500/80">Registry Limits</span>
                        </div>
                        <div className="p-3 space-y-4">
                            <Form.Item label="MAX_LOG_RETENTION" name="max_recent_logs">
                                <InputNumber min={0} max={10000} className="w-full bg-white/[0.02] border-white/10 text-white font-mono" />
                            </Form.Item>
                            <div className="p-2 border border-blue-500/10 bg-blue-500/5 rounded">
                                <p className="text-[7px] text-blue-400 uppercase tracking-widest m-0 leading-relaxed">
                                    CAUTION: High retention values may impact Firestore throughput. Optimal value is 5,000 entries.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </Form>

            <style>{`
                .admin-form-dense .ant-form-item-label > label {
                    color: rgba(255,255,255,0.4) !important;
                    font-size: 8px !important;
                    font-weight: 900 !important;
                    text-transform: uppercase !important;
                    letter-spacing: 0.1em !important;
                }
                .admin-form-dense .ant-form-item {
                    margin-bottom: 0 !important;
                }
                .admin-form-dense .ant-input-number {
                    border-radius: 4px !important;
                }
                .admin-form-dense .ant-input-number-input {
                    font-size: 11px !important;
                }
            `}</style>
        </div>
    );
};

export default AdminConfigMatrix;
