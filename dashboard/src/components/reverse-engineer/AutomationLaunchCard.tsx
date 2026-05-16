import React from 'react';
import { Card, Typography, Space, Row, Col, Input, Select, Button } from 'antd';
import { RobotOutlined, GlobalOutlined, SendOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface AutomationLaunchCardProps {
  url: string;
  setUrl: (url: string) => void;
  taskType: string;
  setTaskType: (type: string) => void;
  instructions: string;
  setInstructions: (inst: string) => void;
  languages: string[];
  setLanguages: (langs: string[]) => void;
  submitting: boolean;
  onSubmit: () => void;
}

const AutomationLaunchCard: React.FC<AutomationLaunchCardProps> = ({
  url,
  setUrl,
  taskType,
  setTaskType,
  instructions,
  setInstructions,
  languages,
  setLanguages,
  submitting,
  onSubmit
}) => {
  return (
    <Card className="glass-card premium-shadow" style={{ marginBottom: 24 }}>
      <Title level={4} style={{ marginBottom: 24, display: 'flex', alignItems: 'center' }}>
        <RobotOutlined style={{ marginRight: 8, color: '#722ed1' }} />
        Launch New Automation Task
      </Title>
      
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Row gutter={16}>
          <Col span={18}>
            <Text strong>Target Website URL</Text>
            <Input 
              size="large" 
              placeholder="https://target-website.com" 
              value={url}
              onChange={e => setUrl(e.target.value)}
              prefix={<GlobalOutlined style={{ color: '#1890ff' }} />}
              style={{ marginTop: 8 }}
            />
          </Col>
          <Col span={6}>
            <Text strong>Task Objective</Text>
            <Select 
              size="large"
              value={taskType}
              onChange={setTaskType}
              style={{ width: '100%', marginTop: 8 }}
            >
              <Option value="reverse_engineer">Reverse Engineer API</Option>
              <Option value="extraction">Data Extraction (Scrape)</Option>
              <Option value="automation">Task Automation (Action)</Option>
              <Option value="security">Security Audit</Option>
            </Select>
          </Col>
        </Row>

        <div>
          <Text strong>Custom Instructions (AI Intent)</Text>
          <TextArea 
            rows={4} 
            placeholder="Tell SupremeAI exactly what to do. Example: 'Go to the pricing page, extract all plans into a table, and find the cheapest annual option.'"
            value={instructions}
            onChange={e => setInstructions(e.target.value)}
            style={{ marginTop: 8, borderRadius: 12 }}
          />
        </div>

        <Row align="middle" justify="space-between">
          <Col>
            <Text type="secondary">Generated SDK Languages: </Text>
            <Select
              mode="multiple"
              value={languages}
              onChange={setLanguages}
              style={{ minWidth: 200, marginLeft: 8 }}
              className="glass-select"
            >
              <Option value="python">Python</Option>
              <Option value="typescript">TypeScript</Option>
              <Option value="java">Java</Option>
              <Option value="go">Go</Option>
            </Select>
          </Col>
          <Col>
            <Button 
              type="primary" 
              size="large" 
              icon={<SendOutlined />} 
              loading={submitting}
              onClick={onSubmit}
              className="pulse-button"
              style={{ paddingLeft: 32, paddingRight: 32, height: 48, borderRadius: 24, fontWeight: 600 }}
            >
              Execute Task
            </Button>
          </Col>
        </Row>
      </Space>
    </Card>
  );
};

export default AutomationLaunchCard;
