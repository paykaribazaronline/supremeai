import React from 'react';
import { Button, Space, Select, Typography, Input, Tooltip } from 'antd';
import { PlusOutlined, ReloadOutlined, SortAscendingOutlined, SortDescendingOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons';
import { ProjectSortField } from './types';

const { Option } = Select;
const { Text } = Typography;

interface ProjectActionToolbarProps {
  searchTerm: string;
  setSearchTerm: (value: string) => void;
  onNewProject: () => void;
  onRefresh: () => void;
  loading: boolean;
  sortBy: ProjectSortField | null;
  setSortBy: (field: ProjectSortField | null) => void;
  sortOrder: 'ascend' | 'descend';
  setSortOrder: (order: 'ascend' | 'descend') => void;
}

const ProjectActionToolbar: React.FC<ProjectActionToolbarProps> = ({
  searchTerm,
  setSearchTerm,
  onNewProject,
  onRefresh,
  loading,
  sortBy,
  setSortBy,
  sortOrder,
  setSortOrder
}) => {
  return (
    <div className="glass-card" style={{ 
      marginBottom: 24, 
      padding: '20px 24px', 
      display: 'flex', 
      justifyContent: 'space-between', 
      alignItems: 'center',
      flexWrap: 'wrap', 
      gap: '20px', 
      borderRadius: '16px',
      background: 'rgba(255, 255, 255, 0.02)',
      border: '1px solid rgba(255, 255, 255, 0.08)',
      backdropFilter: 'blur(12px)'
    }}>
      <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flex: '1 1 300px' }}>
        <Input
          placeholder="প্রজেক্টের নাম বা আইডি দিয়ে খুঁজুন..."
          prefix={<SearchOutlined style={{ color: 'rgba(255,255,255,0.25)' }} />}
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
          style={{ 
            maxWidth: 320, 
            borderRadius: '10px',
            background: 'rgba(255, 255, 255, 0.04)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            color: '#fff',
            height: '42px'
          }}
          className="dark-input"
          suffix={searchTerm && (
            <ClearOutlined 
              onClick={() => setSearchTerm('')} 
              style={{ cursor: 'pointer', color: 'rgba(255,255,255,0.45)' }} 
            />
          )}
        />
      </div>

      <div style={{ display: 'flex', gap: '20px', alignItems: 'center', flexWrap: 'wrap' }}>
        <Space size="large">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1.2px', fontWeight: 600 }}>সর্ট করুন</Text>
            <Select 
              value={sortBy} 
              onChange={setSortBy} 
              style={{ width: 180 }}
              placeholder="বাছাই করুন"
              allowClear
              className="premium-select"
              dropdownClassName="premium-dropdown"
            >
              <Option value="name">প্রজেক্টের নাম</Option>
              <Option value="status">স্ট্যাটাস</Option>
              <Option value="createdAt">তৈরির তারিখ</Option>
            </Select>
          </div>
          
          <Tooltip title={sortOrder === 'ascend' ? 'ক্রমানুসারে' : 'বিপরীত ক্রমানুসারে'}>
            <Button 
              icon={sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />} 
              onClick={() => setSortOrder(sortOrder === 'ascend' ? 'descend' : 'ascend')}
              style={{
                height: '42px',
                width: '42px',
                borderRadius: '10px',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            />
          </Tooltip>
        </Space>

        <div style={{ height: '32px', width: '1px', background: 'rgba(255,255,255,0.08)' }} />

        <Space size="middle">
          <Button 
            type="primary" 
            icon={<PlusOutlined />} 
            onClick={onNewProject}
            style={{ 
              background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', 
              border: 'none',
              height: '42px',
              borderRadius: '10px',
              fontWeight: 600,
              padding: '0 24px',
              boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            নতুন প্রজেক্ট
          </Button>
          
          <Tooltip title="রিফ্রেশ করুন">
            <Button 
              icon={<ReloadOutlined />} 
              onClick={onRefresh}
              loading={loading}
              style={{
                height: '42px',
                width: '42px',
                borderRadius: '10px',
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            />
          </Tooltip>
        </Space>
      </div>
    </div>
  );
};

export default ProjectActionToolbar;

