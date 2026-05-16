import React from 'react';
import { Row, Col, Card, Statistic, Progress, Button } from 'antd';
import { SyncOutlined, PauseCircleOutlined, CheckCircleOutlined } from '@ant-design/icons';

interface LearningStatus {
  mode: string;
  modeDescription: string;
  emergencyPaused: boolean;
  quota: {
    totalUsage: number;
    dailyLimit: number;
    percentageUsed: number;
  };
}

interface LearningStatusOverviewProps {
  status: LearningStatus | null;
  onEmergencyPause: () => void;
  actionLoading: boolean;
}

const LearningStatusOverview: React.FC<LearningStatusOverviewProps> = ({
  status,
  onEmergencyPause,
  actionLoading,
}) => {
  return (
    <Row gutter={16}>
      <Col span={8}>
        <Card bordered={false} className="glass-card">
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.4)' }}>Active Evolution Mode</span>}
            value={status?.mode || 'UNKNOWN'}
            prefix={<SyncOutlined spin={!status?.emergencyPaused} style={{ color: '#10b981' }} />}
            valueStyle={{ color: '#fff', fontSize: '18px', fontWeight: 900 }}
          />
          <div style={{ color: 'rgba(255,255,255,0.45)', fontSize: '12px' }}>{status?.modeDescription}</div>
        </Card>
      </Col>
      <Col span={8}>
        <Card bordered={false} className="glass-card">
          <Statistic 
            title={<span style={{ color: 'rgba(255,255,255,0.4)' }}>Daily Quota Usage</span>}
            value={status?.quota.totalUsage || 0} 
            suffix={`/ ${status?.quota.dailyLimit || 0}`}
            valueStyle={{ color: '#10b981' }}
          />
          <Progress 
            percent={status?.quota.percentageUsed} 
            size="small" 
            strokeColor={{ '0%': '#10b981', '100%': '#3b82f6' }}
          />
        </Card>
      </Col>
      <Col span={8}>
        <Card bordered={false} className="glass-card">
          <Statistic 
            title={<span style={{ color: 'rgba(255,255,255,0.4)' }}>Status</span>}
            value={status?.emergencyPaused ? 'Paused' : 'Active'} 
            valueStyle={{ color: status?.emergencyPaused ? '#ef4444' : '#10b981' }}
            prefix={status?.emergencyPaused ? <PauseCircleOutlined /> : <CheckCircleOutlined />}
          />
          <Button 
            danger={!status?.emergencyPaused}
            type={status?.emergencyPaused ? 'primary' : 'default'}
            size="small"
            style={{ marginTop: 8 }}
            onClick={onEmergencyPause}
            loading={actionLoading}
          >
            {status?.emergencyPaused ? 'Resume Learning' : 'Emergency Pause'}
          </Button>
        </Card>
      </Col>
    </Row>
  );
};

export default LearningStatusOverview;
