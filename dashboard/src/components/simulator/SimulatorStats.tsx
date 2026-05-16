import React from 'react';
import { Row, Col, Card, Statistic } from 'antd';
import { RocketOutlined, MobileOutlined } from '@ant-design/icons';
import { SimulatorStatsData } from './types';

interface SimulatorStatsProps {
  stats: SimulatorStatsData;
}

const SimulatorStats: React.FC<SimulatorStatsProps> = ({ stats }) => {
  return (
    <Row gutter={[24, 24]} style={{ marginBottom: 24 }}>
      <Col span={12}>
        <Card bordered={false} className="glass-card" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>মোট ডিপ্লয়মেন্ট</span>}
            value={stats.totalDeployments}
            prefix={<RocketOutlined style={{ color: '#60a5fa' }} />}
            valueStyle={{ color: '#fff' }}
          />
        </Card>
      </Col>
      <Col span={12}>
        <Card bordered={false} className="glass-card" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>অ্যাক্টিভ সেশন</span>}
            value={stats.activeSessions}
            prefix={<MobileOutlined style={{ color: '#10b981' }} />}
            valueStyle={{ color: '#fff' }}
          />
        </Card>
      </Col>
    </Row>
  );
};

export default SimulatorStats;
