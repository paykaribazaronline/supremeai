import React from 'react';
import { Card, Row, Col, Statistic, Typography, Space, Input, Button, Alert, Tag } from 'antd';
import { BugOutlined, ThunderboltOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface SelfHealingPanelProps {
  healingStatus: any;
  testError: string;
  setTestError: (val: string) => void;
  handleTestFix: () => void | Promise<void>;
  fixing: boolean;
  fixResult: any;
}

const SelfHealingPanel: React.FC<SelfHealingPanelProps> = ({ 
  healingStatus, 
  testError, 
  setTestError, 
  handleTestFix, 
  fixing, 
  fixResult 
}) => {
  return (
    <Card 
      title={<span style={{ color: '#fff' }}><BugOutlined /> Self-Healing System Status</span>}
      bordered={false}
      className="glass-card"
      style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.1)' }}
    >
      <Row gutter={16}>
        <Col span={8}>
          <Statistic 
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>স্ট্যাটাস</span>}
            value={healingStatus?.status?.toUpperCase() || 'ACTIVE'} 
            valueStyle={{ color: '#10b981', fontSize: '18px' }} 
          />
        </Col>
        <Col span={8}>
          <Statistic 
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>অটো-হিলিং</span>}
            value={healingStatus?.autoHealing || 'Enabled'} 
            valueStyle={{ color: '#3b82f6', fontSize: '18px' }} 
          />
        </Col>
        <Col span={8}>
          <Statistic 
            title={<span style={{ color: 'rgba(255,255,255,0.45)' }}>ইনফিনিট লুপ</span>}
            value={healingStatus?.infiniteLoop || 'Active'} 
            valueStyle={{ color: '#f59e0b', fontSize: '18px' }} 
          />
        </Col>
      </Row>

      <div style={{ marginTop: 24 }}>
        <Title level={5} style={{ color: '#fff', marginBottom: 16 }}>
          ইন্টারেক্টিভ এরর সিমুলেটর
        </Title>
        <Space direction="vertical" style={{ width: '100%' }}>
          <TextArea
            placeholder="একটি এরর মেসেজ লিখুন (যেমন: Connection timeout to provider X)..."
            rows={3}
            value={testError}
            onChange={(e) => setTestError(e.target.value)}
            style={{ background: 'rgba(0,0,0,0.2)', color: '#fff', borderColor: 'rgba(255,255,255,0.1)' }}
          />
          <Button 
            type="primary" 
            icon={<ThunderboltOutlined />} 
            onClick={handleTestFix}
            loading={fixing}
            block
            style={{ background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)', border: 'none', height: '40px' }}
          >
            ডিটেক্ট এবং অটো-ফিক্স পরীক্ষা করুন
          </Button>
        </Space>

        {fixResult && (
          <div style={{ marginTop: 16 }}>
            <Alert
              message="অটো-ফিক্স বিশ্লেষণ ফলাফল"
              description={
                <div style={{ marginTop: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.8)' }}>{fixResult.summary || fixResult.status || "প্রক্রিয়াটি সফলভাবে সম্পন্ন হয়েছে।"}</Text>
                  <br />
                  <Space style={{ marginTop: 8 }}>
                    <Tag color="green">Action: {fixResult.actionTaken || "Analyzed"}</Tag>
                    <Tag color="blue">Confidence: {fixResult.confidence || "High"}</Tag>
                  </Space>
                </div>
              }
              type="info"
              style={{ background: 'rgba(139, 92, 246, 0.1)', border: '1px solid rgba(139, 92, 246, 0.2)' }}
            />
          </div>
        )}
      </div>
    </Card>
  );
};

export default SelfHealingPanel;
