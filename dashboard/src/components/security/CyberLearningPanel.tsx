import React from 'react';
import { Card, Space, Typography, Input, Button, Divider, Tag, Switch } from 'antd';
import { LockOutlined, BulbOutlined, RobotOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

interface CyberSkill {
  id: string;
  name: string;
  status: string;
  description: string;
}

interface CyberLearningPanelProps {
  learnTopic: string;
  setLearnTopic: (topic: string) => void;
  onStartLearning: () => void;
  learning: boolean;
  cyberSkills: CyberSkill[];
  autonomousLearningEnabled?: boolean;
  onToggleAutonomous?: (enabled: boolean) => void;
}

const CyberLearningPanel: React.FC<CyberLearningPanelProps> = ({
  learnTopic,
  setLearnTopic,
  onStartLearning,
  learning,
  cyberSkills,
  autonomousLearningEnabled,
  onToggleAutonomous,
}) => {
  return (
    <Card 
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <span style={{ color: '#fff' }}><LockOutlined /> Autonomous Cyber Learning</span>
          <Space>
            <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: '12px' }}>
              <RobotOutlined /> Autonomous Mode
            </Text>
            <Switch 
              size="small" 
              checked={autonomousLearningEnabled} 
              onChange={onToggleAutonomous}
            />
          </Space>
        </div>
      }
      bordered={false}
      className="glass-card"
      style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.1)' }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Text style={{ color: 'rgba(255,255,255,0.6)' }}>নিজে নিজে হ্যাকিং স্কিল শিখে সিস্টেমকে নিরাপদ রাখার মডিউল।</Text>
        <Input 
          placeholder="Vulnerability Topic (e.g., SQL Injection, Zero-day)..." 
          value={learnTopic}
          onChange={e => setLearnTopic(e.target.value)}
          style={{ background: 'rgba(0,0,0,0.2)', color: '#fff' }}
        />
        <Button 
          type="primary" 
          icon={<BulbOutlined />} 
          onClick={onStartLearning}
          loading={learning}
          block
        >
          Initiate Learning Cycle
        </Button>
      </Space>

      <Divider style={{ borderColor: 'rgba(255,255,255,0.05)' }} />
      
      <Title level={5} style={{ color: '#fff' }}>Mastered Skills ({cyberSkills.length})</Title>
      <div style={{ maxHeight: 200, overflowY: 'auto' }}>
        {cyberSkills.map(skill => (
          <div key={skill.id} style={{ marginBottom: 12, padding: 8, background: 'rgba(255,255,255,0.03)', borderRadius: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Text strong style={{ color: '#00f3ff' }}>{skill.name}</Text>
              <Tag color="cyan">{skill.status}</Tag>
            </div>
            <Text style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>{skill.description}</Text>
          </div>
        ))}
      </div>
    </Card>
  );
};

export default CyberLearningPanel;
