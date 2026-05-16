import { RocketOutlined, FieldTimeOutlined } from '@ant-design/icons';
import { Card, Space, Button, InputNumber, Divider, Typography } from 'antd';

const { Text } = Typography;

interface LearningModeControlProps {
  currentMode: string | undefined;
  onModeChange: (mode: string) => void;
  onManualTrigger: () => void;
  actionLoading: boolean;
  interval?: number;
  onIntervalChange?: (interval: number) => void;
}

const LearningModeControl: React.FC<LearningModeControlProps> = ({
  currentMode,
  onModeChange,
  onManualTrigger,
  actionLoading,
  interval = 60,
  onIntervalChange,
}) => {
  return (
    <Card title="Learning Mode & Schedule Control" className="glass-card">
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div>
          <Text strong style={{ display: 'block', marginBottom: 12 }}>Learning Mode Selection</Text>
          <Space size="middle" wrap>
            <Button 
              type={currentMode === 'AGGRESSIVE' ? 'primary' : 'default'} 
              onClick={() => onModeChange('AGGRESSIVE')}
              loading={actionLoading}
            >
              Aggressive
            </Button>
            <Button 
              type={currentMode === 'BALANCED' ? 'primary' : 'default'} 
              onClick={() => onModeChange('BALANCED')}
              loading={actionLoading}
            >
              Balanced
            </Button>
            <Button 
              type={currentMode === 'MANUAL' ? 'primary' : 'default'} 
              onClick={() => onModeChange('MANUAL')}
              loading={actionLoading}
            >
              Manual
            </Button>
            <Button 
              type={currentMode === 'PAUSED' ? 'primary' : 'default'} 
              onClick={() => onModeChange('PAUSED')}
              loading={actionLoading}
              danger
            >
              Paused
            </Button>
            
            <Divider type="vertical" style={{ height: 32 }} />
            
            <Button 
              icon={<RocketOutlined />} 
              onClick={onManualTrigger}
              disabled={currentMode !== 'MANUAL' && currentMode !== 'BALANCED'}
              loading={actionLoading}
            >
              Trigger Neural Training
            </Button>
          </Space>
        </div>

        <Divider style={{ margin: '12px 0' }} />

        <div>
          <Text strong style={{ display: 'block', marginBottom: 12 }}>Auto-Learning Schedule</Text>
          <Space size="middle" align="center">
            <FieldTimeOutlined style={{ fontSize: 20, color: '#1890ff' }} />
            <Text>Run improvement cycle every</Text>
            <InputNumber 
              min={1} 
              max={10080} // 1 week
              value={interval} 
              onChange={(val) => val && onIntervalChange?.(val)}
              disabled={actionLoading}
              addonAfter="minutes"
              style={{ width: 160 }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              (সিস্টেম প্রতি {interval} মিনিট পর পর নতুন ডেটা বিশ্লেষণ করবে)
            </Text>
          </Space>
        </div>
      </Space>
    </Card>
  );
};

export default LearningModeControl;
