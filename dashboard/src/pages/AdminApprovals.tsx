import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Typography, Row, Col, Card, Table, Tag, Button, Space, message, Badge } from 'antd';
import { CheckOutlined, CloseOutlined, SyncOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface Approval {
  id: string;
  type: string;
  request: string;
  status: 'pending' | 'approved' | 'rejected';
  submittedBy: string;
  submittedAt: string;
}

export default function AdminApprovals() {
  const [loading, setLoading] = useState(true);
  const [approvals, setApprovals] = useState<Approval[]>([]);

  useEffect(() => {
    fetchApprovals();
  }, []);

  const fetchApprovals = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/approvals');
      if (res.ok) {
        const data = await res.json();
        setApprovals(Array.isArray(data) ? data : data.approvals || []);
      }
    } catch (error) {
      message.error('Failed to fetch approvals');
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async (id: string, action: 'approve' | 'reject') => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/approvals/${id}/${action}`, {
        method: 'POST'
      });
      if (res.ok) {
        message.success(`Approval ${action}ed`);
        setApprovals(approvals.filter(a => a.id !== id));
      }
    } catch (error) {
      message.error(`Failed to ${action} approval`);
    }
  };

  const columns = [
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (text: string) => <Tag color="blue">{text}</Tag>
    },
    {
      title: 'Request',
      dataIndex: 'request',
      key: 'request'
    },
    {
      title: 'Submitted By',
      dataIndex: 'submittedBy',
      key: 'submittedBy'
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Badge
          status={status === 'pending' ? 'warning' : status === 'approved' ? 'success' : 'error'}
          text={status.toUpperCase()}
        />
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (record: Approval) => record.status === 'pending' ? (
        <Space>
          <Button 
            icon={<CheckOutlined />} 
            size="small"
            onClick={() => handleAction(record.id, 'approve')}
          >Approve</Button>
          <Button 
            icon={<CloseOutlined />} 
            size="small"
            onClick={() => handleAction(record.id, 'reject')}
          >Reject</Button>
        </Space>
      ) : null
    }
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: '24px' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          Approvals Queue
        </Title>
        <Button icon={<SyncOutlined />} onClick={fetchApprovals} className="glass-action-button">
          Refresh
        </Button>
      </div>

      <Card className="glass-card">
        <Table
          columns={columns}
          dataSource={approvals}
          loading={loading}
          pagination={{ pageSize: 10 }}
          locale={{ emptyText: 'No pending approvals' }}
        />
      </Card>
    </motion.div>
  );
}