import React from 'react';
import { Card, Space, Typography, Select, Button } from 'antd';
import { RocketOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { Project } from './types';

const { Text } = Typography;
const { Option } = Select;

interface SimulationControlCardProps {
  projects: Project[];
  selectedAppId?: string;
  onSelectAppId: (appId: string) => void;
  onOpenSimulator: () => void;
}

const SimulationControlCard: React.FC<SimulationControlCardProps> = ({
  projects,
  selectedAppId,
  onSelectAppId,
  onOpenSimulator
}) => {
  return (
    <Card
      className="glass-card"
      style={{ 
        borderRadius: 16, 
        background: 'rgba(255,255,255,0.02)', 
        border: '1px solid rgba(255,255,255,0.1)' 
      }}
      title={<span style={{ color: '#fff' }}><RocketOutlined /> নতুন সিমুলেশন শুরু করুন</span>}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Text type="secondary" style={{ color: 'rgba(255,255,255,0.45)' }}>
          একটি প্রজেক্ট সিলেক্ট করুন যা আপনি সিমুলেট করতে চান:
        </Text>
        <Space wrap>
          <Select 
            placeholder="প্রজেক্ট নির্বাচন করুন" 
            style={{ width: 300 }}
            onChange={onSelectAppId}
            value={selectedAppId}
            dropdownStyle={{ background: '#1a1a1a', border: '1px solid rgba(255,255,255,0.1)' }}
          >
            {projects.map(p => (
              <Option key={p.id} value={p.id}>
                <span style={{ color: '#fff' }}>{p.name} ({p.type})</span>
              </Option>
            ))}
          </Select>
          <Button 
            type="primary" 
            icon={<PlayCircleOutlined />} 
            disabled={!selectedAppId}
            onClick={onOpenSimulator}
            style={{ background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', border: 'none' }}
          >
            সিমুলেটর ওপেন করুন
          </Button>
        </Space>
      </Space>
    </Card>
  );
};

export default SimulationControlCard;
