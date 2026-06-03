import React from 'react';
import { Card, List, Tag } from 'antd';
import { SecurityScanOutlined, CheckCircleOutlined } from '@ant-design/icons';

const SurveillancePanel: React.FC = () => {
  const surveillanceData = [
    { title: 'Firewall Monitoring', status: 'Online', icon: <CheckCircleOutlined style={{ color: '#10b981' }} /> },
    { title: 'Intrusion Detection System', status: 'Active', icon: <CheckCircleOutlined style={{ color: '#10b981' }} /> },
    { title: 'API Security Layer', status: 'Secured', icon: <CheckCircleOutlined style={{ color: '#10b981' }} /> },
    { title: 'Database Encryption', status: 'AES-256 Enabled', icon: <CheckCircleOutlined style={{ color: '#10b981' }} /> }
  ];

  return (
    <Card 
      title={<span style={{ color: '#fff' }}><SecurityScanOutlined /> Cyber Guard Active Surveillance</span>}
      bordered={false}
      className="glass-card"
      style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.1)' }}
    >
      <List
        itemLayout="horizontal"
        dataSource={surveillanceData}
        renderItem={(item) => (
          <List.Item style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <List.Item.Meta
              avatar={item.icon}
              title={<span style={{ color: '#fff' }}>{item.title}</span>}
              description={<span style={{ color: 'rgba(255,255,255,0.45)' }}>{item.status}</span>}
            />
            <Tag color="success">OPERATIONAL</Tag>
          </List.Item>
        )}
      />
    </Card>
  );
};

export default SurveillancePanel;
