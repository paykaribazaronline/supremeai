// AdminLogs.tsx - System Activity Logs Page

import React, { useState, useEffect } from 'react';
import { 
  Layout, Card, Table, Tag, Button, Space, message, Spin, Alert, 
  Input, Select, Typography, Tooltip, Breadcrumb 
} from 'antd';
import { 
  FileTextOutlined, 
  ReloadOutlined, 
  SearchOutlined, 
  SortAscendingOutlined, 
  SortDescendingOutlined,
  DashboardOutlined,
  HistoryOutlined,
  FilterOutlined
} from '@ant-design/icons';
import AdminLayout from '../components/AdminLayout';
import { authUtils } from '../lib/authUtils';

const { Option } = Select;
const { Title, Text } = Typography;

interface ActivityLog {
  id?: string;
  user: string;
  action: string;
  category: string;
  severity: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  outcome: string;
  details?: string;
  timestamp?: string;
}

const AdminLogs: React.FC = () => {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchText, setSearchText] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('');
  
  // Sorting State
  const [sortBy, setSortBy] = useState<keyof ActivityLog | null>('timestamp');
  const [sortOrder, setSortOrder] = useState<'ascend' | 'descend'>('descend');

  const fetchLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await authUtils.fetchWithAuth('/api/admin/logs?severity=' + severityFilter);
      if (!response.ok) throw new Error('Failed to fetch logs');
      const result = await response.json();
      console.log('[AdminLogs] API Result:', result);
      // Handle ApiResponse wrapper: { success: true, data: { logs: [...] } }
      const logData = result.data?.logs || (Array.isArray(result.data) ? result.data : []);
      setLogs(logData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch logs');
      message.error('লগ ডাটা লোড করতে সমস্যা হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  const severityPriority = {
    'ERROR': 4,
    'WARN': 3,
    'INFO': 2,
    'DEBUG': 1
  };

  const processedLogs = React.useMemo(() => {
    let filtered = logs.filter(log => {
      const searchLower = searchText.toLowerCase();
      return (
        log.user?.toLowerCase().includes(searchLower) ||
        log.action?.toLowerCase().includes(searchLower) ||
        log.category?.toLowerCase().includes(searchLower) ||
        log.outcome?.toLowerCase().includes(searchLower)
      );
    });

    if (sortBy) {
      filtered.sort((a, b) => {
        let aVal = (a as any)[sortBy] ?? '';
        let bVal = (b as any)[sortBy] ?? '';

        if (sortBy === 'severity') {
          const aPriority = severityPriority[a.severity] || 0;
          const bPriority = severityPriority[b.severity] || 0;
          return sortOrder === 'ascend' ? aPriority - bPriority : bPriority - aPriority;
        }

        if (sortBy === 'timestamp') {
          return sortOrder === 'ascend' 
            ? new Date(aVal).getTime() - new Date(bVal).getTime()
            : new Date(bVal).getTime() - new Date(aVal).getTime();
        }

        if (typeof aVal === 'string' && typeof bVal === 'string') {
          return sortOrder === 'ascend' 
            ? aVal.localeCompare(bVal) 
            : bVal.localeCompare(aVal);
        }
        return 0;
      });
    }

    return filtered;
  }, [logs, searchText, sortBy, sortOrder]);

  useEffect(() => {
    fetchLogs();
  }, [severityFilter]);

  const columns = [
    {
      title: 'সময়কাল (Timestamp)',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 220,
      render: (text: string) => <Text style={{ color: 'rgba(255,255,255,0.65)', fontFamily: 'monospace' }}>{text}</Text>
    },
    {
      title: 'ইউজার',
      dataIndex: 'user',
      key: 'user',
      render: (text: string) => <Text strong style={{ color: '#fff' }}>{text}</Text>
    },
    {
      title: 'অ্যাকশন',
      dataIndex: 'action',
      key: 'action',
    },
    {
      title: 'ক্যাটাগরি',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => <Tag style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.45)' }}>{category}</Tag>
    },
    {
      title: 'সেভেরিটি',
      dataIndex: 'severity',
      key: 'severity',
      render: (severity: string) => {
        let color = '#3b82f6';
        let bg = 'rgba(59, 130, 246, 0.1)';
        if (severity === 'ERROR') { color = '#ef4444'; bg = 'rgba(239, 68, 68, 0.1)'; }
        if (severity === 'WARN') { color = '#f59e0b'; bg = 'rgba(245, 158, 11, 0.1)'; }
        if (severity === 'DEBUG') { color = '#10b981'; bg = 'rgba(16, 185, 129, 0.1)'; }
        return <Tag style={{ background: bg, border: `1px solid ${color}44`, color: color, fontWeight: 600 }}>{severity}</Tag>;
      },
    },
    {
      title: 'ফলাফল',
      dataIndex: 'outcome',
      key: 'outcome',
    },
  ];

  return (
    <AdminLayout title="System Activity Logs">
      {/* Header Section */}
      <div className="admin-header">
        <Breadcrumb separator=">" style={{ marginBottom: 'var(--space-2)', opacity: 0.7 }}>
          <Breadcrumb.Item href=""><DashboardOutlined /> ড্যাশবোর্ড</Breadcrumb.Item>
          <Breadcrumb.Item><HistoryOutlined /> সিস্টেম অডিট</Breadcrumb.Item>
          <Breadcrumb.Item>অ্যাক্টিভিটি লগস</Breadcrumb.Item>
        </Breadcrumb>
        
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
          <div>
            <Title level={2} className="admin-title">
              সিস্টেম অ্যাক্টিভিটি লগস <span className="admin-badge">LIVE AUDIT</span>
            </Title>
            <Text className="admin-subtitle">
              অডিট ট্রেইল এবং সিস্টেম ইভেন্ট রিয়েল-টাইম মনিটরিং
            </Text>
          </div>
          <Button 
            type="primary" 
            icon={<ReloadOutlined />} 
            onClick={fetchLogs}
            loading={loading}
            className="admin-btn-primary"
            style={{ 
              background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
              border: 'none',
              fontWeight: 600,
              boxShadow: '0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)'
            }}
          >
            রিফ্রেশ করুন
          </Button>
        </div>
      </div>

      {error && <Alert type="error" message={error} action={<Button onClick={fetchLogs}>Retry</Button>} className="admin-empty" style={{ marginBottom: 'var(--space-3)' }} />}

      <Card
        className="glass-card"
        style={{ 
          borderRadius: 'var(--radius-xl)', 
          background: 'rgba(255,255,255,0.02)', 
          border: '1px solid rgba(255,255,255,0.08)',
          marginBottom: 'var(--space-4)',
          boxShadow: '0 clamp(16px, 2.5vw, 32px) clamp(32px, 4vw, 64px) rgba(0, 0, 0, 0.3)',
          overflow: 'hidden'
        }}
        bodyStyle={{ padding: 0 }}
      >
        {/* Modern Toolbar */}
        <div className="admin-toolbar">
          <div className="toolbar-section">
            <div style={{ 
              background: 'rgba(59, 130, 246, 0.1)', 
              padding: 'var(--space-2)', 
              borderRadius: 'var(--radius-md)',
              border: '1px solid rgba(59, 130, 246, 0.2)',
              display: 'flex',
              alignItems: 'center'
            }}>
              <SearchOutlined style={{ color: '#3b82f6', fontSize: 'var(--text-base)' }} />
            </div>
            <Input
              placeholder="ইউজার বা অ্যাকশন দিয়ে খুঁজুন..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              variant="borderless"
              className="admin-search dark-input-minimal"
            />
          </div>
          
          <div className="toolbar-section">
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <Text style={{ 
                color: 'rgba(255,255,255,0.35)', 
                fontSize: 'var(--text-xs)', 
                textTransform: 'uppercase', 
                letterSpacing: '1px', 
                fontWeight: 700 
              }}>সেভেরিটি</Text>
              <Select
                placeholder="সবগুলো"
                value={severityFilter || undefined}
                onChange={(val) => setSeverityFilter(val || '')}
                style={{ width: 140 }}
                allowClear
                className="premium-select"
                dropdownClassName="premium-dropdown"
              >
                <Option value="INFO">ইনফো (Info)</Option>
                <Option value="WARN">ওয়ার্নিং (Warn)</Option>
                <Option value="ERROR">এরর (Error)</Option>
                <Option value="DEBUG">ডিবাগ (Debug)</Option>
              </Select>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <Text style={{ 
                color: 'rgba(255,255,255,0.35)', 
                fontSize: 'var(--text-xs)', 
                textTransform: 'uppercase', 
                letterSpacing: '1px', 
                fontWeight: 700 
              }}>সর্ট করুন</Text>
              <Select
                value={sortBy}
                onChange={(val) => setSortBy(val)}
                style={{ width: 160 }}
                className="premium-select"
                dropdownClassName="premium-dropdown"
              >
                <Option value="timestamp">সময় (Time)</Option>
                <Option value="severity">সেভেরিটি</Option>
                <Option value="user">ইউজার</Option>
                <Option value="action">অ্যাকশন</Option>
                <Option value="category">ক্যাটাগরি</Option>
              </Select>
            </div>

            <Tooltip title={sortOrder === 'ascend' ? 'ক্রমানুসারে' : 'বিপরীত ক্রমানুসারে'}>
              <Button 
                onClick={() => setSortOrder(sortOrder === 'ascend' ? 'descend' : 'ascend')}
                icon={sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
                className="admin-btn-icon hover-bright"
                style={{ 
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  color: '#fff'
                }}
              />
            </Tooltip>
          </div>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <Table
            columns={columns}
            dataSource={processedLogs}
            rowKey={(record) => record.id || record.timestamp || Math.random().toString()}
            loading={loading}
            pagination={{ 
              pageSize: 15,
              showSizeChanger: true,
              pageSizeOptions: ['15', '30', '50', '100']
            }}
            className="admin-table-dark"
          />
        </div>
      </Card>

      <style>{`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .dark-input-minimal::placeholder {
          color: rgba(255,255,255,0.2) !important;
        }

        .premium-select .ant-select-selector {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          height: 42px !important;
          display: flex !important;
          align-items: center !important;
          color: #fff !important;
          transition: all 0.3s ease !important;
        }

        .premium-select:hover .ant-select-selector {
          border-color: rgba(59, 130, 246, 0.5) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .premium-dropdown {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          padding: 8px !important;
          box-shadow: 0 10px 30px rgba(0,0,0,0.5) !important;
        }

        .premium-dropdown .ant-select-item {
          border-radius: 8px !important;
          padding: 8px 12px !important;
        }

        .premium-dropdown .ant-select-item-option-selected {
          background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
          color: white !important;
          font-weight: 600;
        }

        .premium-dropdown .ant-select-item-option-active:not(.ant-select-item-option-selected) {
          background: rgba(255,255,255,0.05) !important;
        }

        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.4) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: var(--text-xs) !important;
          text-transform: uppercase !important;
          letter-spacing: 0.5px !important;
          font-weight: 700 !important;
          padding: var(--space-3) !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          padding: var(--space-3) !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.02) !important;
        }

        .admin-table-dark .ant-pagination-item {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: var(--radius-md) !important;
        }

        .admin-table-dark .ant-pagination-item-active {
          background: var(--neon-blue) !important;
          border-color: var(--neon-blue) !important;
        }

        /* Admin button fixes */
        .admin-btn-primary {
          height: clamp(36px, 5vh, 48px) !important;
          padding: 0 clamp(16px, 2vw, 24px) !important;
          font-size: var(--text-sm) !important;
          border-radius: var(--radius-md) !important;
        }

        .admin-btn-icon {
          height: clamp(36px, 5vh, 48px) !important;
          width: clamp(36px, 5vh, 48px) !important;
          border-radius: var(--radius-md) !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          padding: 0 !important;
        }

        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .dark-input-minimal::placeholder {
          color: rgba(255,255,255,0.2) !important;
        }

        .premium-select .ant-select-selector {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          height: 42px !important;
          display: flex !important;
          align-items: center !important;
          color: #fff !important;
          transition: all 0.3s ease !important;
        }

        .premium-select:hover .ant-select-selector {
          border-color: rgba(59, 130, 246, 0.5) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .premium-dropdown {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          padding: 8px !important;
          box-shadow: 0 10px 30px rgba(0,0,0,0.5) !important;
        }

        .premium-dropdown .ant-select-item {
          color: rgba(255,255,255,0.65) !important;
          border-radius: 8px !important;
          margin-bottom: 4px !important;
        }

        .premium-dropdown .ant-select-item-option-selected {
          background: rgba(59, 130, 246, 0.15) !important;
          color: #3b82f6 !important;
        }

        .hover-bright:hover {
          background: rgba(255,255,255,0.1) !important;
          transform: translateY(-1px);
        }

        /* Table Customizations */
        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.4) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: 12px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
          padding: 16px 24px !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          padding: 16px 24px !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.02) !important;
        }

        .admin-table-dark .ant-pagination-item {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 8px !important;
        }

        .admin-table-dark .ant-pagination-item-active {
          background: #3b82f6 !important;
          border-color: #3b82f6 !important;
        }

        .admin-table-dark .ant-pagination-item a {
          color: rgba(255,255,255,0.65) !important;
        }
      `}</style>
    </AdminLayout>
  );
};

export default AdminLogs;
