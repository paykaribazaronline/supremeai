import React, { useState, useEffect } from 'react';
import { Tabs, Typography, message, Modal, Alert, Button } from 'antd';
import { 
    KeyOutlined, 
    SearchOutlined, 
    BarChartOutlined, 
    SafetyOutlined,
    ThunderboltOutlined,
    CodeOutlined
} from '@ant-design/icons';

// Modular Components
import APIKeysTable from './api-keys/APIKeysTable';
import AddKeyModal from './api-keys/AddKeyModal';
import ModelDiscovery from './api-keys/ModelDiscovery';
import UsageStats from './api-keys/UsageStats';
import APIHealth from './api-keys/APIHealth';
import ApiTestConsole from './ApiTestConsole';

// Types & Hooks
import { SavedAPIKey } from './api-keys/types';

const { TabPane } = Tabs;
const { Title } = Typography;

const APIKeysManager: React.FC = () => {
    const [keys, setKeys] = useState<SavedAPIKey[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [editingKey, setEditingKey] = useState<SavedAPIKey | null>(null);
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [testConsoleVisible, setTestConsoleVisible] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => { fetchKeys(); }, []);

    const fetchKeys = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                setKeys(await response.json());
            } else if (response.status === 401) {
                setError('সেশন শেষ হয়ে গেছে। দয়া করে আবার লগইন করুন।');
            }
        } catch (err) {
            setError('সার্ভারের সাথে সংযোগ করা সম্ভব হচ্ছে না। আপনার ইন্টারনেট কানেকশন চেক করুন।');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const endpoint = editingKey ? `/api/apikeys/${editingKey.id}` : '/api/apikeys';
            const method = editingKey ? 'PUT' : 'POST';
            const response = await fetch(endpoint, {
                method,
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            if (response.ok) {
                message.success(editingKey ? 'এপিআই কী আপডেট করা হয়েছে!' : 'এপিআই কী যোগ করা হয়েছে!');
                setIsModalVisible(false);
                setEditingKey(null);
                fetchKeys();
            } else {
                message.error('সংরক্ষণ করতে ব্যর্থ হয়েছে।');
            }
        } catch (err) {
            message.error('কানেকশন সমস্যা।');
        }
    };

    const handleDelete = async (id: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/apikeys/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('এপিআই কী মুছে ফেলা হয়েছে');
                fetchKeys();
            } else {
                message.error('মুছে ফেলতে ব্যর্থ হয়েছে');
            }
        } catch (err) {
            message.error('কানেকশন সমস্যা');
        }
    };

    const handleTest = async (id: string) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/apikeys/${id}/test`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('এপিআই কী সচল আছে!');
                fetchKeys();
            } else {
                message.error('এপিআই কী কাজ করছে না।');
            }
        } catch (err) {
            message.error('টেস্ট করতে ব্যর্থ হয়েছে।');
        }
    };

    const handleTestAll = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys/test-all', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                message.success('সিস্টেম-ওয়াইড টেস্ট শুরু হয়েছে!');
                fetchKeys();
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="api-keys-manager">
            {error && (
                <Alert
                    message="Error"
                    description={error}
                    type="error"
                    showIcon
                    closable
                    style={{ marginBottom: 16 }}
                />
            )}

            <Tabs defaultActiveKey="keys" size="large" className="premium-tabs">
                <TabPane 
                    tab={<span><KeyOutlined /> এপিআই কী-সমূহ</span>} 
                    key="keys"
                >
                    <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Title level={5} style={{ margin: 0 }}>ম্যানেজ করুন</Title>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <Button icon={<CodeOutlined />} onClick={() => setTestConsoleVisible(true)}>টেস্ট কনসোল</Button>
                            <Button icon={<ThunderboltOutlined />} onClick={handleTestAll} loading={loading}>সব টেস্ট করুন</Button>
                            <Button type="primary" onClick={() => { setEditingKey(null); setIsModalVisible(true); }}>এপিআই কী যোগ করুন</Button>
                        </div>
                    </div>
                    
                    <APIKeysTable 
                        keys={keys}
                        loading={loading}
                        selectedRowKeys={selectedRowKeys}
                        onSelectionChange={setSelectedRowKeys}
                        onEdit={(key) => { setEditingKey(key); setIsModalVisible(true); }}
                        onDelete={handleDelete}
                        onTest={handleTest}
                    />
                </TabPane>

                <TabPane tab={<span><SearchOutlined /> মডেল ডিসকভারি</span>} key="discover">
                    <ModelDiscovery />
                </TabPane>

                <TabPane tab={<span><BarChartOutlined /> ব্যবহার (Usage)</span>} key="usage">
                    <UsageStats />
                </TabPane>

                <TabPane tab={<span><SafetyOutlined /> হেলথ রিপোর্ট</span>} key="health">
                    <APIHealth />
                </TabPane>
            </Tabs>

            <AddKeyModal 
                visible={isModalVisible}
                editingKey={editingKey}
                onCancel={() => { setIsModalVisible(false); setEditingKey(null); }}
                onSave={handleSave}
                loading={loading}
            />

            <ApiTestConsole
                visible={testConsoleVisible}
                onClose={() => setTestConsoleVisible(false)}
                apiKeys={keys.map(k => ({ id: k.id, label: k.label, provider: k.provider, baseUrl: k.baseUrl }))}
            />
        </div>
    );
};

export default APIKeysManager;
