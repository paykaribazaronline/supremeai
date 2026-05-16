import React from 'react';
import { Layout, Typography, Card, Space, Table, Tag, Button } from 'antd';
import { 
  AuditOutlined, 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined,
  FileTextOutlined,
  CodeOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

const AdminRules: React.FC = () => {
  const columns = [
    { title: 'Rule ID', dataIndex: 'id', key: 'id' },
    { title: 'Type', dataIndex: 'type', key: 'type' },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    { title: 'Status', dataIndex: 'active', key: 'active', render: (active: boolean) => (
      <Tag color={active ? 'green' : 'red'}>{active ? 'Active' : 'Inactive'}</Tag>
    )},
    { title: 'Actions', key: 'actions', render: () => (
      <Space>
        <Button icon={<EditOutlined />} size="small" />
        <Button icon={<DeleteOutlined />} size="small" danger />
      </Space>
    )},
  ];

  const data = [
    { key: '1', id: 'RULE-001', type: 'Command', description: 'Deploy application to production', active: true },
    { key: '2', id: 'RULE-002', type: 'Condition', description: 'Require admin approval for API changes', active: true },
    { key: '3', id: 'PLAN-001', type: 'Plan', description: 'Auto-scaling workflow', active: false },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2} style={{ marginBottom: 24, fontWeight: 700 }}>
        সিস্টেম রুলস & কমান্ড
      </Title>

      <Card
        style={{
          background: 'rgba(255,255,255,0.03)',
          border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: 12,
          marginBottom: 24
        }}
        bodyStyle={{ padding: 24 }}
      >
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />}>New Rule</Button>
          <Button icon={<FileTextOutlined />}>Rules</Button>
          <Button icon={<CodeOutlined />}>Plans</Button>
          <Button icon={<FileTextOutlined />}>Commands</Button>
        </Space>
        <Text type="secondary">
          Backend: /admin/rules, /admin/chat-items/rules, /admin/chat-items/plans, /admin/chat-items/commands
        </Text>
      </Card>

      <Card
        style={{
          background: 'rgba(255,255,255,0.03)',
          border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: 12
        }}
        bodyStyle={{ padding: 24 }}
      >
        <Table 
          columns={columns} 
          dataSource={data} 
          pagination={false}
          style={{ color: '#fff' }}
        />
      </Card>
    </div>
  );
};

export default AdminRules;
