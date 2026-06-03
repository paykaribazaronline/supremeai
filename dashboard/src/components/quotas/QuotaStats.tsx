import React from 'react';
import { Row, Col, Card, Statistic } from 'antd';
import { UserOutlined, PieChartOutlined, ApartmentOutlined } from '@ant-design/icons';
import { QuotaStatsData } from './types';

interface QuotaStatsProps {
  stats: QuotaStatsData;
}

const QuotaStats: React.FC<QuotaStatsProps> = ({ stats }) => {
  return (
    <Row gutter={[24, 24]} style={{ marginBottom: 24 }}>
      <Col xs={24} sm={8}>
        <Card bordered={false} className="glass-card" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>মোট ইউজার</span>}
            value={stats.totalUsers}
            prefix={<UserOutlined style={{ color: '#3b82f6' }} />}
            valueStyle={{ color: '#fff' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card bordered={false} className="glass-card" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>অ্যাক্টিভ কোটা (ব্যবহারকারী)</span>}
            value={stats.activeQuotas}
            prefix={<PieChartOutlined style={{ color: '#10b981' }} />}
            valueStyle={{ color: '#fff' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card bordered={false} className="glass-card" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <Statistic
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>কোটা লিমিট অতিক্রম</span>}
            value={stats.overLimit}
            prefix={<ApartmentOutlined style={{ color: '#ef4444' }} />}
            valueStyle={{ color: '#fff' }}
          />
        </Card>
      </Col>
    </Row>
  );
};

export default QuotaStats;
