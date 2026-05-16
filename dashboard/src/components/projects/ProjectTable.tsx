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
        if (status === 'ACTIVE') color = 'green';
        else if (status === 'PAUSED') color = 'orange';
        else if (status === 'COMPLETED') color = 'blue';
        else if (status === 'FAILED') color = 'red';
        return <Tag color={color}>{status}</Tag>;
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
          >
            Edit
          </Button>
          <Select
            size="small"
            value={record.status}
            style={{ width: 110 }}
            onChange={(val) => onUpdateStatus(record.id, val)}
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
      className="custom-table"
    />
  );
};

export default ProjectTable;
