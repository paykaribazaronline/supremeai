import React from 'react';
import { Table, Space, Button, Tag, Select } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Project } from './types';

const { Option } = Select;

interface ProjectTableProps {
  projects: Project[];
  loading: boolean;
  onEdit: (project: Project) => void;
  onDelete: (id: string) => void;
  onUpdateStatus: (id: string, status: string) => void;
}

const ProjectTable: React.FC<ProjectTableProps> = ({
  projects,
  loading,
  onEdit,
  onDelete,
  onUpdateStatus
}) => {
  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      sorter: (a: Project, b: Project) => a.name.localeCompare(b.name),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (desc: string) => desc || '-',
    },
    {
      title: 'Owner',
      dataIndex: 'ownerId',
      key: 'ownerId',
      render: (owner: string) => <span style={{ fontFamily: 'monospace' }}>{owner.substring(0, 8)}...</span>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'default';
        let borderColor = 'transparent';
        if (status === 'ACTIVE') { color = '#00f3ff'; borderColor = 'rgba(0, 243, 255, 0.5)'; }
        else if (status === 'PAUSED') { color = '#faad14'; borderColor = 'rgba(250, 173, 20, 0.5)'; }
        else if (status === 'COMPLETED') { color = '#8b5cf6'; borderColor = 'rgba(139, 92, 246, 0.5)'; }
        else if (status === 'FAILED') { color = '#ff4d4f'; borderColor = 'rgba(255, 77, 79, 0.5)'; }
        return <Tag style={{ background: 'rgba(0,0,0,0.4)', color, borderColor, textShadow: `0 0 5px ${color}`, padding: '0 8px', borderRadius: '4px' }}>{status}</Tag>;
      },
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Project) => (
        <Space>
          <Button 
            size="small" 
            icon={<EditOutlined />} 
            onClick={() => onEdit(record)}
            style={{ background: 'transparent', color: 'var(--neon-blue)', borderColor: 'var(--neon-blue)' }}
          >
            Edit
          </Button>
          <Select
            size="small"
            value={record.status}
            style={{ width: 110 }}
            onChange={(val) => onUpdateStatus(record.id, val)}
            className="premium-select"
            dropdownClassName="premium-dropdown"
          >
            <Option value="ACTIVE">Active</Option>
            <Option value="PAUSED">Paused</Option>
            <Option value="COMPLETED">Completed</Option>
            <Option value="FAILED">Failed</Option>
          </Select>
          <Button 
            size="small" 
            danger 
            icon={<DeleteOutlined />} 
            onClick={() => onDelete(record.id)}
            style={{ background: 'transparent', color: 'var(--neon-red)', borderColor: 'var(--neon-red)' }}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={projects}
      rowKey="id"
      loading={loading}
      pagination={{ pageSize: 10, showSizeChanger: true }}
      scroll={{ x: 1000 }}
      className="custom-table cyber-table"
      rowClassName={() => 'cyber-table-row'}
      style={{ background: 'transparent' }}
    />
  );
};

export default ProjectTable;
