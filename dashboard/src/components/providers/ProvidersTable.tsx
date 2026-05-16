import React from 'react';
import { Table, Space, Button, Tag, Popconfirm, Typography, Collapse, Card, Empty, Badge, Tooltip } from 'antd';
import { EditOutlined, DeleteOutlined, RobotOutlined, KeyOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { Provider } from './types';

const { Panel } = Collapse;
const { Text, Title } = Typography;

interface Props {
  providers: Record<string, Provider[]>;
  loading: boolean;
  onEdit: (provider: Provider) => void;
  onDelete: (id: string) => void;
}

const ProvidersTable: React.FC<Props> = ({ providers, loading, onEdit, onDelete }) => {
  const modelKeys = Object.keys(providers).sort();

  const columns = [
    {
      title: 'Provider / Hints',
      key: 'name',
      render: (_: any, record: Provider) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.name || 'Unknown Provider'}</Text>
          {record.hints && <Text type="secondary" style={{ fontSize: '12px' }}><InfoCircleOutlined /> {record.hints}</Text>}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const color = status === 'active' ? '#52c41a' : status === 'error' ? '#ff4d4f' : '#faad14';
        const label = status ? status.toUpperCase() : 'UNKNOWN';
        return (
          <Badge status={status === 'active' ? 'processing' : 'default'} color={color} text={<Tag color={color} style={{ borderRadius: '12px', border: 'none', color: 'white', fontWeight: 600 }}>{label}</Tag>} />
        );
      },
    },
    {
      title: 'API Key',
      dataIndex: 'apiKey',
      key: 'apiKey',
      render: (key: string) => (
        <Tooltip title="Click Edit to modify">
          <Text code style={{ background: '#f5f5f5', border: '1px solid #d9d9d9' }}>
            <KeyOutlined style={{ marginRight: 4 }} />
            {key ? `****${key.slice(-6)}` : 'EMPTY'}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: 'Work Roles',
      dataIndex: 'assignedRoles',
      key: 'assignedRoles',
      render: (roles: string[]) => (
        <div style={{ maxWidth: 200 }}>
          {roles?.map(r => <Tag key={r} color="blue" style={{ marginBottom: 4, borderRadius: '4px' }}>{r.replace('_', ' ')}</Tag>) || '-'}
        </div>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Provider) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>
            Edit
          </Button>
          <Popconfirm title="Delete this API key?" onConfirm={() => onDelete(record.id!)} okText="Delete" cancelText="No">
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (modelKeys.length === 0 && !loading) {
    return <Empty description="No providers found matching your search" />;
  }

  return (
    <div className="providers-grouped-list" style={{ marginTop: '20px' }}>
      <Collapse defaultActiveKey={modelKeys} ghost expandIconPosition="end">
        {modelKeys.map(model => (
          <Panel 
            header={
              <Space>
                <RobotOutlined style={{ color: '#1890ff' }} />
                <Title level={5} style={{ margin: 0 }}>{model}</Title>
                <Badge count={providers[model].length} style={{ backgroundColor: '#52c41a' }} />
              </Space>
            } 
            key={model}
            style={{ marginBottom: '16px', background: 'rgba(255,255,255,0.5)', borderRadius: '12px', border: '1px solid #eee' }}
          >
            <Table
              columns={columns}
              dataSource={providers[model]}
              rowKey="id"
              pagination={false}
              size="middle"
              className="inner-provider-table"
            />
          </Panel>
        ))}
      </Collapse>
    </div>
  );
};

export default ProvidersTable;
