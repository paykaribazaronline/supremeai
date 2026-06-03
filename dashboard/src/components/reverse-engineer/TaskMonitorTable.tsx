import React from 'react';
import { Card, Table, Space, Typography, Tag, Progress, Badge, Tooltip, Button } from 'antd';
import { PlayCircleOutlined, BulbOutlined, EyeOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { Job } from './types';

const { Text } = Typography;

interface TaskMonitorTableProps {
  jobs: Job[];
  loading: boolean;
  onView: (job: Job) => void;
  onCancel: (jobId: string) => void;
  onRefresh: () => void;
}

const TaskMonitorTable: React.FC<TaskMonitorTableProps> = ({
  jobs,
  loading,
  onView,
  onCancel,
  onRefresh
}) => {
  const columns = [
    {
      title: 'Target Site',
      dataIndex: 'url',
      key: 'url',
      render: (text: string, record: Job) => (
        <Space direction="vertical" size={0}>
          <Text strong ellipsis={{ tooltip: text }} style={{ maxWidth: 250 }}>
            {text}
          </Text>
          <Space>
            <Tag color="cyan">{record.taskType || 'Reverse Engineer'}</Tag>
            {record.results?.alternative_suggestions && record.results.alternative_suggestions.length > 0 && (
              <Badge count={<BulbOutlined style={{ color: '#faad14' }} />} offset={[5, 0]}>
                <Tag color="gold">Suggestions Available</Tag>
              </Badge>
            )}
          </Space>
        </Space>
      )
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'blue';
        if (status === 'COMPLETED') color = 'green';
        if (status === 'FAILED') color = 'red';
        if (status === 'CANCELLED') color = 'orange';
        return <Badge status={color as any} text={status} />;
      }
    },
    {
      title: 'Progress',
      dataIndex: 'progress',
      key: 'progress',
      width: 200,
      render: (p: number, record: Job) => (
        <Space direction="vertical" style={{ width: '100%' }} size={0}>
          <Progress 
            percent={Math.round(p)} 
            size="small" 
            status={p === 100 ? 'success' : 'active'} 
            strokeColor={{ '0%': '#108ee9', '100%': '#87d068' }} 
          />
          <Text type="secondary" style={{ fontSize: 11 }}>{record.currentPhase || 'Idle'}</Text>
        </Space>
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      align: 'right' as any,
      render: (_: any, record: Job) => (
        <Space>
          {record.status === 'COMPLETED' && (
            <Tooltip title="View Insights & Results">
              <Button 
                type="primary" 
                shape="circle" 
                icon={<EyeOutlined />} 
                onClick={() => onView(record)} 
              />
            </Tooltip>
          )}
          {(['PENDING', 'ANALYZING', 'GENERATING', 'RUNNING'].includes(record.status)) && (
            <Tooltip title="Abort Task">
              <Button 
                danger 
                shape="circle" 
                icon={<DeleteOutlined />} 
                onClick={() => onCancel(record.jobId)} 
              />
            </Tooltip>
          )}
          <Button shape="circle" icon={<ReloadOutlined />} onClick={onRefresh} />
        </Space>
      )
    }
  ];

  return (
    <Card 
      className="glass-card" 
      title={<Space><PlayCircleOutlined style={{ color: '#52c41a' }} /> Live Task Monitor</Space>}
      extra={<Button type="link" onClick={onRefresh} loading={loading}>Sync Now</Button>}
    >
      <Table
        columns={columns}
        dataSource={jobs}
        rowKey="jobId"
        loading={loading}
        pagination={{ pageSize: 6 }}
        className="premium-table"
      />
    </Card>
  );
};

export default TaskMonitorTable;
