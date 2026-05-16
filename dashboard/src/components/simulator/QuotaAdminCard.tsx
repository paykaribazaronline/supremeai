import React from 'react';
import { Card, Typography, Space, InputNumber, Button } from 'antd';
import { DatabaseOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface QuotaAdminCardProps {
  onSetQuota: (userId: string, quota: number) => void;
}

const QuotaAdminCard: React.FC<QuotaAdminCardProps> = ({ onSetQuota }) => {
  return (
    <Card
      className="glass-card"
      style={{ 
        borderRadius: 16, 
        marginTop: 24,
        background: 'rgba(255,255,255,0.02)', 
        border: '1px solid rgba(255,255,255,0.1)' 
      }}
      title={<span style={{ color: '#fff' }}><DatabaseOutlined /> কোটা অ্যাডমিনিস্ট্রেশন</span>}
    >
      <Text type="secondary" style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>
        সিমুলেটর কোটা ম্যানেজ করতে ইউজার আইডি ব্যবহার করুন:
      </Text>
      <div style={{ marginTop: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <InputNumber 
            placeholder="Quota (1-20)" 
            min={1} 
            max={20} 
            id="quotaInput"
            style={{ 
              width: '100%', 
              background: 'rgba(255,255,255,0.05)', 
              border: '1px solid rgba(255,255,255,0.1)',
              color: '#fff'
            }}
          />
          <Button 
            type="primary"
            block
            onClick={() => {
              const userId = window.prompt('User ID দিন:');
              const quota = (document.getElementById('quotaInput') as HTMLInputElement)?.value;
              if (userId && quota) onSetQuota(userId, parseInt(quota));
            }}
            style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', border: 'none' }}
          >
            কোটা সেট করুন
          </Button>
        </Space>
      </div>
    </Card>
  );
};

export default QuotaAdminCard;
