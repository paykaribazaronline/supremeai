import React from 'react';
import { Card, Statistic, Progress, Alert, Typography } from 'antd';
import { HeartOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface HealthScoreCardProps {
  healthScore: number;
  healthStatus: string;
  healthReason?: string;
}

const HealthScoreCard: React.FC<HealthScoreCardProps> = ({ healthScore, healthStatus, healthReason }) => {
  return (
    <Card 
      bordered={false} 
      className="glass-card"
      style={{ height: '100%', background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.1)' }}
    >
      <Statistic
        title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>সিস্টেম হেলথ স্কোর</span>}
        value={healthScore}
        suffix="/ 100"
        prefix={<HeartOutlined style={{ color: healthStatus === 'healthy' ? '#10b981' : '#ef4444' }} />}
        valueStyle={{ color: '#fff', fontSize: '24px' }}
      />
      <div style={{ marginTop: 20, textAlign: 'center' }}>
        <Progress
          type="dashboard"
          percent={healthScore}
          strokeColor={{
            '0%': '#ef4444',
            '50%': '#f59e0b',
            '100%': '#10b981',
          }}
          trailColor="rgba(255,255,255,0.05)"
        />
      </div>
      <div style={{ marginTop: 16 }}>
        <Alert
          message={healthReason || "All systems operational"}
          type={healthStatus === 'healthy' ? "success" : "warning"}
          showIcon
          style={{ borderRadius: 8, background: 'rgba(255,255,255,0.05)', border: 'none' }}
        />
      </div>
    </Card>
  );
};

export default HealthScoreCard;
