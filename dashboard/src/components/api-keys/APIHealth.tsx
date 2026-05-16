import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Space, Button, Badge, Modal, message, Typography, Empty, Spin } from 'antd';
import { 
    SafetyOutlined, 
    DeleteOutlined, 
    ReloadOutlined, 
    HistoryOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons';
import { APIHealthReport } from './types';

const { Title, Text } = Typography;

const APIHealth: React.FC = () => {
    const [reports, setReports] = useState<APIHealthReport[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => { fetchReports(); }, []);

    const fetchReports = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/apikeys/reports', {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                setReports(await response.json());
            }
        } catch (err) {
            // message.error('হেলথ রিপোর্ট লোড করতে ব্যর্থ হয়েছে');
        } finally {
            setLoading(false);
        }
    };

    const handleRemoveDeadKeys = async (report: APIHealthReport) => {
        const deadIds = report.deadKeyDetails?.map(d => d.id) || [];
        if (deadIds.length === 0) {
            message.info('এই রিপোর্টে কোনো ডেড (Dead) কী নেই');
            return;
        }

        Modal.confirm({
            title: `${deadIds.length}টি ইনঅ্যাক্টিভ কী মুছে ফেলবেন?`,
            icon: <ExclamationCircleOutlined />,
            content: 'এটি স্থায়ীভাবে সকল ইনঅ্যাক্টিভ এপিআই কী মুছে ফেলবে যা এই রিপোর্টে চিহ্নিত করা হয়েছে।',
            okText: 'হ্যাঁ, মুছুন',
            okType: 'danger',
            cancelText: 'না',
            onOk: async () => {
                setLoading(true);
                try {
                    const token = localStorage.getItem('authToken');
                    const response = await fetch('/api/apikeys/bulk', {
                        method: 'DELETE',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({ keyIds: deadIds }),
                    });
                    if (response.ok) {
                        message.success(`${deadIds.length}টি কী সফলভাবে মুছে ফেলা হয়েছে`);
                        fetchReports();
                    } else {
                        message.error('কী মুছতে সমস্যা হয়েছে');
                    }
                } catch (err) {
                    message.error('কানেকশন সমস্যা');
                } finally {
                    setLoading(false);
                }
            }
        });
    };

    const columns = [
        { 
            title: 'রিপোর্ট তৈরির সময়', 
            dataIndex: 'createdAt', 
            key: 'createdAt',
            render: (d: string) => (
                <Space>
                    <HistoryOutlined />
                    <Text>{new Date(d).toLocaleString()}</Text>
                </Space>
            )
        },
        { 
            title: 'মোট টেস্ট করা হয়েছে', 
            dataIndex: 'totalKeysTested', 
            key: 'totalKeysTested',
            align: 'center' as const
        },
        { 
            title: 'অবস্থা', 
            key: 'status',
            render: (_: any, r: APIHealthReport) => (
                <Space size="middle">
                    <Badge count={r.activeKeys} showZero overflowCount={999} style={{ backgroundColor: '#52c41a' }} title="Active Keys" />
                    <Badge count={r.deadKeys} showZero overflowCount={999} style={{ backgroundColor: '#f5222d' }} title="Dead Keys" />
                    <Badge count={r.rotationDueKeys} showZero overflowCount={999} style={{ backgroundColor: '#faad14' }} title="Rotation Due" />
                </Space>
            )
        },
        {
            title: 'অ্যাকশন',
            key: 'actions',
            align: 'right' as const,
            render: (_: any, record: APIHealthReport) => (
                <Space>
                    <Button 
                        size="small" 
                        danger 
                        icon={<DeleteOutlined />} 
                        onClick={() => handleRemoveDeadKeys(record)}
                        disabled={record.deadKeys === 0}
                    >
                        ডেড কী মুছুন
                    </Button>
                </Space>
            )
        }
    ];

    return (
        <Card className="premium-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <div>
                    <Title level={4} style={{ margin: 0 }}><SafetyOutlined /> এপিআই হেলথ রিপোর্ট</Title>
                    <Text type="secondary">আপনার এপিআই কী-গুলোর স্বাস্থ্য এবং কার্যকারিতার ইতিহাস।</Text>
                </div>
                <Button icon={<ReloadOutlined />} onClick={fetchReports} loading={loading}>রিফ্রেশ</Button>
            </div>

            <Table 
                columns={columns} 
                dataSource={reports} 
                rowKey="id" 
                loading={loading}
                locale={{ emptyText: <Empty description="এখনও কোনো হেলথ রিপোর্ট তৈরি করা হয়নি।" /> }}
                pagination={{ pageSize: 5 }}
            />
        </Card>
    );
};

export default APIHealth;
