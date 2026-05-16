import React, { useState, useEffect } from 'react';
import { 
  Typography, Card, Table, Tag, Button, Space, message, Spin, 
  InputNumber, Form, Modal, Divider, Row, Col, Progress, Statistic, Breadcrumb, Input
} from 'antd';
import { 
  DashboardOutlined, 
  SecurityScanOutlined, 
  ReloadOutlined, 
  EditOutlined, 
  UndoOutlined,
  ThunderboltOutlined,
  UserOutlined,
  BarChartOutlined
} from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface TierQuota {
  tier: string;
  quota: number;
  maxApis: number;
  maxSimulator: number;
}

interface UserUsage {
  apiKey: string;
  userId: string;
  requestCount: number;
  status: string;
  lastUsed?: string;
}

const AdminQuotas: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [quotas, setQuotas] = useState<TierQuota[]>([]);
  const [usage, setUsage] = useState<UserUsage[]>([]);
  const [isEditModalVisible, setIsEditModalVisible] = useState(false);
  const [editingTier, setEditingTier] = useState<TierQuota | null>(null);
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [configResp, usageResp] = await Promise.all([
        authUtils.fetchWithAuth('/api/admin/quotas/config'),
        authUtils.fetchWithAuth('/api/admin/quotas/usage')
      ]);

      const configResult = await configResp.json();
      const usageResult = await usageResp.json();

      if (configResult.success) {
        const config = configResult.data;
        const tierList: TierQuota[] = Object.keys(config.tierQuotas).map(tier => ({
          tier,
          quota: config.tierQuotas[tier],
          maxApis: config.tierMaxApis[tier] || 0,
          maxSimulator: config.tierMaxSimulatorInstalls[tier] || 0
        }));
        setQuotas(tierList);
      }

      if (usageResult.success) {
        setUsage(usageResult.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch quota data:', error);
      message.error('কোটা ডাটা লোড করতে সমস্যা হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleEdit = (record: TierQuota) => {
    setEditingTier(record);
    form.setFieldsValue(record);
    setIsEditModalVisible(true);
  };

  const handleUpdate = async (values: any) => {
    setLoading(true);
    try {
      // Fetch current config first to update only specific parts
      const currentResp = await authUtils.fetchWithAuth('/api/admin/quotas/config');
      const currentResult = await currentResp.json();
      const currentConfig = currentResult.data;

      const newTierQuotas = { ...currentConfig.tierQuotas, [values.tier]: values.quota };
      const newTierMaxApis = { ...currentConfig.tierMaxApis, [values.tier]: values.maxApis };
      const newTierMaxSimulator = { ...currentConfig.tierMaxSimulatorInstalls, [values.tier]: values.maxSimulator };

      const updateResp = await authUtils.fetchWithAuth('/api/admin/quotas/config', {
        method: 'POST',
        body: JSON.stringify({
          tierQuotas: newTierQuotas,
          tierMaxApis: newTierMaxApis,
          tierMaxSimulatorInstalls: newTierMaxSimulator
        })
      });

      const updateResult = await updateResp.json();
      if (updateResult.success) {
        message.success(`${values.tier} টিয়ার আপডেট করা হয়েছে`);
        setIsEditModalVisible(false);
        fetchData();
      }
    } catch (error) {
      message.error('আপডেট করতে ব্যর্থ হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  const handleResetUsage = async (apiKey: string) => {
    try {
      const resp = await authUtils.fetchWithAuth(`/api/admin/quotas/reset/${apiKey}`, {
        method: 'POST'
      });
      const result = await resp.json();
      if (result.success) {
        message.success('ইউসেজ রিসেট করা হয়েছে');
        fetchData();
      }
    } catch (error) {
      message.error('রিসেট করতে ব্যর্থ হয়েছে');
    }
  };

  const quotaColumns = [
    {
      title: 'ইউজার টিয়ার',
      dataIndex: 'tier',
      key: 'tier',
      render: (tier: string) => (
        <Tag color={tier === 'ADMIN' ? 'gold' : 'blue'} style={{ fontWeight: 700, borderRadius: 6 }}>
          {tier}
        </Tag>
      )
    },
    {
      title: 'মাসিক কোটা (AI Calls)',
      dataIndex: 'quota',
      key: 'quota',
      render: (quota: number) => quota === -1 ? 'Unlimited' : quota.toLocaleString()
    },
    {
      title: 'সর্বোচ্চ API Key',
      dataIndex: 'maxApis',
      key: 'maxApis',
    },
    {
      title: 'সিমুলেটর লিমিট',
      dataIndex: 'maxSimulator',
      key: 'maxSimulator',
    },
    {
      title: 'অ্যাকশন',
      key: 'action',
      render: (_: any, record: TierQuota) => (
        <Button 
          type="text" 
          icon={<EditOutlined />} 
          onClick={() => handleEdit(record)}
          style={{ color: '#3b82f6' }}
        >
          সম্পাদনা
        </Button>
      )
    }
  ];

  const usageColumns = [
    {
      title: 'ইউজার/API Key',
      dataIndex: 'apiKey',
      key: 'apiKey',
      render: (key: string, record: UserUsage) => (
        <Space direction="vertical" size={0}>
          <Text strong style={{ color: '#fff' }}>{record.userId}</Text>
          <Text type="secondary" style={{ fontSize: 10, fontFamily: 'monospace' }}>{key}</Text>
        </Space>
      )
    },
    {
      title: 'ব্যবহৃত (Usage)',
      dataIndex: 'requestCount',
      key: 'requestCount',
      render: (count: number) => (
        <Space>
          <Progress 
            percent={Math.min(100, (count / 100) * 100)} 
            size="small" 
            showInfo={false} 
            strokeColor={count > 80 ? '#ef4444' : '#10b981'} 
            style={{ width: 50 }}
          />
          <Text style={{ color: '#fff' }}>{count.toLocaleString()}</Text>
        </Space>
      )
    },
    {
      title: 'অবস্থা',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'active' ? 'success' : 'error'}>{status.toUpperCase()}</Tag>
      )
    },
    {
      title: 'শেষ ব্যবহার',
      dataIndex: 'lastUsed',
      key: 'lastUsed',
      render: (date?: string) => date ? new Date(date).toLocaleString('bn-BD') : 'কখনো না'
    },
    {
      title: 'অ্যাকশন',
      key: 'action',
      render: (_: any, record: UserUsage) => (
        <Button 
          type="text" 
          danger 
          icon={<UndoOutlined />} 
          onClick={() => handleResetUsage(record.apiKey)}
        >
          রিসেট
        </Button>
      )
    }
  ];

  return (
    <AdminLayout title="Quota Management">
      <div className="admin-header">
        <Breadcrumb separator=">" style={{ marginBottom: 'var(--space-2)', opacity: 0.7 }}>
          <Breadcrumb.Item href=""><DashboardOutlined /> ড্যাশবোর্ড</Breadcrumb.Item>
          <Breadcrumb.Item><SecurityScanOutlined /> সিস্টেম কন্ট্রোল</Breadcrumb.Item>
          <Breadcrumb.Item>কোটা ম্যানেজমেন্ট</Breadcrumb.Item>
        </Breadcrumb>
        
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
          <div>
            <Title level={2} className="admin-title">
              কোটা ও লিমিট ম্যানেজমেন্ট <span className="admin-badge">SYSTEM CONTROLS</span>
            </Title>
            <Text className="admin-subtitle">
              ব্যবহারকারীর টিয়ার ভিত্তিক সীমাবদ্ধতা এবং লাইভ ইউসেজ মনিটরিং
            </Text>
          </div>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />} 
            onClick={fetchData}
            loading={loading}
            className="admin-btn-primary"
            style={{ 
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              border: 'none'
            }}
          >
            রিফ্রেশ করুন
          </Button>
        </div>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={8}>
          <Card className="glass-card stat-card" style={{ background: 'linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(37,99,235,0.05) 100%)' }}>
            <Statistic 
              title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>মোট API ইউজার</span>} 
              value={usage.length} 
              prefix={<UserOutlined style={{ color: '#3b82f6' }} />} 
              valueStyle={{ color: '#fff', fontWeight: 800 }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass-card stat-card" style={{ background: 'linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(5,150,105,0.05) 100%)' }}>
            <Statistic 
              title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>সিস্টেম হেলথ</span>} 
              value={98.4} 
              precision={1}
              suffix="%" 
              prefix={<ThunderboltOutlined style={{ color: '#10b981' }} />} 
              valueStyle={{ color: '#fff', fontWeight: 800 }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass-card stat-card" style={{ background: 'linear-gradient(135deg, rgba(245,158,11,0.1) 0%, rgba(217,119,6,0.05) 100%)' }}>
            <Statistic 
              title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>গড় ইউসেজ</span>} 
              value={124} 
              prefix={<BarChartOutlined style={{ color: '#f59e0b' }} />} 
              valueStyle={{ color: '#fff', fontWeight: 800 }}
            />
          </Card>
        </Col>

        <Col xs={24}>
          <Card 
            title={<span style={{ color: '#fff' }}><EditOutlined /> টিয়ার কনফিগারেশন</span>} 
            className="glass-card"
            bodyStyle={{ padding: 0 }}
          >
            <Table 
              dataSource={quotas} 
              columns={quotaColumns} 
              pagination={false} 
              loading={loading}
              className="admin-table-dark"
              rowKey="tier"
            />
          </Card>
        </Col>

        <Col xs={24}>
          <Card 
            title={<span style={{ color: '#fff' }}><BarChartOutlined /> লাইভ ইউসেজ ট্র্যাকার</span>} 
            className="glass-card"
            bodyStyle={{ padding: 0 }}
          >
            <Table 
              dataSource={usage} 
              columns={usageColumns} 
              pagination={{ pageSize: 10 }} 
              loading={loading}
              className="admin-table-dark"
              rowKey="apiKey"
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title={`Edit Quota: ${editingTier?.tier}`}
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        onOk={() => form.submit()}
        confirmLoading={loading}
        className="premium-modal"
        okText="আপডেট করুন"
        cancelText="বাতিল"
      >
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item name="tier" hidden><Input /></Form.Item>
          
          <Form.Item 
            name="quota" 
            label="মাসিক কোটা (AI Calls)" 
            rules={[{ required: true }]}
            extra="-১ মানে অসীম বা আনলিমিটেড"
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="maxApis" label="সর্বোচ্চ API Keys" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="maxSimulator" label="সিমুলেটর লিমিট" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <style>{`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          border-radius: 20px;
          background: rgba(255, 255, 255, 0.02);
          border: 1px solid rgba(255, 255, 255, 0.08);
          box-shadow: 0 15px 35px rgba(0,0,0,0.2);
        }

        .stat-card {
          padding: 10px;
          transition: transform 0.3s ease;
        }

        .stat-card:hover {
          transform: translateY(-5px);
        }

        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.45) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: 11px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.03) !important;
        }

        .premium-modal .ant-modal-content {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1);
          border-radius: 24px;
        }

        .premium-modal .ant-modal-header {
          background: transparent !important;
          border-bottom: 1px solid rgba(255,255,255,0.05);
        }

        .premium-modal .ant-modal-title {
          color: #fff !important;
        }

        .ant-form-item-label > label {
          color: rgba(255,255,255,0.65) !important;
        }

        .ant-input-number {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          color: #fff !important;
          border-radius: 12px;
        }
      `}</style>
    </AdminLayout>
  );
};

export default AdminQuotas;
