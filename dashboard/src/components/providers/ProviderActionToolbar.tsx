import React from 'react';
import { Space, Button, Popconfirm, Select, Input } from 'antd';
import { 
  PlusOutlined, 
  ReloadOutlined, 
  ThunderboltOutlined, 
  DeleteOutlined, 
  SearchOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  CloseOutlined
} from '@ant-design/icons';

interface Props {
  loading: boolean;
  testingAll: boolean;
  deadCount: number;
  onAdd: () => void;
  onRefresh: () => void;
  onTestAll: () => void;
  onRemoveDead: () => void;
  searchTerm: string;
  setSearchTerm: (val: string) => void;
  sortBy: string;
  setSortBy: (val: any) => void;
  sortOrder: 'ascend' | 'descend';
  setSortOrder: (val: 'ascend' | 'descend') => void;
}

const ProviderActionToolbar: React.FC<Props> = ({ 
  loading, 
  testingAll, 
  deadCount, 
  onAdd, 
  onRefresh, 
  onTestAll, 
  onRemoveDead,
  searchTerm,
  setSearchTerm,
  sortBy,
  setSortBy,
  sortOrder,
  setSortOrder
}) => {
  return (
    <div className="glass-card flex flex-col md:flex-row gap-4 justify-between items-center mb-6 p-4">
      <Space wrap className="w-full md:w-auto">
        <div className="relative group">
          <Input
            placeholder="সার্চ প্রোভাইডার..."
            prefix={<SearchOutlined className="text-white/30 group-focus-within:text-blue-400 transition-colors" />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full md:w-64 h-10 bg-black/40 border-white/10 hover:border-white/20 focus:border-blue-500/50 text-white rounded-lg transition-all"
            suffix={searchTerm && (
              <CloseOutlined 
                className="text-white/30 hover:text-white cursor-pointer" 
                onClick={() => setSearchTerm('')} 
              />
            )}
          />
        </div>

        <div className="flex items-center gap-2 bg-black/20 p-1 rounded-lg border border-white/5">
          <Select
            value={sortBy}
            onChange={setSortBy}
            className="w-32 dark-select-compact"
            variant="borderless"
            popupClassName="dark-dropdown"
            options={[
              { label: 'নাম', value: 'name' },
              { label: 'টাইপ', value: 'type' },
              { label: 'স্ট্যাটাস', value: 'status' },
              { label: 'এপিআই সংখ্যা', value: 'apiCount' },
            ]}
          />
          <Button
            type="text"
            icon={sortOrder === 'ascend' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
            onClick={() => setSortOrder(sortOrder === 'ascend' ? 'descend' : 'ascend')}
            className="text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 h-8 w-8 flex items-center justify-center rounded-md transition-all"
          />
        </div>
      </Space>

      <Space wrap>
        <Button 
          icon={<ThunderboltOutlined />} 
          onClick={onTestAll} 
          loading={testingAll}
          className="glass-action-button"
          style={{ color: '#34d399', borderColor: 'rgba(52, 211, 153, 0.3)', fontWeight: 700 }}
        >
          সব টেস্ট করুন
        </Button>

        {deadCount > 0 && (
          <Popconfirm title="সব অক্রিয় প্রোভাইডার রিমুভ করবেন?" onConfirm={onRemoveDead} okText="হ্যাঁ" cancelText="না">
            <Button 
              danger 
              icon={<DeleteOutlined />}
              className="cyber-danger-button"
            >
              {deadCount} অক্রিয় প্রোভাইডার মুছুন
            </Button>
          </Popconfirm>
        )}

        <Button 
          icon={<ReloadOutlined />} 
          onClick={onRefresh} 
          loading={loading}
          className="glass-action-button"
        />

        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={onAdd}
          className="cyber-button"
          style={{ minWidth: 140 }}
        >
          নতুন প্রোভাইডার
        </Button>
      </Space>
    </div>
  );
};

export default ProviderActionToolbar;

